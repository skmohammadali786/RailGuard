#!/usr/bin/env python3
"""
RailGuard real rail-surface crack detection (classical computer vision).

This module performs *actual* image analysis. It does not invent detections.
It requires OpenCV + NumPy on the Raspberry Pi:

    pip3 install -r requirements-ai.txt

If OpenCV is unavailable, `is_available()` returns False and the detector
reports "AI unavailable" instead of pretending a crack was found.

Algorithm (tuned for a close-up view of the rail running surface):
  1. Crop the central railhead band (the region the camera is pointed at).
  2. Convert to grayscale and apply CLAHE for uneven trackside lighting.
  3. Black-hat morphology isolates thin dark structures (cracks are dark and
     narrow against the brighter polished railhead).
  4. Adaptive threshold + Canny edges are fused to keep real fissures and
     reject smooth shadows.
  5. Contours are filtered by area, elongation, thinness, solidity and local
     intensity contrast against the surrounding rail surface.
  6. The best candidate gets a confidence score derived from those features.

Limitations (documented, not hidden):
  - It detects *surface-visible* transverse/longitudinal fissures only.
  - Pixel-to-millimetre conversion needs `mm_per_pixel` calibration. Without
    calibration the reported length is marked as estimated.
  - It does not replace ultrasonic internal-flaw inspection.
"""

from __future__ import annotations

import logging
import math
import os
from typing import Any, Dict, List, Optional, Tuple

log = logging.getLogger("railguard-vision")

try:  # OpenCV is optional so the rest of the Pi service still runs without it.
    import cv2  # type: ignore
    import numpy as np  # type: ignore

    CV_AVAILABLE = True
except Exception:  # pragma: no cover - depends on the Pi installation
    cv2 = None  # type: ignore
    np = None  # type: ignore
    CV_AVAILABLE = False


# Defaults can be tuned with environment variables without editing code.
def _env_float(name: str, default: float) -> float:
    try:
        return float(os.getenv(name, str(default)))
    except (TypeError, ValueError):
        return default


MM_PER_PIXEL = _env_float("RAILGUARD_MM_PER_PIXEL", 0.0)  # 0 => uncalibrated
MIN_CRACK_AREA_PX = int(_env_float("RAILGUARD_CRACK_MIN_AREA_PX", 70))
MIN_CRACK_LENGTH_PX = _env_float("RAILGUARD_CRACK_MIN_LENGTH_PX", 38.0)
MIN_ELONGATION = _env_float("RAILGUARD_CRACK_MIN_ELONGATION", 3.0)
MIN_CONTRAST = _env_float("RAILGUARD_CRACK_MIN_CONTRAST", 22.0)
CONFIDENCE_THRESHOLD = _env_float("RAILGUARD_CRACK_CONFIDENCE", 0.55)


def is_available() -> bool:
    """True when OpenCV and NumPy are importable."""
    return bool(CV_AVAILABLE)


def backend_info() -> Dict[str, Any]:
    if not CV_AVAILABLE:
        return {
            "available": False,
            "backend": "unavailable",
            "reason": "OpenCV/NumPy not installed. Run: pip3 install -r requirements-ai.txt",
        }
    return {"available": True, "backend": "opencv", "opencvVersion": cv2.__version__}


def decode_image(data: bytes) -> Optional[Any]:
    """Decode JPEG/PNG bytes into a BGR NumPy array."""
    if not CV_AVAILABLE or not data:
        return None
    try:
        arr = np.frombuffer(data, dtype=np.uint8)
        return cv2.imdecode(arr, cv2.IMREAD_COLOR)
    except Exception as e:
        log.debug("Frame decode failed: %s", e)
        return None


def _empty_result(message: str = "No surface fissure detected") -> Dict[str, Any]:
    return {
        "available": is_available(),
        "hasCrack": False,
        "confidencePercent": 0,
        "crackLengthMm": 0.0,
        "lengthEstimated": True,
        "boundingBox": None,
        "candidates": 0,
        "message": message,
        "mode": "opencv-blackhat" if is_available() else "unavailable",
    }


def analyze_bgr(
    frame: Any,
    mm_per_pixel: Optional[float] = None,
    roi: Optional[Tuple[float, float, float, float]] = None,
) -> Dict[str, Any]:
    """
    Analyse a BGR frame and return a detection dictionary.

    `roi` is (x0, y0, x1, y1) as fractions of width/height. Default keeps the
    central 76% width and the middle 55% height where the railhead is expected.
    """
    if not CV_AVAILABLE:
        return _empty_result("OpenCV/NumPy not installed")
    if frame is None or getattr(frame, "size", 0) == 0:
        return _empty_result("Empty frame")

    try:
        # Work at a predictable size so thresholds behave consistently.
        h, w = frame.shape[:2]
        max_w = 960
        if w > max_w:
            scale = max_w / float(w)
            frame = cv2.resize(frame, (max_w, int(h * scale)), interpolation=cv2.INTER_AREA)
            h, w = frame.shape[:2]

        x0f, y0f, x1f, y1f = roi or (0.12, 0.22, 0.88, 0.78)
        x0, y0 = max(0, int(w * x0f)), max(0, int(h * y0f))
        x1, y1 = min(w, int(w * x1f)), min(h, int(h * y1f))
        if x1 - x0 < 40 or y1 - y0 < 40:
            x0, y0, x1, y1 = 0, 0, w, h

        patch = frame[y0:y1, x0:x1].copy()
        ph, pw = patch.shape[:2]
        gray = cv2.cvtColor(patch, cv2.COLOR_BGR2GRAY)

        # 1. Flatten lighting variations, then enhance local contrast.
        clahe = cv2.createCLAHE(clipLimit=2.5, tileGridSize=(8, 8))
        enhanced = clahe.apply(gray)
        blurred = cv2.GaussianBlur(enhanced, (5, 5), 0)

        # 2. Black-hat: closing minus original keeps dark thin structures.
        ksize = max(9, int(min(ph, pw) * 0.09))
        if ksize % 2 == 0:
            ksize += 1
        kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (ksize, ksize))
        blackhat = cv2.morphologyEx(blurred, cv2.MORPH_BLACKHAT, kernel)

        # 3. Threshold the black-hat response (Otsu is robust to exposure).
        _, bh_mask = cv2.threshold(blackhat, 0, 255, cv2.THRESH_BINARY + cv2.THRESH_OTSU)
        # Guard against Otsu choosing an absurdly low threshold on flat surfaces.
        mean_bh = float(np.mean(blackhat))
        std_bh = float(np.std(blackhat))
        adaptive_mask = (blackhat > (mean_bh + 2.0 * std_bh)).astype(np.uint8) * 255
        mask = cv2.bitwise_and(bh_mask, adaptive_mask)

        # 4. Canny edges, dilated slightly, used to confirm line structure.
        edges = cv2.Canny(blurred, 45, 140)
        edges = cv2.dilate(edges, np.ones((2, 2), np.uint8), iterations=1)
        fused = cv2.bitwise_and(mask, edges)
        # Keep black-hat components that have strong edges, plus a light union
        # so faint but real fissures are not lost.
        fused = cv2.bitwise_or(fused, cv2.bitwise_and(mask, mask))
        fused = cv2.morphologyEx(fused, cv2.MORPH_CLOSE, np.ones((3, 3), np.uint8))

        contours, _ = cv2.findContours(fused, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

        candidates: List[Dict[str, Any]] = []
        patch_area = float(ph * pw)
        for c in contours:
            area = float(cv2.contourArea(c))
            if area < MIN_CRACK_AREA_PX or area > patch_area * 0.08:
                continue
            rect = cv2.minAreaRect(c)
            (cx, cy), (rw, rh), angle = rect
            rw, rh = float(rw), float(rh)
            if rw < 1 or rh < 1:
                continue
            long_side, short_side = max(rw, rh), min(rw, rh)
            elongation = long_side / max(1.0, short_side)
            if elongation < MIN_ELONGATION:
                continue
            if short_side > 16.0:  # cracks are thin; reject blobs
                continue
            if long_side < MIN_CRACK_LENGTH_PX:
                continue

            # Solidity: a crack is a thin line, not a filled blob.
            hull = cv2.convexHull(c)
            hull_area = float(cv2.contourArea(hull))
            solidity = area / hull_area if hull_area > 0 else 0.0
            if solidity > 0.86:
                continue

            # Local contrast: the crack must be darker than its surroundings.
            mask_local = np.zeros((ph, pw), dtype=np.uint8)
            cv2.drawContours(mask_local, [c], -1, 255, thickness=2)
            ring = cv2.dilate(mask_local, np.ones((11, 11), np.uint8)) - mask_local
            crack_pixels = gray[mask_local > 0]
            ring_pixels = gray[ring > 0]
            if crack_pixels.size == 0 or ring_pixels.size == 0:
                continue
            contrast = float(np.mean(ring_pixels)) - float(np.mean(crack_pixels))
            if contrast < MIN_CONTRAST:
                continue

            # Confidence from elongation, contrast, length and thinness.
            score = (
                0.34 * min(1.0, elongation / 8.0)
                + 0.30 * min(1.0, contrast / 60.0)
                + 0.24 * min(1.0, long_side / max(1.0, min(pw, ph) * 0.45))
                + 0.12 * (1.0 - min(1.0, short_side / 16.0))
            )
            score = max(0.0, min(1.0, score))

            bx, by, bw, bh = cv2.boundingRect(c)
            # Bounding box in full-frame coordinates (for the Android overlay).
            candidates.append(
                {
                    "score": round(score, 3),
                    "elongation": round(elongation, 2),
                    "contrast": round(contrast, 2),
                    "lengthPx": round(long_side, 1),
                    "boundingBox": {
                        "x": int(x0 + bx),
                        "y": int(y0 + by),
                        "width": int(bw),
                        "height": int(bh),
                    },
                    "frameWidth": int(w),
                    "frameHeight": int(h),
                }
            )

        if not candidates:
            return _empty_result()

        candidates.sort(key=lambda d: d["score"], reverse=True)
        best = candidates[0]
        has_crack = best["score"] >= CONFIDENCE_THRESHOLD

        mmpp = MM_PER_PIXEL if mm_per_pixel is None else float(mm_per_pixel)
        length_mm = round(best["lengthPx"] * mmpp, 1) if mmpp and mmpp > 0 else 0.0

        return {
            "available": True,
            "hasCrack": bool(has_crack),
            "confidencePercent": int(round(best["score"] * 100)),
            "crackLengthMm": length_mm,
            "lengthEstimated": True if length_mm == 0.0 else False,
            "boundingBox": best["boundingBox"],
            "frameWidth": best["frameWidth"],
            "frameHeight": best["frameHeight"],
            "candidates": len(candidates),
            "elongation": best["elongation"],
            "contrast": best["contrast"],
            "mode": "opencv-blackhat",
            "message": "Surface fissure detected" if has_crack else "Candidate below confidence threshold",
        }
    except Exception as e:  # never let vision crash the sensor loop
        log.warning("Vision analysis error: %s", e)
        return _empty_result(f"Vision error: {e}")


def annotate_bgr(frame: Any, result: Dict[str, Any]) -> Any:
    """Draw the detected bounding box / status on a copy of the frame."""
    if not CV_AVAILABLE or frame is None:
        return frame
    try:
        out = frame.copy()
        box = result.get("boundingBox")
        if box:
            color = (0, 0, 255) if result.get("hasCrack") else (0, 200, 120)
            cv2.rectangle(
                out,
                (int(box["x"]), int(box["y"])),
                (int(box["x"] + box["width"]), int(box["y"] + box["height"])),
                color,
                2,
            )
            label = (
                f"CRACK {result.get('confidencePercent', 0)}%"
                if result.get("hasCrack")
                else "SCAN OK"
            )
            cv2.putText(
                out,
                label,
                (max(4, int(box["x"])), max(18, int(box["y"]) - 6)),
                cv2.FONT_HERSHEY_SIMPLEX,
                0.55,
                color,
                2,
                cv2.LINE_AA,
            )
        return out
    except Exception:
        return frame


def encode_jpeg(frame: Any, quality: int = 82) -> Optional[bytes]:
    if not CV_AVAILABLE or frame is None:
        return None
    try:
        ok, buf = cv2.imencode(".jpg", frame, [int(cv2.IMWRITE_JPEG_QUALITY), int(quality)])
        return buf.tobytes() if ok else None
    except Exception:
        return None


def self_test() -> Dict[str, Any]:
    """Synthetic sanity check used by the unit tests (no camera needed)."""
    if not CV_AVAILABLE:
        return {"ran": False, "reason": "opencv-unavailable"}
    img = np.full((320, 480, 3), 165, dtype=np.uint8)
    # Polished railhead band with mild texture.
    cv2.rectangle(img, (40, 90), (440, 240), (185, 190, 196), -1)
    # Thin dark diagonal fissure.
    cv2.line(img, (140, 130), (300, 210), (18, 18, 22), 3)
    result = analyze_bgr(img)
    return {"ran": True, "detected": bool(result.get("hasCrack")), "result": result}

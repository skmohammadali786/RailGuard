#!/usr/bin/env python3
"""Tests for the real OpenCV crack detector.

Skipped automatically when OpenCV/NumPy are not installed (see
requirements-ai.txt). This means CI without the optional dependency still
passes, while a Pi with OpenCV exercises the real algorithm.
"""

import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import crack_vision


@unittest.skipUnless(crack_vision.is_available(), "OpenCV/NumPy not installed")
class TestCrackVision(unittest.TestCase):
    def test_synthetic_crack_is_detected(self):
        result = crack_vision.self_test()
        self.assertTrue(result["ran"])
        self.assertTrue(result["detected"], f"synthetic crack missed: {result}")
        self.assertGreater(result["result"]["confidencePercent"], 0)

    def test_blank_rail_produces_no_crack(self):
        import cv2
        import numpy as np

        img = np.full((320, 480, 3), 170, dtype=np.uint8)
        cv2.rectangle(img, (40, 90), (440, 240), (190, 195, 200), -1)
        result = crack_vision.analyze_bgr(img)
        self.assertFalse(result["hasCrack"])

    def test_backend_info_reports_available(self):
        self.assertTrue(crack_vision.backend_info()["available"])


if __name__ == "__main__":
    unittest.main()

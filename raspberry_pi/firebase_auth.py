#!/usr/bin/env python3
"""
Firebase ID-token handling for the RailGuard Raspberry Pi.

Firebase Realtime Database REST calls need a valid Firebase ID token. ID tokens
expire after ~1 hour. This module supports three modes:

1. `--refresh-token` + `--api-key` (recommended, long-running installs)
   The manager refreshes the ID token automatically before it expires.

2. `--auth <ID_TOKEN>` (quick tests)
   Works until the token expires (~1 h). A warning is logged.

3. Environment variables
   RAILGUARD_ID_TOKEN, RAILGUARD_REFRESH_TOKEN, RAILGUARD_FIREBASE_API_KEY

Security note: the refresh token is a credential. Keep it in
/etc/railguard/railguard.env with mode 600 and never commit it.
"""

from __future__ import annotations

import json
import logging
import os
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from typing import Any, Dict, Optional

log = logging.getLogger("railguard-auth")

REFRESH_URL = "https://securetoken.googleapis.com/v1/token?key={api_key}"


class FirebaseTokenManager:
    def __init__(
        self,
        id_token: Optional[str] = None,
        refresh_token: Optional[str] = None,
        api_key: Optional[str] = None,
    ):
        self._id_token = (id_token or "").strip() or None
        self._refresh_token = (refresh_token or "").strip() or None
        self._api_key = (api_key or "").strip() or None
        self._obtained_at = time.time() if self._id_token else 0.0
        self._expires_in = 3600
        self._lock = threading.RLock()
        self._last_error: Optional[str] = None
        self._last_refresh_ok: Optional[bool] = None

    # ------------------------------------------------------------------ status
    def can_refresh(self) -> bool:
        return bool(self._refresh_token and self._api_key)

    def has_token(self) -> bool:
        with self._lock:
            return bool(self._id_token)

    def seconds_remaining(self) -> int:
        with self._lock:
            if not self._id_token:
                return 0
            return max(0, int(self._expires_in - (time.time() - self._obtained_at)))

    def status(self) -> Dict[str, Any]:
        return {
            "hasToken": self.has_token(),
            "canRefresh": self.can_refresh(),
            "secondsRemaining": self.seconds_remaining(),
            "lastRefreshOk": self._last_refresh_ok,
            "lastError": self._last_error,
        }

    # ------------------------------------------------------------------ token
    def get_token(self) -> Optional[str]:
        """Return a usable ID token, refreshing it when necessary."""
        with self._lock:
            if self._id_token and self.seconds_remaining() > 120:
                return self._id_token
            if self.can_refresh():
                if self.refresh():
                    return self._id_token
            return self._id_token

    def refresh(self) -> bool:
        """Refresh the ID token using the securetoken endpoint."""
        with self._lock:
            if not self.can_refresh():
                return False
            payload = urllib.parse.urlencode(
                {
                    "grant_type": "refresh_token",
                    "refresh_token": self._refresh_token,
                }
            ).encode("utf-8")
            req = urllib.request.Request(
                REFRESH_URL.format(api_key=self._api_key or ""),
                data=payload,
                method="POST",
            )
            req.add_header("Content-Type", "application/x-www-form-urlencoded")
            try:
                with urllib.request.urlopen(req, timeout=8) as resp:
                    body = json.loads(resp.read().decode("utf-8"))
                new_token = body.get("id_token")
                if not new_token:
                    raise ValueError("refresh response contained no id_token")
                self._id_token = new_token
                self._refresh_token = body.get("refresh_token", self._refresh_token)
                self._expires_in = int(body.get("expires_in", 3600))
                self._obtained_at = time.time()
                self._last_refresh_ok = True
                self._last_error = None
                log.info("[✓] Firebase ID token refreshed (valid for %ds)", self._expires_in)
                return True
            except urllib.error.HTTPError as e:
                detail = e.read().decode("utf-8", errors="replace")[:300]
                self._last_error = f"HTTP {e.code}: {detail}"
                self._last_refresh_ok = False
                log.warning("Firebase token refresh failed: %s", self._last_error)
                return False
            except Exception as e:
                self._last_error = str(e)
                self._last_refresh_ok = False
                log.warning("Firebase token refresh error: %s", e)
                return False

    def start_refresh_loop(self, stop_event: Optional[threading.Event] = None) -> threading.Thread:
        """Background thread that refreshes the token well before expiry."""

        def _loop():
            while not (stop_event and stop_event.is_set()):
                try:
                    if self.can_refresh() and self.seconds_remaining() < 600:
                        self.refresh()
                except Exception as e:  # pragma: no cover
                    log.debug("Token refresh loop error: %s", e)
                time.sleep(60)

        t = threading.Thread(target=_loop, name="railguard-token-refresh", daemon=True)
        t.start()
        return t


def from_args_or_env(args: Any) -> FirebaseTokenManager:
    """Build a manager from argparse namespace and/or environment variables."""
    id_token = getattr(args, "auth", None) or os.getenv("RAILGUARD_ID_TOKEN")
    refresh_token = getattr(args, "refresh_token", None) or os.getenv("RAILGUARD_REFRESH_TOKEN")
    api_key = (
        getattr(args, "firebase_api_key", None)
        or getattr(args, "api_key", None)
        or os.getenv("RAILGUARD_FIREBASE_API_KEY")
    )

    manager = FirebaseTokenManager(
        id_token=id_token,
        refresh_token=refresh_token,
        api_key=api_key,
    )
    if manager.has_token() and not manager.can_refresh():
        log.warning(
            "Firebase token supplied without --refresh-token/--api-key. "
            "Cloud sync will stop after about one hour."
        )
    if not manager.has_token() and not manager.can_refresh():
        log.warning(
            "No Firebase credentials: cloud sync disabled. "
            "Local Wi-Fi API still works. See README 'Firebase authentication'."
        )
    return manager

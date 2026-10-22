#!/usr/bin/env python3
"""Build the QR payload (and PNG) for QR-provisioning SanjayaMdmSample as Device Owner.

Example:
  ./tools/make_qr.py \
    --apk app/build/outputs/apk/release/app-release.apk \
    --cert mdmsample.pem \
    --url https://example.com/mdm/app-release.apk \
    --ssid factory-wifi --wifi-password secret \
    --out qr.png

--cert is the signing certificate exported from the release keystore:
  keytool -exportcert -rfc -alias mdmsample -keystore release.keystore -file mdmsample.pem

Prints the payload JSON. Writes a QR PNG too when the `qrcode` module is installed
(pip install qrcode); otherwise paste the JSON into any QR generator.
"""

import argparse
import base64
import hashlib
import json
import sys

COMPONENT = "com.teachmint.mdmsample/.MdmAdminReceiver"


def b64_sha256(data: bytes) -> str:
    return base64.b64encode(hashlib.sha256(data).digest()).decode()


def cert_der(path: str) -> bytes:
    raw = open(path, "rb").read()
    if b"-----BEGIN" in raw:  # PEM armor -> DER
        body = b"".join(
            line for line in raw.splitlines() if not line.startswith(b"-----"))
        return base64.b64decode(body)
    return raw


def main() -> int:
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--url", required=True, help="public HTTPS URL of the signed APK")
    p.add_argument("--apk", help="signed APK file, for the package checksum")
    p.add_argument("--cert", help="signing cert (PEM or DER), for the signature checksum")
    p.add_argument("--ssid", help="Wi-Fi SSID the panel should join")
    p.add_argument("--wifi-password", help="Wi-Fi password")
    p.add_argument("--wifi-security", default="WPA", choices=["WPA", "WEP", "NONE"])
    p.add_argument("--locale", default="en_IN")
    p.add_argument("--timezone", default="Asia/Kolkata")
    p.add_argument("--allow-camera", action="store_true", help="do not block the camera")
    p.add_argument("--allow-mic", action="store_true", help="do not block the microphone")
    p.add_argument("--out", help="write the QR code to this PNG (needs `qrcode`)")
    args = p.parse_args()

    payload = {
        "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME": COMPONENT,
        "android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_DOWNLOAD_LOCATION": args.url,
        "android.app.extra.PROVISIONING_SKIP_ENCRYPTION": True,
        "android.app.extra.PROVISIONING_LOCALE": args.locale,
        "android.app.extra.PROVISIONING_TIME_ZONE": args.timezone,
        "android.app.extra.PROVISIONING_ADMIN_EXTRAS_BUNDLE": {
            "blockCamera": not args.allow_camera,
            "blockMic": not args.allow_mic,
        },
    }
    if args.cert:
        payload["android.app.extra.PROVISIONING_DEVICE_ADMIN_SIGNATURE_CHECKSUM"] = b64_sha256(cert_der(args.cert))
    if args.apk:
        with open(args.apk, "rb") as f:
            payload["android.app.extra.PROVISIONING_DEVICE_ADMIN_PACKAGE_CHECKSUM"] = b64_sha256(f.read())
    if not args.cert and not args.apk:
        p.error("at least one of --cert or --apk is required, or Android cannot verify the download")
    if args.ssid:
        payload["android.app.extra.PROVISIONING_WIFI_SSID"] = args.ssid
        payload["android.app.extra.PROVISIONING_WIFI_SECURITY_TYPE"] = args.wifi_security
        if args.wifi_password:
            payload["android.app.extra.PROVISIONING_WIFI_PASSWORD"] = args.wifi_password

    text = json.dumps(payload, indent=2)
    print(text)

    if args.out:
        try:
            import qrcode
        except ImportError:
            print("\n`qrcode` not installed (pip install qrcode); paste the JSON above into any QR generator.",
                  file=sys.stderr)
            return 1
        qrcode.make(json.dumps(payload)).save(args.out)
        print(f"\nwrote {args.out}")
    return 0


if __name__ == "__main__":
    sys.exit(main())

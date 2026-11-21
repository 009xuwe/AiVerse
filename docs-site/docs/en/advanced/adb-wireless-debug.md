# Using adb to Wirelessly Debug Other Devices

Once adb is installed inside AiVerse's Linux container, you can use wireless debugging (Android 11 and above) to connect to another Android phone, or to the very phone AiVerse is running on — no computer needed the whole time.

## Use Cases

-   **Debug another phone**: install apps, capture logs, take screenshots, simulate input, upload and download files.
-   **Debug this device**: perform the same operations on the phone AiVerse is currently running on, from inside AiVerse.
-   Operations requiring root are not covered: adb runs as `shell` (uid 2000).

## Environment Baseline

Item

Value

System

Debian GNU/Linux 12 (bookworm) aarch64 (PRoot container)

adb

34.0.5 (Debian backports; the repository's default version is too old and lacks the `pair` command)

Target device

Android 11 and above (tested on Android 15 and Android 16 in this guide)

Network

When debugging another phone, both devices must be on the same LAN; debugging this device goes through `127.0.0.1` and does not depend on the LAN

Privileges

shell (uid 2000), not root

## 1\. Install adb

The adb bundled with the container repository is too old and lacks the `pair` command needed for wireless debugging, so install it from backports:

bash

```
apt update
apt install -y -t bookworm-backports adb
adb version
# Should print Version 34.x
```

**Note**: adb is installed into the container's system directory, so you need to reinstall it after the container is reset, and the pairing records are lost along with it.

## 2\. Pairing (Only Needed Once)

Start on the target phone:

1.  Settings → About phone → tap "Build number" 7 times to enable Developer options;
2.  Developer options → turn on "Wireless debugging";
3.  Tap "Pair device with pairing code", and the screen will show the **IP address and port** along with a **6-digit pairing code**.

It's a good idea to show the "Wireless debugging" settings page and AiVerse side by side so you can operate while looking — as soon as the pairing dialog is switched away from or closed, the port and pairing code become invalid immediately. The pairing port can also be discovered automatically with the script at the end of this article; only the 6-digit pairing code has to be read off the screen.

Back in the AiVerse terminal, replace the port and pairing code with the values shown on screen:

bash

```
adb pair 192.168.1.23:37123 123456
# On success it prints Successfully paired to ...
```

**Note**: the pairing port and pairing code live only briefly and become invalid the moment the dialog closes, so you have to reopen it. Once pairing succeeds, the device remembers this host and you don't need to pair again.

## 3\. Connect

bash

```
adb connect 192.168.1.23:40001
adb devices -l
```

`192.168.1.23:40001` is the "IP address and port" shown on the wireless debugging home page; note that it is not the same port as the one used for pairing.

The connection port changes every time wireless debugging is turned back on; you can check the phone screen again, or discover it automatically with the script at the end of this article.

## 4\. Debugging This Device

When debugging the phone AiVerse is running on, use `127.0.0.1` as the address (the container shares the phone's network and does not depend on the LAN):

bash

```
adb pair 127.0.0.1:37123 123456
adb connect 127.0.0.1:40001
adb devices
```

**Note**: you can also connect using the phone's LAN IP, but the same device will then show up twice (one `127.0.0.1` entry and one LAN IP entry); use `adb -s ` to target a device, or `adb disconnect` to drop the extra one.

The value of debugging this device is that Android restricts regular apps from calling system commands directly (`screencap`, `pm`, `dumpsys` and so on will be denied), whereas running them through adb as `shell` is not subject to that restriction.

## Common Operations

bash

```
adb shell getprop ro.product.model      # Device model
adb shell pm list packages              # Installed apps
adb shell logcat -d                     # Dump the current log
adb exec-out screencap -p > screen.png  # Take a screenshot
adb push local.txt /sdcard/             # Upload a file
adb pull /sdcard/photo.jpg ./           # Download a file
adb shell input keyevent KEYCODE_HOME   # Simulate a key press
```

To install an app use `adb install app.apk`; some devices also require enabling "Install apps via USB" in Developer options.

All of these commands run as the `shell` user, which can read and write `/sdcard` but cannot access restricted directories such as `/data/data`.

## Troubleshooting

**Error `protocol fault (couldn't read status message)`**

The most common cause is an expired pairing port (the dialog was closed or reopened); reopen the dialog and pair again with the new port and pairing code. If you're sure the port is current and still get this error, simply retry the same command — some adb versions have this known issue, and the second attempt often succeeds.

**Error `failed to connect to ...: Connection refused`**

The connection port has expired, or the device has not been paired yet. Check the port on the wireless debugging home page again and re-pair if necessary.

**You have to reconnect in every new terminal**

The adb service exits when the terminal session ends, so you need to run `adb connect` again in a new session. Just run `adb start-server`, `adb connect` and the subsequent commands in the same terminal.

**`adb mdns services` returns an empty list**

adb's built-in mDNS discovery does not work inside the container — the port 5353 it needs is already taken by the phone system's mDNS service. This does not affect manual connections; use the script below for automatic discovery.

## Appendix: Discovering Ports Automatically

Once wireless debugging is on, the device broadcasts `_adb-tls-pairing._tcp` (the pairing service, only while the pairing dialog is open) and `_adb-tls-connect._tcp` (the connection service) on the LAN. Save the following script as `discover_adb.py`; after running it, it lists the "IP:port" of each:

python

```
#!/usr/bin/env python3
"""Scan the LAN for adb wireless-debugging endpoints"""
import socket
import struct
import time

SERVICES = {
    'Pairing port': '_adb-tls-pairing._tcp.local',
    'Connection port': '_adb-tls-connect._tcp.local',
}

def encode(name):
    out = b''
    for part in name.split('.'):
        out += bytes([len(part)]) + part.encode()
    return out + b'\x00'

def read_name(data, off):
    labels, jumped, start = [], False, off
    while True:
        length = data[off]
        if length == 0:
            off += 1
            break
        if length & 0xC0 == 0xC0:
            ptr = struct.unpack('!H', data[off:off + 2])[0] & 0x3FFF
            if not jumped:
                start = off + 2
            off, jumped = ptr, True
            continue
        labels.append(data[off + 1:off + 1 + length].decode())
        off += 1 + length
    return '.'.join(labels), (start if jumped else off)

def scan(service, seconds=6):
    # Setting the top bit of the class field requests a unicast response: the
    # pairing service only replies with multicast to ordinary queries, so you
    # won't receive anything without binding 5353; the connection service
    # replies with unicast
    query = (struct.pack('!HHHHHH', 0, 0, 1, 0, 0, 0)
             + encode(service)
             + struct.pack('!HH', 12, 0x8001))
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
    sock.setsockopt(socket.IPPROTO_IP, socket.IP_ADD_MEMBERSHIP,
                    struct.pack('4s4s', socket.inet_aton('224.0.0.251'),
                                socket.inet_aton('0.0.0.0')))
    sock.bind(('', 0))
    sock.settimeout(0.3)
    endpoints = set()
    deadline = time.time() + seconds
    while time.time() &lt; deadline:
        sock.sendto(query, ('224.0.0.251', 5353))
        try:
            data, addr = sock.recvfrom(9000)
        except socket.timeout:
            continue
        qd, an, ns, ar = struct.unpack('!HHHH', data[4:12])
        off = 12
        for _ in range(qd):
            _, off = read_name(data, off)
            off += 4
        for _ in range(an + ns + ar):
            name, off = read_name(data, off)
            rtype, _, _, rdlen = struct.unpack('!HHIH', data[off:off + 10])
            off += 10
            rdata = data[off:off + rdlen]
            off += rdlen
            if rtype == 33 and service.split('.')[0] in name:
                port = struct.unpack('!HHH', rdata[:6])[2]
                endpoints.add(f'{addr[0]}:{port}')
    sock.close()
    return endpoints

for label, service in SERVICES.items():
    for endpoint in sorted(scan(service)):
        print(f'{label}  {endpoint}')
```

Usage:

bash

```
python3 discover_adb.py
# Pairing port  192.168.1.23:37123
# Connection port  192.168.1.23:40001
adb pair 192.168.1.23:37123 123456
adb connect 192.168.1.23:40001
```

**Note**: the pairing service only broadcasts while the "Pair device with pairing code" dialog is open, so run the script with the dialog open; the 6-digit pairing code always has to be read from the phone screen. The list may contain multiple results (including stale ports from before the device rebooted) — just try `adb connect` on each one.

# NetMonitor Pro — Real VPN Monitor

- Android `VpnService` with per-app selection.
- Real TUN-to-SOCKS forwarding through `hev-socks5-tunnel`.
- TCP and UDP destination logging.
- CSV/JSON export.
- Arabic UI.

Important: Android only permits one active VPN at a time. The user must approve the system VPN dialog. The monitor is local; it does not upload captured data.

The native tunnel engine is built in GitHub Actions from `heiher/hev-socks5-tunnel`.
\n## What is actually captured\nThe app uses Android `VpnService` with per-app allowlisting, then forwards the selected apps through a local SOCKS5 endpoint and a native tun2socks engine. TCP CONNECT and UDP relay destinations are recorded locally as IP/host, port, protocol and byte/event counts. Android's VPN API requires the user to approve the VPN dialog, and only one VPN can be active at a time.\n
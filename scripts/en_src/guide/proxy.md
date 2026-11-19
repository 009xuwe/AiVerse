# Network Proxy

::: tip Version note
The network proxy is available from 1.0.0.
:::

Under "Settings → Runtime Environment → Network Proxy" you configure a global proxy so that all of the App's network requests (AI chat, MCP, model lists, update checks, update package downloads, mirror downloads, web search) as well as commands inside the container (`curl`, `git`, `npm`, `pip`, etc.) go through the proxy.

## Proxy Settings

- **Enable Proxy**: The master switch. Turn it off and all requests go back to direct connections.
- **Type**: Tap to pick HTTP or SOCKS5 from a bottom sheet.
  - **HTTP**: Forwards through a CONNECT tunnel and can proxy all http/https traffic. A local Clash instance at `127.0.0.1:7890` is this type.
  - **SOCKS5**: TCP-level forwarding (Clash usually uses port `7891`). Note that npm and pip inside the container have limited SOCKS5 support.
- **Server Address / Port**: The proxy address.
- **Authentication** (optional): Fill this in when the proxy requires a username and password. A local proxy without authentication (such as Clash) needs no configuration.
- **Bypass Proxy For**: Comma-separated addresses that connect directly. Supports hostnames, the wildcard `*`, subdomains `*.example.com`, `host:port`, and CIDR ranges. The default value already includes localhost and common private network ranges.

Once everything is filled in, a **Connectivity Test** card appears below:

- **Test URL**: Defaults to `https://www.google.com`; you can change it to any target.
- **Test Proxy**: Sends one request to that address through the current proxy to confirm the proxy actually works. The test does not display the exit IP, nor does it send your device IP to any third-party service.

## Provider-Level Proxy

An individual provider can also have its own dedicated proxy. Under "AI Providers → Edit Page → Options → Network Proxy", the interface is basically the same as the global proxy page (minus "Bypass Proxy For").

When enabled, this provider's AI requests go through the dedicated proxy while other requests still use the global proxy; when disabled, it follows the global setting. Changes take effect together with saving the provider edit page.

The matching rule is an exact match on the provider's Base URL **host plus path prefix**, so providers on the same host with different paths can each have a different proxy, and requests are dispatched by the most specific path prefix. Only when multiple providers share exactly the same host and path (same endpoint, different keys) can they not be distinguished; in that case the one configured first takes effect.

This suits the case where multiple providers share one gateway domain but differ in path, and each needs a different exit.

::: tip Container commands are not affected by provider-level proxy
Commands inside the container always use the global proxy.
:::

## When Changes Take Effect

All changes **take effect immediately**; new connections use the proxy right away, with no need to restart the App.

However, long-running processes already started inside the container will not pick up the new environment variables — you need to restart that process or the whole container. The container's proxy is implemented through environment variables: for the HTTP type it injects `HTTP_PROXY` / `HTTPS_PROXY` / `ALL_PROXY` / `NO_PROXY`; for the SOCKS5 type it injects `ALL_PROXY=socks5h://...` (which uses remote DNS).

## Notes

- The proxy configuration is stored in the App's private directory and does not affect system-level proxy settings.
- When the proxy is unavailable (wrong address, proxy not running), network requests will fail; turning off "Enable Proxy" restores direct connections.
- **In remote SSH mode, commands on the server do not go through the local proxy**; you need to configure it on the server yourself.
- HTTPS proxies are not supported (that is, proxies where the connection to the proxy server itself uses TLS).

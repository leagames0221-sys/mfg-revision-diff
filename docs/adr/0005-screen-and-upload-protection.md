# ADR 0005: How the screen and the upload are protected

Status: accepted

Source entries (S...), author's decisions (D...) and searches without result (N...) refer to [../sources.md](../sources.md).

## Context

The screen shows text that comes from files the person uploads, and it accepts a file upload by POST. It runs on the person's own PC, so a web page the person opens in the same browser can also try to talk to it.

What can go wrong, and what the sources say:

- **Text from a file becomes script in the page** (cross-site scripting). A step may be called `<script>alert(1)</script>`. The OWASP XSS cheat sheet: "use HTML entity encoding for that variable." and a table of five characters to convert (S11.1). It also says not to put variables in places where output encoding is not enough: "Don't place variables into dangerous contexts as even with output encoding, it will not prevent an XSS attack fully." (S11.1).
- **Another site makes the browser send a POST to the local server** (cross-site request forgery). A form post is a "simple" request, and OWASP says such requests "introduce risk of CSRF because browsers permit them to be sent to any origin" (S11.2). The OWASP CSRF cheat sheet: "If the Origin header is present, verify that its value matches the target origin." and "If the Origin header is not present, verify that the hostname in the Referer header matches the target origin." The match has to be strict, "does not pass your origin check" for `example.org.attacker.com`, and "Missing headers and `Origin: null` are not evidence of a same-origin request." The most secure way to know the target origin is to be told it by the program: "Configure your application to simply know its target origin" (S11.2).
- **A web page reaches the local server through a changed DNS answer** (DNS rebinding). A specification for local servers says: "Servers MUST validate the Origin header on all incoming connections to prevent DNS rebinding attacks" and "When running locally, servers SHOULD bind only to localhost (127.0.0.1)" (S11.4). That page is written for MCP servers; the situation (a local server that a browser can reach) is the same.
- **A very large upload** uses the memory of the PC. The XML reader already has limits of 8 MiB, depth 64 and 200,000 elements (D11).
- **A hostile XML file** (external entities, DOCTYPE). Handled by `SecureXml` (sources.md S2.1, `XxeTest`).
- **Text that a spreadsheet reads as a formula.** Handled in ADR 0004.

## Decision

We will apply six protections, each in one place with its own tests.

1. **Escape all file text.** `Html.esc` (`Html.java` L14) converts `&`, `<`, `>`, `"` and `'` to the entities of the OWASP table (S11.1). Every string from a file (names, IDs, parts, the file names, the "what changed" text, parser messages) goes through it in `Pages.java` (`esc(` is called in `head`, `error`, `result` and `cell`). File text is put only into element text; the attributes hold fixed words and the server's own ids; there is no inline script, no inline style and no URL built from file text, so the "dangerous contexts" of S11.1 are not used. Test: `ServerTest.textFromTheFilesIsEscapedInTheResultPage` (a step name `<script>alert(1)</script>` and a file name `<img src=x onerror=alert(2)>.xml` are shown as text and the page has exactly one `<script` element, the one that loads `/app.js`), `SmallPartsTest.escapesTheFiveCharactersOfTheOwaspTable`.
2. **Accept a POST only from a page of this server.** `OriginGuard.sourceAllowed` (`OriginGuard.java` L33) compares the `Origin` header as a whole string with the two origins that belong to the server's own port (`http://127.0.0.1:<port>`, `http://localhost:<port>`); without `Origin` it compares the origin of the `Referer`; with neither, or with `Origin: null`, it refuses; a `Sec-Fetch-Site` other than `same-origin` is refused (S11.2, D22). `Server.compare` asks this before it reads the body (`Server.java` L200-L204). Test: `ServerTest.aPostFromAnotherOriginIsRefused` (another site, `null`, a host that starts like ours, another scheme, another port, no header, a foreign Referer, cross-site fetch metadata: all 403; the server's own origin and `localhost` and a same-site Referer: 303), `SmallPartsTest.theOriginIsComparedAsAWholeString` and `theRefererIsUsedOnlyWithoutOriginAndOnlyByItsOrigin`.
3. **Listen on 127.0.0.1 only and check the Host header.** `HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0)` (`Server.java` L53); `OriginGuard.hostAllowed` (L28) refuses a request whose `Host` is not `127.0.0.1:<port>` or `localhost:<port>` (S11.4, D22). Tests: `ServerTest.aRequestForAnotherHostNameIsRefused` (plain socket, `Host: evil.example`: 403), `theServerListensOnTheLoopbackAddressOnly` (connecting through each non-loopback address of the PC fails).
4. **Limit the request body to 8 MiB**, the value that already limits an input file (D11, D19). `Server.readBody` (`Server.java` L168) looks at `Content-Length` and, for a body that is sent in chunks, reads at most 8 MiB + 1 bytes; a larger body is read and thrown away up to 16 MiB, so that the browser can still show the answer, and is refused with 413 (`drain` L191). Tests: `aBodyLargerThanTheLimitIsRefused` (declared length), `aBodyWithoutLengthThatGrowsPastTheLimitIsRefused` (chunked), `aBodyJustUnderTheLimitIsReadAndRefusedOnlyBecauseItIsNotB2mml` (a body of 8 MiB minus 4 KiB passes the limit and is refused later, as 400, because it is not B2MML).
5. **Read XML only with `SecureXml`.** `Server.compare` calls `SecureXml.parse` (`Server.java` L248-L249), the XML reader that refuses DOCTYPE and has external access off, then `B2mmlReader.fromDocument`. Test: `ServerTest.aDoctypeIsRefused` (an external entity that points to a file of the PC is refused with 400 and nothing of the file is in the answer).
6. **Send a policy with every response.** `Content-Security-Policy: default-src 'none'; script-src 'self'; style-src 'self'; form-action 'self'; base-uri 'none'; frame-ancestors 'none'`, `X-Content-Type-Options: nosniff`, `Cache-Control: no-store` (`Server.java` L42 and L286-L289, D22). OWASP calls a CSP "an additional layer of defense" and not the primary defence (S11.1); the primary defence is item 1. Test: `ServerTest.theUploadPageIsServedWithAPolicyThatForbidsInlineScripts`.

The multipart body is read by `Multipart.parse` (`Multipart.java` L32) following RFC 7578 and RFC 2046 (S11.3, S11.3b); it refuses a body without a boundary, with a part that is not closed, a part without a name, more than 8 parts or a header block over 4096 bytes (D19). Tests: `SmallPartsTest` (the five `Multipart` tests).

## Alternatives considered

| decision | candidate | why not |
|---|---|---|
| Output | Escape when the file is read | The data would be changed for every later use (the Excel file would contain entities). Escaping where the text enters HTML keeps the data as it is (S11.1 says to encode the variable "to a HTML context"). |
| Output | Sanitise HTML, allow some tags | The screen never needs markup from a file. Encoding is enough and has no allow-list to maintain. |
| Output | Rely on the CSP alone | OWASP: a CSP "should not be your primary defense mechanism" |
| POST source | Anti-forgery token in a hidden field | Needs a token per page and a store for it; with no login and no cookies there is no session to bind it to. The cheat sheet says of the header check that it is "also commonly used with unauthenticated requests" (S11.2). A token could be added later. |
| POST source | `SameSite` cookies | There is no cookie and no login. |
| POST source | Accept any origin because the server is local | A page opened in the same browser can send the POST (S11.2); it could also upload files of its choice and fill the result store. |
| Host | No Host check, only Origin | A GET through a rebound DNS name is not covered by an Origin check on POST; the result pages hold the person's data (S11.4). |
| Limit | Stream the body to a temporary file | More code, a file to clean up, and the XML limit is 8 MiB anyway. |
| Limit | Limit each file, not the body | A body of any size would be read before a file is looked at. |

## Consequences

- Positive: escaped `<script>` names, a POST from another origin and a body that is too large each have tests; the escape and the Origin check also have a mutation check (`docs/validation/mutation-check.md`).
- Positive: the protections do not depend on a framework; each is a few lines in one class.
- Negative: the target origin is fixed to the port the server was started with. A reverse proxy or a changed `Host` would be refused; that is intended for a tool that is run locally.
- Negative: the Host list contains only `127.0.0.1` and `localhost`; the address `[::1]` is not accepted (the server binds the address that `InetAddress.getLoopbackAddress()` returns, and the printed URL uses 127.0.0.1).
- Negative: a cross-origin POST with a large body that is refused with 403 is not read; the browser may report a reset connection instead of the page. Only a script on another site would see this.
- Negative: there are no cookies, so there is no `HttpOnly` or `Secure` to set; if a login is ever added, this ADR has to be revisited.
- Negative: the result pages are open to any local process that can guess a 128-bit id and send a correct Host header; they are not protected against other programs on the same PC.
- Not covered: HTTPS; rate limits; a log of refused requests; the behaviour of browsers that send neither `Origin` nor `Referer` on a form POST (they are refused, S11.2); how the spreadsheet program treats the downloaded file after it is saved as CSV (ADR 0004).

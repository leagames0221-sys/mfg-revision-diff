# ADR 0003: Build the screen on the JDK's own HttpServer

Status: accepted

Source entries (S...) and author's decisions (D...) refer to [../sources.md](../sources.md).

## Context

The comparison needs a screen that a person can open on their own PC: pick two B2MML files (or the bundled samples), see every step and bill row with its category, narrow the list, jump between differences, and download an Excel file.

Facts that shape the choice:

- **The screen is local.** It reads files from the person's disk and shows them back; it needs no accounts, no database and no network access from other machines. It should listen on 127.0.0.1 only (S11.4).
- **One new library is already needed** for Excel (ADR 0004). The fewer further libraries, the smaller the part of the dependency check (README) that the project has to keep clean.
- **The pages are few**: an upload form, a result page, a file download, two static files.
- **The tool must start with what a Java 17 installation and the local Maven repository provide**, so that it runs from `target/classes` with a short script (`scripts/run.ps1`, `scripts/run.sh`, D26), without a plugin that bundles the libraries into one jar.
- The JDK ships `com.sun.net.httpserver.HttpServer`: "This class implements a simple HTTP server." (S11.4b). It is bound to an address and a port; a request for which no handler is found is answered with 404 (S11.4b).

## Decision

We will use `com.sun.net.httpserver.HttpServer` from the JDK, bound to the loopback address only, with one handler that routes the five paths (`Server.java` L53-L56, `Server.handle` L91 onward).

- Binding: `HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0)` (`Server.java` L53).
- Pages are built with a `StringBuilder` in `Pages.java`; there is no template engine. The page loads `/app.css` and `/app.js` from the class path; there is no inline script or style, which lets the response header forbid both (ADR 0005).
- The filter is a query parameter and is applied on the server (`Server.filterOf`, `RowModel.filter`), so the result page works for each filter without script. Only "previous / next difference" needs script (`src/main/resources/web/app.js`).
- The rows come from `RowModel`, the same class the Excel writer reads (`RowModel.java` L60, L72), so the screen and the file cannot disagree.
- The result is kept in memory under a random id so that the page and the download can be fetched with GET (`ResultStore`, D19).
- What the screen shows follows the products that were read: the symbols of the SAP routing comparison with the added moved mark (S4.6), the Oracle filter names (S4.7) with a seventh filter for moved steps, previous / next buttons (S11.10a, S11.10b), yellow cells (S11.10c), an export of the list (S11.10b). The words and the layout are the author's (D20).

## Alternatives considered

| candidate | what it is | why not |
|---|---|---|
| **Jakarta Servlet with a servlet container** | "Jakarta Servlet defines a server-side API for handling HTTP requests and responses." (S11.11). It is an API; a container (Tomcat, Jetty or another) has to be added to run it. | A container and its dependencies would be new libraries for five pages. Not tried here (sources.md N11). |
| **Spring Boot** | "For servlet application, Spring Boot includes support for embedded Tomcat and Jetty servers." (S11.11b) | The same container plus the Spring libraries and a build plugin that packages the application; the project keeps to one runtime library (POI). The benefit (dependency injection, templates, security filters) is not needed for five pages. Not tried here (N11). |
| **Command-line tool and Excel file only** | what ADR 0001 / `Cli` already offer | No filters, no previous / next, no colour; the people who read a process-plan comparison work in a browser or in Excel. The command line stays as it was. |
| **JDK HttpServer** | S11.4b | **Chosen.** No new library; loopback binding is one argument; the whole server is one class of about 300 lines. |

## Consequences

- Positive: the screen adds no library. `mvn test` plus `scripts/run.ps1` or `scripts/run.sh` starts it (README).
- Positive: the server code is small enough to read in one sitting, and the protections of ADR 0005 are all in the classes `Server`, `OriginGuard`, `Html` and `Multipart`, each with its tests.
- Negative: the JDK's server is "simple" (S11.4b). It has no routing library, no session, no request logging and no multipart reader; the multipart reader is written here (`Multipart.java`, RFC 7578 and RFC 2046, S11.3, S11.3b) and has its own tests. A bug in it is this project's bug.
- Negative: results are kept in memory (the last 16, D19) and are lost when the server stops. A person who wants to keep a result uses the Excel download.
- Negative: `com.sun.net.httpserver` is in the JDK module `jdk.httpserver`; a Java runtime without that module cannot start the screen. The command line and the tests of the matching do not need it.
- Not covered: HTTPS, login, several users at once (the server has 4 worker threads, D19), uploads larger than 8 MiB, schema validation from the screen (the command line has `--validate`).

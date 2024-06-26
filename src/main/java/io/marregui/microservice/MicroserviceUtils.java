package io.marregui.microservice;

import io.marregui.util.ILogger;
import io.marregui.util.Logger;
import io.marregui.util.ThrlStringBuilder;
import spark.Request;
import spark.Response;
import spark.Spark;

public final class MicroserviceUtils {
    private static final ILogger LOGGER = Logger.loggerFor(MicroserviceUtils.class);
    private static final String[] NO_COMMAND_PARAMETERS = new String[]{ /* no params */};

    public enum HttpHeaders {
        ContentType("Content-Type"),
        TransferEncoding("Transfer-Encoding"),
        Host("Host"),
        Accept("Accept");

        private final String str;

        HttpHeaders(String str) {
            this.str = str;
        }

        public String str() {
            return this.str;
        }

        @Override
        public String toString() {
            return this.str;
        }
    }

    public static final String ROOT_PATH = "/"; // where the microservice resides (each ms works on a specific port)
    public static final String PATH_SEP = "/";
    public static final String APP_JSON = "application/json";
    public static final String CHUNKED = "chunked";
    public static final String CRLF = "\r\n";

    /**
     * Calls Spark.port(port) to set the web port for the microservice
     *
     * @param port Web port (http://url:{port}/)
     */
    public static void setPort(int port) {
        Spark.port(port);
        LOGGER.info(String.format("setPort(%d)", port));
    }

    /**
     * @param name
     * @return Makes name into a keyword (colon prefix, e.g. 'dateOfBirth' ->
     * ':dateOfBirth')
     */
    public static String makeKeyword(String name) {
        return name.startsWith(":") ? name : String.format(":%s", name);
    }

    /**
     * @param resource
     * @return A HTTP GET header for the resource
     */
    public static String createRequestHeader(String resource) {
        StringBuilder sb = ThrlStringBuilder.get();
        sb.append("GET ").append(resource).append(" HTTP/1.1").append(CRLF);
        sb.append(HttpHeaders.Host.str()).append(": ignore").append(CRLF);
        sb.append(HttpHeaders.Accept.str()).append(": ").append(APP_JSON).append(CRLF);
        sb.append(MicroserviceUtils.CRLF);
        return sb.toString();
    }

    /**
     * Sets Content-Type: application/json, Transfer-Encoding: chunked to the
     * response headers
     *
     * @param res (spark.Response)
     */
    static void setResponseHeaders(Response res) {
        res.header(HttpHeaders.ContentType.str(), APP_JSON);
        res.header(HttpHeaders.TransferEncoding.str(), CHUNKED);
    }

    /**
     * Concatenates the path items as in:
     * <pre>
     *    <b>/command</b>: with withParameters == false
     *    <b>/command/*</b>: with withParameters == true
     * </pre>
     *
     * @param command        Command, as in http://url:port/command[/*]
     * @param withParameters if true, we append /* to the above route
     * @return a route path
     */
    public static String routePath(String command, boolean withParameters) {
        if (null == command || command.isEmpty()) {
            throw new IllegalArgumentException("Command null or empty String");
        }
        StringBuilder sb = ThrlStringBuilder.get();
        sb.append(ROOT_PATH).append(command);
        if (withParameters) {
            sb.append(PATH_SEP).append("*");
        }
        String route = sb.toString();
        LOGGER.info("routePath(%s) = %s", command, route);
        return route;
    }

    /**
     * The request will contain a path info that looks like:
     * <pre>
     *     <b>/command/param1/param2/param//with//white//space//number3/param4</b>
     * </pre>
     * <p>
     * The *optional* parameters related part of the path info is split, with '//'
     * being interpreted as a single white space character, and '/' as the parameter
     * separator. Thus the above yields:
     *
     * <pre>
     *     <b>{"param1", "param2", "param with white space number3", "param4"}</b>
     * </pre>
     *
     * @param command
     * @param req
     * @return
     */
    public static String[] extractCommandParameters(String command, Request req) {
        String requestPathInfo = req.pathInfo();
        LOGGER.info("req.pathInfo(): %s", requestPathInfo);
        int basePathlen = routePath(command, false).length();
        String params = (requestPathInfo.length() > basePathlen) ? requestPathInfo.substring(basePathlen + 1) : null;
        if (null != params && false == params.isEmpty()) {
            String[] split = params.replaceAll("//", " ").split("/");
            for (int i = 0; i < split.length; i++) {
                LOGGER.info("p%d: %s", i, split[i]);
            }
            return split;
        }
        return NO_COMMAND_PARAMETERS;
    }

    private MicroserviceUtils() {
        throw new UnsupportedOperationException("no instances are allowed, it is a utilities class");
    }
}

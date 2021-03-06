package com.laytonsmith.PureUtilities;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contains methods to simplify web connections.
 *
 * @author lsmith
 */
public final class WebUtility {
    
    private WebUtility(){}

    private static int urlRetrieverPoolId = 0;
    private static ExecutorService urlRetrieverPool = Executors.newCachedThreadPool(new ThreadFactory() {
        public Thread newThread(Runnable r) {
            return new Thread(r, "URLRetrieverThread-" + ( ++urlRetrieverPoolId ));
        }
    });

    public enum HTTPMethod {

        POST, GET
    }

    public static final class HTTPResponse {

        private String rawResponse = null;
        private List<HTTPHeader> headers = new LinkedList<HTTPHeader>();
        private String responseText;
        private int responseCode;
        private String content;

        private HTTPResponse(String responseText, int responseCode, Map<String, List<String>> headers, String response) {
            this.responseText = responseText;
            this.responseCode = responseCode;
            for (String key : headers.keySet()) {
                for (String value : headers.get(key)) {
                    this.headers.add(new HTTPHeader(key, value));
                }
            }
            content = response;
        }

        /**
         * Gets the contents of this HTTP request
         *
         * @return
         */
        public String getContent() {
            return this.content;
        }

        /**
         * Gets the value of the first header returned.
         *
         * @param key
         * @return
         */
        public String getFirstHeader(String key) {
            for (HTTPHeader header : headers) {
                if (header.getHeader().equalsIgnoreCase(key)) {
                    return header.getValue();
                }
            }
            return null;
        }

        public List<String> getHeaders(String key) {
            List<String> list = new ArrayList<String>();
            for (HTTPHeader header : headers) {
                list.add(header.getValue());
            }
            return list;
        }

        public int getResponseCode() {
            return responseCode;
        }

        @Override
        public String toString() {
            if (rawResponse == null) {
                rawResponse = "HTTP/1.1 " + responseCode + " " + responseText + "\n";
                for (HTTPHeader h : headers) {
                    if (h.getHeader() == null) {
                        continue;
                    }
                    rawResponse += h.getHeader() + ": " + h.getValue() + "\n";
                }
                rawResponse += "\n" + content;
            }
            return rawResponse;
        }
    }

    public static final class HTTPCookies {

        private static final class HTTPCookie {

            String name;
            String value;
            String domain;
            String path;
            long expiration = 0;
            boolean httpOnly = false;
            boolean secureOnly = false;
        }
        private final List<HTTPCookie> cookies = new ArrayList<HTTPCookies.HTTPCookie>();

        public void addCookie(String unparsedValue) {
            //TODO
        }

        public void addCookie(String name, String value, String domain, String path, long expiration, Boolean httpOnly) {
            HTTPCookie cookie = new HTTPCookie();
            cookie.name = name;
            cookie.value = value;
            cookie.domain = domain;
            cookie.path = path;
            cookie.expiration = expiration;
            if (httpOnly == null) {
                cookie.httpOnly = false;
                cookie.secureOnly = false;
            } else if (httpOnly) {
                cookie.httpOnly = true;
                cookie.secureOnly = false;
            } else {
                cookie.httpOnly = false;
                cookie.secureOnly = true;
            }
            cookies.add(cookie);
        }

        public void addCookie(String name, String value, String domain, String path) {
            addCookie(name, value, domain, path, 0, null);
        }

        public String getCookies(URL url) {
            List<HTTPCookie> usable = new ArrayList<HTTPCookies.HTTPCookie>();
            for (int i = 0; i < cookies.size(); i++) {
                HTTPCookie cookie = cookies.get(i);
                if (cookie.expiration > ( System.currentTimeMillis() / 1000 )) {
                    //This cookie is expired. Remove it.
                    cookies.remove(i);
                    i--;
                    continue;
                }
                //If it's http only, and we aren't in http, continue.
                if (cookie.httpOnly && !url.getProtocol().equals("http")) {
                    continue;
                }
                //Or it's secure only, and we aren't in https, continue.
                if (cookie.secureOnly && !url.getProtocol().equals("https")) {
                    continue;
                }

                //If we aren't in the correct domain
                if (!url.getHost().endsWith(cookie.domain)) {
                    continue;
                }
                //Or if we aren't in the right path
                if (!url.getPath().startsWith(cookie.path)) {
                    continue;
                }

                //If we're still here, it's good.
                usable.add(cookie);
            }
            StringBuilder b = new StringBuilder();
            for (HTTPCookie cookie : usable) {
                if (b.length() != 0) {
                    b.append("; ");
                }
                try {
                    b.append(URLEncoder.encode(cookie.name, "UTF-8")).append("=").append(URLEncoder.encode(cookie.value, "UTF-8"));
                }
                catch (UnsupportedEncodingException ex) {
                    Logger.getLogger(WebUtility.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            return b.toString();
        }
    }

    public static final class HTTPHeader {

        private String header;
        private String value;

        private HTTPHeader(String header, String value) {
            this.header = header;
            this.value = value;
        }

        public String getHeader() {
            return header;
        }

        public String getValue() {
            return value;
        }
    }

    public static interface HTTPResponseCallback {

        public void response(HTTPResponse response);

        public void error(Throwable error);
    }

    /**
     * Gets a web page based on the parameters specified. This is a blocking
     * call, if you wish for it to be event driven, consider using the GetPage
     * that requires a HTTPResponseCallback.
     *
     * @param url The url to navigate to
     * @param method The HTTP method to use
     * @param parameters The parameters to be sent. Parameters can be also
     * specified directly in the URL, and they will be merged.
     * @param cookieStash An instance of a cookie stash to use, or null if none
     * is needed. Cookies will automatically be added and used from this
     * instance.
     * @param followRedirects If 300 code responses should automatically be
     * followed.
     * @return
     * @throws IOException
     */
    public static HTTPResponse GetPage(URL url, HTTPMethod method, Map<String, String> parameters, HTTPCookies cookieStash, boolean followRedirects) throws IOException {
        if (cookieStash != null) {
            throw new UnsupportedOperationException("Cookies are not yet supported. Send null for the cookieStash parameter for the time being.");
        }
        //First, let's check to see that the url is properly formatted. If there are parameters, and this is a GET request, we want to tack them on to the end.
        if (parameters != null && !parameters.isEmpty() && method == HTTPMethod.GET) {
            StringBuilder b = new StringBuilder(url.getQuery() == null ? "" : url.getQuery());
            if (b.length() != 0) {
                b.append("&");
            }
            b.append(encodeParameters(parameters));
            String query = b.toString();
            url = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getPath() + "?" + query);
        }

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(followRedirects);
        if (method == HTTPMethod.POST) {
            conn.setDoOutput(true);
            String params = encodeParameters(parameters);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", Integer.toString(params.length()));
            OutputStream os = new BufferedOutputStream(conn.getOutputStream());
            WriteStringToOutputStream(params, os);
            os.close();
        }

        InputStream is;
        if (conn.getErrorStream() != null) {
            is = conn.getErrorStream();
        } else {
            is = conn.getInputStream();
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(is));
        String line;
        StringBuilder b = new StringBuilder();
        while (( line = in.readLine() ) != null) {
            b.append(line).append("\n");
        }
        in.close();
        return new HTTPResponse(conn.getResponseMessage(), conn.getResponseCode(), conn.getHeaderFields(), b.toString());
    }

    private static String encodeParameters(Map<String, String> parameters) {
        if (parameters == null) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        for (String key : parameters.keySet()) {
            String value = parameters.get(key);
            try {
                b.append(URLEncoder.encode(key, "UTF-8")).append("=").append(URLEncoder.encode(value, "UTF-8"));
            }
            catch (UnsupportedEncodingException ex) {
                Logger.getLogger(WebUtility.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return b.toString();
    }

    private static void WriteStringToOutputStream(String data, OutputStream os) throws IOException {
        for (Character c : data.toCharArray()) {
            os.write((int) c.charValue());
        }
    }

    /**
     * A very simple convenience method to get a page
     *
     * @param url
     * @return
     */
    public static HTTPResponse GetPage(URL url) throws IOException {
        return GetPage(url, HTTPMethod.GET, null, null, true);
    }

    /**
     * A very simple convenience method to get a page
     *
     * @param url
     * @return
     * @throws IOException
     */
    public static HTTPResponse GetPage(String url) throws IOException {
        return GetPage(new URL(url));
    }

    /**
     * A very simple convenience method to get a page. Only the contents are
     * returned by this method.
     *
     * @param url
     * @return
     * @throws IOException
     */
    public static String GetPageContents(URL url) throws IOException {
        return GetPage(url).getContent();
    }

    /**
     * A very simple convenience method to get a page. Only the contents are
     * returned by this method.
     *
     * @param url
     * @return
     * @throws IOException
     */
    public static String GetPageContents(String url) throws IOException {
        return GetPage(url).getContent();
    }

    /**
     * Makes an asynchronous call to a URL, and runs the callback when finished.
     */
    public static void GetPage(final URL url, final HTTPMethod method, final Map<String, String> parameters, final HTTPCookies cookieStash, final boolean followRedirects, final HTTPResponseCallback callback) {
        urlRetrieverPool.submit(new Runnable() {
            public void run() {
                try {
                    HTTPResponse response = GetPage(url, method, parameters, cookieStash, followRedirects);
                    if (callback == null) {
                        return;
                    }
                    callback.response(response);
                }
                catch (IOException ex) {
                    if (callback == null) {
                        return;
                    }
                    callback.error(ex);
                }
            }
        });
    }
}

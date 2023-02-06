package ru.yandex.autotests.market.stat.requests;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by entarrion on 22.11.16.
 */
public class SenderUtils {
    public static LightweightResponse send(RequestData request) {
        HttpURLConnection urlConnection = getConnectionForMethod(request);
        try {
            urlConnection.connect();
        } catch (IOException e) {
            throw new IllegalStateException("Can't open a connection to the " + request.toString(), e);
        } finally {
            urlConnection.disconnect();
        }
        return new LightweightResponseImpl(
                getResponseBody(urlConnection),
                getResponseHeaders(urlConnection),
                getResponseCookies(urlConnection),
                urlConnection.getContentType(),
                getResponseCodeSatus(urlConnection)
        );
    }

    private static HttpURLConnection getConnectionForMethod(RequestData request) {
        switch (request.getMethod()) {
            case GET:
                return get(request);
            case PUT:
                return put(request);
            case POST:
                return post(request);
            case DELETE:
                return delete(request);
            case HEAD:
                return head(request);
            case TRACE:
                throw new IllegalStateException("Not supports the TRACE method");
            case OPTIONS:
                throw new IllegalStateException("Not supports the OPTIONS method");
            case PATCH:
                throw new IllegalStateException("Not supports the PATCH method");
            default:
                throw new IllegalStateException("Unknown HTTP method " + request.getMethod());
        }
    }

    private static HttpURLConnection get(RequestData request) {
        return baseConnection(request);
    }

    private static HttpURLConnection head(RequestData request) {
        return baseConnection(request);
    }

    private static HttpURLConnection put(RequestData request) {
        return post(request);
    }

    private static HttpURLConnection delete(RequestData request) {
        return baseConnection(request);
    }

    private static HttpURLConnection post(RequestData request) {
        HttpURLConnection urlConnection = baseConnection(request);
        List<RequestParam> params = request.getParams().stream().filter(it -> !it.isUrlParam()).collect(Collectors.toList());
        if (params.isEmpty() && Objects.isNull(request.getBody())) {
            return urlConnection;
        }
        urlConnection.setInstanceFollowRedirects(false);
        urlConnection.setDoOutput(true);
        urlConnection.setUseCaches(false);
        if (!params.isEmpty() && Objects.nonNull(request.getBody())) {
            //TODO реализовать //https://ru.wikipedia.org/wiki/Multipart/form-data
            throw new IllegalStateException("Not support multipart/form-data");
        } else if (!params.isEmpty()) {
            String charsetAsString = urlConnection.getRequestProperty("charset");
            Charset charset = Objects.nonNull(charsetAsString) ? Charset.forName(charsetAsString) : StandardCharsets.UTF_8;
            byte[] postData = RequestUtils.formatQueryParams(params, true).getBytes(charset);
            String contentType = urlConnection.getRequestProperty("content-type");
            if (Objects.isNull(contentType)) {
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            }
            if (Objects.isNull(charsetAsString)) {
                urlConnection.setRequestProperty("charset", "utf-8");
            }
            urlConnection.setRequestProperty("Content-Length", Integer.toString(postData.length));
            try (OutputStream os = urlConnection.getOutputStream(); OutputStream wr = new DataOutputStream(os)) {
                wr.write(postData);
                return urlConnection;
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        } else {
            if (Objects.isNull(urlConnection.getRequestProperty("content-type"))) {
                urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            }
            try (OutputStream os = urlConnection.getOutputStream();
                 OutputStream wr = new DataOutputStream(os);
                 InputStream is = request.getBody().get()) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = is.read(buf)) > 0) {
                    wr.write(buf, 0, len);
                }
                return urlConnection;
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private static HttpURLConnection baseConnection(RequestData request) {
        HttpURLConnection urlConnection = getHttpURLConnection(request);
        urlConnection = setRequestMethod(urlConnection, request);
        urlConnection = setRequestTimeout(urlConnection, request);
        urlConnection = setRequestHeaders(urlConnection, request);
        urlConnection = setRequestCookies(urlConnection, request);
        return urlConnection;
    }

    private static HttpURLConnection setRequestCookies(HttpURLConnection connection, RequestData request) {
//        CookieManager cookieManager = new CookieManager();
//        cookieManager.getCookieStore().removeAll();
//        for (HttpCookie cookie : request.getCookies()) {
//            cookieManager.getCookieStore().add(null, cookie);
//        }
//        CookieHandler.setDefault(cookieManager);
        connection.setRequestProperty("Cookie",
                request.getCookies().stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(";")));
        return connection;
    }

    private static HttpURLConnection setRequestHeaders(HttpURLConnection connection, RequestData request) {
        for (RequestParam header : request.getHeaders()) {
            connection.setRequestProperty(header.getKey(), header.getValue());
        }
        return connection;
    }

    private static HttpURLConnection getHttpURLConnection(final RequestData request) {
        URL url = request.asUrl();
        try {
            HttpURLConnection urlConnection;
            if (request.getScheme().equals(RequestData.Scheme.HTTPS)) {
                disableSSLCertificateChecking();
                urlConnection = (HttpsURLConnection) url.openConnection();
            } else {
                urlConnection = (HttpURLConnection) url.openConnection();
            }
            return urlConnection;
        } catch (IOException e) {
            throw new IllegalStateException("Can't open a connection to the " + url.toString(), e);
        }
    }

    private static HttpURLConnection setRequestMethod(HttpURLConnection connection, RequestData request) {
        try {
            connection.setRequestMethod(request.getMethod().name().toUpperCase());
        } catch (ProtocolException e) {
            throw new IllegalStateException("Can't set the request method", e);
        }
        return connection;
    }

    private static HttpURLConnection setRequestTimeout(HttpURLConnection connection, RequestData request) {
        try {
            connection.setReadTimeout(request.getTimeout());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Can't set the request timeout", e);
        }
        return connection;
    }


    private static void disableSSLCertificateChecking() {
        SSLContext sslContext;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, new java.security.SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new IllegalStateException("Error creating ssl context", e);
        }
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
        HttpsURLConnection.setDefaultHostnameVerifier((s, sslSession) -> true);
    }

    private static InputStream getResponseBody(HttpURLConnection connection) {
        try {
            if (getResponseCodeSatus(connection) == HttpURLConnection.HTTP_OK) {
                return connection.getInputStream();
            } else {
                return connection.getErrorStream();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Can't get the response body", e);
        }
    }

    private static List<HttpCookie> getResponseCookies(HttpURLConnection connection) {
        List<HttpCookie> responseCookies = new ArrayList<>();
        List<String> cookiesHeader = connection.getHeaderFields().get("Set-Cookie");
        if (cookiesHeader == null || cookiesHeader.isEmpty()) {
            return responseCookies;
        }
        for (String cookie : cookiesHeader) {
            responseCookies.addAll(HttpCookie.parse(cookie));
        }
        return responseCookies;
    }

    private static int getResponseCodeSatus(HttpURLConnection connection) {
        try {
            return connection.getResponseCode();
        } catch (IOException e) {
            throw new IllegalStateException("Can't get a response code", e);
        }
    }

    private static List<RequestParam> getResponseHeaders(HttpURLConnection connection) {
        List<RequestParam> responseHeaders = new ArrayList<>();
        connection.getHeaderFields().entrySet().forEach(it -> {
            if (it.getKey() != null) {
                responseHeaders.addAll(it.getValue().stream()
                        .map(value -> new RequestParam(it.getKey(), value))
                        .collect(Collectors.toList()));
            }
        });
        return responseHeaders;
    }
}
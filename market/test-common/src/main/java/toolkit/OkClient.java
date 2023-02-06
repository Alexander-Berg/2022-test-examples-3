package toolkit;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.moczul.ok2curl.CurlInterceptor;
import io.qameta.allure.okhttp3.AllureOkHttp3;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.slf4j.Logger;

@Slf4j
public class OkClient {
    private static final ThreadLocal<String> RESPONSE_THREAD_LOCAL = new ThreadLocal<>();
    private static final ThreadLocal<String> REQUEST_THREAD_LOCAL = new ThreadLocal<>();
    private static final ThreadLocal<String> CURL_THREAD_LOCAL = new ThreadLocal<>();
    private static final int TIMEOUT = 30;
    private static final ConcurrentHashMap<Pair<HttpUrl, Headers>, Pair<Response, String>> CACHE_REQUESTS =
        new ConcurrentHashMap<>();
    private static final TrustManager[] TRUST_ALL_CERTS = new TrustManager[]{
        new X509TrustManager() {
            @Override
            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) {
            }

            @Override
            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                return new java.security.cert.X509Certificate[]{};
            }
        }
    };
    private final OkHttpClient okHttpClient;
    private final Interceptor cache = chain -> {
        Pair<Response, String> responsePair = simpleCache(chain);
        Response response = responsePair.getFirst();
        return response.newBuilder()
            .body(ResponseBody.create(Objects.requireNonNull(response.body()).contentType(), responsePair.getSecond()))
            .build();
    };
    private final Interceptor logger = chain -> {
        Pair<Response, String> responseStringPair = proceedRequest(chain);
        Response response = responseStringPair.getFirst();
        return response.newBuilder()
            .body(ResponseBody.create(
                Objects.requireNonNull(response.body()).contentType(),
                responseStringPair.getSecond()
            ))
            .build();
    };

    private final Interceptor curl = new CurlInterceptor(msg -> {
        CURL_THREAD_LOCAL.set(msg);
        log.debug(msg);
    });

    private final Interceptor requiredParams = chain -> {
        Request request = chain.request();
        request = addParamToRequest(request, getRequiredParamsMap());
        return chain.proceed(request);
    };

    public OkClient(boolean cacheOn) {
        okHttpClient = getBuilder()
            .addInterceptor(cacheOn ? cache : logger)
            .addInterceptor(new AllureOkHttp3())
            .addInterceptor(curl)
            .build();
    }

    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }

    public static String getRequest() {
        return REQUEST_THREAD_LOCAL.get();
    }

    public static String getCurlRequest() {
        return CURL_THREAD_LOCAL.get();
    }

    public static String getResponse() {
        String s = RESPONSE_THREAD_LOCAL.get();
        return s == null || s.isEmpty() ? "Empty response" : s;
    }


    private static Map<String, String> getRequiredParamsMap() {
        Map<String, String> params = new HashMap<>();
        params.put("timestamp", String.valueOf(Instant.now().getEpochSecond()));
        return params;
    }

    private Request addParamToRequest(Request request, Map<String, String> params) {
        HttpUrl.Builder builderUrl = request.url().newBuilder();
        params.forEach(builderUrl::addQueryParameter);
        HttpUrl url = builderUrl.build();
        return request
                .newBuilder()
                .url(url)
                .build();
    }

    private String bodyToString(final RequestBody request) {
        try {
            final Buffer buffer = new Buffer();
            if (request != null) {
                request.writeTo(buffer);
            } else {
                return "";
            }
            return buffer.readUtf8();
        } catch (final IOException e) {
            return "did not work";
        }
    }

    private Pair<Response, String> proceedRequest(Interceptor.Chain chain) {
        try {
            Request request = chain.request();
            String requestBody = bodyToString(request.body());
            REQUEST_THREAD_LOCAL.set(request.url() + (request.method().equals("GET") ? "" : "\n" + requestBody));
            Response response = chain.proceed(request);
            String responseBody = Objects.requireNonNull(response.body()).string();
            RESPONSE_THREAD_LOCAL.set(response.headers() + "\n" + responseBody);
            log.debug(String.format("Request url is %s", request.url()));
            log.debug(String.format("Request body is %s", requestBody));
            log.debug(String.format("Response for %s  with headers %s and body response\n %s \n",
                    response.request().url(), response.headers(), responseBody));
            return new Pair<>(response, responseBody);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }


    private Pair<Response, String> simpleCache(Interceptor.Chain chain) {
        Request request = chain.request();
        if (request.method().equals("GET")) {
            Pair<HttpUrl, Headers> pairRequest = new Pair<>(request.url(), request.headers());
            if (CACHE_REQUESTS.containsKey(pairRequest)) {
                return CACHE_REQUESTS.get(pairRequest);
            }
            Pair<Response, String> pairResponse = proceedRequest(chain);
            CACHE_REQUESTS.put(pairRequest, pairResponse);
            return pairResponse;
        }
        return proceedRequest(chain);
    }

    public Response makeRequest(Request request) {
        Response response = null;
        try {
            response = okHttpClient.newCall(request).execute();
            response.close();
        } catch (Exception e) {
            log.debug(Logger.ROOT_LOGGER_NAME, e);
        }
        return response;
    }

    static String getResponseText(Response response) {
        try {
            assert response.body() != null;
            return response.body().string();
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    private OkHttpClient.Builder getBuilder() {
        try {
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, TRUST_ALL_CERTS, new java.security.SecureRandom());
            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            return new OkHttpClient.Builder()
                    .hostnameVerifier((s, sslSession) -> true)
                    .sslSocketFactory(sslSocketFactory, (X509TrustManager) TRUST_ALL_CERTS[0])
                    .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                    .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
                    .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }


}

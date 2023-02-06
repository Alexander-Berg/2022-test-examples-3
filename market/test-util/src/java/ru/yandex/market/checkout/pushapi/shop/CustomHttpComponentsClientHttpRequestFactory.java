package ru.yandex.market.checkout.pushapi.shop;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * @author msavelyev
 */
public class CustomHttpComponentsClientHttpRequestFactory extends HttpComponentsClientHttpRequestFactory
    implements InitializingBean
{

    private CloseableHttpClient httpClient;
    private TrustManager trustManager;
    private X509HostnameVerifier hostnameVerifier;
    private int readTimeout = 10000;
    private int connectTimeout = 10000;
    private int defaultMaxPerRoute = 300;
    private int maxTotal = 300;

    @Override
    public HttpClient getHttpClient() {
        if(httpClient == null) {
            return super.getHttpClient();
        } else {
            return httpClient;
        }
    }

    @Override
    public void setReadTimeout(int timeout) {
        this.readTimeout = timeout;
    }

    @Override
    public void setConnectTimeout(int timeout) {
        this.connectTimeout = timeout;
    }

    @Required
    public void setTrustManager(TrustManager trustManager) {
        this.trustManager = trustManager;
    }

    public void setHostnameVerifier(X509HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
    }

    public void setDefaultMaxPerRoute(int defaultMaxPerRoute) {
        this.defaultMaxPerRoute = defaultMaxPerRoute;
    }

    public void setMaxTotal(int maxTotal) {
        this.maxTotal = maxTotal;
    }

    @Override
    protected HttpUriRequest createHttpUriRequest(HttpMethod httpMethod, URI uri) {
        final HttpRequestBase httpRequestBase = (HttpRequestBase) super.createHttpUriRequest(httpMethod, uri);
        final RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(connectTimeout)
            .setSocketTimeout(readTimeout)
            .build();
        httpRequestBase.setConfig(requestConfig);
        return httpRequestBase;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if(trustManager == null) {
            throw new RuntimeException("trustManager must be set");
        }

        try {
            final TrustManager[] tm = new TrustManager[] { trustManager };
            final SSLContext context = SSLContext.getInstance("SSL");
            context.init(new KeyManager[0], tm, new SecureRandom());

            final SSLConnectionSocketFactory factory = SNISSLConnectionSocketFactory.create(context, hostnameVerifier);
            final PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(
                RegistryBuilder.<ConnectionSocketFactory>create()
                    .register("http", new PlainConnectionSocketFactory())
                    .register("https", factory)
                    .build()
            );
            connectionManager.setDefaultMaxPerRoute(defaultMaxPerRoute);
            connectionManager.setMaxTotal(maxTotal);

            this.httpClient = HttpClientBuilder.create().setConnectionManager(connectionManager).build();
        } catch(KeyManagementException|NoSuchAlgorithmException e) {
            throw new RuntimeException("can't instantiate CustomHttpComponentsClientHttpRequestFactory", e);
        }
    }

    private static class SNISSLConnectionSocketFactory extends SSLConnectionSocketFactory {
        public static SNISSLConnectionSocketFactory create(SSLContext context, X509HostnameVerifier hostnameVerifier) {
            if(hostnameVerifier == null) {
                return new SNISSLConnectionSocketFactory(context);
            } else {
                return new SNISSLConnectionSocketFactory(context, hostnameVerifier);
            }
        }

        public SNISSLConnectionSocketFactory(SSLContext context) {
            super(context);
        }

        public SNISSLConnectionSocketFactory(SSLContext context, X509HostnameVerifier hostnameVerifier) {
            super(context, hostnameVerifier);
        }

        @Override
        public Socket connectSocket(
            int connectTimeout, Socket socket, HttpHost host, InetSocketAddress remoteAddress,
            InetSocketAddress localAddress, HttpContext context
        ) throws IOException {
            if (socket instanceof SSLSocket) {
                try {
                    PropertyUtils.setProperty(socket, "host", host.getHostName());
                } catch (NoSuchMethodException|IllegalAccessException|InvocationTargetException ex) {
                    throw new RuntimeException("can't set host on SSLSocket", ex);
                }
            }
            return super.connectSocket(connectTimeout, socket, host, remoteAddress, localAddress, context);
        }
    }
}

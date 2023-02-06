package org.apache.http.impl.client;

import java.io.Closeable;
import java.util.List;

import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.Lookup;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.impl.execchain.ClientExecChain;

public class HackedInternalHttpClient extends InternalHttpClient {

    @SuppressWarnings("checkstyle:ParameterNumber")
    public HackedInternalHttpClient(ClientExecChain execChain, HttpClientConnectionManager connManager,
                                    HttpRoutePlanner routePlanner, Lookup<CookieSpecProvider> cookieSpecRegistry,
                                    Lookup<AuthSchemeProvider> authSchemeRegistry, CookieStore cookieStore,
                                    CredentialsProvider credentialsProvider, RequestConfig defaultConfig,
                                    List<Closeable> closeables) {
        super(execChain, connManager, routePlanner, cookieSpecRegistry, authSchemeRegistry, cookieStore,
                credentialsProvider, defaultConfig, closeables);
    }
}

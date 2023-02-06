package ru.yandex.market.checkout.pushapi.shop;

import org.apache.http.conn.ssl.X509HostnameVerifier;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.security.cert.X509Certificate;

/**
 * @author msavelyev
 */
public class NaiveHostVerifier implements X509HostnameVerifier {

    @Override
    public void verify(String host, SSLSocket ssl) throws IOException {
    }

    @Override
    public void verify(String host, X509Certificate cert) throws SSLException {
    }

    @Override
    public void verify(String host, String[] cns, String[] subjectAlts) throws SSLException {
    }

    @Override
    public boolean verify(String s, SSLSession sslSession) {
        return false;
    }
}

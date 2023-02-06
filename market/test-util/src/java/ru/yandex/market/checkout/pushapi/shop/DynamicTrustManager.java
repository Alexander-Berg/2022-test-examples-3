package ru.yandex.market.checkout.pushapi.shop;

import org.apache.commons.codec.binary.Hex;
import org.apache.log4j.Logger;
import ru.yandex.common.util.digest.DigestUtils;
import ru.yandex.market.checkout.pushapi.client.entity.shop.Settings;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

/**
 * @author msavelyev
 */
public class DynamicTrustManager implements X509TrustManager {

    private static final Logger log = Logger.getLogger(DynamicTrustManager.class);

    private X509TrustManager delegate;
    private static final char[] defaultPass = "changeit".toCharArray();

    public DynamicTrustManager() {
        this(null);
    }

    public DynamicTrustManager(String customTrustStorePath) {
        try {
            final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            );
            final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

            final String javaHome = System.getProperty("java.home");
            keyStore.load(new FileInputStream(new File(javaHome + "/lib/security/cacerts")), defaultPass);
            if(customTrustStorePath != null) {
                keyStore.load(getClass().getResourceAsStream(customTrustStorePath), defaultPass);
            }
            trustManagerFactory.init(keyStore);

            delegate = findX509TrustManager(trustManagerFactory);
        } catch(Exception e) {
            throw new RuntimeException("can't create DynamicTrustManager", e);
        }

        if(delegate == null) {
            throw new RuntimeException("X509TrustManager not found");
        }
    }

    public void checkClientTrusted(X509Certificate[] cert, String authType) throws CertificateException {
        delegate.checkClientTrusted(cert, authType);
    }

    public void checkServerTrusted(X509Certificate[] cert, String authType) throws CertificateException {
        try {
            delegate.checkServerTrusted(cert, authType);
        } catch(CertificateException e) {
            if(cert.length < 1) {
                throw new CertificateException("zero certificates found");
            }
            final X509Certificate c = cert[0];

            final Settings settings = ThreadLocalSettings.getSettings();
            final byte[] expected = settings.getFingerprint();

            final byte[] actual = DigestUtils.sha1(c.getEncoded());

            log.info(
                "Checking fingerprints. Expected: "
                    + safeHex(expected) + "."
                    + " Actual: "
                    + safeHex(actual)
            );

            if(!Arrays.equals(expected, actual)) {
                throw new CertificateException(
                    "expected and actual fingerprints aren't the same. Expected: "
                    + safeHex(expected) + "."
                    + " Actual: "
                    + safeHex(actual)
                );
            }
        }
    }

    public X509Certificate[] getAcceptedIssuers() {
        return delegate.getAcceptedIssuers();
    }

    private String safeHex(byte[] bytes) {
        if(bytes == null) {
            return null;
        } else {
            return Hex.encodeHexString(bytes);
        }
    }

    private X509TrustManager findX509TrustManager(TrustManagerFactory trustManagerFactory) {
        for(TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
            if(trustManager instanceof X509TrustManager) {
                return (X509TrustManager) trustManager;
            }
        }

        return null;
    }

}

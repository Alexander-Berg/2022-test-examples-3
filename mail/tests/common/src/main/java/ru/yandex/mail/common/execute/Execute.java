package ru.yandex.mail.common.execute;

import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.net.URI;

public class Execute extends ExternalResource {
    private static String LOCALHOST = "localhost";

    private String username = "";
    private String host = "";
    private String key = "";

    private Shell shell;

    public Execute() { }

    public Execute withUsername(String username) {
        this.username = username;
        return this;
    }

    public Execute withHost(String host) {
        this.host = URI.create(host).getHost();
        return this;
    }

    public Execute withKey(String key) {
        this.key = key;
        return this;
    }

    public Shell shell() throws IOException {
        if (host.contains(LOCALHOST)) {
            return new Local();
        } else {
            return new RemoteSsh(username, host, key);
        }
    }
}

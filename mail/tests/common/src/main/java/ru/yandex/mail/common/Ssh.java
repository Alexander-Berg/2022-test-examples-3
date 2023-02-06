package ru.yandex.mail.common;

import ch.ethz.ssh2.Connection;
import org.apache.log4j.Logger;
import org.junit.rules.ExternalResource;

import java.io.IOException;
import java.net.URI;

import static java.lang.String.format;

public class Ssh extends ExternalResource {
    private Connection getSSHConnection(String testServer, String login, String authKey) {
        Connection conn;
        logger.trace("Try to connect by ssh to " + testServer);
        conn = new Connection(testServer);
        try {
            conn.connect();
            logger.trace("Authentication with public key...");
            boolean isAuthenticated = conn.authenticateWithPublicKey(login, authKey.toCharArray(), "");

            if (!isAuthenticated) {
                throw new IOException(format("Authentication failed (login: %s)", login));
            }
        } catch (IOException exc) {
            throw new RuntimeException("Connection to " + testServer + " failed: " + exc.getMessage(), exc);
        }
        logger.trace("Connection succeeded");
        return conn;
    }

    private String username = "";
    private String host = "";
    private String key = "";

    private Connection conn;
    private Logger logger = Logger.getLogger(this.getClass());

    public Ssh() { }

    public Ssh withUsername(String username) {
        this.username = username;
        return this;
    }

    public Ssh withHost(String host) {
        this.host = URI.create(host).getHost();
        return this;
    }

    public Ssh withKey(String key) {
        this.key = key;
        return this;
    }

    @Override
    protected void before() {
        auth();
    }

    @Override
    protected void after() {
        close();
    }

    public Connection conn() {
        return conn;
    }

    public void close() {
        conn.close();
    }

    public Ssh auth() {
        conn = getSSHConnection(host, username, key);

        return this;
    }
}

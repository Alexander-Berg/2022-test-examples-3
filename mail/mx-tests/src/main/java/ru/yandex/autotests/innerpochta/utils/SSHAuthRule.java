package ru.yandex.autotests.innerpochta.utils;

import java.net.URI;

import ch.ethz.ssh2.Connection;
import org.junit.rules.ExternalResource;

public class SSHAuthRule extends ExternalResource {
    private String login = "robot-gerrit";
    private String host;
    private Ssh ssh;
    private String privateKey;

    private SSHAuthRule(String host, String privateKey) {
        this.host = host;
        this.privateKey = privateKey;
    }

    private SSHAuthRule(URI uri, String privateKey) {
        this.host = uri.getHost();
        this.privateKey = privateKey;
    }

    public SSHAuthRule withLogin(String login) {
        this.login = login;
        return this;
    }

    public static SSHAuthRule sshOn(String host, String privateKey) {
        return new SSHAuthRule(host, privateKey);
    }

    public static SSHAuthRule sshOn(URI uri, String privateKey) {
        return new SSHAuthRule(uri, privateKey);
    }

    public SSHAuthRule authenticate() {
        this.ssh = Ssh.withKeyOn(this.host, this.privateKey).withLogin(this.login).auth();
        return this;
    }

    protected void before() {
        this.authenticate();
    }

    protected void after() {
        this.ssh.close();
    }

    public Ssh ssh() {
        return this.ssh;
    }

    public Connection conn() {
        return this.ssh.conn();
    }
}

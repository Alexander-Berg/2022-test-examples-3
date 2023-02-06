package ru.yandex.autotests.testpers.ssh;

import org.junit.rules.ExternalResource;

import java.net.URI;

/**
 * User: lanwen
 * Date: 20.04.14
 * Time: 3:20
 */
public class SSHAuthRule extends ExternalResource {

    private String login = Ssh.DEFAULT_SSH_LOGIN;

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

    public void authenticate() {
        ssh = Ssh.withKeyOn(host, privateKey).withLogin(login).auth();
    }

    @Override
    protected void before() {
        authenticate();
    }


    @Override
    protected void after() {
        ssh.close();
    }


    public Ssh ssh() {
        return ssh;
    }
}

package ru.yandex.msearch.proxy.suggest.utils;

public class MailUser {
    private final long prefix;
    private final String mdb;
    private final String email;

    public MailUser(final long prefix, final String mdb, final String email) {
        this.prefix = prefix;
        this.mdb = mdb;
        this.email = email;
    }

    public long prefix() {
        return prefix;
    }

    public String mdb() {
        return mdb;
    }

    public String email() {
        return email;
    }
}

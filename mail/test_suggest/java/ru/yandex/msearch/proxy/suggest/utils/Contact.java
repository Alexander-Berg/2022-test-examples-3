package ru.yandex.msearch.proxy.suggest.utils;

import java.util.Arrays;

/**
 * User: stassiak
 * Date: 19.05.14
 */
/*
* {
            "id": -2,
            "email": "yantester@yandex.ru",
            "name": "\u043A\u0440\u0430\u0441\u043A\u0430",
            "phones": [

            ],
            "t": "1396856750",
            "u": -1
        }
* */
public class Contact {
    int id;
    String email;
    String name;
    String[] phones;
    String t;
    int u;

    public Contact(final String email, final String name) {
        this(
            -2,
            email,
            name,
            new String[0],
            Long.toString(System.currentTimeMillis() / 1000),
            -1);
    }

    public Contact(
        final int id,
        final String email,
        final String name,
        final String[] phones,
        final String t,
        final int u)
    {
        this.id = id;
        this.email = email;
        this.name = name;
        this.phones = phones;
        this.t = t;
        this.u = u;
    }

    public int getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String[] getPhones() {
        return phones;
    }

    public Long getT() {
        return Long.valueOf(t);
    }

    public String getTStr() {
        return t;
    }

    public int getU() {
        return u;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Contact)) return false;

        Contact contact = (Contact) o;

        if (id != contact.id) return false;
        if (u != contact.u) return false;
        if (!email.equals(contact.email)) return false;
        if (!name.equals(contact.name)) return false;
        if (!Arrays.equals(phones, contact.phones)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + email.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + Arrays.hashCode(phones);
        result = 31 * result + u;
        return result;
    }

    @Override
    public String toString() {
        return "Contact{" +
            "id=" + id +
            ", email='" + email + '\'' +
            ", name='" + name + '\'' +
            ", phones=" + Arrays.toString(phones) +
            ", t='" + Long.toString(Long.parseLong(t) / 100) + '\'' +
            ", u='" + u + '\'' +
            '}';
    }
}
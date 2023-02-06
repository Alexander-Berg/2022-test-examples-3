package ru.yandex.personal.mail.search.metrics.scraper.mocks;

import com.google.common.base.Objects;

public class SimpleAccount {
    private final String name;
    private final String password;

    public SimpleAccount(String name, String password) {
        this.name = name;
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SimpleAccount that = (SimpleAccount) o;
        return Objects.equal(name, that.name) &&
                Objects.equal(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, password);
    }
}

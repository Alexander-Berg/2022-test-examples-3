package ru.yandex.market.core.balance;

import java.util.Objects;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import ru.yandex.market.core.balance.model.FullClientInfo;

/**
 * Hamcrest матчер для модели {@link FullClientInfo}.
 */
public final class FullClientInfoMatcher extends TypeSafeMatcher<FullClientInfo> {

    private final String name;
    private final String phone;
    private final String email;

    public FullClientInfoMatcher(String name, String phone, String email) {
        this.name = name;
        this.phone = phone;
        this.email = email;
    }

    @Override
    protected boolean matchesSafely(FullClientInfo item) {
        return Objects.equals(name, item.getName())
                && Objects.equals(phone, item.getPhone())
                && Objects.equals(email, item.getEmail());
    }

    @Override
    protected void describeMismatchSafely(FullClientInfo item, Description mismatchDescription) {
        mismatchDescription.appendText("was ")
                .appendText(toString(item.getName(), item.getPhone(), item.getEmail()));
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(toString(name, phone, email));
    }

    private String toString(String name, String phone, String email) {
        return "{" +
                "\"name\": " + name + ", " +
                "\"phone\": " + phone + ", " +
                "\"email\": " + email +
                "}";
    }
}

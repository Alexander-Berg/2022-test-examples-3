package ru.yandex.market.api.matchers;

import com.google.common.base.MoreObjects;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.internal.blackbox.data.LinkedAccount;

/**
 * @author dimkarp93
 */
public class LinkedAccountMatcher {
    public static Matcher<LinkedAccount> noLinkedAccount() {
        return linkedAccount(-1L, false);
    }

    public static Matcher<LinkedAccount> linkedAccount(long puid, boolean isLinked) {
        return Matchers.allOf(
            ApiMatchers.map(
                    LinkedAccount::getPuid,
                    "puid",
                    Matchers.is(puid),
                    LinkedAccountMatcher::toStr
            ),
            ApiMatchers.map(
                    LinkedAccount::isLinked,
                    "islinked",
                    Matchers.is(isLinked),
                    LinkedAccountMatcher::toStr
            )
        );
    }

    public static String toStr(LinkedAccount account) {
        if (null == account) {
            return "null";
        }
        return MoreObjects.toStringHelper(LinkedAccount.class)
                .add("puid", account.getPuid())
                .add("islinked", account.isLinked())
                .toString();
    }
}

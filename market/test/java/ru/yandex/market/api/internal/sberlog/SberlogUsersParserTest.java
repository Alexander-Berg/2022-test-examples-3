package ru.yandex.market.api.internal.sberlog;

import java.util.List;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.ApiMatchers;
import ru.yandex.market.api.common.Result;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.blackbox.data.LinkedAccount;
import ru.yandex.market.api.internal.blackbox.data.OauthUser;
import ru.yandex.market.api.matchers.LinkedAccountMatcher;
import ru.yandex.market.api.util.ResourceHelpers;

/**
 * @authror dimkarp93
 */
public class SberlogUsersParserTest extends UnitTestBase {
    private final SberlogUsersParser parser = new SberlogUsersParser();

    @Test
    public void parseUsers() {
        Result<List<OauthUser>, SberlogStatus> result = parse("sberlog_users.json");
        Assert.assertTrue(result.isOk());

        List<OauthUser> users = result.getValue();

        Assert.assertThat(users, Matchers.hasSize(2));

        OauthUser user1 = users.get(0);
        Assert.assertEquals(1152921504667398196L, user1.getUid());
        Assert.assertEquals("Василий", user1.getFirstName());
        Assert.assertEquals("Пупкин", user1.getLastName());
        Assert.assertThat(
                user1.getPhones(),
                Matchers.containsInAnyOrder(
                        "81233213344",
                        "+79095612567"
                )
        );
        Assert.assertEquals("user@yandex.ru", user1.getEmail());

        OauthUser user2 = users.get(1);
        Assert.assertEquals(1152921504667398195L, user2.getUid());
        Assert.assertEquals("Елена", user2.getFirstName());
        Assert.assertEquals("Иванова", user2.getLastName());
        Assert.assertThat(
                user2.getPhones(),
                Matchers.contains("+79095612435")
        );
        Assert.assertEquals("user2@yandex.ru", user2.getEmail());
    }

    @Test
    public void emptyPhones() {
        Result<List<OauthUser>, SberlogStatus> result = parse("sberlog_users_empty_phones.json");
        Assert.assertTrue(result.isOk());

        List<OauthUser> users = result.getValue();

        Assert.assertThat(users, Matchers.hasSize(2));

        OauthUser user1 = users.get(0);
        Assert.assertThat(user1.getPhones(), Matchers.empty());

        OauthUser user2 = users.get(1);
        Assert.assertThat(user2.getPhones(), Matchers.empty());
    }

    @Test
    public void error() {
        Result<List<OauthUser>, SberlogStatus> result = parse("sberlog_users_error.json");
        Assert.assertTrue(result.hasError());

        SberlogStatus sberlogStatus = result.getError();

        Assert.assertThat(sberlogStatus.getCode(), Matchers.is("0"));
        Assert.assertThat(sberlogStatus.getText(), Matchers.is("blabla"));
    }

    @Test
    public void empty() {
        Result<List<OauthUser>, SberlogStatus> result = parse("sberlog_empty.json");
        Assert.assertFalse(result.hasError());
        Assert.assertNull(result.getValue());
    }

    @Test
    public void emptyArray() {
        Result<List<OauthUser>, SberlogStatus> result = parse("sberlog_empty_array.json");
        Assert.assertFalse(result.hasError());
        Assert.assertNull(result.getValue());
    }

    @Test
    public void incorrectUsers() {
        Result<List<OauthUser>, SberlogStatus> result = parse("sberlog_users_incorrect.json");
        Assert.assertFalse(result.hasError());
        OauthUser user = result.getValue().get(0);
        Assert.assertThat(user.getErrorCode(), Matchers.is("1"));
        Assert.assertThat(user.getErrorMessage(), Matchers.is("sberlog_error"));
    }

    @Test
    public void linkedAccounts() {
        Result<List<OauthUser>, SberlogStatus> result = parse("sberlog_users_with_linked.json");
        Assert.assertFalse(result.hasError());

        Matcher<? super Iterable<OauthUser>> matchers = Matchers.contains(
                matcher(LinkedAccountMatcher.linkedAccount(12L, true)),
                matcher(LinkedAccountMatcher.noLinkedAccount()),
                matcher(LinkedAccountMatcher.noLinkedAccount()),
                matcher(LinkedAccountMatcher.noLinkedAccount()),
                matcher(LinkedAccountMatcher.noLinkedAccount()),
                matcher(LinkedAccountMatcher.linkedAccount(789L, true)),
                matcher(LinkedAccountMatcher.linkedAccount(66L, true))
        );

        Assert.assertThat(result.getValue(), matchers);
    }

    private static Matcher<OauthUser> matcher(Matcher<LinkedAccount> matcher) {
        return ApiMatchers.map(
                OauthUser::getLinkedAccount,
                "linkedAccount",
                matcher
        );
    }


    public Result<List<OauthUser>, SberlogStatus> parse(String filename) {
        byte[] data = ResourceHelpers.getResource(filename);
        return parser.parse(data);
    }
}

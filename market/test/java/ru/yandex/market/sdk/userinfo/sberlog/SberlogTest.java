package ru.yandex.market.sdk.userinfo.sberlog;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.sdk.userinfo.domain.LinkedAccount;
import ru.yandex.market.sdk.userinfo.domain.SberlogInfo;
import ru.yandex.market.sdk.userinfo.domain.Sex;
import ru.yandex.market.sdk.userinfo.domain.Uid;
import ru.yandex.market.sdk.userinfo.matcher.OptionalMatcher;
import ru.yandex.market.sdk.userinfo.matcher.dsl.SberlogInfoDsl;
import ru.yandex.market.sdk.userinfo.matcher.dsl.StatusDsl;
import ru.yandex.market.sdk.userinfo.matcher.dsl.UidDsl;
import ru.yandex.market.sdk.userinfo.matcher.dsl.UserInfoDsl;
import ru.yandex.market.sdk.userinfo.serialize.SberlogDeserializer;
import ru.yandex.market.sdk.userinfo.serialize.json.ObjectMappers;

/**
 * @authror dimkarp93
 */
public class SberlogTest {

    private final SberlogDeserializer deserializer = new SberlogDeserializer(ObjectMappers.json());

    @Test
    public void twoUsersFullParse() {
        List<SberlogInfo> infos = parse("sberlog_two_users_full_parse.json");

        UserInfoDsl user1 = new UserInfoDsl()
                .setSex(Sex.MALE)
                .setRawSex("1")
                .setFirstName("Сидор")
                .setLastName("Петров")
                .setBirthDate(LocalDate.of(2010, 1, 10))
                .setEmails(Matchers.containsInAnyOrder("cip@yandex.ru", "cip@ya.ru"))
                .setUid(UidDsl.asUid(Uid.ofSberlog(1141L)).toMatcher())
                .setCAPIDisplayName("Петров Сидор")
                .setFio("Петров Сидор")
                .setFullFio("Петров Сидор Иванович")
                .setPhones(Matchers.containsInAnyOrder("81233213344", "+79095612567"))
                .setAvatar("https://avatars.mds.yandex.net/get-yapic/0/0-0/1", "1");

        UserInfoDsl user2 = new UserInfoDsl()
                .setSex(Sex.FEMALE)
                .setRawSex("2")
                .setFirstName("Елена")
                .setLastName("Сидорова")
                .setBirthDate(LocalDate.of(1666, 6, 13))
                .setEmails(Matchers.containsInAnyOrder("epc666@yandex.ru", "epc666@ya.ru"))
                .setUid(UidDsl.asUid(Uid.ofSberlog(1142L)).toMatcher())
                .setCAPIDisplayName("Сидорова Елена")
                .setFio("Сидорова Елена")
                .setFullFio("Сидорова Елена Петровна")
                .setPhones(Matchers.emptyIterable())
                .setAvatar("https://avatars.mds.yandex.net/get-yapic/0/0-0/1", "1");


        Assert.assertThat(
                infos,
                Matchers.containsInAnyOrder(
                        new SberlogInfoDsl()
                                .setFatherName(OptionalMatcher.of("Иванович"))
                                .setStatus(new StatusDsl().setCode(0).setText("ok").toMatcher())
                                .setUserInfo(user1)
                                .toMatcher(),
                        new SberlogInfoDsl()
                                .setFatherName(OptionalMatcher.of("Петровна"))
                                .setStatus(new StatusDsl().setCode(0).setText("ok").toMatcher())
                                .setUserInfo(user2)
                                .toMatcher()
                )
        );
    }

    @Test
    public void onlyRequiredParse() {
        List<SberlogInfo> infos = parse("sberlog_required_parse.json");

        UserInfoDsl user = new UserInfoDsl()
                .setUid(UidDsl.asUid(Uid.ofSberlog(666L)).toMatcher())
                .setEmails(Matchers.emptyIterable())
                .setSex(OptionalMatcher.not())
                .setRawSex("0")
                .setBirthDate(OptionalMatcher.not())
                .setFirstName(OptionalMatcher.not())
                .setLastName(OptionalMatcher.not())
                .setCAPIDisplayName(OptionalMatcher.not());

        Assert.assertThat(
                infos,
                Matchers.contains(
                        new SberlogInfoDsl()
                                .setFatherName(OptionalMatcher.not())
                                .setStatus(new StatusDsl().setCode(1).setText("error").toMatcher())
                                .setUserInfo(user)
                                .toMatcher()
                )
        );
    }

    @Test
    public void emptyArray() {
        List<SberlogInfo> infos = parse("sberlog_empty.json");

        Assert.assertThat(infos, Matchers.empty());
    }

    private List<SberlogInfo> parse(String filename) {
        InputStream stream = SberlogTest.class.getResourceAsStream(filename);
        return deserializer.deserializeSberlogInfos(stream);
    }

    private LinkedAccount parseIsLinked(String filename) {
        InputStream stream = SberlogTest.class.getResourceAsStream(filename);
        return deserializer.deserializeIsLinked(stream);
    }

    @Test
    public void linkedAccount() {
        LinkedAccount res = parseIsLinked("linked.json");
        Assert.assertTrue(res.isLinked());
        Assert.assertNotNull(res.getMarketid());
        Assert.assertEquals(1152921504667398196L, res.getMarketid().longValue());
        Assert.assertEquals(22595528234623423L, res.getPuid());
    }

    @Test
    public void notLinkedAccount() {
        LinkedAccount res = parseIsLinked("not-linked.json");
        Assert.assertFalse(res.isLinked());
        Assert.assertNull(res.getMarketid());
        Assert.assertEquals(22595528234623423L, res.getPuid());
    }

}

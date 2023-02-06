package ru.yandex.market.sdk.userinfo.passport;

import java.io.InputStream;
import java.time.LocalDate;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.sdk.userinfo.domain.PassportResponse;
import ru.yandex.market.sdk.userinfo.domain.Sex;
import ru.yandex.market.sdk.userinfo.domain.Uid;
import ru.yandex.market.sdk.userinfo.matcher.MapMatcher;
import ru.yandex.market.sdk.userinfo.matcher.OptionalMatcher;
import ru.yandex.market.sdk.userinfo.matcher.dsl.ExceptionDsl;
import ru.yandex.market.sdk.userinfo.matcher.dsl.PassportInfoDsl;
import ru.yandex.market.sdk.userinfo.matcher.dsl.UidDsl;
import ru.yandex.market.sdk.userinfo.matcher.dsl.UserInfoDsl;
import ru.yandex.market.sdk.userinfo.serialize.PassportDeserilizer;
import ru.yandex.market.sdk.userinfo.serialize.json.ObjectMappers;

/**
 * @authror dimkarp93
 */
public class PassportTest {
    private final PassportDeserilizer deserializer = new PassportDeserilizer(ObjectMappers.json());

    @Test
    public void twoUsersFullParse() {
        PassportResponse response = parse("passport_two_users_full_parse.json");

        UserInfoDsl user1 = new UserInfoDsl()
                .setUid(UidDsl.asUid(Uid.ofPassport(4022479524L)).toMatcher())
                .setSex(Sex.FEMALE)
                .setRawSex("2")
                .setBirthDate(LocalDate.of(1990, 6, 7))
                .setFirstName("Ирина")
                .setLastName("Петрова")
                .setEmails(
                        Matchers.containsInAnyOrder(
                                "user-info-sdk-tst1-dimkarp93@yandex.ru",
                                "user-info-sdk-tst1-dimkarp93@yandex.by",
                                "user-info-sdk-tst1-dimkarp93@yandex.kz",
                                "user-info-sdk-tst1-dimkarp93@yandex.ua",
                                "user-info-sdk-tst1-dimkarp93@yandex.com",
                                "user-info-sdk-tst1-dimkarp93@ya.ru"
                        )
                )
                .setFio("Петрова Ирина")
                .setFullFio("Петрова Ирина")
                .setCAPIDisplayName("Петрова Ирина")
                .setAvatar(OptionalMatcher.not(), "")
                .setPhones(Matchers.contains("71111111111"));
        UserInfoDsl user2 = new UserInfoDsl()
                .setUid(UidDsl.asUid(Uid.ofPassport(4022479718L)).toMatcher())
                .setSex(Sex.MALE)
                .setRawSex("1")
                .setBirthDate(LocalDate.of(1930, 2, 21))
                .setFirstName("Иван")
                .setLastName("Сидоров")
                .setEmails(
                        Matchers.containsInAnyOrder(
                                "user-info-sdk-tst2-dimkarp93@yandex.ru",
                                "user-info-sdk-tst2-dimkarp93@yandex.by",
                                "user-info-sdk-tst2-dimkarp93@yandex.kz",
                                "user-info-sdk-tst2-dimkarp93@yandex.ua",
                                "user-info-sdk-tst2-dimkarp93@yandex.com",
                                "user-info-sdk-tst2-dimkarp93@ya.ru"
                        )
                )
                .setFio("Сидоров Иван")
                .setFullFio("Сидоров Иван")
                .setCAPIDisplayName("Сидоров Иван")
                .setAvatar(
                        OptionalMatcher.of("https://avatars.mds.yandex" +
                                ".net/get-yapic/1450/AcrohaOtlGG98Vh2XcvtCRK8Es-1/islands-200"),
                        "islands-200"
                )
                .setPhones(Matchers.containsInAnyOrder("+72222222222", "+73333333333"));

        Assert.assertThat(
                response.getUsers(),
                Matchers.containsInAnyOrder(
                        new PassportInfoDsl()
                                .setKarma(Matchers.is(5))
                                .setKarmaStatus(Matchers.is(0))
                                .setDisplayName(OptionalMatcher.of("user-info-sdk-tst1-dimkarp93"))
                                .setDbFields(
                                        new MapMatcher<>(
                                                Matchers.hasEntry("userinfo.birth_date.uid", "1990-06-07"),
                                                Matchers.hasEntry("userinfo.firstname.uid", "Ирина"),
                                                Matchers.hasEntry("userinfo.lastname.uid", "Петрова")
                                        )
                                )
                                .setAttributes(
                                        new MapMatcher<>(
                                                Matchers.hasEntry("1007", "Петрова Ирина")
                                        )
                                )
                                .isYaPlus(true)
                                .isYaPlusRaw("1")
                                .setStaffLogin("petir")
                                .setStaffLoginRaw("petir")
                                .setLocaions(
                                        Matchers.contains(
                                                "{\"Домашний\":\"Ул. Сезам д. 10 кв. 15\",\"Рабочий\":\"Республика " +
                                                        "Мордор, Гиблая область, гора смерти, пещера 4\"}"
                                        )
                                )
                                .setUserInfo(user1)
                                .toMatcher(),
                        new PassportInfoDsl()
                                .setKarma(Matchers.is(12))
                                .setKarmaStatus(Matchers.is(1))
                                .setDisplayName(OptionalMatcher.of("user-info-sdk-tst2-dimkarp93"))
                                .setDbFields(
                                        new MapMatcher<>(
                                                Matchers.hasEntry("userinfo.birth_date.uid", "1930-02-21"),
                                                Matchers.hasEntry("userinfo.firstname.uid", "Иван"),
                                                Matchers.hasEntry("userinfo.lastname.uid", "Сидоров")
                                        )
                                )
                                .setAttributes(
                                        new MapMatcher<>(
                                                Matchers.hasEntry("1007", "Сидоров Иван")
                                        )
                                )
                                .isYaPlus(false)
                                .isYaPlusRaw("0")
                                .setStaffLogin(OptionalMatcher.not())
                                .setStaffLoginRaw("")
                                .setLocaions(
                                        Matchers.contains(
                                                "{\"addresses\":[{\"firstname\":\"Василий\",\"phone-extra\":\"\",\"street\":\"Льва Толстого\",\"floor\":\"\",\"suite\":\"\",\"lastname\":\"Пупкин\",\"email\":\"help@yandex.ru\",\"fathersname\":\"\",\"city\":\"Яндекс (Москва)\",\"id\":\"132030410312558\",\"metro\":\"Парк Культуры\",\"intercom\":\"\",\"country\":\"Россия\",\"cargolift\":\"yes\",\"phone\":\"+7 495 739-70-00\",\"entrance\":\"2\",\"zip\":\"\",\"comment\":\"\",\"building\":\"16\",\"title\":\"Яндекс\",\"flat\":\"1\"}]}"
                                        )
                                )
                                .setUserInfo(user2)
                                .toMatcher()
                )
        );
        Assert.assertThat(response.getException(), OptionalMatcher.not());
    }

    @Test
    public void empty() {
        PassportResponse response = parse("passport_empty.json");
        Assert.assertThat(response, Matchers.notNullValue());
        Assert.assertThat(response.getUsers(), Matchers.nullValue());
        Assert.assertThat(response.getException(), OptionalMatcher.not());
    }

    @Test
    public void emptyUsers() {
        PassportResponse response = parse("passport_empty_users.json");
        Assert.assertThat(response, Matchers.notNullValue());
        Assert.assertThat(response.getUsers(), Matchers.empty());
        Assert.assertThat(response.getException(), OptionalMatcher.not());
    }

    @Test
    public void notFound() {
        PassportResponse response = parse("passport_not_found.json");
        Assert.assertThat(
                response.getUsers(),
                Matchers.contains(
                        new PassportInfoDsl()
                                .setKarma(Matchers.is(0))
                                .setKarmaStatus(Matchers.is(0))
                                .toMatcher()
                )
        );
    }

    @Test
    public void exception() {
        PassportResponse response = parse("passport_error.json");
        Assert.assertThat(
                response.getException(),
                OptionalMatcher.existAnd(
                        new ExceptionDsl()
                                .setId(2)
                                .setValue("INVALID_PARAMS")
                                .toMatcher()
                )
        );
    }

    @Test
    public void fioFromDbFields() {
        PassportResponse response = parse("passport_fio_from_dbfields_parse.json");
        Assert.assertThat(
                response.getUsers(),
                Matchers.contains(
                        new PassportInfoDsl()
                                .setLogin("login_user-info-sdk-tst2-dimkarp93")
                                .setDisplayName("user-info-sdk-tst2-dimkarp93")
                                .setUserInfo(
                                        new UserInfoDsl()
                                                .setFio("Посторонним В")
                                                .setFullFio("Посторонним В")
                                                .setFirstName("Иван")
                                                .setLastName("Сидоров")
                                                .setCAPIDisplayName("Посторонним В")
                                                .setFio(OptionalMatcher.not())
                                                .setFullFio(OptionalMatcher.not())
                                )
                                .toMatcher()
                )
        );
    }
    @Test
    public void fioFromAttrPrefer() {
        PassportResponse response = parse("passport_fio_from_attr_prefer_parse.json");
        Assert.assertThat(
                response.getUsers(),
                Matchers.contains(
                        new PassportInfoDsl()
                                .setLogin("login_user-info-sdk-tst2-dimkarp93")
                                .setDisplayName("user-info-sdk-tst2-dimkarp93")
                                .setUserInfo(
                                        new UserInfoDsl()
                                                .setFio("Ошибков Ошибка")
                                                .setFullFio("Ошибков Ошибка")
                                                .setFirstName("Иван")
                                                .setLastName("Сидоров")
                                                .setCAPIDisplayName("Ошибков Ошибка")
                                                .setFio(OptionalMatcher.not())
                                                .setFullFio(OptionalMatcher.not())
                                )
                                .toMatcher()
                )
        );
    }


    @Test
    public void firstAndLastNameFromAttrPrefer() {
        PassportResponse response = parse("passport_firstname_and_lastname_from_attr_prefer_parse.json");
        Assert.assertThat(
                response.getUsers(),
                Matchers.contains(
                        new PassportInfoDsl()
                                .setLogin("login_user-info-sdk-tst2-dimkarp93")
                                .setDisplayName("user-info-sdk-tst2-dimkarp93")
                                .setUserInfo(
                                        new UserInfoDsl()
                                                .setFio("Посторонним В")
                                                .setFullFio("Посторонним В")
                                                .setFirstName("Тест")
                                                .setLastName("Тестов")
                                                .setCAPIDisplayName("Посторонним В")
                                                .setFio(OptionalMatcher.not())
                                                .setFullFio(OptionalMatcher.not())
                                )
                                .toMatcher()
                )
        );
    }
    @Test
    public void firstAndLastNameFromDbFieldsPrefer() {
        PassportResponse response = parse("passport_firstname_and_lastname_from_db_fields_parse.json");
        Assert.assertThat(
                response.getUsers(),
                Matchers.contains(
                        new PassportInfoDsl()
                                .setLogin("login_user-info-sdk-tst2-dimkarp93")
                                .setDisplayName("user-info-sdk-tst2-dimkarp93")
                                .setUserInfo(
                                        new UserInfoDsl()
                                                .setFio("Посторонним В")
                                                .setFullFio("Посторонним В")
                                                .setFirstName("Иван")
                                                .setLastName("Сидоров")
                                                .setCAPIDisplayName("Посторонним В")
                                                .setFio(OptionalMatcher.not())
                                                .setFullFio(OptionalMatcher.not())
                                )
                                .toMatcher()
                )
        );
    }


    @Test
    public void publicNameFromDisplayName() {
        PassportResponse response = parse("passport_public_name_from_display_name.json");
        Assert.assertThat(
            response.getUsers(),
            Matchers.contains(
                new PassportInfoDsl()
                    .setUserInfo(
                        new UserInfoDsl()
                            .setPublicName("Козьма П.")
                    )
                    .toMatcher()
            )
        );
    }

    @Test
    public void firstNameNotExistsParse() {
        PassportResponse response = parse("passport_first_name_not_exists_parse.json");
        Assert.assertThat(
                response.getUsers(),
                Matchers.contains(
                        new PassportInfoDsl()
                                .setLogin("login_user-info-sdk-tst2-dimkarp93")
                                .setDisplayName("user-info-sdk-tst2-dimkarp93")
                                .setUserInfo(
                                        new UserInfoDsl()
                                                .setFirstName(OptionalMatcher.not())
                                                .setLastName("Сидоров")
                                                .setCAPIDisplayName("user-info-sdk-tst2-dimkarp93")
                                                .setFio(OptionalMatcher.not())
                                                .setFullFio(OptionalMatcher.not())
                                )
                                .toMatcher()
                )
        );
    }

    @Test
    public void lastNameNotExistsParse() {
        PassportResponse response = parse("passport_last_name_not_exists_parse.json");
        Assert.assertThat(
                response.getUsers(),
                Matchers.contains(
                        new PassportInfoDsl()
                                .setLogin("login_user-info-sdk-tst2-dimkarp93")
                                .setDisplayName("user-info-sdk-tst2-dimkarp93")
                                .setUserInfo(
                                        new UserInfoDsl()
                                                .setFirstName("Иван")
                                                .setLastName(OptionalMatcher.not())
                                                .setCAPIDisplayName("user-info-sdk-tst2-dimkarp93")
                                                .setFio(OptionalMatcher.not())
                                                .setFullFio(OptionalMatcher.not())
                                )
                                .toMatcher()
                )
        );
    }

    @Test
    public void namesNotExistFromLoginParse() {
        PassportResponse response = parse("passport_names_not_exists_parse.json");
        Assert.assertThat(
                response.getUsers(),
                Matchers.contains(
                        new PassportInfoDsl()
                                .setLogin("login_user-info-sdk-tst2-dimkarp93")
                                .setDisplayName(OptionalMatcher.not())
                                .setUserInfo(
                                        new UserInfoDsl()
                                                .setFirstName(OptionalMatcher.not())
                                                .setLastName(OptionalMatcher.not())
                                                .setCAPIDisplayName("login_user-info-sdk-tst2-dimkarp93")
                                                .setFio(OptionalMatcher.not())
                                                .setFullFio(OptionalMatcher.not())
                                )
                                .toMatcher()
                )
        );
    }

    @Test
    public void preferPhoneAttrsParse() {
        PassportResponse response = parse("passport_prefer_phone_attrs_parse.json");
        Assert.assertThat(
                response.getUsers(),
                Matchers.contains(
                        new PassportInfoDsl()
                                .setLogin("login_user-info-sdk-tst2-dimkarp93")
                                .setDisplayName("user-info-sdk-tst2-dimkarp93")
                                .setUserInfo(
                                        new UserInfoDsl()
                                                .setFirstName(OptionalMatcher.not())
                                                .setLastName("Сидоров")
                                                .setCAPIDisplayName("user-info-sdk-tst2-dimkarp93")
                                                .setFio(OptionalMatcher.not())
                                                .setFullFio(OptionalMatcher.not())
                                                .setPhones(Matchers.containsInAnyOrder("+72222222222", "+73333333333"))
                                )
                                .toMatcher()
                )
        );
    }

    @Test
    public void phoneFromFieldIfEmptyAttrsParse() {
        PassportResponse response = parse("passport_from_field_if_empty_attrs_parse.json");
        Assert.assertThat(
                response.getUsers(),
                Matchers.contains(
                        new PassportInfoDsl()
                                .setLogin("login_user-info-sdk-tst2-dimkarp93")
                                .setDisplayName("user-info-sdk-tst2-dimkarp93")
                                .setUserInfo(
                                        new UserInfoDsl()
                                                .setFirstName(OptionalMatcher.not())
                                                .setLastName("Сидоров")
                                                .setCAPIDisplayName("user-info-sdk-tst2-dimkarp93")
                                                .setFio(OptionalMatcher.not())
                                                .setFullFio(OptionalMatcher.not())
                                                .setPhones(Matchers.contains("71111111111"))
                                )
                                .toMatcher()
                )
        );
    }

    @Test
    public void phoneIgnoreEmptyParse() {
        PassportResponse response = parse("passport_phone_ignore_empty_parse.json");
        Assert.assertThat(
                response.getUsers(),
                Matchers.contains(
                        new PassportInfoDsl()
                                .setLogin("login_user-info-sdk-tst2-dimkarp93")
                                .setDisplayName("user-info-sdk-tst2-dimkarp93")
                                .setUserInfo(
                                        new UserInfoDsl()
                                                .setFirstName(OptionalMatcher.not())
                                                .setLastName("Сидоров")
                                                .setCAPIDisplayName("user-info-sdk-tst2-dimkarp93")
                                                .setFio(OptionalMatcher.not())
                                                .setFullFio(OptionalMatcher.not())
                                                .setPhones(Matchers.emptyIterable())
                                )
                                .toMatcher()
                )
        );
    }

    private PassportResponse parse(String filename) {
        InputStream stream = PassportTest.class.getResourceAsStream(filename);
        return deserializer.deserialize(stream);
    }
}

package ru.yandex.market.sdk.userinfo;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import ru.yandex.market.sdk.userinfo.client.PassportClient;
import ru.yandex.market.sdk.userinfo.client.SberlogClient;
import ru.yandex.market.sdk.userinfo.domain.AggregateUserInfo;
import ru.yandex.market.sdk.userinfo.domain.Error;
import ru.yandex.market.sdk.userinfo.domain.Field;
import ru.yandex.market.sdk.userinfo.domain.Options;
import ru.yandex.market.sdk.userinfo.domain.PassportError;
import ru.yandex.market.sdk.userinfo.domain.PassportResponse;
import ru.yandex.market.sdk.userinfo.domain.SberlogInfo;
import ru.yandex.market.sdk.userinfo.domain.Sex;
import ru.yandex.market.sdk.userinfo.domain.Uid;
import ru.yandex.market.sdk.userinfo.matcher.OptionalMatcher;
import ru.yandex.market.sdk.userinfo.matcher.dsl.AggregateUserInfoDsl;
import ru.yandex.market.sdk.userinfo.matcher.dsl.PassportErrorDsl;
import ru.yandex.market.sdk.userinfo.matcher.dsl.PassportInfoDsl;
import ru.yandex.market.sdk.userinfo.matcher.dsl.SberlogErrorDsl;
import ru.yandex.market.sdk.userinfo.matcher.dsl.SberlogInfoDsl;
import ru.yandex.market.sdk.userinfo.matcher.dsl.UidDsl;
import ru.yandex.market.sdk.userinfo.matcher.dsl.UserInfoDsl;
import ru.yandex.market.sdk.userinfo.service.ResolveUidServiceImpl;
import ru.yandex.market.sdk.userinfo.service.ServiceOptions;
import ru.yandex.market.sdk.userinfo.service.UserInfoService;
import ru.yandex.market.sdk.userinfo.service.UserInfoServiceImpl;
import ru.yandex.market.sdk.userinfo.util.Result;
import ru.yandex.passport.tvmauth.NativeTvmClient;
import ru.yandex.passport.tvmauth.TvmApiSettings;

/**
 * @authror dimkarp93
 */
public class MainTest {
    private static final int APPLICATION_ID = 2011282;
    private static final List<Field> POPULAR_FIELDS = Arrays.asList(Field.EMAILS,
            Field.FIRST_NAME,
            Field.LAST_NAME,
            Field.BIRTH_DATE,
            Field.SEX,
            Field.REGNAME
    );

    private static final UserInfoSource PASSPORT = TestConstants.ENVIRONMENT.getPassport();
    private static final UserInfoSource SBERLOG = TestConstants.ENVIRONMENT.getSberlog();

    private static NativeTvmClient tvmClient;
    private static SberlogClient sberlogClient;
    private static PassportClient passportClient;
    private static UserInfoService userInfoService;

    @BeforeClass
    public static void setUp() {

        TvmApiSettings settings = TvmApiSettings.create();
        settings.setSelfTvmId(APPLICATION_ID);
        settings.enableServiceTicketChecking();
        String tvmSecret = System.getenv("TVM_SECRET");
        settings.enableServiceTicketsFetchOptions(
                tvmSecret,
                new int[]{PASSPORT.getApplicationId(), SBERLOG.getApplicationId()}
        );

        tvmClient = NativeTvmClient.create(settings);
        sberlogClient = new SberlogClient(SBERLOG.getHost());
        passportClient = new PassportClient(PASSPORT.getHost());
        userInfoService = new UserInfoServiceImpl(
                TestConstants.ENVIRONMENT,
                new ServiceOptions(tvmClient::getServiceTicketFor),
                new ResolveUidServiceImpl()
        );
    }

    @Test
    public void getSberUserInfo() {
        String serviceTicket = tvmClient.getServiceTicketFor(SBERLOG.getApplicationId());
        List<SberlogInfo> sberlogInfos = sberlogClient.getUserInfo(Arrays.asList(1141L, 1142L), serviceTicket);
        Assert.assertThat(
                sberlogInfos,
                Matchers.containsInAnyOrder(
                        new SberlogInfoDsl()
                                .setFatherName("Иванович")
                                .setUserInfo(
                                        new UserInfoDsl()
                                                .setUid(UidDsl.asUid(Uid.ofSberlog(1141L)).toMatcher())
                                                .setFirstName("Сидор")
                                                .setLastName("Петров")
                                                .setFio("Петров Сидор")
                                                .setCAPIDisplayName("Петров Сидор")
                                                .setBirthDate(LocalDate.of(2010, 1, 10))
                                                .setSex(Sex.MALE)
                                )
                                .toMatcher(),
                        new SberlogInfoDsl()
                                .setFatherName("Петровна")
                                .setUserInfo(
                                        new UserInfoDsl()
                                                .setUid(UidDsl.asUid(Uid.ofSberlog(1142L)).toMatcher())
                                                .setFirstName("Елена")
                                                .setLastName("Сидорова")
                                                .setFio("Сидорова Елена")
                                                .setCAPIDisplayName("Сидорова Елена")
                                                .setAvatar("https://avatars.mds.yandex.net/get-yapic/0/0-0/1", "1")
                                                .setBirthDate(LocalDate.of(1666, 6, 13))
                                                .setSex(Sex.FEMALE)
                                )
                                .toMatcher()
                )
        );
    }

    @Test
    public void getPassUserInfo() {
        String serviceTicket = tvmClient.getServiceTicketFor(PASSPORT.getApplicationId());
        PassportResponse response = passportClient.getUserInfo(Arrays.asList(4022479524L, 4022479718L), "127.0.0.1",
                serviceTicket, POPULAR_FIELDS);
        Assert.assertThat(
                response.getUsers(),
                Matchers.containsInAnyOrder(
                        new PassportInfoDsl()
                                .setLogin("user-info-sdk-tst1-dimkarp93")
                                .setDisplayName(OptionalMatcher.of("user-info-sdk-tst1-dimkarp93"))
                                .setUserInfo(new UserInfoDsl()
                                        .setUid(UidDsl.asUid(Uid.ofPassport(4022479524L)).toMatcher())
                                        .setSex(Sex.FEMALE)
                                        .setBirthDate(LocalDate.of(1990, 6, 7))
                                        .setFirstName("Ирина")
                                        .setLastName("Петрова")
                                        .setFio("Петрова Ирина")
                                        .setCAPIDisplayName("Петрова Ирина")
                                        .setEmails(Matchers.hasItems("user-info-sdk-tst1-dimkarp93@yandex.ru"))
                                        .setAvatar(OptionalMatcher.not(), ""))
                                .toMatcher(),
                        new PassportInfoDsl()
                                .setDisplayName(OptionalMatcher.of("user-info-sdk-tst2-dimkarp93"))
                                .setUserInfo(new UserInfoDsl()
                                        .setUid(UidDsl.asUid(Uid.ofPassport(4022479718L)).toMatcher())
                                        .setSex(Sex.MALE)
                                        .setBirthDate(LocalDate.of(1930, 2, 21))
                                        .setFirstName("Иван")
                                        .setLastName("Сидоров")
                                        .setFio("Сидоров Иван")
                                        .setCAPIDisplayName("Сидоров Иван")
                                        .setEmails(Matchers.hasItems("user-info-sdk-tst2-dimkarp93@yandex.ru"))
                                        .setAvatar(
                                                OptionalMatcher.of(
                                                        "https://avatars.mds.yandex" +
                                                                ".net/get-yapic/1450/AcrohaOtlGG98Vh2XcvtCRK8Es-1/1"),
                                                "1"))
                                .toMatcher()

                )
        );
    }

    @Test
    public void getUserInfoEmptyIds() {
        Result<List<AggregateUserInfo>, ? extends Error> result = userInfoService.getUserInfo(null, null);

        Assert.assertTrue(result.isOk());
        Assert.assertThat(
                result.getValue(),
                Matchers.empty()
        );

        Result<List<AggregateUserInfo>, ? extends Error> result2 =
                userInfoService.getUserInfo(Collections.emptyList(), null);

        Assert.assertTrue(result2.isOk());
        Assert.assertThat(
                result2.getValue(),
                Matchers.empty()
        );
    }

    @Test
    public void getUserInfoEmptyIdsAsync() {
        Result<List<AggregateUserInfo>, ? extends Error> result = userInfoService
                .getUserInfoAsync(null, null)
                .get();

        Assert.assertTrue(result.isOk());
        Assert.assertThat(
                result.getValue(),
                Matchers.empty()
        );

        Result<List<AggregateUserInfo>, ? extends Error> result2 =
                userInfoService.getUserInfo(Collections.emptyList(), null);

        Assert.assertTrue(result2.isOk());
        Assert.assertThat(
                result2.getValue(),
                Matchers.empty()
        );
    }

    @Test
    public void getUserInfoByUidType() {
        Options options = new Options();
        //Только для интеграционого тестирования в продакшен лучше передавать реальный
        options.setIp("127.0.0.1");
        options.setFields(POPULAR_FIELDS);

        Result<List<AggregateUserInfo>, ? extends Error> result = userInfoService.getUserInfoRaw(
                Arrays.asList(2190550858753009250L, 4022479718L),
                options
        );

        Assert.assertTrue(result.isOk());
        Assert.assertThat(
                result.getValue(),
                Matchers.containsInAnyOrder(
                        new AggregateUserInfoDsl()
                                .setSber(new SberlogInfoDsl()
                                        .setFatherName("Васильевич")
                                )
                                .setUserInfo(new UserInfoDsl()
                                        .setUid(UidDsl.asUid(Uid.ofSberlog(2190550858753009250L)).toMatcher())
                                        .setFirstName("Василий")
                                        .setLastName("Пупкин")
                                        .setBirthDate(LocalDate.of(2009, 12, 30))
                                        .setSex(Sex.MALE)
                                        .setFio("Пупкин Василий")
                                        .setCAPIDisplayName("Пупкин Василий")
                                        .setEmails(Matchers.containsInAnyOrder("user@yandex.ru", "user@ya.ru")))
                                .toMatcher(),
                        new AggregateUserInfoDsl()
                                .setPassport(new PassportInfoDsl()
                                        .setLogin("user-info-sdk-tst2-dimkarp93")
                                )
                                .setUserInfo(new UserInfoDsl()
                                        .setUid(UidDsl.asUid(Uid.ofPassport(4022479718L)).toMatcher())
                                        .setSex(Sex.MALE)
                                        .setBirthDate(LocalDate.of(1930, 2, 21))
                                        .setFirstName("Иван")
                                        .setLastName("Сидоров")
                                        .setFio("Сидоров Иван")
                                        .setCAPIDisplayName("Сидоров Иван")
                                        .setEmails(Matchers.hasItems("user-info-sdk-tst2-dimkarp93@yandex.ru"))
                                )
                                .toMatcher()

                )
        );
    }

    @Test
    public void getUserInfoByUidTypeAsync() {
        Options options = new Options();
        //Только для интеграционого тестирования в продакшен лучше передавать реальный
        options.setIp("127.0.0.1");
        options.setFields(POPULAR_FIELDS);

        Result<List<AggregateUserInfo>, ? extends Error> result = userInfoService.getUserInfoRawAsync(
                Arrays.asList(2190550858753009250L, 4022479718L),
                options
        ).get();

        Assert.assertTrue(result.isOk());
        Assert.assertThat(
                result.getValue(),
                Matchers.containsInAnyOrder(
                        new AggregateUserInfoDsl()
                                .setSber(new SberlogInfoDsl()
                                        .setFatherName("Васильевич")
                                )
                                .setUserInfo(new UserInfoDsl()
                                        .setUid(UidDsl.asUid(Uid.ofSberlog(2190550858753009250L)).toMatcher())
                                        .setFirstName("Василий")
                                        .setLastName("Пупкин")
                                        .setBirthDate(LocalDate.of(2009, 12, 30))
                                        .setSex(Sex.MALE)
                                        .setFio("Пупкин Василий")
                                        .setCAPIDisplayName("Пупкин Василий")
                                        .setEmails(Matchers.containsInAnyOrder("user@yandex.ru", "user@ya.ru")))
                                .toMatcher(),
                        new AggregateUserInfoDsl()
                                .setPassport(new PassportInfoDsl()
                                        .setLogin("user-info-sdk-tst2-dimkarp93")
                                )
                                .setUserInfo(new UserInfoDsl()
                                        .setUid(UidDsl.asUid(Uid.ofPassport(4022479718L)).toMatcher())
                                        .setSex(Sex.MALE)
                                        .setBirthDate(LocalDate.of(1930, 2, 21))
                                        .setFirstName("Иван")
                                        .setLastName("Сидоров")
                                        .setFio("Сидоров Иван")
                                        .setCAPIDisplayName("Сидоров Иван")
                                        .setEmails(Matchers.hasItems("user-info-sdk-tst2-dimkarp93@yandex.ru"))
                                )
                                .toMatcher()

                )
        );
    }

    @Test
    public void getUserInfoByUidNumber() {
        Options options = new Options();
        //Только для интеграционого тестирования в продакшен лучше передавать реальный
        options.setIp("127.0.0.1");
        options.setFields(POPULAR_FIELDS);

        Result<List<AggregateUserInfo>, ? extends Error> result = userInfoService.getUserInfoRaw(
                Arrays.asList(2190550858753009250L, 4022479718L),
                options
        );

        Assert.assertTrue(result.isOk());
        Assert.assertThat(
                result.getValue(),
                Matchers.containsInAnyOrder(
                        new AggregateUserInfoDsl()
                                .setSber(new SberlogInfoDsl()
                                        .setFatherName("Васильевич")
                                )
                                .setUserInfo(new UserInfoDsl()
                                        .setUid(UidDsl.asUid(Uid.ofSberlog(2190550858753009250L)).toMatcher())
                                        .setFirstName("Василий")
                                        .setLastName("Пупкин")
                                        .setBirthDate(LocalDate.of(2009, 12, 30))
                                        .setSex(Sex.MALE)
                                        .setFio("Пупкин Василий")
                                        .setCAPIDisplayName("Пупкин Василий")
                                        .setEmails(Matchers.containsInAnyOrder("user@yandex.ru", "user@ya.ru")))
                                .toMatcher(),
                        new AggregateUserInfoDsl()
                                .setPassport(new PassportInfoDsl()
                                        .setLogin("user-info-sdk-tst2-dimkarp93")
                                )
                                .setUserInfo(new UserInfoDsl()
                                        .setUid(UidDsl.asUid(Uid.ofPassport(4022479718L)).toMatcher())
                                        .setSex(Sex.MALE)
                                        .setBirthDate(LocalDate.of(1930, 2, 21))
                                        .setFirstName("Иван")
                                        .setLastName("Сидоров")
                                        .setFio("Сидоров Иван")
                                        .setCAPIDisplayName("Сидоров Иван")
                                        .setEmails(Matchers.hasItems("user-info-sdk-tst2-dimkarp93@yandex.ru"))
                                )
                                .toMatcher()

                )
        );
    }

    @Test
    public void getUserInfoByUidNumberOnlyPassport() {
        Options options = new Options();
        //Только для интеграционого тестирования в продакшен лучше передавать реальный
        options.setIp("127.0.0.1");
        options.setFields(POPULAR_FIELDS);

        Result<List<AggregateUserInfo>, ? extends Error> result = userInfoService.getUserInfoRaw(
                Collections.singletonList(4022479718L),
                options
        );

        Assert.assertTrue(result.isOk());
        Assert.assertThat(
                result.getValue(),
                Matchers.contains(
                        new AggregateUserInfoDsl()
                                .setPassport(new PassportInfoDsl()
                                        .setLogin("user-info-sdk-tst2-dimkarp93")
                                )
                                .setUserInfo(new UserInfoDsl()
                                        .setUid(UidDsl.asUid(Uid.ofPassport(4022479718L)).toMatcher())
                                        .setSex(Sex.MALE)
                                        .setBirthDate(LocalDate.of(1930, 2, 21))
                                        .setFirstName("Иван")
                                        .setLastName("Сидоров")
                                        .setFio("Сидоров Иван")
                                        .setCAPIDisplayName("Сидоров Иван")
                                        .setEmails(Matchers.hasItems("user-info-sdk-tst2-dimkarp93@yandex.ru"))
                                )
                                .toMatcher()

                )
        );
    }

    @Test
    public void getUserInfoByUidNumberOnlySberlog() {
        Options options = new Options();
        //Только для интеграционого тестирования в продакшен лучше передавать реальный
        options.setIp("127.0.0.1");
        options.setFields(POPULAR_FIELDS);

        Result<List<AggregateUserInfo>, ? extends Error> result = userInfoService.getUserInfoRaw(
                Collections.singletonList(2190550858753009250L),
                options
        );

        Assert.assertTrue(result.isOk());
        Assert.assertThat(
                result.getValue(),
                Matchers.contains(
                        new AggregateUserInfoDsl()
                                .setSber(new SberlogInfoDsl()
                                        .setFatherName("Васильевич")
                                )
                                .setUserInfo(new UserInfoDsl()
                                        .setUid(UidDsl.asUid(Uid.ofSberlog(2190550858753009250L)).toMatcher())
                                        .setFirstName("Василий")
                                        .setLastName("Пупкин")
                                        .setBirthDate(LocalDate.of(2009, 12, 30))
                                        .setSex(Sex.MALE)
                                        .setFio("Пупкин Василий")
                                        .setCAPIDisplayName("Пупкин Василий")
                                        .setEmails(Matchers.containsInAnyOrder("user@yandex.ru", "user@ya.ru")))
                                .toMatcher()

                )
        );
    }

    @Test
    public void getUserInfoWithPassportException() {
        Options options = new Options();
        //Только для интеграционого тестирования в продакшен лучше передавать реальный
        options.setIp("127.0.0.1");
        options.setFields(POPULAR_FIELDS);

        Result<List<AggregateUserInfo>, ? extends Error> result = userInfoService.getUserInfoRaw(
                Collections.singleton(-1L),
                options
        );

        Assert.assertFalse(result.isOk());
        Assert.assertThat(
                (PassportError) result.getError(),
                new PassportErrorDsl()
                        .setId(2)
                        .toMatcher()
        );
    }

    @Test
    public void getUserInfoWithSberlogException() {
        Options options = new Options();
        //Только для интеграционого тестирования в продакшен лучше передавать реальный
        options.setIp("127.0.0.1");
        options.setFields(POPULAR_FIELDS);

        Result<List<AggregateUserInfo>, ? extends Error> result = userInfoService.getUserInfoRaw(
                //last
                Collections.singleton(2_305_843_009_213_693_951L),
                options
        );

        Assert.assertFalse(result.isOk());
        Assert.assertThat(
                result.getError(),
                new SberlogErrorDsl()
                        .setCode(1)
                        .setType(Error.Type.SBERLOG_EXCEPTION)
                        .toMatcher()

        );
    }
}

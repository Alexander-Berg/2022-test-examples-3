package ru.yandex.chemodan.app.psbilling.core.mocks;

import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.inside.passport.PassportDomain;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.blackbox2.Blackbox2;
import ru.yandex.inside.passport.blackbox2.BlackboxQueryable;
import ru.yandex.inside.passport.blackbox2.BlackboxType;
import ru.yandex.inside.passport.blackbox2.protocol.BlackboxMethod;
import ru.yandex.inside.passport.blackbox2.protocol.request.params.BlackboxSid;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxCorrectResponse;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxDbFields;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxDisplayName;
import ru.yandex.misc.email.Email;
import ru.yandex.misc.ip.IpAddress;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.when;

@Configuration
public class Blackbox2MockConfiguration {

    private Blackbox2 mock;

    @Primary
    @Bean
    public Blackbox2 blackbox2() {
        mock = Mockito.mock(Blackbox2.class,
                Mockito.withSettings().spiedInstance(Blackbox2.cons(BlackboxType.TEST)).defaultAnswer(CALLS_REAL_METHODS));
        return mock;
    }

    public void mockUserInfo(PassportUid uid, BlackboxCorrectResponse userInfo) {
        BlackboxQueryable queryable = Mockito.mock(BlackboxQueryable.class);
        when(mock.query()).thenReturn(queryable);
        when(queryable.userInfo(any(IpAddress.class), Mockito.eq(uid), Mockito.<ListF<String>>any()))
                .thenReturn(userInfo);
        when(queryable.userInfo(any(IpAddress.class), Mockito.eq(uid), Mockito.any(), Mockito.<ListF<Integer>>any()))
                .thenReturn(userInfo);
        when(queryable.userInfo(
                any(), Mockito.eq(Option.of(uid)), any(), any(), any(), any(), any(), any(),
                anyBoolean(), any(), anyBoolean(), anyBoolean())).thenReturn(userInfo);
    }

    public void mockUserInfo(Email email, BlackboxCorrectResponse userInfo) {
        BlackboxQueryable queryable = Mockito.mock(BlackboxQueryable.class);
        when(mock.query()).thenReturn(queryable);

        when(queryable.userInfo(any(IpAddress.class), eq(email.getEmail()), eq(BlackboxSid.SMTP), any()))
                .thenReturn(userInfo);
        when(queryable.userInfo(any(IpAddress.class), eq(email.getEmail()), eq(BlackboxSid.SMTP), any(), any()))
                .thenReturn(userInfo);
        when(queryable.userInfo(any(IpAddress.class), eq(email.getEmail()), eq(BlackboxSid.SMTP), any(), any(), any()))
                .thenReturn(userInfo);
    }

    public void reset() {
        Mockito.reset(mock);
    }

    public static BlackboxCorrectResponse getBlackboxResponse(
            String login, String firstName, Option<String> displayName, Option<String> publicName,
            Option<String> language, Option<String> timeZone, Option<String> country) {
        Option<BlackboxDisplayName> displayNameO = displayName.map(
                name -> new BlackboxDisplayName(name, Option.empty(), Option.empty(), Option.empty(), publicName));
        return new BlackboxCorrectResponse(
                BlackboxMethod.USER_INFO, Option.empty(), Option.empty(), 200, Option.empty(), Option.empty(),
                Option.of(login),
                Cf.map(BlackboxDbFields.FIRSTNAME, firstName,
                        BlackboxDbFields.LANG, language.getOrNull(),
                        BlackboxDbFields.TIMEZONE_DB_FIELD, timeZone.getOrNull(),
                        BlackboxDbFields.COUNTRY, country.getOrNull()),
                Cf.list(), Cf.list(),
                Option.empty(), Cf.map(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.of("regname"), displayNameO);

    }

    public static BlackboxCorrectResponse getUidResponse(PassportUid uid) {
        return new BlackboxCorrectResponse(
                BlackboxMethod.USER_INFO, Option.empty(), Option.empty(), 200, Option.empty(),
                Option.of(Tuple2.tuple(uid, PassportDomain.YANDEX_RU)),
                Option.empty(), Cf.map(), Cf.list(), Cf.list(),
                Option.empty(), Cf.map(), Option.empty(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.empty()
        );
    }
}

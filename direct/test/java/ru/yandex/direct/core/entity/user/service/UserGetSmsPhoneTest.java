package ru.yandex.direct.core.entity.user.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.direct.blackbox.client.BlackboxClient;
import ru.yandex.direct.test.utils.RandomNumberUtils;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmService;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxCorrectResponse;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxOAuthStatus;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxPhone;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxResponseBuilder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.inside.passport.blackbox2.protocol.BlackboxMethod.USER_INFO;


@RunWith(MockitoJUnitRunner.class)
@ParametersAreNonnullByDefault
public class UserGetSmsPhoneTest {

    private long uid;
    private String expectedSmsPhone;

    @Mock
    private BlackboxClient blackboxClient;
    @Mock
    private TvmIntegration tvmIntegration;

    @InjectMocks
    private UserService userService;

    @Before
    public void initTest() {
        uid = RandomNumberUtils.nextPositiveLong();
        expectedSmsPhone = generateSmsNumber();
        doReturn("TVM_TICKET")
                .when(tvmIntegration).getTicket(TvmService.BLACKBOX_MIMINO);
    }

    @Test
    public void checkGetSmsPhones() {
        mockBlackboxResponse(uid, List.of(createBlackboxPhone(expectedSmsPhone, true)));
        Map<Long, String> smsPhones = userService.getSmsPhones(List.of(uid));
        Map<Long, String> expectedMap = Map.of(uid, expectedSmsPhone);

        assertThat(smsPhones, beanDiffer(expectedMap));
    }

    @Test
    public void checkGetSmsPhones_whenUserWithoutPhone() {
        mockBlackboxResponse(uid, Collections.emptyList());
        Map<Long, String> smsPhones = userService.getSmsPhones(List.of(uid));
        Map<Long, String> expectedMap = Collections.emptyMap();

        assertThat(smsPhones, beanDiffer(expectedMap));
    }

    @Test
    public void checkGetSmsPhones_whenUserPhoneValueIsNull() {
        mockBlackboxResponse(uid, List.of(createBlackboxPhone(null, true)));
        Map<Long, String> smsPhones = userService.getSmsPhones(List.of(uid));
        Map<Long, String> expectedMap = Collections.emptyMap();

        assertThat(smsPhones, beanDiffer(expectedMap));
    }

    @Test
    public void checkGetSmsPhones_whenUserWithManyPhones_checkGetDefaultNumber() {
        List<BlackboxPhone> blackboxPhones = List.of(createBlackboxPhone(generateSmsNumber(), false),
                createBlackboxPhone(expectedSmsPhone, true));
        mockBlackboxResponse(uid, blackboxPhones);
        Map<Long, String> smsPhones = userService.getSmsPhones(List.of(uid));
        Map<Long, String> expectedMap = Map.of(uid, expectedSmsPhone);

        assertThat(smsPhones, beanDiffer(expectedMap));
    }


    private void mockBlackboxResponse(Long uid, List<BlackboxPhone> blackboxPhones) {
        PassportUid passportUid = PassportUid.cons(uid);
        BlackboxCorrectResponse response = new BlackboxResponseBuilder(USER_INFO)
                .setStatus(BlackboxOAuthStatus.VALID.value())
                .setPhones(Cf.toList(blackboxPhones))
                .build()
                .getOrThrow();
        doReturn(Map.of(passportUid, response))
                .when(blackboxClient)
                .userInfoBulk(any(), eq(List.of(passportUid)), isNull(), eq(Optional.empty()), eq(Optional.empty()),
                        eq(false), any(), any());
    }

    private static BlackboxPhone createBlackboxPhone(@Nullable String smsPhone, boolean isDefaultNumber) {
        return new BlackboxPhone(RandomNumberUtils.nextPositiveLong(), Option.empty(), Option.empty(), Option.empty(),
                Option.empty(), Option.ofNullable(smsPhone), Option.empty(), Option.empty(),
                Option.of(isDefaultNumber), Option.empty());
    }

    private static String generateSmsNumber() {
        return String.format("+7%s*****%s", RandomStringUtils.randomNumeric(3), RandomStringUtils.randomNumeric(2));
    }

}

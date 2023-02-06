package ru.yandex.direct.core.entity.user.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.blackbox.client.BlackboxClient;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.config.DirectConfigFactory;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmIntegrationImpl;
import ru.yandex.direct.tvm.TvmService;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.blackbox2.Blackbox2;
import ru.yandex.inside.passport.blackbox2.BlackboxRawRequestExecutor;
import ru.yandex.inside.passport.blackbox2.BlackboxRequestExecutorWithRetries;
import ru.yandex.inside.passport.blackbox2.protocol.BlackboxHttpException;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxFatalException;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxPhone;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.ip.Ipv4Address;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.tvm.TvmService.DIRECT_SCRIPTS_TEST;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore("For manual run only because tests to use external service")
@ParametersAreNonnullByDefault
public class BlackboxGetPhoneQueryTest {

    private static final String BLACKBOX_URL_PPCDEV = "http://ppctest-proxy.ppc.yandex.ru:7088/blackbox/";
    private static final PassportUid UID1_WITH_PHONE = new PassportUid(121);
    private static final PassportUid UID2_WITH_PHONE = new PassportUid(127);
    private static final PassportUid UID3_WITH_PHONE = new PassportUid(224);

    private static final String UID1_MASKED_E164_NUMBER = "+7000*****55";
    private static final String UID2_MASKED_E164_NUMBER = "+7000*****79";

    private static final Ipv4Address USER_IP = Ipv4Address.parse("127.0.0.1");
    private static final String MESSAGE_WRONG_TVM_TICKET = "failed to check service ticket: Malformed ticket";

    private TvmIntegration tvmIntegration;
    private BlackboxClient blackboxClient;

    @Before
    public void initTest() {
        Map<String, Object> conf = new HashMap<>();
        conf.put("tvm.enabled", true);
        conf.put("tvm.app_id", DIRECT_SCRIPTS_TEST.getId());
        conf.put("tvm.secret", "file://~/.direct-tokens/tvm2_direct-scripts-test");
        DirectConfig directConfig = DirectConfigFactory.getConfig(EnvironmentType.DEVELOPMENT, conf);
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.initialize();
        tvmIntegration = spy(TvmIntegrationImpl.create(directConfig, scheduler));

        var blackbox2 = new Blackbox2(
                new BlackboxRequestExecutorWithRetries(
                        new BlackboxRawRequestExecutor(BLACKBOX_URL_PPCDEV,
                                Timeout.milliseconds(5000)),
                        2
                ));
        blackboxClient = new BlackboxClient(blackbox2);
    }

    private BlackboxGetPhoneQuery getBlackboxGetPhoneQuery(List<PassportUid> uids) {
        BlackboxGetPhoneQuery blackboxGetPhoneQuery = spy(new BlackboxGetPhoneQuery(blackboxClient, tvmIntegration,
                EnvironmentType.DEVELOPMENT, uids));
        // TODO(dimitrovsd): исправить тест
        // when(blackboxGetPhoneQuery.getUserAddress()).thenReturn(USER_IP);
        return blackboxGetPhoneQuery;
    }

    /**
     * При запросе в blackbox с правильным Tvm тикетом для получения телефона пользователя с определенным uid  ->
     * получим корректную маску-номер для данного uid
     */
    @Test
    public void call_withOneUid() {
        BlackboxGetPhoneQuery blackboxGetPhoneQuery = getBlackboxGetPhoneQuery(List.of(UID1_WITH_PHONE));
        Map<PassportUid, Optional<BlackboxPhone>> map = blackboxGetPhoneQuery.call();
        assertThat(map.get(UID1_WITH_PHONE).get().getMaskedE164Number().get()).isEqualTo(UID1_MASKED_E164_NUMBER);
    }

    /**
     * При запросе в blackbox с правильным Tvm тикетом для получения телефонов пользователей с определенными uid ->
     * получим корректные маски-номера для данных uid
     */
    @Test
    public void call_withMultipleUids() {
        BlackboxGetPhoneQuery blackboxGetPhoneQuery = getBlackboxGetPhoneQuery(List.of(UID1_WITH_PHONE,
                UID2_WITH_PHONE));
        Map<PassportUid, Optional<BlackboxPhone>> map = blackboxGetPhoneQuery.call();
        assertThat(map.get(UID1_WITH_PHONE).get().getMaskedE164Number().get()).isEqualTo(UID1_MASKED_E164_NUMBER);
        assertThat(map.get(UID2_WITH_PHONE).get().getMaskedE164Number().get()).isEqualTo(UID2_MASKED_E164_NUMBER);
    }

    /**
     * При запросе в blackbox с не правильным Tvm тикетом для получения телефона пользователя c определенным uid ->
     * получим исключение {@link BlackboxFatalException}
     */
    @Test
    public void call_oneUidwithWrongTvm() {
        when(tvmIntegration.getTicket(TvmService.BLACKBOX_MIMINO)).thenReturn("wrong_ticket");
        BlackboxGetPhoneQuery blackboxGetPhoneQuery = getBlackboxGetPhoneQuery(List.of(UID2_WITH_PHONE));
        try {
            blackboxGetPhoneQuery.call();
            Assert.fail();
        } catch (BlackboxFatalException e) {
            assertThat(e.getMessage()).contains(MESSAGE_WRONG_TVM_TICKET);
        }
    }

    /**
     * При запросе в blackbox с не правильным Tvm тикетом для получения телефонов пользователей с определенными uid ->
     * получим исключение {@link BlackboxHttpException}
     */
    @Test
    public void call_multipleUidsWithWrongTvm() {
        when(tvmIntegration.getTicket(TvmService.BLACKBOX_MIMINO)).thenReturn("wrong_ticket");
        BlackboxGetPhoneQuery blackboxGetPhoneQuery = getBlackboxGetPhoneQuery(List.of(UID2_WITH_PHONE,
                UID3_WITH_PHONE));
        try {
            blackboxGetPhoneQuery.call();
            Assert.fail();
        } catch (BlackboxHttpException e) {
            assertThat(e.getCause().getMessage()).contains(MESSAGE_WRONG_TVM_TICKET);
        }
    }
}

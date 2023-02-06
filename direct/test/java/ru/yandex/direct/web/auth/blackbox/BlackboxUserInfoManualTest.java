package ru.yandex.direct.web.auth.blackbox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import ru.yandex.direct.blackbox.client.BlackboxClient;
import ru.yandex.direct.config.DirectConfig;
import ru.yandex.direct.config.DirectConfigFactory;
import ru.yandex.direct.env.EnvironmentType;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmIntegrationImpl;
import ru.yandex.inside.passport.BasicUserInfo;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.blackbox2.Blackbox2;
import ru.yandex.inside.passport.blackbox2.BlackboxRawRequestExecutor;
import ru.yandex.inside.passport.blackbox2.BlackboxRequestExecutorWithRetries;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxCorrectResponse;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxDbFields;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxFatalException;
import ru.yandex.misc.io.http.Timeout;
import ru.yandex.misc.ip.Ipv4Address;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.tvm.TvmService.BLACKBOX_MIMINO;
import static ru.yandex.direct.tvm.TvmService.DIRECT_SCRIPTS_TEST;

@Ignore("Для ручного запуска. Ходит в blackbox")
@ParametersAreNonnullByDefault
public class BlackboxUserInfoManualTest {

    private static final Ipv4Address USER_IP = Ipv4Address.parse("127.0.0.1");
    private static final PassportUid UID1 = new PassportUid(141887503L);
    private static final PassportUid UID2 = new PassportUid(141887502L);
    private static final String LOGIN = "sasha-tur-1";
    private static final String FIO = "Pupkin Vasily";
    private static final String BLACKBOX_URL = "http://ppctest-proxy.ppc.yandex.ru:7088/blackbox/";

    private static final List<String> DBFIELDS = List.of(BlackboxDbFields.LOGIN, BlackboxDbFields.FIO);
    private TvmIntegration tvmIntegration;
    private BlackboxClient blackboxClient;

    @Before
    public void prepare() {
        Map<String, Object> conf = new HashMap<>();
        conf.put("tvm.enabled", true);
        conf.put("tvm.app_id", DIRECT_SCRIPTS_TEST.getId());
        conf.put("tvm.secret", "file://~/.direct-tokens/tvm2_direct-scripts-test");
        DirectConfig directConfig = DirectConfigFactory.getConfig(EnvironmentType.DEVELOPMENT, conf);
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.initialize();
        tvmIntegration = TvmIntegrationImpl.create(directConfig, scheduler);

        blackboxClient = new BlackboxClient(
                new Blackbox2(
                        new BlackboxRequestExecutorWithRetries(
                                new BlackboxRawRequestExecutor(BLACKBOX_URL,
                                        Timeout.milliseconds(5000)),
                                2
                        ))
        );
    }

    @Test
    public void takeUserInfo() {
        String tvmTicket = tvmIntegration.getTicket(BLACKBOX_MIMINO);
        BlackboxCorrectResponse resp = blackboxClient
                .userInfo(USER_IP,
                        UID1,
                        DBFIELDS,
                        tvmTicket);
        BasicUserInfo userInfo = resp.getBasicUserInfo();
        assertThat(userInfo.getUid().getUid()).isEqualTo(UID1.getUid());
        assertThat(userInfo.getLogin()).isEqualTo(LOGIN);
        assertThat(resp.getDbFields().getTs(BlackboxDbFields.FIO)).isEqualTo(FIO);
    }

    @Test
    public void takeUserInfoWithWrongTvmTicket() {
        try {
            blackboxClient.userInfo(USER_IP,
                    UID2,
                    DBFIELDS,
                    "wrong_tvm_ticket");
            Assert.fail();
        } catch (BlackboxFatalException e) {
            assertThat(e.getMessage()).contains("Failed to check service ticket: Malformed ticket");
        }
    }
}

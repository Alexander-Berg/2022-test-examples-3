package ru.yandex.market.crm.campaign.external.appmetrica;

import java.util.List;

import javax.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.crm.campaign.placeholders.AppPropertiesConfiguration;
import ru.yandex.market.crm.core.services.external.appmetrica.HttpAppMetricaApiClient;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.AndroidPushMessageContent;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.AppMetricaPushMessage;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.DeviceIdType;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.PushBatchItem;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.PushBatchRequest;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.PushDevice;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.PushSendGroup;
import ru.yandex.market.crm.core.services.external.appmetrica.domain.PushTransfer;
import ru.yandex.market.crm.core.services.jackson.JacksonConfig;
import ru.yandex.market.crm.core.test.TestEnvironmentResolver;
import ru.yandex.market.mcrm.http.HttpClientConfiguration;

import static ru.yandex.market.crm.core.domain.mobile.MetricaMobileApp.MARKET_ANDROID;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ContextConfiguration(classes = {
        AppPropertiesConfiguration.class,
        TestEnvironmentResolver.class,
        HttpClientConfiguration.class,
        HttpAppMetricaApiClient.class,
        JacksonConfig.class
})
@RunWith(SpringJUnit4ClassRunner.class)
@Ignore
public class AppMetricaClientTest {

    private static final Logger LOG = LoggerFactory.getLogger(AppMetricaClientTest.class);

    private final static String TEST_GROUP_NAME = "LiluCRM test group";

    private static final String VIVG_GOOGLE_AID = "dd069ff8-650f-4b87-b056-b8d748c3fa53";
    private static final String VIVG_PUSHSAMPLE_TOKEN = "d_6H6lf8Yxo" +
            ":APA91bEhTfWrhRWBjjOCFgcrPA2FQG3zEUAOvDJ97aJHc0YLKd9W2yo9aYxdrYUqY_SzX5J50eDvVzjy-g" +
            "-ITTY9PK3gt6sKRRadmp4Yk_4rji-QxnvwxpMauVNPq02yJhMcCudF4mUU";
    private static final String VIVG_MARKET_TOKEN = "c2bEAjnIRos" +
            ":APA91bFT_OqGnhfoYaygIbzavQGE5nNUnv115Tm1TSlmZrWVlIrrNnJs6suk0HH3e5_HN-qQ3mrv7I9J9SibaFyyFmQtZ6gH8F3ogJ" +
            "-YL-lZ7ShMBJvhz7MxvG8lrQfjihjrjxPLk8ye";
    private static final String apershukov_GOOGLE_AID = "4ea01d5c-e8fb-473e-95a6-90355c709c28";
    private static final String apershukov_MARKET_PUSHTOKEN = "fQWK904FNso" +
            ":APA91bFO27XMKaEujua0CCBDPHgDuOpxfZA2JowMUQOqck5jd_H94ZgtwOkW7PwAOVenjT6thOJfFVSWWp8V4L79fju8Xa_fS1GGPudtQuWMwwdwc9D5M94fMrj8CzPhJ31xNiVoUrWR";
    private static final String apershukov_MARKET_PUSHTOKEN2 = "APA91bFMi7MaFwwmIuQ6QJJ-t_yjeOeWm" +
            "-WvgNmc6wCXnM8o4vY32Yn2byKKflkyKJhWoYvPaKos" +
            "-E_ZGkn51qgFh4WohygEKf3dmh2BWEeiaNwPVhfRcAaA506U2nAFzsy9wnNTqVki";


    @Inject
    HttpAppMetricaApiClient apiClient;

    @Test
    public void sendTestPush() {
        //apiClient.getAppInfo(MobileApp.MARKET_ANDROID);

        PushSendGroup group = ensureTestGroup();
        AppMetricaPushMessage<AndroidPushMessageContent> appMetricaPushMessage =
                new AppMetricaPushMessage<AndroidPushMessageContent>()
                        .setContent(new AndroidPushMessageContent()
                                .setTitle("Тестовое пуш сообщение")
                                .setText("Текст тестового\nпуш сообщения")
                                .setIconBackground("00ff00ff")
                                .setImage("http://avatars.mds.yandex.net/get-market-lilucrm/404497/f6dc2d21-1faf-4465" +
                                        "-a1b7" +
                                        "-cd0f0c981043/orig")
                                .setData("{\n  \"test\": \"data\"\n}")
                                .setLedColor(0x0000ff00)
                                .setLedInterval(2_000)
                                .setLedPauseInterval(5_000)
                                .setVibration(new int[]{0, 3000})//, 300, 300, 200, 200, 200, 200, 200, 500})
                        )
                        .setOpenApp();

        PushBatchRequest pushRequest = new PushBatchRequest()
                .setGroup(group)
                .setTag("tst");

        PushBatchItem item = new PushBatchItem();
        item.setAndroidPushMessage(appMetricaPushMessage);
        item.addDevice(new PushDevice(DeviceIdType.ANDROID_PUSH_TOKEN, VIVG_MARKET_TOKEN));

        int transferId = apiClient.sendPushBatch(pushRequest);
        PushTransfer pushTransfer = null;
        do {
            pushTransfer = apiClient.getSendPushStatus(transferId);
            LOG.debug("#push transfer status: {}", pushTransfer.getStatus());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {

            }

        } while (pushTransfer.getStatus().needWait());

    }

    private PushSendGroup ensureTestGroup() {
        List<PushSendGroup> groups = apiClient.getPushSendGroups(MARKET_ANDROID.getId());
        PushSendGroup group = groups.stream()
                .filter(g -> TEST_GROUP_NAME.equals(g.getName()))
                .findAny()
                .orElse(null);
        if (null != group) {
            return group;
        }
        return apiClient.createGroup(MARKET_ANDROID.getId(), TEST_GROUP_NAME);
    }
}

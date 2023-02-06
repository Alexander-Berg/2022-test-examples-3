package ru.yandex.market.api.cpa;


import java.util.EnumSet;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.api.cpa.DistributedPushApiSettingsRepository.SettingsType;
import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.CheckouterShopApi;
import ru.yandex.market.checkout.pushapi.client.PushApi;
import ru.yandex.market.checkout.pushapi.settings.AuthType;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.checkout.pushapi.settings.Features;
import ru.yandex.market.checkout.pushapi.settings.Settings;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.campaign.model.CampaignType;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.geobase.TimezoneDao;
import ru.yandex.market.core.param.ParamService;
import ru.yandex.market.core.param.model.BooleanParamValue;
import ru.yandex.market.core.param.model.StringParamValue;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.api.cpa.DistributedPushApiSettingsRepository.SettingsType.PRODUCTION;
import static ru.yandex.market.core.param.model.ParamType.CPA_PUSH_AUTH_TYPE;
import static ru.yandex.market.core.param.model.ParamType.CPA_PUSH_FORMAT;
import static ru.yandex.market.core.param.model.ParamType.CPA_PUSH_URL;
import static ru.yandex.market.core.param.model.ParamType.GENERIC_PROMO_BUNDLES_SUPPORTED;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
class HttpSyncPushApiSettingsRepositoryTest extends FunctionalTest {

    private static final long SHOP = 1L;
    private static final long ACTION = 1L;

    @Autowired
    private HttpSyncPushApiSettingsRepository pushApiSettingsRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private ParamService paramService;
    @Autowired
    private TimezoneDao timezoneDao;
    @Autowired
    private PushApi pushApi;
    @Autowired
    private CheckouterClient checkouterClient;
    @Autowired
    private CheckouterShopApi checkouterShopApi;

    @BeforeEach
    void prepare() {
        TimeZone timeZone = TimeZone.getTimeZone("Europe/Moscow");
        timezoneDao.createTimezone(timeZone.getID(),
                (int) SECONDS.convert(timeZone.getRawOffset(), TimeUnit.MILLISECONDS));

        Mockito.when(checkouterClient.shops()).thenReturn(checkouterShopApi);
        Mockito.doNothing().when(checkouterShopApi).pushSchedules(any());

    }

    @Test
    @DbUnitDataSet(before = "HttpSyncPushApiSettingsRepositoryTest.before.csv")
    void shouldSaveFeatureFlagsToShopSettingsFromParams() {
        Settings settings = typicalSettings();
        saveAsParams(settings);
        Map<SettingsType, Settings> settingsMap = pushApiSettingsRepository.getPushApiSettings(
                PartnerId.partnerId(SHOP, CampaignType.SUPPLIER), EnumSet.of(PRODUCTION));

        assertThat(settingsMap, notNullValue());
        assertThat(settingsMap, hasEntry(is(PRODUCTION), hasProperty("features", is(settings.getFeatures()))));
    }

    @Test
    @DbUnitDataSet(before = "HttpSyncPushApiSettingsRepositoryTest.before.csv")
    void shouldGetFeatureFlagsFromParams() {
        pushApiSettingsRepository.savePushApiSettings(
                PartnerId.supplierId(SHOP),
                ImmutableMap.of(PRODUCTION,
                        Settings.builder()
                                .urlPrefix("localhost")
                                .authToken("auth")
                                .authType(AuthType.URL)
                                .partnerInterface(false)
                                .features(Features.builder()
                                        .enabledGenericBundleSupport(false)
                                        .build())
                                .build()),
                1L
        );

        Map<SettingsType, Settings> settingsMap = pushApiSettingsRepository.getPushApiSettings(
                PartnerId.partnerId(SHOP, CampaignType.SUPPLIER), EnumSet.of(PRODUCTION));

        assertThat(settingsMap, notNullValue());
        assertThat(settingsMap.get(PRODUCTION).getFeatures().isEnabledGenericBundleSupport(), is(false));

        paramService.setParam(
                new BooleanParamValue(GENERIC_PROMO_BUNDLES_SUPPORTED, SHOP, true), ACTION);

        settingsMap = pushApiSettingsRepository.getPushApiSettings(
                PartnerId.partnerId(SHOP, CampaignType.SUPPLIER), EnumSet.of(PRODUCTION));

        assertThat(settingsMap, notNullValue());
        assertThat(settingsMap.get(PRODUCTION).getFeatures().isEnabledGenericBundleSupport(), is(true));
    }

    @Test
    @DbUnitDataSet(before = "HttpSyncPushApiSettingsRepositoryTest.before.csv")
    void shouldSaveFeatureFlagsToShopSettings() {
        Settings settings = typicalSettings();

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                pushApiSettingsRepository.storePushApiSettings(SHOP, Map.of(PRODUCTION, settings));
            }
        });

        Map<SettingsType, Settings> settingsMap = pushApiSettingsRepository.getPushApiSettings(
                PartnerId.partnerId(SHOP, CampaignType.SUPPLIER), EnumSet.of(PRODUCTION));

        assertThat(settingsMap, notNullValue());
        assertThat(settingsMap, hasEntry(is(PRODUCTION), hasProperty("features", is(settings.getFeatures()))));
    }

    @Test
    @DbUnitDataSet(before = "HttpSyncPushApiSettingsRepositoryTest.before.csv")
    void shouldSavePushApiSettingsWithoutType() {

        pushApiSettingsRepository.savePushApiSettings(
                PartnerId.supplierId(SHOP),
                ImmutableMap.of(PRODUCTION,
                        Settings.builder()
                                .urlPrefix("localhost")
                                .authToken("auth")
                                .authType(AuthType.URL)
                                .partnerInterface(false)
                                .features(Features.builder()
                                        .enabledGenericBundleSupport(true)
                                        .build())
                                .build()),
                1L
        );

        verify(pushApi).settings(
                eq(SHOP),
                ArgumentMatchers.refEq(Settings.builder()
                        .urlPrefix("localhost")
                        .authToken("auth")
                        .authType(AuthType.URL)
                        .dataType(DataType.JSON)
                        .partnerInterface(false)
                        .features(Features.builder()
                                .enabledGenericBundleSupport(true)
                                .build())
                        .build()),
                eq(false)
        );
    }

    private void saveAsParams(@Nonnull Settings settings) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                paramService.setParam(
                        new StringParamValue(CPA_PUSH_AUTH_TYPE, SHOP, settings.getAuthType().name()), ACTION);
                paramService.setParam(
                        new StringParamValue(CPA_PUSH_FORMAT, SHOP, settings.getDataType().name()), ACTION);
                paramService.setParam(
                        new StringParamValue(CPA_PUSH_URL, SHOP, settings.getUrlPrefix()), ACTION);
                paramService.setParam(
                        new BooleanParamValue(GENERIC_PROMO_BUNDLES_SUPPORTED, SHOP,
                                settings.getFeatures().isEnabledGenericBundleSupport()), ACTION);
            }
        });

    }

    private static Settings typicalSettings() {
        return Settings.builder()
                .urlPrefix("https://localhost")
                .authToken("auth")
                .dataType(DataType.JSON)
                .authType(AuthType.URL)
                .partnerInterface(false)
                .features(Features.builder()
                        .enabledGenericBundleSupport(true)
                        .build())
                .build();
    }
}

package ru.yandex.market.checkout.checkouter.storage.shop;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.checkout.application.AbstractServicesTestBase;
import ru.yandex.market.checkout.pushapi.settings.AuthType;
import ru.yandex.market.checkout.pushapi.settings.DataType;
import ru.yandex.market.checkout.pushapi.settings.Features;
import ru.yandex.market.checkout.pushapi.settings.Settings;
import ru.yandex.market.checkouter.jooq.tables.records.ShopSettingsRecord;
import ru.yandex.market.request.trace.RequestContextHolder;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.yandex.market.checkout.util.SettingsUtils.sameSettings;
import static ru.yandex.market.checkouter.jooq.Tables.SHOP_SETTINGS;

/**
 * @author kl1san
 */
public class PushApiShopSettingsDaoTest extends AbstractServicesTestBase {

    public static final long SHOP_ID = 500;
    public static final String URL_PREFIX = "prefix";
    public static final String AUTH_TOKEN = "token";
    public static final DataType DATA_TYPE = DataType.XML;
    public static final AuthType AUTH_TYPE = AuthType.URL;
    public static final byte[] FINGERPRINT = new byte[]{0x01, 0x02};
    public static final Boolean PARTNER_INTERFACE = Boolean.TRUE;
    public static final String CHANGER_ID = "changerId";

    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private PushApiShopSettingsDao settingsDao;
    @Autowired
    DSLContext dsl;

    @Test
    public void shouldGetOnlyForSandbox() {
        updateSettings(SHOP_ID, getDefaultSettings(), true);
        Settings sbx = settingsDao.getSettings(SHOP_ID, true);
        Settings prod = settingsDao.getSettings(SHOP_ID, false);
        assertNotNull(sbx);
        assertNull(prod);
    }

    @Test
    public void shouldGetFeatures() {
        updateSettings(SHOP_ID, getDefaultSettings().toBuilder().features(Features.builder()
                .enabledGenericBundleSupport(true)
                .build())
                .build(), true);
        Settings sbx = settingsDao.getSettings(SHOP_ID, true);
        assertNotNull(sbx);
        assertNotNull(sbx.getFeatures());
        assertTrue(sbx.getFeatures().isEnabledGenericBundleSupport());
    }

    @Test
    public void shouldStoreAllFields() {
        Settings defaultSettings = getDefaultSettings();
        updateSettings(SHOP_ID, defaultSettings, false);
        Settings storedSettings = settingsDao.getSettings(SHOP_ID, false);
        assertThat(storedSettings, sameSettings(defaultSettings));
    }

    @Test
    public void shouldDeleteSettings() {
        updateSettings(SHOP_ID, getDefaultSettings(), false);
        assertNotNull(settingsDao.getSettings(SHOP_ID, false));
        deleteSettings(SHOP_ID, false);
        assertNull(settingsDao.getSettings(SHOP_ID, false));
    }

    @Test
    public void shouldUpdateSettings() {
        updateSettings(SHOP_ID, getDefaultSettings(), false);
        Settings newSettings = new Settings("newPrefix", "newToken", DataType.JSON, AuthType.HEADER,
                new byte[]{0x01, 0x02});
        updateSettings(SHOP_ID, newSettings, false);
        Settings updatedSettings = settingsDao.getSettings(SHOP_ID, false);
        assertThat(updatedSettings, sameSettings(newSettings));
    }

    @Test
    public void shouldSaveStubSettings() {
        Settings stubSettings = Settings.builder()
                .partnerInterface(false)
                .build();
        updateSettings(SHOP_ID, stubSettings, false);
        Settings updatedSettings = settingsDao.getSettings(SHOP_ID, false);
        assertThat(updatedSettings, sameSettings(stubSettings));
    }

    @Test
    public void shouldSaveBigUrl() {
        Settings settings = getDefaultSettings().toBuilder()
                .urlPrefix("https://online.myshopwithverybigurllength" +
                        ".ru/api/yandex/market/2e0c2222-2046-22e1-8131-f8fc000093e2/offer/8738497d-7f2c/8738497d-7f2c" +
                        "/8738497d-7f2c/8738497d-7f2c/8738497d-7f2c/")
                .build();
        updateSettings(SHOP_ID, settings, false);
        Settings updatedSettings = settingsDao.getSettings(SHOP_ID, false);
        assertThat(updatedSettings, sameSettings(settings));
    }

    @Test
    public void shouldSaveAuditInfo() {
        RequestContextHolder.createNewContext();
        updateSettings(SHOP_ID, getDefaultSettings(), false);
        ShopSettingsRecord settingsEntity =
                dsl.selectFrom(SHOP_SETTINGS)
                        .where(SHOP_SETTINGS.SHOP_ID.eq(SHOP_ID))
                        .and(SHOP_SETTINGS.SANDBOX.eq(false))
                        .fetchSingle();
        assertNotNull(settingsEntity.getUpdatedAt());
        assertNotNull(settingsEntity.getRequestId());
    }

    private void updateSettings(long shopId, Settings settings, boolean sandbox) {
        transactionTemplate.execute(tc -> {
            settingsDao.updateSettings(shopId, settings, sandbox);
            return null;
        });
    }

    private void deleteSettings(long shopId, boolean sandbox) {
        transactionTemplate.execute(tc -> {
            settingsDao.deleteSettings(shopId, sandbox);
            return null;
        });
    }

    private Settings getDefaultSettings() {
        return Settings.builder()
                .urlPrefix(URL_PREFIX)
                .authToken(AUTH_TOKEN)
                .dataType(DATA_TYPE)
                .authType(AUTH_TYPE)
                .fingerprint(FINGERPRINT)
                .partnerInterface(PARTNER_INTERFACE)
                .changerId(CHANGER_ID)
                .build();
    }

}

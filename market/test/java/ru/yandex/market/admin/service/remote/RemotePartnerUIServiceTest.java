package ru.yandex.market.admin.service.remote;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.admin.ui.model.partner.UICampaignType;
import ru.yandex.market.admin.ui.model.partner.UIPartnerInfo;
import ru.yandex.market.checkout.pushapi.client.PushApi;
import ru.yandex.market.checkout.pushapi.settings.Settings;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Функциональные тесты для {@link RemotePartnerUIService}.
 *
 * @author avetokhin 29.08.18.
 */
class RemotePartnerUIServiceTest extends FunctionalTest {
    private static final UIPartnerInfo P_1 = partner(1, 100, "Supplier123Shop", UICampaignType.SUPPLIER);
    private static final UIPartnerInfo P_2 = partner(2, 200, "SupplierTest", UICampaignType.SUPPLIER);
    private static final UIPartnerInfo P_3 = partner(3, 300, "testShop", UICampaignType.SHOP);
    private static final UIPartnerInfo P_4 = partner(5, 500, "MobileShopTest", UICampaignType.SHOP);

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.SHORT)
            .withLocale(Locale.ROOT)
            .withZone(ZoneId.systemDefault());
    /**
     * Поскольку пуш апи для админки используется только в тестинге, всегда false.
     */
    private static final boolean PUSHAPI_SANDOB = false;

    @Autowired
    private Clock clock;

    @Autowired
    private PushApi pushApi;

    @Autowired
    private RemotePartnerUIService partnerUIService;

    private static UIPartnerInfo partner(long id, long campaignId, String name, UICampaignType type) {
        var partner = new UIPartnerInfo();
        partner.setField(UIPartnerInfo.ID, id);
        partner.setField(UIPartnerInfo.NAME, name);
        partner.setField(UIPartnerInfo.CAMPAIGN_ID, campaignId);
        partner.setField(UIPartnerInfo.TYPE, type);
        return partner;
    }

    @Test
    @DbUnitDataSet(before = "RemotePartnerUIServiceTest.before.csv")
    void searchPartner() {
        // Сначала P_3 - совпадает начало имени, затем P_2 - название короче, затем P_5.
        var partners = partnerUIService.searchPartner("test", 0, 10);
        assertThat(partners).containsExactly(
                P_3,
                P_2,
                P_4
        );

        // Аналогично предыдущему, но с ограничением по выбранным строкам
        partners = partnerUIService.searchPartner("test", 1, 3);
        assertThat(partners).containsExactly(
                P_2,
                P_4
        );

        // Совпало название P_4 и домен P_5.
        // По доменам поиск пока убрали, см MBI-82831
//        partners = partnerUIService.searchPartner("vape", 0, 9);
//        assertThat(partners).containsExactly(
//                P_4
//        );

        // Совпал один идентификатор P_2 и часть названия P_1.
        partners = partnerUIService.searchPartner("2", 0, 10);
        assertThat(partners).containsExactly(
                P_2,
                P_1
        );
    }

    @Test
    @DbUnitDataSet(
            before = "RemotePartnerUIServiceTest.before.csv",
            after = "RemotePartnerUIServiceTest.afterUpdateManager.csv"
    )
    void updateManagerId() {
        partnerUIService.updateManagerId(1, -1);
        partnerUIService.updateManagerId(2, 1001);
    }

    @DisplayName("Получить дату окончания детализации push api логов")
    @Test
    @DbUnitDataSet(before = "RemoteSupplierUIServiceTest.before.csv")
    void getDateUntilForceLogResponseTest() {
        var shopId = 1L;
        var instant = Instant.EPOCH;
        var timestamp = Timestamp.from(instant);
        var settings = Mockito.mock(Settings.class);

        doReturn(timestamp)
                .when(settings).getForceLogResponseUntil();
        doReturn(settings)
                .when(pushApi).getSettings(shopId, PUSHAPI_SANDOB);
        var formattedDate = partnerUIService.getDateUntilForceLogResponse(shopId);
        verify(pushApi).getSettings(shopId, PUSHAPI_SANDOB);
        assertThat(formattedDate).isEqualTo(DATE_TIME_FORMAT.format(instant));

        doReturn(null)
                .when(settings).getForceLogResponseUntil();
        formattedDate = partnerUIService.getDateUntilForceLogResponse(shopId);
        verify(pushApi, times(2)).getSettings(shopId, PUSHAPI_SANDOB);
        assertThat(formattedDate).isNull();
    }

    @DisplayName("Установить дату окончания детализации push api логов")
    @Test
    @DbUnitDataSet(before = "RemoteSupplierUIServiceTest.before.csv")
    void setDateUntilForceLogResponse() {
        var shopId = 1L;
        var days = 2;
        var instant = Instant.EPOCH;
        var instant2 = Instant.EPOCH.plus(days, ChronoUnit.DAYS);
        var timestamp = Timestamp.from(instant);
        var settings = Mockito.mock(Settings.class);

        doReturn(timestamp)
                .when(settings).getForceLogResponseUntil();
        doReturn(settings).
                when(pushApi).getSettings(shopId, PUSHAPI_SANDOB);
        doReturn(instant).
                when(clock).instant();
        var formattedDate = partnerUIService.setDateUntilForceLogResponse(shopId, days);
        verify(pushApi).settings(shopId, settings, PUSHAPI_SANDOB);
        assertThat(formattedDate).isEqualTo(DATE_TIME_FORMAT.format(instant2));
    }

}

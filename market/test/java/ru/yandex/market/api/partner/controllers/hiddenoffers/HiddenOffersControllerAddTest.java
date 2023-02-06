package ru.yandex.market.api.partner.controllers.hiddenoffers;

import java.util.List;

import javax.annotation.Nonnull;

import Market.DataCamp.DataCampOfferMeta;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.helper.PartnerApiFunctionalTestHelper;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.logbroker.event.datacamp.PapiHiddenOfferDataCampEvent;
import ru.yandex.market.core.logbroker.event.datacamp.SyncChangeOfferLogbrokerEvent;
import ru.yandex.market.mbi.util.MbiAsserts;

import static ru.yandex.market.api.partner.logbroker.util.ChangeOfferLogbrokerTestUtil.assertEvent;
import static ru.yandex.market.api.partner.logbroker.util.ChangeOfferLogbrokerTestUtil.getLogbrokerEvents;

/**
 * Date: 27.11.2020
 * Project: arcadia-market_mbi_mbi
 *
 * @author alexminakov
 */
@DbUnitDataSet(before = "HiddenOffersController/add/csv/add.before.csv")
public class HiddenOffersControllerAddTest extends AbstractHiddenOffersControllerTest {

    public HiddenOffersControllerAddTest() {
        super("add", true);
    }

    @DbUnitDataSet(
            before = "HiddenOffersController/add/csv/shopPull.before.csv",
            after = "HiddenOffersController/add/csv/shopPull.after.csv"
    )
    @Test
    @DisplayName("Скрытие офферов для белого pull магазина.")
    void addHiddenOffers_shopPull_successful() {
        List<SyncChangeOfferLogbrokerEvent> logbrokerEvents = sendShopRequest("shopPull.request", 101002L);

        assertShop(logbrokerEvents.get(0), 102L, 1002L,
                DataCampOfferMeta.DataSource.PULL_PARTNER_API, DataCampOfferMeta.DataSource.PULL_PARTNER_API);
    }

    @DbUnitDataSet(
            after = "HiddenOffersController/add/csv/shopPullToPush.after.csv"
    )
    @Test
    @DisplayName("Скрытие офферов для белого магазина, который переключается в пуш. Сохраняются в базу.")
    void addHiddenOffers_shopPullToPush_successful() {
        List<SyncChangeOfferLogbrokerEvent> logbrokerEvents = sendShopRequest("shopPullToPush.request", 101005L);

        assertShop(logbrokerEvents.get(0), 106L, 1005L,
                DataCampOfferMeta.DataSource.MARKET_PRICELABS, DataCampOfferMeta.DataSource.PUSH_PARTNER_API);
    }

    @DbUnitDataSet(
            before = "HiddenOffersController/add/csv/shopPush.before.csv",
            after = "HiddenOffersController/add/csv/shopPush.after.csv"
    )
    @Test
    @DisplayName("Скрытие офферов для белого push магазина.")
    void addHiddenOffers_shopPush_successful() {
        List<SyncChangeOfferLogbrokerEvent> logbrokerEvents = sendShopRequest("shopPush.request", 101003L);

        assertShop(logbrokerEvents.get(0), 105L, 1003L,
                DataCampOfferMeta.DataSource.MARKET_PRICELABS, DataCampOfferMeta.DataSource.PUSH_PARTNER_API);
    }

    @DbUnitDataSet(
            before = "HiddenOffersController/add/csv/directPush.before.csv",
            after = "HiddenOffersController/add/csv/directPush.after.csv"
    )
    @Test
    @DisplayName("Скрытие офферов для direct push магазина.")
    void addHiddenOffers_directPush_successful() {
        List<SyncChangeOfferLogbrokerEvent> logbrokerEvents = sendShopRequest("directPush.request", 101010);

        assertDirect(logbrokerEvents.get(0), 110L, 1010L,
                DataCampOfferMeta.DataSource.MARKET_PRICELABS, DataCampOfferMeta.DataSource.PUSH_PARTNER_API);
    }

    @DbUnitDataSet(
            before = "HiddenOffersController/add/csv/supplierPull.before.csv",
            after = "HiddenOffersController/add/csv/supplierPull.after.csv"
    )
    @Test
    @DisplayName("Скрытие офферов для синего pull магазина.")
    void addHiddenOffers_supplierPull_successful() {
        assertSupplierRequest("supplierPull.request", 101001L, 101L, 1001L,
                DataCampOfferMeta.DataSource.PULL_PARTNER_API, true);
    }

    @DbUnitDataSet(
            before = "HiddenOffersController/add/csv/supplierPush.before.csv",
            after = "HiddenOffersController/add/csv/supplierPush.after.csv"
    )
    @Test
    @DisplayName("Скрытие офферов для синего push магазина.")
    void addHiddenOffers_supplierPush_successful() {
        assertSupplierRequest("supplierPush.request", 101004L, 106L, 1004L,
                DataCampOfferMeta.DataSource.PUSH_PARTNER_API, false);
    }

    @DbUnitDataSet(
            before = "HiddenOffersController/add/csv/addHiddenOffersForMigratingFbs.before.csv",
            after = "HiddenOffersController/add/csv/addHiddenOffersForMigratingFbs.after.csv"
    )
    @Test
    @DisplayName("Скрытие офферов для многоскладового fbs в процессе расклейки")
    void addHiddenOffersForMigratingFbs() {

        mockUltraControllerClient();
        assertResponse("supplierPush.request", 101004);
        List<SyncChangeOfferLogbrokerEvent> supplierLogbrokerEvents = getLogbrokerEvents(marketQuickLogbrokerService, 2);
        org.assertj.core.api.Assertions.assertThat(supplierLogbrokerEvents).hasSize(2);

        assertSupplier(supplierLogbrokerEvents.get(0), 106, 1004, 100L, DataCampOfferMeta.DataSource.PUSH_PARTNER_API, false);
        assertEvent(supplierLogbrokerEvents, 0, PapiHiddenOfferDataCampEvent.class);

        assertSupplier(supplierLogbrokerEvents.get(1), 101, 1001, 100L, DataCampOfferMeta.DataSource.PUSH_PARTNER_API, false);
        assertEvent(supplierLogbrokerEvents, 1, PapiHiddenOfferDataCampEvent.class);
    }

    @DbUnitDataSet(before = "HiddenOffersController/add/csv/supplierPush.locked.before.csv")
    @Test
    @DisplayName("Скрытие офферов для синего push магазина. Миграция бизнеса - падаем.")
    void addHiddenOffers_supplierPush_locked_fail() {
        try {
            addHiddenOffers_supplierPush_successful();
            Assertions.fail("Migration check failed: check ignored");
        } catch (HttpClientErrorException ex) {
            Assertions.assertEquals(HttpStatus.LOCKED, ex.getStatusCode());
            Assertions.assertEquals("Locked", ex.getStatusText());
            MbiAsserts.assertJsonEquals("{\n" +
                    "  \"status\": \"ERROR\",\n" +
                    "  \"errors\": [\n" +
                    "    {\n" +
                    "      \"code\": \"LOCKED\",\n" +
                    "      \"message\": \"Partner is in business migration: 1004\"\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}", ex.getResponseBodyAsString());
        }
    }

    @Nonnull
    @Override
    protected ResponseEntity<String> getResponseEntity(String file, long campaignId) {
        return PartnerApiFunctionalTestHelper.postForJson(
                getUrl(campaignId, Format.JSON),
                file,
                USER_ID
        );
    }
}

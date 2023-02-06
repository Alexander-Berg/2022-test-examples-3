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
@DbUnitDataSet(before = "HiddenOffersController/delete/csv/delete.before.csv")
public class HiddenOffersControllerDeleteTest extends AbstractHiddenOffersControllerTest {

    public HiddenOffersControllerDeleteTest() {
        super("delete", false);
    }

    @DbUnitDataSet(
            before = "HiddenOffersController/delete/csv/shopPull.before.csv",
            after = "HiddenOffersController/delete/csv/shopPull.after.csv"
    )
    @Test
    @DisplayName("Скрытие офферов для белого pull магазина.")
    void deleteHiddenOffers_shopPull_successful() {
        List<SyncChangeOfferLogbrokerEvent> logbrokerEvents = sendShopRequest("shopPull.request", 101002L);

        assertShop(logbrokerEvents.get(0), 102L, 1002L,
                DataCampOfferMeta.DataSource.PULL_PARTNER_API, DataCampOfferMeta.DataSource.PULL_PARTNER_API);
    }

    @DbUnitDataSet(
            before = "HiddenOffersController/delete/csv/shopPullToPush.before.csv",
            after = "HiddenOffersController/delete/csv/shopPullToPush.after.csv"
    )
    @Test
    @DisplayName("Скрытие офферов для белого магазина, который переключается в push.")
    void deleteHiddenOffers_shopPullToPush_successful() {
        List<SyncChangeOfferLogbrokerEvent> logbrokerEvents = sendShopRequest("shopPullToPush.request", 101005L);

        assertShop(logbrokerEvents.get(0), 106L, 1005L,
                DataCampOfferMeta.DataSource.MARKET_PRICELABS, DataCampOfferMeta.DataSource.PUSH_PARTNER_API);
    }

    @DbUnitDataSet(
            before = "HiddenOffersController/delete/csv/shopPush.before.csv",
            after = "HiddenOffersController/delete/csv/shopPush.after.csv"
    )
    @Test
    @DisplayName("Скрытие офферов для белого push магазина.")
    void deleteHiddenOffers_shopPush_successful() {
        List<SyncChangeOfferLogbrokerEvent> logbrokerEvents = sendShopRequest("shopPush.request", 101003L);

        assertShop(logbrokerEvents.get(0), 105L, 1003L,
                DataCampOfferMeta.DataSource.MARKET_PRICELABS, DataCampOfferMeta.DataSource.PUSH_PARTNER_API);
    }

    @DbUnitDataSet(
            before = "HiddenOffersController/delete/csv/supplierPull.before.csv",
            after = "HiddenOffersController/delete/csv/supplierPull.after.csv"
    )
    @Test
    @DisplayName("Скрытие офферов для синего pull магазина.")
    void deleteHiddenOffers_supplierPull_successful() {
        assertSupplierRequest("supplierPull.request", 101001L, 101L, 1001L,
                DataCampOfferMeta.DataSource.PULL_PARTNER_API, true);
    }

    @DbUnitDataSet(
            before = "HiddenOffersController/delete/csv/supplierPush.before.csv",
            after = "HiddenOffersController/delete/csv/supplierPush.after.csv"
    )
    @Test
    @DisplayName("Скрытие офферов для синего push магазина.")
    void deleteHiddenOffers_supplierPush_successful() {
        assertSupplierRequest("supplierPush.request", 101004L, 106L, 1004L,
                DataCampOfferMeta.DataSource.PUSH_PARTNER_API, false);
    }

    @DbUnitDataSet(
            before = "HiddenOffersController/delete/csv/deleteHiddenOffersForMultiFbs.before.csv",
            after = "HiddenOffersController/delete/csv/deleteHiddenOffersForMultiFbs.after.csv"
    )
    @Test
    @DisplayName("Скрытие офферов для многоскладового fbs 1:n в процессе расклейки")
    void deleteHiddenOffersForMultiFbs() {
        mockUltraControllerClient();

        assertResponse("supplierPush.request", 101004);

        List<SyncChangeOfferLogbrokerEvent> logbrokerEvents = getLogbrokerEvents(marketQuickLogbrokerService, 2);
        org.assertj.core.api.Assertions.assertThat(logbrokerEvents).hasSize(2);

        assertSupplier(logbrokerEvents.get(0), 106, 1004, 100L, DataCampOfferMeta.DataSource.PUSH_PARTNER_API, false);
        assertEvent(logbrokerEvents, 0, PapiHiddenOfferDataCampEvent.class);

        assertSupplier(logbrokerEvents.get(1), 101, 1001, 100L, DataCampOfferMeta.DataSource.PUSH_PARTNER_API, false);
        assertEvent(logbrokerEvents, 1, PapiHiddenOfferDataCampEvent.class);
    }

    @DbUnitDataSet(before = "HiddenOffersController/delete/csv/supplierPush.locked.before.csv")
    @Test
    @DisplayName("Скрытие офферов для синего push магазина. Миграция бизнеса - падаем.")
    void deleteHiddenOffers_supplierPush_locked_fail() {
        try {
            deleteHiddenOffers_supplierPush_successful();
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
        return PartnerApiFunctionalTestHelper.deleteForJson(
                getUrl(campaignId, Format.JSON),
                file,
                USER_ID
        );
    }
}

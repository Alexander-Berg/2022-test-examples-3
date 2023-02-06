package ru.yandex.market.abo.core.storage.json.hidden;

import java.util.EnumSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.EmptyTest;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.abo.api.entity.offer.hidden.details.AssessorHidingDetails;
import ru.yandex.market.abo.api.entity.offer.hidden.details.DeliveryComparison;
import ru.yandex.market.abo.api.entity.offer.hidden.details.DeliveryOption;
import ru.yandex.market.abo.api.entity.offer.hidden.details.HidingDetails;
import ru.yandex.market.abo.api.entity.offer.hidden.details.PriceComparison;
import ru.yandex.market.abo.api.entity.offer.hidden.details.PriceWithCurrency;
import ru.yandex.market.abo.api.entity.offer.hidden.details.StockComparison;
import ru.yandex.market.abo.api.entity.problem.partner.PartnerCheckMethod;
import ru.yandex.market.abo.core.storage.json.model.JsonEntityType;
import ru.yandex.market.abo.core.storage.json.model.JsonStorageId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author artemmz
 * created on 27.06.17.
 */
public class JsonHidingDetailsServiceTest extends EmptyTest {
    @Autowired
    private JsonHidingDetailsService service;

    @Test
    public void testFindSave() {
        JsonStorageId storageId = new JsonStorageId("cm_id", JsonEntityType.HIDDEN_BY_COMPLAINT);
        JsonHidingDetails jsonHidingDetails = new JsonHidingDetails(
                storageId,
                HidingDetails.newBuilder()
                        .stockComparison(new StockComparison(true, false))
                        .assessorHidingDetails(new AssessorHidingDetails(1, 1, PartnerCheckMethod.PHONE, "comment"))
                        .deliveryComparison(new DeliveryComparison(
                                new DeliveryOption(123.0, Currency.RUR),
                                new DeliveryOption(456.0, Currency.USD)
                        ))
                        .priceComparison(new PriceComparison(
                                new PriceWithCurrency(11.0, Currency.EUR),
                                new PriceWithCurrency(500.0, Currency.GBP)
                        ))
                        .build()
        );

        service.save(jsonHidingDetails);
        JsonHidingDetails fromDbDetails = service.findOne(storageId);

        assertEquals(jsonHidingDetails, fromDbDetails);
        assertEquals(
                jsonHidingDetails.getStoredEntity().getAssessorHidingDetails().getAssessorComment(),
                fromDbDetails.getStoredEntity().getAssessorHidingDetails().getAssessorComment()
        );
        assertEquals(
                jsonHidingDetails.getStoredEntity().getStockComparison(),
                fromDbDetails.getStoredEntity().getStockComparison()
        );
    }

    @Test
    public void testCleanOld() {
        String ID = "5";
        HidingDetails details = HidingDetails.newBuilder().checkRegionId(1L).build();

        JsonHidingDetails feedFromDb = service.saveFeedDiff(new JsonStorageId(ID, JsonEntityType.HIDDEN_BY_COMPLAINT), details);
        JsonHidingDetails cartFromDb = service.saveCartDiff(Long.parseLong(ID), details);
        flushAndClear();

        Stream.of(feedFromDb, cartFromDb).forEach(Assertions::assertNotNull);
        service.setCleanOlderThanDays(0);
        service.clean();
        flushAndClear();
        EnumSet.of(JsonEntityType.HIDDEN_BY_COMPLAINT, JsonEntityType.HIDDEN_CART_DIFF)
                .forEach(type -> assertNull(service.findOne(new JsonStorageId(ID, type))));
    }
}

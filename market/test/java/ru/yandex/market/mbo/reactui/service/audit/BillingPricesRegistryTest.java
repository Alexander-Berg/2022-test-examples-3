package ru.yandex.market.mbo.reactui.service.audit;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.billing.PaidAction;
import ru.yandex.market.mbo.db.billing.dao.FullPaidEntry;
import ru.yandex.market.mbo.gwt.models.audit.AuditAction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * @author dergachevfv
 * @since 12/19/19
 */
public class BillingPricesRegistryTest {

    private static final double PRICE = 10D;
    private static final double PRICE_CONVERTED = 300D;
    private static final long USER1 = 100L;
    private static final long USER2 = 200L;
    private static final String OFFER_ID_1 = "1000";
    private static final String OFFER_ID_2 = "2000";
    private static final String OFFER_ID_3 = "3000";

    private BillingPricesRegistry pricesRegistry;

    @Before
    public void init() {
        List<FullPaidEntry> paidEntries = List.of(
            // classified entries:
            // 1. non-offer entries with auditActionId present
            FullPaidEntry.newBuilder().price(PRICE).auditActionId(1L).uid(USER1).build(),
            FullPaidEntry.newBuilder().price(PRICE).auditActionId(1L).uid(USER2).build(),
            FullPaidEntry.newBuilder().price(PRICE).auditActionId(2L).uid(USER1).build(),
            // 2. offer actions with linkData present
            FullPaidEntry.newBuilder().price(PRICE).linkData(OFFER_ID_1)
                .uid(USER1).operationId((long) PaidAction.YANG_SKU_MAPPING.getId())
                .build(),
            FullPaidEntry.newBuilder().price(PRICE).linkData(OFFER_ID_1)
                .uid(USER2).operationId((long) PaidAction.YANG_SKU_MAPPING_VERIFICATION.getId())
                .build(),
            FullPaidEntry.newBuilder().price(PRICE).linkData(OFFER_ID_2)
                .uid(USER1).operationId((long) PaidAction.YANG_SKU_MAPPING.getId())
                .build(),
            FullPaidEntry.newBuilder().price(PRICE).linkData(OFFER_ID_3)
                .uid(USER2).operationId((long) PaidAction.YANG_SKU_MAPPING_CORRECTION.getId())
                .build(),
            // unclassified entries:
            // 1. non-offer entries without auditActionId
            FullPaidEntry.newBuilder().price(PRICE).uid(USER1).build(),
            // 2. offer actions without linkData
            FullPaidEntry.newBuilder().price(PRICE)
                .uid(USER1).operationId((long) PaidAction.YANG_SKU_MAPPING.getId())
                .build()
        );

        List<FullPaidEntry> operatorErrorsPaidEntries = List.of(
            FullPaidEntry.newBuilder().price(PRICE).linkData(OFFER_ID_3)
                .uid(USER1).operationId((long) PaidAction.YANG_SKU_MAPPING.getId())
                .build()
        );

        pricesRegistry = new BillingPricesRegistry(paidEntries, operatorErrorsPaidEntries);
    }

    @Test
    public void testPopPriceForOperatorError() {
        List<FullPaidEntry> leftPaidEntriesBefore = pricesRegistry.getLeftPaidEntries();
        BillingPricesRegistry operatorErrorsRegistry = pricesRegistry.forOperatorErrors();

        Optional<BigDecimal> price = operatorErrorsRegistry.popPrice(USER1, OFFER_ID_3);

        Assertions.assertThat(price).isPresent();
        Assertions.assertThat(price).contains(BigDecimal.valueOf(PRICE_CONVERTED));

        Assertions.assertThat(operatorErrorsRegistry.getLeftPaidEntries()).isEmpty();
        Assertions.assertThat(leftPaidEntriesBefore)
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrderElementsOf(pricesRegistry.getLeftPaidEntries());
    }

    @Test
    public void testPopPriceByAuditActions() {
        AuditAction action = new AuditAction().setActionId(1L);

        Optional<BigDecimal> price = pricesRegistry.popPrice(USER1, action);

        Assertions.assertThat(price).isPresent();
        Assertions.assertThat(price).contains(BigDecimal.valueOf(PRICE_CONVERTED));

        Assertions.assertThat(pricesRegistry.getLeftPaidEntries())
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                // this was popped
                // FullPaidEntry.newBuilder().price(PRICE_CONVERTED).auditActionId(1L).uid(USER1).build(),
                FullPaidEntry.newBuilder().price(PRICE_CONVERTED).auditActionId(1L).uid(USER2).build(),
                FullPaidEntry.newBuilder().price(PRICE_CONVERTED).auditActionId(2L).uid(USER1).build(),
                FullPaidEntry.newBuilder().price(PRICE_CONVERTED).linkData(OFFER_ID_1)
                    .uid(USER1).operationId((long) PaidAction.YANG_SKU_MAPPING.getId())
                    .build(),
                FullPaidEntry.newBuilder().price(PRICE_CONVERTED).linkData(OFFER_ID_1)
                    .uid(USER2).operationId((long) PaidAction.YANG_SKU_MAPPING_VERIFICATION.getId())
                    .build(),
                FullPaidEntry.newBuilder().price(PRICE_CONVERTED).linkData(OFFER_ID_2)
                    .uid(USER1).operationId((long) PaidAction.YANG_SKU_MAPPING.getId())
                    .build(),
                FullPaidEntry.newBuilder().price(PRICE_CONVERTED).linkData(OFFER_ID_3)
                    .uid(USER2).operationId((long) PaidAction.YANG_SKU_MAPPING_CORRECTION.getId())
                    .build(),
                FullPaidEntry.newBuilder().price(PRICE_CONVERTED).uid(USER1).build(),
                FullPaidEntry.newBuilder().price(PRICE_CONVERTED)
                    .uid(USER1).operationId((long) PaidAction.YANG_SKU_MAPPING.getId())
                    .build()
            );
    }

    @Test
    public void testPopPriceByOfferId() {
        Optional<BigDecimal> price = pricesRegistry.popPrice(USER1, OFFER_ID_1);

        Assertions.assertThat(price).isPresent();
        Assertions.assertThat(price).contains(BigDecimal.valueOf(PRICE_CONVERTED));

        Assertions.assertThat(pricesRegistry.getLeftPaidEntries())
            .usingFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                FullPaidEntry.newBuilder().price(PRICE_CONVERTED).auditActionId(1L).uid(USER1).build(),
                FullPaidEntry.newBuilder().price(PRICE_CONVERTED).auditActionId(1L).uid(USER2).build(),
                FullPaidEntry.newBuilder().price(PRICE_CONVERTED).auditActionId(2L).uid(USER1).build(),
                // this was popped
                // FullPaidEntry.newBuilder().price(PRICE_CONVERTED).linkData(OFFER_ID_1)
                //     .uid(USER1).operationId((long) PaidAction.YANG_SKU_MAPPING.getId())
                //     .build(),
                FullPaidEntry.newBuilder().price(PRICE_CONVERTED).linkData(OFFER_ID_1)
                    .uid(USER2).operationId((long) PaidAction.YANG_SKU_MAPPING_VERIFICATION.getId())
                    .build(),
                FullPaidEntry.newBuilder().price(PRICE_CONVERTED).linkData(OFFER_ID_2)
                    .uid(USER1).operationId((long) PaidAction.YANG_SKU_MAPPING.getId())
                    .build(),
                FullPaidEntry.newBuilder().price(PRICE_CONVERTED).linkData(OFFER_ID_3)
                    .uid(USER2).operationId((long) PaidAction.YANG_SKU_MAPPING_CORRECTION.getId())
                    .build(),
                FullPaidEntry.newBuilder().price(PRICE_CONVERTED).uid(USER1).build(),
                FullPaidEntry.newBuilder().price(PRICE_CONVERTED)
                    .uid(USER1).operationId((long) PaidAction.YANG_SKU_MAPPING.getId())
                    .build()
            );
    }
}

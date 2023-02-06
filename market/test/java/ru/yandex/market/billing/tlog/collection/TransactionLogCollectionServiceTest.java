package ru.yandex.market.billing.tlog.collection;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.billing.tlog.config.TransactionLogConfig;
import ru.yandex.market.billing.tlog.model.ExportServiceType;
import ru.yandex.market.billing.tlog.model.TransactionLogItem;
import ru.yandex.market.billing.tlog.yt.TransactionLogDao;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.billing.fulfillment.promo.SupplierPromoTariffService;
import ru.yandex.market.core.partner.contract.PartnerContractDao;
import ru.yandex.market.core.supplier.SupplierService;

@ParametersAreNonnullByDefault
class TransactionLogCollectionServiceTest extends FunctionalTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2020-09-15T10:00:00Z"),
            ZoneOffset.systemDefault());

    @Autowired
    private TransactionLogDao transactionLogDao;
    @Autowired
    private RevenueTransactionLogCollectionDao revenueTransactionLogCollectionDao;
    @Autowired
    private PartnerContractDao supplierContractDao;
    @Autowired
    private SupplierService supplierService;
    @Autowired
    private SupplierPromoTariffService supplierPromoTariffService;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private TransactionLogCollectionService<TransactionLogItem> transactionLogCollectionService;

    @BeforeEach
    void init() {
        transactionLogCollectionService = new TransactionLogCollectionService<>(
                CLOCK,
                TransactionLogConfig.getTransactionLogConfig(),
                transactionLogDao,
                revenueTransactionLogCollectionDao,
                supplierContractDao,
                supplierService,
                supplierPromoTariffService,
                transactionTemplate
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testFee.before.csv",
            after = "TransactionLogCollectionServiceTest.testFee.after.csv"
    )
    void testFee() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.FEE)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testFeeWithPayload.before.csv",
            after = "TransactionLogCollectionServiceTest.testFeeWithPayload.after.csv"
    )
    void testFeeWithPayload() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.FEE)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testFeeCorrection.before.csv",
            after = "TransactionLogCollectionServiceTest.testFeeCorrection.after.csv"
    )
    void testFeeCorrection() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.FEE_CORRECTION)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testFeeCancellation.before.csv",
            after = "TransactionLogCollectionServiceTest.testFeeCancellation.after.csv"
    )
    void testFeeCancellation() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.FEE_CANCELLATION)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testFeeCancellationWithPayload.before.csv",
            after = "TransactionLogCollectionServiceTest.testFeeCancellationWithPayload.after.csv"
    )
    void testFeeCancellationWithPayload() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.FEE_CANCELLATION)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testLoyaltyParticipationFee.before.csv",
            after = "TransactionLogCollectionServiceTest.testLoyaltyParticipationFee.after.csv"
    )
    void testLoyaltyParticipationFee() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.LOYALTY_PARTICIPATION_FEE)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testLoyaltyParticipationFeeWithPayload.before.csv",
            after = "TransactionLogCollectionServiceTest.testLoyaltyParticipationFeeWithPayload.after.csv"
    )
    void testLoyaltyParticipationFeeWithPayload() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.LOYALTY_PARTICIPATION_FEE)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testLoyaltyParticipationFeeCorrection.before.csv",
            after = "TransactionLogCollectionServiceTest.testLoyaltyParticipationFeeCorrection.after.csv"
    )
    void testLoyaltyParticipationFeeCorrection() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.LOYALTY_PARTICIPATION_FEE_CORRECTION)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testLoyaltyParticipationFeeCancellation.before.csv",
            after = "TransactionLogCollectionServiceTest.testLoyaltyParticipationFeeCancellation.after.csv"
    )
    void testLoyaltyParticipationFeeCancellation() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.LOYALTY_PARTICIPATION_FEE_CANCELLATION)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testLoyaltyParticipationFeeCancellationWithPayload.before.csv",
            after = "TransactionLogCollectionServiceTest.testLoyaltyParticipationFeeCancellationWithPayload.after.csv"
    )
    void testLoyaltyParticipationFeeCancellationWithPayload() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.LOYALTY_PARTICIPATION_FEE_CANCELLATION)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testFfProcessing.before.csv",
            after = "TransactionLogCollectionServiceTest.testFfProcessing.after.csv"
    )
    void testFfProcessing() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.FF_PROCESSING)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testFfProcessingWithPayload.before.csv",
            after = "TransactionLogCollectionServiceTest.testFfProcessingWithPayload.after.csv"
    )
    void testFfProcessingWithPayload() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.FF_PROCESSING)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testFfProcessingCorrection.before.csv",
            after = "TransactionLogCollectionServiceTest.testFfProcessingCorrection.after.csv"
    )
    void testFfProcessingCorrection() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.FF_PROCESSING_CORRECTION)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testStorage.before.csv",
            after = "TransactionLogCollectionServiceTest.testStorage.after.csv"
    )
    void testStorage() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.STORAGE)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testStorageCorrection.before.csv",
            after = "TransactionLogCollectionServiceTest.testStorageCorrection.after.csv"
    )
    void testStorageCorrection() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.STORAGE_CORRECTION)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testFfWithdraw.before.csv",
            after = "TransactionLogCollectionServiceTest.testFfWithdraw.after.csv"
    )
    void testFfWithdraw() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.FF_WITHDRAW)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testFfWithdrawCorrection.before.csv",
            after = "TransactionLogCollectionServiceTest.testFfWithdrawCorrection.after.csv"
    )
    void testFfWithdrawCorrection() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.FF_WITHDRAW_CORRECTION)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testSorting.before.csv",
            after = "TransactionLogCollectionServiceTest.testSorting.after.csv"
    )
    void testSorting() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.SORTING)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testSortingCorrection.before.csv",
            after = "TransactionLogCollectionServiceTest.testSortingCorrection.after.csv"
    )
    void testSortingCorrection() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.SORTING_CORRECTION)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testFFXdocSupply.before.csv",
            after = "TransactionLogCollectionServiceTest.testFFXdocSupply.after.csv"
    )
    void testFFXdocSupply() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.FF_XDOC_SUPPLY)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testFFSurplusSupply.before.csv",
            after = "TransactionLogCollectionServiceTest.testFFSurplusSupply.after.csv"
    )
    void testFFSurplusSupply() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.FF_SURPLUS_SUPPLY)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testFFSurplusSupplyCorrection.before.csv",
            after = "TransactionLogCollectionServiceTest.testFFSurplusSupplyCorrection.after.csv"
    )
    void testFFSurplusSupplyCorrection() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.FF_SURPLUS_SUPPLY_CORRECTION)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testSupplyShortage.before.csv",
            after = "TransactionLogCollectionServiceTest.testSupplyShortage.after.csv"
    )
    void testSupplyShortage() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.SUPPLY_SHORTAGE)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testSupplyShortageCorrection.before.csv",
            after = "TransactionLogCollectionServiceTest.testSupplyShortageCorrection.after.csv"
    )
    void testSupplyShortageCorrection() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.SUPPLY_SHORTAGE_CORRECTION)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testDeliveryToCustomer.before.csv",
            after = "TransactionLogCollectionServiceTest.testDeliveryToCustomer.after.csv"
    )
    void testDeliveryToCustomer() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.DELIVERY_TO_CUSTOMER)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testCrossregionalDelivery.before.csv",
            after = "TransactionLogCollectionServiceTest.testCrossregionalDelivery.after.csv"
    )
    void testCrossregionalDelivery() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.CROSSREGIONAL_DELIVERY)
        );
    }


    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testDeliveryToCustomerWithPayload.before.csv",
            after = "TransactionLogCollectionServiceTest.testDeliveryToCustomerWithPayload.after.csv"
    )
    void testDeliveryToCustomerWithPayload() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.DELIVERY_TO_CUSTOMER)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testDeliveryToCustomerCorrection.before.csv",
            after = "TransactionLogCollectionServiceTest.testDeliveryToCustomerCorrection.after.csv"
    )
    void testDeliveryToCustomerCorrection() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.DELIVERY_TO_CUSTOMER_CORRECTION)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testDeliveryToCustomerReturn.before.csv",
            after = "TransactionLogCollectionServiceTest.testDeliveryToCustomerReturn.after.csv"
    )
    void testDeliveryToCustomerReturn() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.DELIVERY_TO_CUSTOMER_RETURN)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testDeliveryToCustomerReturnWithPayload.before.csv",
            after = "TransactionLogCollectionServiceTest.testDeliveryToCustomerReturnWithPayload.after.csv"
    )
    void testDeliveryToCustomerReturnWithPayload() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.DELIVERY_TO_CUSTOMER_RETURN)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testDeliveryToCustomerReturnCorrection.before.csv",
            after = "TransactionLogCollectionServiceTest.testDeliveryToCustomerReturnCorrection.after.csv"
    )
    void testDeliveryToCustomerReturnCorrection() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.DELIVERY_TO_CUSTOMER_RETURN_CORRECTION)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testCrossregionalDeliveryCorrection.before.csv",
            after = "TransactionLogCollectionServiceTest.testCrossregionalDeliveryCorrection.after.csv"
    )
    void testCrossregionalDeliveryCorrection() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.CROSSREGIONAL_DELIVERY_CORRECTION)
        );
    }


    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testSelfRequestedDisposal.before.csv",
            after = "TransactionLogCollectionServiceTest.testSelfRequestedDisposal.after.csv"
    )
    void testSelfRequestedDisposal() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.SELF_REQUESTED_DISPOSAL)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testSelfRequestedDisposalCorrection.before.csv",
            after = "TransactionLogCollectionServiceTest.testSelfRequestedDisposalCorrection.after.csv"
    )
    void testSelfRequestedDisposalCorrection() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.SELF_REQUESTED_DISPOSAL_CORRECTION)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testAxaptaRevenue.before.csv",
            after = "TransactionLogCollectionServiceTest.testAxaptaRevenue.after.csv"
    )
    void testAxaptaRevenue() { /* этот тест падает из IDEA при локальной ТЗ отличной от +10 и это ОК */
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.AXAPTA_REVENUE)
        );
    }

    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testOrderReturnsStorage.before.csv",
            after = "TransactionLogCollectionServiceTest.testOrderReturnsStorage.after.csv"
    )
    @Test
    void testOrderReturnsStorage() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Set.of(ExportServiceType.ORDER_RETURN_STORAGE)
        );
    }

    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testOrderReturnsStorageCorrection.before.csv",
            after = "TransactionLogCollectionServiceTest.testOrderReturnsStorageCorrection.after.csv"
    )
    @Test
    void testOrderReturnsStorageCorretion() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Set.of(ExportServiceType.ORDER_RETURN_STORAGE_CORRECTION)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testTwoCorrectionOnOneOrder.before.csv",
            after = "TransactionLogCollectionServiceTest.testTwoCorrectionOnOneOrder.after.csv"
    )
    void testTwoCorrectionOnOneOrder() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Set.of(ExportServiceType.DELIVERY_TO_CUSTOMER_CORRECTION)
        );
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Set.of(ExportServiceType.FF_PROCESSING_CORRECTION)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testSupplierWithoutContract.before.csv",
            after = "TransactionLogCollectionServiceTest.testSupplierWithoutContract.after.csv"
    )
    void testSupplierWithoutContract() {
        try {
            transactionLogCollectionService.collectTransactionLogItemsForService(
                    Collections.singleton(ExportServiceType.FEE)
            );
        } catch (Exception e) {
            Assert.assertEquals("Unable to find client_id for supplier_id 775", e.getMessage());
        }
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testAgencyCommission.before.csv",
            after = "TransactionLogCollectionServiceTest.testAgencyCommission.after.csv"
    )
    void testAgencyCommission() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Set.of(ExportServiceType.AGENCY_COMMISSION)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testAgencyCommissionCorrection.before.csv",
            after = "TransactionLogCollectionServiceTest.testAgencyCommissionCorrection.after.csv"
    )
    void testAgencyCommissionCorrection() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Set.of(ExportServiceType.AGENCY_COMMISSION_CORRECTION)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testAgencyCommissionDBSDelivery.before.csv",
            after = "TransactionLogCollectionServiceTest.testAgencyCommissionDBSDelivery.after.csv"
    )
    void testAgencyCommissionDBSDelivery() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Set.of(ExportServiceType.AGENCY_COMMISSION_DBS_DELIVERY)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testAgencyCommissionCorrectionDBSDelivery.before.csv",
            after = "TransactionLogCollectionServiceTest.testAgencyCommissionCorrectionDBSDelivery.after.csv"
    )
    void testAgencyCommissionCorrectionDBSDelivery() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Set.of(ExportServiceType.AGENCY_COMMISSION_CORRECTION_DBS_DELIVERY)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testCpaAuctionPromotion.before.csv",
            after = "TransactionLogCollectionServiceTest.testCpaAuctionPromotion.after.csv"
    )
    void testCpaAuctionPromotion() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Set.of(ExportServiceType.CPA_AUCTION_PROMOTION)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.virutalSupplier.before.csv",
            after = "TransactionLogCollectionServiceTest.virutalSupplier.after.csv"
    )
    void testIgnoreVirtualSupplier() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.AGENCY_COMMISSION_DBS_DELIVERY)
        );
    }

    @Test
    @DisplayName("Экспорт услуги EXPRESS_DELIVERED в tlog")
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testExpressDelivered.before.csv",
            after = "TransactionLogCollectionServiceTest.testExpressDelivered.after.csv"
    )
    void testExpressDelivered() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.EXPRESS_DELIVERED)
        );
    }

    @Test
    @DisplayName("Экспорт услуги EXPRESS_CANCELLED_BY_PARTNER в tlog")
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testExpressDeliveredCancelledByPartner.before.csv",
            after = "TransactionLogCollectionServiceTest.testExpressDeliveredCancelledByPartner.after.csv"
    )
    void testExpressDeliveredCancelledByPartner() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.EXPRESS_CANCELLED_BY_PARTNER)
        );
    }

    @Test
    @DisplayName("Экспорт услуги EXPRESS_DELIVERED_CORRECTION в tlog")
    @DbUnitDataSet(
            before = "TransactionLogCollectionServiceTest.testExpressDeliveredCorrection.before.csv",
            after = "TransactionLogCollectionServiceTest.testExpressDeliveredCorrection.after.csv"
    )
    void testExpressDeliveredCorrection() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.EXPRESS_DELIVERED_CORRECTION)
        );
    }

}

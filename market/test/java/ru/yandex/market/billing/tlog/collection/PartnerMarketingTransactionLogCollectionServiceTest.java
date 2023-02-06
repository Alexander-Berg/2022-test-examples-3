package ru.yandex.market.billing.tlog.collection;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.config.OldFirstPartySuppliersIds;
import ru.yandex.market.billing.fulfillment.promo.SupplierPromoTariffDao;
import ru.yandex.market.billing.model.tlog.ExportServiceType;
import ru.yandex.market.billing.service.environment.EnvironmentService;
import ru.yandex.market.billing.tlog.config.PartnerMarketingTransactionLogConfig;
import ru.yandex.market.billing.tlog.dao.PartnerMarketingTransactionLogDao;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.campaign.CampaignDao;
import ru.yandex.market.core.partner.PartnerContractServiceFactory;
import ru.yandex.market.core.partner.PartnerDao;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
class PartnerMarketingTransactionLogCollectionServiceTest extends FunctionalTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2021-06-01T11:28:00Z"),
            ZoneOffset.systemDefault());

    @Autowired
    private PartnerMarketingTransactionLogDao marketingTransactionLogDao;
    @Autowired
    private PartnerMarketingTransactionLogCollectionDao partnerMarketingTransactionLogCollectionDao;
    @Autowired
    private CampaignDao campaignDao;
    @Autowired
    private PartnerDao partnerDao;
    @Autowired
    private PartnerContractServiceFactory contractServiceFactory;
    @Autowired
    private SupplierPromoTariffDao supplierPromoTariffDao;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private EnvironmentService environmentService;
    @Autowired
    private OldFirstPartySuppliersIds oldFirstPartySuppliersIds;

    private PartnerMarketingTransactionLogCollectionService transactionLogCollectionService;

    @BeforeEach
    void init() {
        transactionLogCollectionService = new PartnerMarketingTransactionLogCollectionService(
                CLOCK,
                PartnerMarketingTransactionLogConfig.getTransactionLogConfig(),
                marketingTransactionLogDao,
                partnerMarketingTransactionLogCollectionDao,
                campaignDao,
                contractServiceFactory,
                partnerDao,
                supplierPromoTariffDao,
                transactionTemplate,
                environmentService,
                oldFirstPartySuppliersIds
        );
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerMarketingTransactionLogCollectionServiceTest.testFixedCampaigns.before.csv",
            after = "PartnerMarketingTransactionLogCollectionServiceTest.testFixedCampaigns.after.csv"
    )
    void testFixedCampaigns() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.PARTNER_MARKETING_FIXED_SERVICES)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerMarketingTransactionLogCollectionServiceTest.testCompensationalCampaigns.before.csv",
            after = "PartnerMarketingTransactionLogCollectionServiceTest.testCompensationalCampaigns.after.csv"
    )
    void testCompensationalCampaigns() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.PARTNER_MARKETING_COMPENSATIONAL_SERVICES)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerMarketingTransactionLogCollectionServiceTest.testCompensationalCampaigns.before.csv",
            after = "PartnerMarketingTransactionLogCollectionServiceTest.testCompensationalCampaigns.after.csv"
    )
    void testCompensationalCampaigns_afterRebillingWithIgnoredOrders() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.PARTNER_MARKETING_COMPENSATIONAL_SERVICES)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerMarketingTransactionLogCollectionServiceTest.testFixedCorrections.before.csv",
            after = "PartnerMarketingTransactionLogCollectionServiceTest.testFixedCorrections.after.csv"
    )
    void testFixedCorrections() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.PARTNER_MARKETING_FIXED_SERVICES_CORRECTION)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerMarketingTransactionLogCollectionServiceTest.testCompensationalCorrections.before.csv",
            after = "PartnerMarketingTransactionLogCollectionServiceTest.testCompensationalCorrections.after.csv"
    )
    void testCompensationalCorrections() {
        transactionLogCollectionService.collectTransactionLogItemsForService(
                Collections.singleton(ExportServiceType.PARTNER_MARKETING_COMPENSATIONAL_SERVICES_CORRECTION)
        );
    }

    @Test
    @DbUnitDataSet(
            before = "PartnerMarketingTransactionLogCollectionServiceTest.testPartnerWithoutContract.before.csv",
            after = "PartnerMarketingTransactionLogCollectionServiceTest.testPartnerWithoutContract.after.csv"
    )
    void testPartnerWithoutContract() {
        try {
            transactionLogCollectionService.collectTransactionLogItemsForService(
                    Collections.singleton(ExportServiceType.PARTNER_MARKETING_FIXED_SERVICES)
            );
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Unable to find client_id for partner_id 775");
        }
    }

}

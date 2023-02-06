package ru.yandex.market.admin.service.remote;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.admin.FunctionalTest;
import ru.yandex.market.admin.ui.model.supplier.UISupplyBillingCorrectionItem;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.fulfillment.correction.SupplyBillingCorrectionService;
import ru.yandex.market.mbi.environment.EnvironmentService;

/**
 * {@link RemoteSupplyBillingCorrectionUIService}
 */
class RemoteSupplyBillingCorrectionUIServiceTest extends FunctionalTest {
    @Autowired
    private SupplyBillingCorrectionService supplyBillingCorrectionService;

    private RemoteSupplyBillingCorrectionUIService correctionService;

    @Autowired
    private EnvironmentService environmentService;

    @BeforeEach
    void init() {
        Instant instant = LocalDateTime.of(2019, Month.FEBRUARY, 5, 12, 40, 32)
                .atZone(ZoneId.systemDefault())
                .toInstant();
        Clock clock = Clock.fixed(instant, ZoneOffset.UTC);
        correctionService = new RemoteSupplyBillingCorrectionUIService(
                supplyBillingCorrectionService,null, environmentService, clock);
    }

    @Test
    @DisplayName("Создать новые корректировки")
    @DbUnitDataSet(
            before = "RemoteSupplyBillingCorrectionUIServiceTest.create.before.csv",
            after = "RemoteSupplyBillingCorrectionUIServiceTest.create.after.csv"
    )
    void test_createCorrection() {
        correctionService.createCorrection(createCorrection("ff_withdraw", -95));
        correctionService.createCorrection(createCorrection("ff_withdraw", -45));
        correctionService.createCorrection(createCorrection("ff_storage_billing", 80));
    }

    private UISupplyBillingCorrectionItem createCorrection(String serviceType, int amount) {
        UISupplyBillingCorrectionItem result = new UISupplyBillingCorrectionItem();

        result.setField(UISupplyBillingCorrectionItem.UID, 55);
        result.setField(UISupplyBillingCorrectionItem.LOGIN, "ya");
        result.setField(UISupplyBillingCorrectionItem.SUPPLIER_ID, 12);
        result.setField(UISupplyBillingCorrectionItem.SHOP_SKU, "sku_1");
        result.setField(UISupplyBillingCorrectionItem.SERVICE_TYPE, serviceType);
        result.setField(UISupplyBillingCorrectionItem.COMMENT, "comment");
        result.setField(UISupplyBillingCorrectionItem.AMOUNT, amount);

        return result;
    }
}

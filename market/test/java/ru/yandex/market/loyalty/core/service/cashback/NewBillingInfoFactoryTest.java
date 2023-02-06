package ru.yandex.market.loyalty.core.service.cashback;

import java.math.BigDecimal;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.loyalty.core.dao.CashbackProps;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.cashback.BillingSchema;
import ru.yandex.market.loyalty.core.model.order.Item;
import ru.yandex.market.loyalty.core.model.order.ItemKey;
import ru.yandex.market.loyalty.core.model.order.PartnerCashback;
import ru.yandex.market.loyalty.core.service.exception.CashbackPromoException;
import ru.yandex.market.loyalty.core.test.MarketLoyaltyCoreMockedDbTestBase;
import ru.yandex.market.loyalty.core.utils.PartnerCashbackUtil;

import static org.junit.Assert.assertEquals;

public class NewBillingInfoFactoryTest extends MarketLoyaltyCoreMockedDbTestBase {
    private static final int HYPER_CATEGORY_ID = 1000;
    private static final int SUPPLIER_ID = 1000;
    @Autowired
    private NewBillingInfoFactory newBillingInfoFactory;
    @Autowired
    private PartnerCashbackService partnerCashbackService;
    @Autowired
    private PartnerCashbackUtil partnerCashbackUtil;

    @Test
    public void shouldUseSolidBillingSchemaForNotPartner() throws CashbackPromoException {
        partnerCashbackUtil.registerTariffs();

        final BillingInfo billingInfo = newBillingInfoFactory.createBillingInfo(
                createItem(HYPER_CATEGORY_ID, SUPPLIER_ID, false),
                createItemCashbackPromo(BigDecimal.valueOf(5), null, createPromo(BillingSchema.SPLIT)));

        assertEquals(billingInfo.getPartnerId(), Long.valueOf(0));
        assertEquals(billingInfo.getPartnerCashbackPercent(), BigDecimal.valueOf(0));
        assertEquals(billingInfo.getMarketCashbackPercent(), BigDecimal.valueOf(5));
    }

    @Test
    public void shouldUseSolidBillingSchemaForPartnerIfPromoHasSolidSchema() throws CashbackPromoException {
        partnerCashbackUtil.registerTariffs();

        final BillingInfo billingInfo = newBillingInfoFactory.createBillingInfo(
                createItem(HYPER_CATEGORY_ID, SUPPLIER_ID, true),
                createItemCashbackPromo(BigDecimal.valueOf(5), SUPPLIER_ID, createPromo(BillingSchema.SOLID)));

        assertEquals(billingInfo.getPartnerId(), Long.valueOf(0));
        assertEquals(billingInfo.getPartnerCashbackPercent(), BigDecimal.valueOf(0));
        assertEquals(billingInfo.getMarketCashbackPercent(), BigDecimal.valueOf(5));
    }

    @Test
    public void shouldUseSplitBillingSchemaForPartnerIfPromoHasSplitSchema() throws CashbackPromoException {
        partnerCashbackUtil.registerTariffs();

        final BillingInfo billingInfo = newBillingInfoFactory.createBillingInfo(
                createItem(HYPER_CATEGORY_ID, SUPPLIER_ID, true),
                createItemCashbackPromo(BigDecimal.valueOf(5), SUPPLIER_ID, createPromo(BillingSchema.SPLIT)));

        assertEquals(billingInfo.getPartnerId(), Long.valueOf(SUPPLIER_ID));
        assertEquals(billingInfo.getPartnerCashbackPercent(), BigDecimal.valueOf(3.7));
        assertEquals(billingInfo.getMarketCashbackPercent(), BigDecimal.valueOf(1.3));
    }

    @Test
    public void shouldUseItemSupplierIdIfPromoHasNoPartnerCashback() throws CashbackPromoException {
        partnerCashbackUtil.registerTariffs();

        final BillingInfo billingInfo = newBillingInfoFactory.createBillingInfo(
                createItem(HYPER_CATEGORY_ID, SUPPLIER_ID, true),
                createItemCashbackPromo(BigDecimal.valueOf(5), null, null));

        assertEquals(billingInfo.getPartnerId(), Long.valueOf(SUPPLIER_ID));
        assertEquals(billingInfo.getPartnerCashbackPercent(), BigDecimal.valueOf(3.7));
        assertEquals(billingInfo.getMarketCashbackPercent(), BigDecimal.valueOf(1.3));
    }

    private CashbackProps createPromo(BillingSchema billingSchema) {
        return CashbackProps.builder().setBillingSchema(billingSchema).build();
    }

    @NotNull
    private BillingPromoInfo createItemCashbackPromo(BigDecimal nominal, Integer partnerId,
                                                     CashbackProps loyaltyPromo) {
        return new BillingPromoInfo(nominal,
                Optional.ofNullable(partnerId).map(id -> new PartnerCashback(partnerCashbackService.getPartnerCashbackCurrentVersionId(), id)).orElse(null),
                loyaltyPromo
        );
    }

    @SuppressWarnings("SameParameterValue")
    @NotNull
    private Item createItem(int hyperCategoryId, long supplierId, boolean loyaltyProgramPartner) {
        return Item.Builder.create()
                .withKey(ItemKey.ofFeedOffer(1L, "1"))
                .withPrice(BigDecimal.valueOf(1000))
                .withQuantity(BigDecimal.valueOf(3))
                .withDownloadable(false)
                .withHyperCategoryId(hyperCategoryId)
                .withSku("100")
                .withVendorId(1000L)
                .withWeight(BigDecimal.ONE)
                .withVolume(10L)
                .withSupplierId(supplierId)
                .withWarehouseId(1000)
                .loyaltyProgramPartner(loyaltyProgramPartner)
                .withPlatform(CoreMarketPlatform.BLUE)
                .withPayByYaPlus(0)
                .build();
    }
}

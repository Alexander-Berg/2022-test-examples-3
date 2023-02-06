package ru.yandex.market.checkout.checkouter.balance.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.ShopMetaDataBuilder;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.pay.PaymentGoal;
import ru.yandex.market.checkout.checkouter.shop.PaymentArticle;
import ru.yandex.market.checkout.checkouter.shop.PaymentClass;
import ru.yandex.market.checkout.checkouter.shop.PrepayType;
import ru.yandex.market.checkout.checkouter.shop.ShopMetaData;

import static ru.yandex.market.checkout.util.balance.TrustMockConfigurer.LOAD_PARTNER_STUB;

public class CreateProductDisableLoadPartnerTest extends AbstractWebTestBase {

    private static final long CAMPAIGN_ID = 111L;
    private static final long CLIENT_ID = 222L;
    private static final String YA_MONEY_ID = "abc";
    private static final int AGENCY_COMMISSION = 0;

    @Autowired
    private TrustService trustService;

    private ShopMetaData shopMetaData;

    @BeforeEach
    public void setUp() throws Exception {
        shopMetaData = ShopMetaDataBuilder.createTestDefault()
                .withCampaiginId(CAMPAIGN_ID)
                .withClientId(CLIENT_ID)
                .withSandboxClass(PaymentClass.YANDEX)
                .withProdClass(PaymentClass.YANDEX)
                .withYaMoneyId(YA_MONEY_ID)
                .withArticles(new PaymentArticle[0])
                .withPrepayType(PrepayType.YANDEX_MARKET)
                .withInn("inn")
                .withPhone("phone")
                .withAgencyCommission(AGENCY_COMMISSION)
                .build();

        trustMockConfigurer.mockWholeTrust();
    }

    @Test
    void disabledLoadPartner() {
        checkouterProperties.setDisabledLoadPartner(true);
        long loadCallCount = countLoadPartner();
        Assertions.assertEquals(0, loadCallCount);
    }

    @Test
    void enabledLoadPartner() {
        checkouterProperties.setDisabledLoadPartner(false);
        long loadCallCount = countLoadPartner();
        Assertions.assertEquals(1, loadCallCount);
    }

    private long countLoadPartner() {
        trustService.createServiceProduct(shopMetaData, PaymentGoal.ORDER_POSTPAY, Color.BLUE);
        return trustMockConfigurer.servedEvents().stream()
                .filter(serveEvent -> serveEvent.getStubMapping().getName().equals(LOAD_PARTNER_STUB))
                .count();
    }
}

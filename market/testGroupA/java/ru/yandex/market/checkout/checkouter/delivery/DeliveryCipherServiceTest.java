package ru.yandex.market.checkout.checkouter.delivery;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.market.checkout.checkouter.feature.CheckouterFeatureResolverStub;
import ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType;
import ru.yandex.market.checkout.cipher.CipherService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DeliveryCipherServiceTest {

    private DeliveryCipherService service;
    private CheckouterFeatureResolverStub checkouterFeatureResolverStub;

    @BeforeEach
    public void setUp() throws IOException {
        CipherService cipher = new CipherService("Blowfish", "ayFVMGPqmKf4pZ0rnsGMGQ==");
        service = new DeliveryCipherService(cipher);
        checkouterFeatureResolverStub = new CheckouterFeatureResolverStub();
        service.setCheckouterFeatureReader(checkouterFeatureResolverStub);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void should(boolean enableHashTimeInterval) {
        checkouterFeatureResolverStub.writeValue(BooleanFeatureType.ENABLE_HASH_DELIVERY_TIME_INTERVAL,
                enableHashTimeInterval);
        Delivery delivery = new Delivery();
        delivery.setShopDeliveryId("1");
        delivery.setServiceName("name");
        delivery.setPrice(new BigDecimal(100));
        delivery.setBuyerPrice(new BigDecimal(200));
        delivery.setType(DeliveryType.DELIVERY);
        DeliveryDates dates = new DeliveryDates(new Date(), new Date(),
                enableHashTimeInterval ? LocalTime.of(10, 11, 12) : null,
                enableHashTimeInterval ? LocalTime.of(12, 13, 14) : null);
        delivery.setDeliveryDates(dates);
        delivery.setDeliveryPartnerType(DeliveryPartnerType.SHOP);
        delivery.setMarketBranded(true);
        delivery.setMarketPartner(true);
        delivery.setMarketPostTerm(null);
        service.cipherDelivery(delivery);
        assertNotNull(delivery.getHash());
        Delivery newDelivery = new Delivery();
        newDelivery.setHash(delivery.getHash());
        service.decipherDelivery(newDelivery);
        assertEquals(delivery.getShopDeliveryId(), newDelivery.getShopDeliveryId());
        assertEquals(delivery.isMarketBranded(), newDelivery.isMarketBranded());
        assertEquals(delivery.isMarketPartner(), newDelivery.isMarketPartner());
        assertFalse(newDelivery.isMarketPostTerm());
        assertEquals(delivery.getDeliveryDates(), newDelivery.getDeliveryDates());
    }
}

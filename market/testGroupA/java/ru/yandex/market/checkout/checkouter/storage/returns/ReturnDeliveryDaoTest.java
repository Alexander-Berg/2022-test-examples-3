package ru.yandex.market.checkout.checkouter.storage.returns;

import java.math.BigDecimal;
import java.time.Clock;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.checkouter.delivery.tracking.Track;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.returns.AbstractReturnTestBase;
import ru.yandex.market.checkout.checkouter.returns.DeliveryOptionPrice;
import ru.yandex.market.checkout.checkouter.returns.Return;
import ru.yandex.market.checkout.checkouter.returns.ReturnDelivery;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

public class ReturnDeliveryDaoTest extends AbstractReturnTestBase {

    public static final long TRACK_ID = 111L;
    public static final DeliveryOptionPrice PRICE = new DeliveryOptionPrice(BigDecimal.valueOf(185.35), Currency.RUR);
    @Autowired
    private ReturnDeliveryDao returnDeliveryDao;

    private Order order;
    private Return aReturn;

    @Test
    public void priceChangesSuccess() {
        ReturnDelivery returnDelivery = returnHelper.getDefaultReturnDelivery();
        returnDelivery.setTrack(getTrack());
        returnDelivery.setReturnId(aReturn.getId());
        final ReturnDelivery rdBefore = createReturnDeliveryInDb(returnDelivery);
        assertThat(rdBefore.getId(), allOf(notNullValue(), not(equalTo(0L))));
        Assertions.assertNull(rdBefore.getPrice().getValue());
        Assertions.assertNotNull(rdBefore.getTrack().getId());

        transactionTemplate.execute((txStatus) -> {
            returnDeliveryDao.setReturnDeliveryPrice(aReturn.getId(), PRICE.getValue(), PRICE.getCurrency().name());
            return null;
        });

        ReturnDelivery rd = returnDeliveryDao.findReturnDeliveryById(rdBefore.getId()).orElseThrow();
        Assertions.assertNotNull(rd.getPrice());
        Assertions.assertEquals(PRICE.getValue(), rd.getPrice().getValue());
        Assertions.assertEquals(PRICE.getCurrency(), rd.getPrice().getCurrency());
        Assertions.assertNotNull(rd.getTrack().getId());
    }

    private ReturnDelivery createReturnDeliveryInDb(ReturnDelivery returnDeliveryToSave) {
        Long id = transactionTemplate.execute((txStatus) -> returnDeliveryDao
                .insertReturnDelivery(returnDeliveryToSave, order.getId()));
        if (id == null) {
            throw new RuntimeException("Return delivery not saved");
        }
        //добавил честное чтение из бд с присвоением ID, для последующего присвоения его треку
        ReturnDelivery savedRd = returnDeliveryDao.findReturnDeliveryById(id)
                .orElseThrow(() -> new RuntimeException("Return Delivery not found"));

        transactionTemplate.execute(tx -> {
            aReturn.setDelivery(savedRd);
            returnDeliveryDao.createReturnDeliveryTrack(aReturn, "12345", Clock.systemDefaultZone());
            return null;
        });
        var rdWithTrack = returnDeliveryDao.findReturnDeliveryById(id)
                .orElseThrow(() -> new RuntimeException("Return Delivery not found"));
        assertThat(savedRd.getId(), equalTo(id));
        return rdWithTrack;
    }

    @BeforeEach
    public void setUpSuite() {
        Pair<Order, Return> orderAndReturn = returnHelper.createOrderAndReturn(defaultBlueOrderParameters(), null);
        order = orderAndReturn.getFirst();
        aReturn = orderAndReturn.getSecond();
    }

    private Track getTrack() {
        Track track = new Track();
        track.setId(TRACK_ID);
        track.setDeliveryServiceId(DELIVERY_SERVICE_ID);
        return track;
    }
}

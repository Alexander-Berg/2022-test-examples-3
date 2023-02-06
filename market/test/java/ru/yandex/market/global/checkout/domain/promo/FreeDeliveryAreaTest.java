package ru.yandex.market.global.checkout.domain.promo;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.actualize.OrderActualization;
import ru.yandex.market.global.checkout.domain.promo.apply.free_delivery_area.FreeDeliveryAreaArgs;
import ru.yandex.market.global.checkout.domain.promo.apply.free_delivery_area.FreeDeliveryAreaCommonState;
import ru.yandex.market.global.checkout.domain.promo.apply.free_delivery_area.FreeDeliveryAreaPromoApplyHandler;
import ru.yandex.market.global.checkout.domain.promo.model.PromoType;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.factory.TestPromoFactory;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.market.global.common.jooq.Point;
import ru.yandex.market.global.common.jooq.Polygon;
import ru.yandex.market.global.db.jooq.enums.EPromoAccessType;
import ru.yandex.market.global.db.jooq.enums.EPromoApplicationType;
import ru.yandex.market.global.db.jooq.tables.pojos.OrderItem;
import ru.yandex.market.global.db.jooq.tables.pojos.Promo;
import ru.yandex.market.global.db.jooq.tables.pojos.PromoUser;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class FreeDeliveryAreaTest extends BaseFunctionalTest {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(FreeDeliveryAreaTest.class).build();
    private static final double IN_POLYGON_LAT = 34.6473598;
    private static final double IN_POLYGON_LON = 31.7849556;
    private static final double OUT_OF_POLYGON_LAT = 34.6456218;
    private static final double OUT_OFPOLYGON_LON = 31.7892509;
    private static final long MIN_ITEMS_PRICE = 100_00L;

    private final FreeDeliveryAreaPromoApplyHandler freeDeliveryAreaPromoApplyHandler;

    private final Clock clock;
    private final TestOrderFactory testOrderFactory;
    private final TestPromoFactory testPromoFactory;

    @Test
    public void testDeliveryCostResetsIfPointInPolygon() {
        Promo promo = createFreeDeliveryAreaPromo();
        OrderActualization actualization = createOrderActualization(IN_POLYGON_LAT, IN_POLYGON_LON, MIN_ITEMS_PRICE);
        PromoUser promoUser = createPromoUser(actualization.getOrder().getUid());

        freeDeliveryAreaPromoApplyHandler.apply(promo, promoUser, actualization);

        Assertions.assertThat(actualization.getOrder().getDeliveryCostForRecipient())
                .isEqualTo(0);
    }

    @Test
    public void testDeliveryCostPreservedIfTotalItemsCostLow() {
        Promo promo = createFreeDeliveryAreaPromo();
        OrderActualization actualization = createOrderActualization(
                IN_POLYGON_LAT, IN_POLYGON_LON, MIN_ITEMS_PRICE - 1
        );
        PromoUser promoUser = createPromoUser(actualization.getOrder().getUid());

        freeDeliveryAreaPromoApplyHandler.apply(promo, promoUser, actualization);

        Assertions.assertThat(actualization.getOrder().getDeliveryCostForRecipient())
                .isNotEqualTo(0);
    }

    @Test
    public void testDeliveryCostPreservedIfPointOutOfPolygon() {
        Promo promo = createFreeDeliveryAreaPromo();
        OrderActualization actualization = createOrderActualization(
                OUT_OF_POLYGON_LAT, OUT_OFPOLYGON_LON, MIN_ITEMS_PRICE
        );
        PromoUser promoUser = createPromoUser(actualization.getOrder().getUid());

        freeDeliveryAreaPromoApplyHandler.apply(promo, promoUser, actualization);

        Assertions.assertThat(actualization.getOrder().getDeliveryCostForRecipient())
                .isNotEqualTo(0);
    }

    private PromoUser createPromoUser(long uid) {
        return new PromoUser()
                .setUsed(true)
                .setUsedAt(OffsetDateTime.now(clock))
                .setUid(uid);
    }

    private OrderActualization createOrderActualization(double x, double y, long itemPrice) {
        return TestOrderFactory.buildOrderActualization(
                TestOrderFactory.CreateOrderActualizationBuilder.builder()
                        .setupOrder(o -> o
                                .setDeliveryCostForRecipient(1000L)
                                .setTotalItemsCost(itemPrice)
                        )
                        .setupDelivery(d -> d.setRecipientCoordinates(
                                new Point()
                                        .setLat(BigDecimal.valueOf(x))
                                        .setLon(BigDecimal.valueOf(y))
                        ))
                        .setupItems(l -> List.of(
                                RANDOM.nextObject(OrderItem.class)
                                        .setPrice(itemPrice)
                                        .setCount(1L)
                        ))
                        .build()
        );
    }

    private Promo createFreeDeliveryAreaPromo() {
        return testPromoFactory.createPromo(TestPromoFactory.CreatePromoBuilder.builder()
                .setupPromo(p -> p
                        .setName("FREE_DELIVERY_AREA")
                        .setAccessType(EPromoAccessType.ALL_UNLIMITED)
                        .setType(PromoType.FREE_DELIVERY_AREA.name())
                        .setApplicationType(EPromoApplicationType.UNCONDITIONAL)
                        .setValidTill(OffsetDateTime.now(clock))
                )
                .setupState(() -> new FreeDeliveryAreaCommonState()
                        .setTotalCountUsed(0)
                )
                .setupArgs((a) -> new FreeDeliveryAreaArgs()
                        .setArea(new Polygon()
                                .setPoints(List.of(
                                        new Point()
                                                .setLat(BigDecimal.valueOf(34.6434546))
                                                .setLon(BigDecimal.valueOf(31.7818822)),
                                        new Point()
                                                .setLat(BigDecimal.valueOf(34.6466303))
                                                .setLon(BigDecimal.valueOf(31.7862232)),
                                        new Point()
                                                .setLat(BigDecimal.valueOf(34.6549129))
                                                .setLon(BigDecimal.valueOf(31.782247)),
                                        new Point()
                                                .setLat(BigDecimal.valueOf(34.6434546))
                                                .setLon(BigDecimal.valueOf(31.7818822))
                                ))

                        )
                        .setMinTotalItemsCost(MIN_ITEMS_PRICE)
                ).build()
        );
    }
}

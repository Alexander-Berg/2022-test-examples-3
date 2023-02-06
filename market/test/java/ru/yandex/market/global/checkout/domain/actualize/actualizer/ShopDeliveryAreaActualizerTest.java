package ru.yandex.market.global.checkout.domain.actualize.actualizer;

import java.math.BigDecimal;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.actualize.ActualizationError;
import ru.yandex.market.global.checkout.domain.actualize.OrderActualization;
import ru.yandex.market.global.checkout.factory.TestOrderFactory;
import ru.yandex.market.global.checkout.factory.TestOrderFactory.CreateOrderActualizationBuilder;
import ru.yandex.market.global.common.jooq.Point;
import ru.yandex.mj.generated.server.model.GeoPointDto;
import ru.yandex.mj.generated.server.model.GeoPolygonDto;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ShopDeliveryAreaActualizerTest extends BaseFunctionalTest {
    private static final double IN_POLYGON_NEAR_LAT = 32.05123863466847;
    private static final double IN_POLYGON_NEAR_LON = 34.79659455566249;

    private static final double OUT_OF_POLYGON_NEAR_LAT = 31.98963722778369;
    private static final double OUT_OF_POLYGON_NEAR_LON = 34.774452529642325;

    private static final double IN_POLYGON_FAR_LAT = 32.10093303682538;
    private static final double IN_POLYGON_FAR_LON = 34.8254336669912;

    private static final double OUT_OF_POLYGON_FAR_LAT = 32.10093303682538;
    private static final double OUT_OF_POLYGON_FAR_LON = 34.879678662109036;

    private static final double SHOP_LAT = 32.013803675009164;
    private static final double SHOP_LON = 34.78775399475068;

    private static final GeoPolygonDto DELIVERY_AREA = new GeoPolygonDto().points(List.of(
            new GeoPointDto()
                    .lat(32.11693175006755)
                    .lon(34.78329079895013),
            new GeoPointDto()
                    .lat(32.01694821981134)
                    .lon(34.74003213195793),
            new GeoPointDto()
                    .lat(31.998518168317897)
                    .lon(34.80183022766106),
            new GeoPointDto()
                    .lat(32.10144445389724)
                    .lon(34.84955209045404),
            new GeoPointDto()
                    .lat(32.11693175006755)
                    .lon(34.78329079895013)
    ));

    private static final RecursiveComparisonConfiguration RECURSIVE_COMPARISON_CONFIGURATION =
            RecursiveComparisonConfiguration.builder()
                    .withIgnoreAllExpectedNullFields(true)
                    .build();

    private final TestOrderFactory testOrderFactory;
    private final ShopDeliveryAreaActualizer shopDeliveryAreaActualizer;

    @Test
    public void testOutOfAreaFarProduceError() {
        OrderActualization actualization = createOrderActualization(OUT_OF_POLYGON_FAR_LAT, OUT_OF_POLYGON_FAR_LON);

        actualization = shopDeliveryAreaActualizer.actualize(actualization);

        Assertions.assertThat(actualization.getErrors())
                .usingRecursiveFieldByFieldElementComparator(RECURSIVE_COMPARISON_CONFIGURATION)
                .contains(new ActualizationError()
                        .setCode(ActualizationError.Code.SHOP_DELIVERY_AREA_MISMATCH)
                );
    }

    @Test
    public void testOutOfAreaNearProduceError() {
        OrderActualization actualization = createOrderActualization(OUT_OF_POLYGON_NEAR_LAT, OUT_OF_POLYGON_NEAR_LON);

        actualization = shopDeliveryAreaActualizer.actualize(actualization);

        Assertions.assertThat(actualization.getErrors())
                .usingRecursiveFieldByFieldElementComparator(RECURSIVE_COMPARISON_CONFIGURATION)
                .contains(new ActualizationError()
                        .setCode(ActualizationError.Code.SHOP_DELIVERY_AREA_MISMATCH)
                );
    }

    @Test
    public void testInAreaFarProduceError() {
        OrderActualization actualization = createOrderActualization(IN_POLYGON_FAR_LAT, IN_POLYGON_FAR_LON);

        actualization = shopDeliveryAreaActualizer.actualize(actualization);

        Assertions.assertThat(actualization.getErrors())
                .usingRecursiveFieldByFieldElementComparator(RECURSIVE_COMPARISON_CONFIGURATION)
                .contains(new ActualizationError()
                        .setCode(ActualizationError.Code.SHOP_DELIVERY_AREA_MISMATCH)
                );
    }

    @Test
    public void testInAreaNearProduceNoError() {
        OrderActualization actualization = createOrderActualization(IN_POLYGON_NEAR_LAT, IN_POLYGON_NEAR_LON);

        actualization = shopDeliveryAreaActualizer.actualize(actualization);

        Assertions.assertThat(actualization.getErrors()).isEmpty();
    }

    private OrderActualization createOrderActualization(double recipientLat, double recipientLon) {
        return testOrderFactory.buildOrderActualization(
                CreateOrderActualizationBuilder.builder()
                        .setupShop(s -> s
                                .deliveryArea(DELIVERY_AREA)
                                .address(s.getAddress().coordinates(new GeoPointDto()
                                                .lat(SHOP_LAT)
                                                .lon(SHOP_LON)
                                        )
                                ))
                        .setupDelivery(d -> d.setRecipientCoordinates(new Point()
                                .setLat(BigDecimal.valueOf(recipientLat))
                                .setLon(BigDecimal.valueOf(recipientLon))
                        ))
                        .build()
        );
    }
}

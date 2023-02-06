package ru.yandex.market.checkout.checkouter.actualization.actualizers.v2;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.DimensionsResponse;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.MultiCartResponse;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.common.report.model.ActualDeliveryResult;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ActualizeV2DimensionTest extends AbstractWebTestBase {

    @Test
    public void shouldReturnDimension() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        ActualDeliveryResult actualDeliveryResult =
                parameters.getReportParameters().getActualDelivery().getResults().get(0);

        BigDecimal weightInKg = BigDecimal.valueOf(1);
        BigDecimal width = BigDecimal.valueOf(11);
        BigDecimal height = BigDecimal.valueOf(22);
        BigDecimal depth = BigDecimal.valueOf(33);
        actualDeliveryResult.setWeight(weightInKg);
        actualDeliveryResult.setDimensions(List.of(width, height, depth));

        MultiCartResponse cart = orderCreateHelper.multiCartActualize(parameters);

        assertEquals(1, cart.getCarts().size());
        assertEquals(1, cart.getCarts().get(0).getDimensions().size());
        DimensionsResponse dimensionsResponse = cart.getCarts().get(0).getDimensions().get(0);
        assertEquals(weightInKg.longValue() * 1000, dimensionsResponse.getWeight());
        assertEquals(width.longValue(), dimensionsResponse.getWidth());
        assertEquals(height.longValue(), dimensionsResponse.getHeight());
        assertEquals(depth.longValue(), dimensionsResponse.getDepth());
    }
}

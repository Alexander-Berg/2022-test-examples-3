package ru.yandex.market.logistic.gateway.common.model.validation;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.logistic.gateway.client.exceptions.GatewayValidationException;
import ru.yandex.market.logistic.gateway.client.utils.RequestValidator;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.CargoType;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.Item;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.response.GetReferenceItemsResponse;
import ru.yandex.market.logistic.gateway.utils.DeliveryDtoFactory;
import ru.yandex.market.logistic.gateway.utils.FulfillmentDtoFactory;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(MockitoJUnitRunner.class)
public class ValidationTest {

    /**
     * Проверка работы валидации, в частности {@link ItemValidator}.
     */
    @Test
    public void testItemValidation() throws Exception {
        Item.ItemBuilder itemBuilder = new Item.ItemBuilder(
            "Item",
            1,
            new BigDecimal(1),
            CargoType.UNKNOWN,
            Collections.singletonList(CargoType.UNKNOWN));

        RequestValidator.validate(itemBuilder.build());

        itemBuilder
            .setBoxCount(2)
            .setBoxCapacity(2);

        assertThatThrownBy(() -> RequestValidator.validate(itemBuilder.build()))
            .isInstanceOf(GatewayValidationException.class);
    }

    /**
     * Проверка работы валидации, в частности {@link ItemValidator}.
     */
    @Test
    public void testFFItemInstancesValidation() throws Exception {
        Item.ItemBuilder itemBuilder = new Item.ItemBuilder(
                "Item",
                1,
                new BigDecimal(1),
                CargoType.UNKNOWN,
                Collections.singletonList(CargoType.UNKNOWN));

        RequestValidator.validate(itemBuilder.build());

        itemBuilder.setInstances(FulfillmentDtoFactory.createItemInstances());

        RequestValidator.validate(itemBuilder.build());
    }

    /**
     * Проверка работы валидации, в частности {@link ItemValidator}.
     */
    @Test
    public void testDSItemInstancesValidation() throws Exception {
        ru.yandex.market.logistic.gateway.common.model.delivery.Item.ItemBuilder itemBuilder =
            new ru.yandex.market.logistic.gateway.common.model.delivery.Item.ItemBuilder(
                "Item",
                1,
                new BigDecimal(1)
        );

        RequestValidator.validate(itemBuilder.build());

        itemBuilder.setInstances(DeliveryDtoFactory.createItemInstances());

        RequestValidator.validate(itemBuilder.build());
    }

    @Test
    public void testEmptyItemReferenceValidation() {
        GetReferenceItemsResponse emptyResponse = new GetReferenceItemsResponse(List.of());
        RequestValidator.validate(emptyResponse);

        GetReferenceItemsResponse nullResponse = new GetReferenceItemsResponse(null);

        assertThatThrownBy(() -> RequestValidator.validate(nullResponse))
            .isInstanceOf(GatewayValidationException.class);
    }
}

package ru.yandex.market.checkout.checkouter.order;

import java.util.List;

import io.qameta.allure.Epic;
import io.qameta.allure.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.allure.Epics;
import ru.yandex.market.checkout.allure.Stories;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.validation.ValidationResult;
import ru.yandex.market.checkout.helpers.utils.Parameters;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

/**
 * @author sergeykoles
 * Created on: 19.01.18
 */
public class FulfilmentWarehouseOrderConstraintTest extends AbstractWebTestBase {

    @Test
    @Epic(Epics.FULFILMENT)
    @Story(Stories.BUSINESS_RULES)
    @DisplayName("Проверяем, что fulfilment заказ с sku, shopSku успешно проходит проверку")
    public void testBothSKUsPass() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.setCheckCartErrors(false);
        parameters.getOrder().getItems().forEach(item -> item.setCount(1));
        parameters.setColor(Color.BLUE);
        MultiCart mc = orderCreateHelper.cart(parameters);
        assertThat(mc.getCarts().stream().findFirst().get().getValidationErrors(), anyOf(nullValue(), empty()));
    }

    @Test
    @Epic(Epics.FULFILMENT)
    @Story(Stories.BUSINESS_RULES)
    @DisplayName("Проверяем, что fulfilment заказ без sku не проходит проверку")
    public void testNoSkuFail() {
        Parameters parameters = defaultBlueOrderParameters();
        parameters.cleanFulfillmentInfo();
        parameters.setCheckCartErrors(false);
        parameters.getOrder().getItems().forEach(item -> {
            item.setSku("");
            item.setMsku(null);
            item.setCount(1);
        });
        parameters.setColor(Color.BLUE);
        MultiCart mc = orderCreateHelper.cart(parameters);
        List<ValidationResult> validationErrors = mc.getValidationErrors();
        assertThat(validationErrors,
                allOf(notNullValue(), hasSize(1))
        );
    }
}

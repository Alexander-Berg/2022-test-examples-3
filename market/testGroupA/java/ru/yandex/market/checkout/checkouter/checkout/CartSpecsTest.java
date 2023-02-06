package ru.yandex.market.checkout.checkouter.checkout;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.common.report.model.specs.InternalSpec;
import ru.yandex.market.common.report.model.specs.Specs;
import ru.yandex.market.common.report.model.specs.UsedParam;

/**
 * Тест обогащения OrderItem спеками
 */
public class CartSpecsTest extends AbstractWebTestBase {

    @Test
    public void shouldReturnOrderItemWIthSpecs() {
        var parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.getReportParameters()
                .getOffers().forEach(offer -> offer.setSpecs(
                        new Specs(Set.of(
                                new InternalSpec("vidal", List.of(new UsedParam("J05AX13"))),
                                new InternalSpec("medicine")
                        ))
                ));

        var actualMultiCart = orderCreateHelper.cart(parameters);

        var actualOrder = actualMultiCart.getCarts().get(0);
        Specs expectedSpecs = new Specs(Set.of(
                new InternalSpec("vidal", List.of(new UsedParam("J05AX13"))),
                new InternalSpec("medicine")
        ));
        var actualSpecs = actualOrder.getItems().stream()
                .findFirst()
                .orElseThrow(() -> new NullPointerException("No OrderItems is Order"))
                .getMedicalSpecsInternal();
        Assertions.assertEquals(expectedSpecs, actualSpecs);
    }
}

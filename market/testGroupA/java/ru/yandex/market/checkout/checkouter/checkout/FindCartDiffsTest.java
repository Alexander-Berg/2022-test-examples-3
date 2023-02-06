package ru.yandex.market.checkout.checkouter.checkout;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.client.CartParameters;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.feature.type.common.ComplexFeatureType;
import ru.yandex.market.checkout.checkouter.order.Color;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.util.CheckouterPropertiesImpl;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;
import ru.yandex.market.checkout.test.providers.ActualDeliveryProvider;

import static org.assertj.core.api.Assertions.assertThat;

public class FindCartDiffsTest extends AbstractWebTestBase {

    @Test
    public void emptyCartDiffsTest() throws Exception {
        var parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        orderCreateHelper.initializeMock(parameters);

        var multiCart = parameters.getBuiltMultiCart();
        var cartParameters = CartParameters.builder()
                .withUid(parameters.getBuyer().getUid())
                .withRgb(Color.WHITE)
                .build();
        var cartDiffResult = client.findCartDiffs(multiCart, cartParameters);
        assertThat(cartDiffResult.getCartDiffs().values())
                .allMatch(List::isEmpty);
        assertThat(cartDiffResult.getMultiCartFailures())
                .isEmpty();
        assertThat(cartDiffResult.getMultiCartValidationErrors())
                .isEmpty();
    }

    @Test
    public void notEmptyCartDiffsTest() throws Exception {
        var parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.getReportParameters().setActualDelivery(ActualDeliveryProvider.builder()
                .addPost(30)
                .build()
        );
        parameters.getPushApiDeliveryResponses().removeIf(delivery -> delivery.getType() == DeliveryType.POST);
        parameters.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        checkouterProperties.setEnableUnifiedTariffs(true);
        checkouterFeatureWriter.writeValue(ComplexFeatureType.TARIFFS_AND_LIFT_EXPERIMENT_TOGGLE,
                CheckouterPropertiesImpl.TariffsAndLiftExperimentToggle.FORCE);
        orderCreateHelper.initializeMock(parameters);

        var multiCart = parameters.getBuiltMultiCart();
        var cartParameters = CartParameters.builder()
                .withUid(parameters.getBuyer().getUid())
                .withRgb(Color.WHITE)
                .build();
        var cartDiffResult = client.findCartDiffs(multiCart, cartParameters);
        assertThat(cartDiffResult.getCartDiffs().values())
                .anyMatch(CollectionUtils::isNotEmpty);
        assertThat(cartDiffResult.getMultiCartFailures())
                .isEmpty();
        assertThat(cartDiffResult.getMultiCartValidationErrors())
                .isEmpty();
    }
}

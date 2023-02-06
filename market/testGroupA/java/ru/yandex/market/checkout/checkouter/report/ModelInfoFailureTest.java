package ru.yandex.market.checkout.checkouter.report;

import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import org.junit.jupiter.api.Test;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.common.report.model.MarketReportPlace;

public class ModelInfoFailureTest extends AbstractWebTestBase {

    @Test
    public void modelInfoFailureDoesNotAffectCheckout() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        parameters.getReportParameters().addMappingBuilderModifier(
                MarketReportPlace.MODEL_INFO,
                mappingBuilder -> mappingBuilder.willReturn(
                        ResponseDefinitionBuilder.responseDefinition().withStatus(500)
                )
        );
        Order order = orderCreateHelper.createOrder(parameters);
    }
}

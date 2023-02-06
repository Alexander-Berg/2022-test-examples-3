package ru.yandex.market.checkout.checkouter.actualization.actualizers.v2;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.delivery.AvailableDeliveryType;
import ru.yandex.market.checkout.checkouter.delivery.DeliveryType;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;
import ru.yandex.market.checkout.checkouter.report.Experiments;
import ru.yandex.market.checkout.checkouter.trace.CheckoutContextHolder;
import ru.yandex.market.checkout.checkouter.v2.multicart.actualize.response.MultiCartResponse;
import ru.yandex.market.checkout.helpers.utils.Parameters;
import ru.yandex.market.checkout.providers.BlueParametersProvider;
import ru.yandex.market.checkout.providers.WhiteParametersProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS;
import static ru.yandex.market.checkout.checkouter.report.Experiments.MARKET_UNIFIED_TARIFFS_VALUE;
import static ru.yandex.market.checkout.checkouter.trace.CheckoutContextHolder.getExperiments;

public class AvailableDeliveryTypesMutationTest extends AbstractWebTestBase {

    @Test
    public void shouldReturnReportAvailableDeliveryTypes() {
        Parameters parameters = BlueParametersProvider.defaultBlueOrderParameters();
        AvailableDeliveryType availableDeliveryType = AvailableDeliveryType.POST;

        mockReportAvailableTypes(parameters, List.of(availableDeliveryType));

        MultiCartResponse cart = orderCreateHelper.multiCartActualize(parameters);

        assertEquals(1, cart.getCarts().size());
        assertEquals(1, cart.getCarts().get(0).getAvailableDeliveryTypes().size());
        assertEquals(availableDeliveryType, cart.getCarts().get(0).getAvailableDeliveryTypes().iterator().next());
    }

    @Test
    public void shouldReportDeliveryTypeForBlueNonFF() {
        Parameters parameters = BlueParametersProvider.defaultBlueNonFulfilmentOrderParameters();
        AvailableDeliveryType reportAvailableType = AvailableDeliveryType.POST;
        DeliveryType pushApiDeliveryType = DeliveryType.PICKUP;

        mockReportAvailableTypes(parameters, List.of(reportAvailableType));
        mockPushApiAvailableTypes(parameters, pushApiDeliveryType);

        MultiCartResponse cart = orderCreateHelper.multiCartActualize(parameters);

        assertEquals(1, cart.getCarts().size());
        assertEquals(1, cart.getCarts().get(0).getAvailableDeliveryTypes().size());
        assertEquals(reportAvailableType, cart.getCarts().get(0).getAvailableDeliveryTypes().iterator().next());
    }

    @Test
    public void shouldPushApiDeliveryTypeForEda() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.getReportParameters().setIsEda(true);
        AvailableDeliveryType reportAvailableType = AvailableDeliveryType.POST;
        DeliveryType pushApiDeliveryType = DeliveryType.PICKUP;

        mockReportAvailableTypes(parameters, List.of(reportAvailableType));
        mockPushApiAvailableTypes(parameters, pushApiDeliveryType);

        MultiCartResponse cart = orderCreateHelper.multiCartActualize(parameters);

        assertEquals(1, cart.getCarts().size());
        assertEquals(1, cart.getCarts().get(0).getAvailableDeliveryTypes().size());
        assertEquals(AvailableDeliveryType.fromDeliveryType(pushApiDeliveryType),
                cart.getCarts().get(0).getAvailableDeliveryTypes().iterator().next());
    }

    @Test
    public void shouldPushApiDeliveryTypeForWhiteWithEmptyReportType() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        DeliveryType pushApiDeliveryType = DeliveryType.PICKUP;

        mockReportAvailableTypes(parameters, Collections.emptyList());
        mockPushApiAvailableTypes(parameters, pushApiDeliveryType);

        MultiCartResponse cart = orderCreateHelper.multiCartActualize(parameters);

        assertEquals(1, cart.getCarts().size());
        assertEquals(1, cart.getCarts().get(0).getAvailableDeliveryTypes().size());
        assertEquals(AvailableDeliveryType.fromDeliveryType(pushApiDeliveryType),
                cart.getCarts().get(0).getAvailableDeliveryTypes().iterator().next());
    }

    @Test
    public void shouldReportDeliveryTypeForWhiteOrder() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        AvailableDeliveryType reportAvailableType = AvailableDeliveryType.POST;
        DeliveryType pushApiDeliveryType = DeliveryType.PICKUP;

        mockReportAvailableTypes(parameters, List.of(reportAvailableType));
        mockPushApiAvailableTypes(parameters, pushApiDeliveryType);

        MultiCartResponse cart = orderCreateHelper.multiCartActualize(parameters);

        assertEquals(1, cart.getCarts().size());
        assertEquals(1, cart.getCarts().get(0).getAvailableDeliveryTypes().size());
        assertEquals(reportAvailableType, cart.getCarts().get(0).getAvailableDeliveryTypes().iterator().next());
    }

    @Test
    public void shouldPushApiDeliveryTypeForTariffEnabled() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.setAcceptMethod(OrderAcceptMethod.PUSH_API);
        parameters.setExperiments(enableUnifiedTariffs());
        AvailableDeliveryType reportAvailableType = AvailableDeliveryType.POST;
        DeliveryType pushApiDeliveryType = DeliveryType.PICKUP;

        mockReportAvailableTypes(parameters, List.of(reportAvailableType));
        mockPushApiAvailableTypes(parameters, pushApiDeliveryType);

        MultiCartResponse cart = orderCreateHelper.multiCartActualize(parameters);

        assertEquals(1, cart.getCarts().size());
        assertEquals(1, cart.getCarts().get(0).getAvailableDeliveryTypes().size());
        assertEquals(AvailableDeliveryType.fromDeliveryType(pushApiDeliveryType),
                cart.getCarts().get(0).getAvailableDeliveryTypes().iterator().next());
    }

    @Test
    public void shouldReportDeliveryTypeForTariffEnabledWithShopAdmin() {
        Parameters parameters = WhiteParametersProvider.defaultWhiteParameters();
        parameters.setShopAdmin(true);
        parameters.setExperiments(enableUnifiedTariffs());
        AvailableDeliveryType reportAvailableType = AvailableDeliveryType.POST;
        DeliveryType pushApiDeliveryType = DeliveryType.PICKUP;

        mockReportAvailableTypes(parameters, List.of(reportAvailableType));
        mockPushApiAvailableTypes(parameters, pushApiDeliveryType);

        MultiCartResponse cart = orderCreateHelper.multiCartActualize(parameters);

        assertEquals(1, cart.getCarts().size());
        assertEquals(1, cart.getCarts().get(0).getAvailableDeliveryTypes().size());
        assertEquals(reportAvailableType, cart.getCarts().get(0).getAvailableDeliveryTypes().iterator().next());
    }

    @Test
    public void shouldAllDeliveryType() {
        Parameters parameters = BlueParametersProvider.clickAndCollectOrderParameters();
        AvailableDeliveryType reportAvailableType = AvailableDeliveryType.POST;
        DeliveryType pushApiDeliveryType = DeliveryType.PICKUP;

        mockReportAvailableTypes(parameters, List.of(reportAvailableType));
        mockPushApiAvailableTypes(parameters, pushApiDeliveryType);

        MultiCartResponse cart = orderCreateHelper.multiCartActualize(parameters);

        assertEquals(1, cart.getCarts().size());
        assertEquals(2, cart.getCarts().get(0).getAvailableDeliveryTypes().size());
        assertEquals(
                Set.of(AvailableDeliveryType.fromDeliveryType(pushApiDeliveryType), reportAvailableType),
                cart.getCarts().get(0).getAvailableDeliveryTypes());
    }

    private Experiments enableUnifiedTariffs() {
        checkouterProperties.setEnableUnifiedTariffs(true);
        final Experiments experiments = getExperiments().with(
                Map.of(MARKET_UNIFIED_TARIFFS, MARKET_UNIFIED_TARIFFS_VALUE));
        CheckoutContextHolder.setExperiments(experiments);
        return experiments;
    }


    private void mockPushApiAvailableTypes(Parameters parameters, DeliveryType deliveryType) {
        parameters.getPushApiDeliveryResponses().forEach(a -> a.setType(deliveryType));
    }

    private void mockReportAvailableTypes(Parameters parameters, List<AvailableDeliveryType> types) {
        parameters.getReportParameters().getActualDelivery().getResults().get(0).setAvailableDeliveryMethods(
                types.stream()
                        .map(AvailableDeliveryType::getCode)
                        .collect(Collectors.toList())
        );
    }
}

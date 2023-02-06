package ru.yandex.market.delivery.transport_manager.service.register;

import java.time.Instant;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.OrderBindingType;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.Register;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterType;
import ru.yandex.market.delivery.transport_manager.factory.DeliveryFactory;
import ru.yandex.market.delivery.transport_manager.factory.FulfillmentFactory;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;
import ru.yandex.market.delivery.transport_manager.repository.mappers.register.RegisterMapper;
import ru.yandex.market.delivery.transport_manager.service.order.OrderBindingService;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;

public class OldRegisterPlanServiceTest extends AbstractContextualTest {

    @Autowired
    private OldRegisterPlanService oldRegisterPlanService;

    @Autowired
    private RegisterMapper registerMapper;

    @Autowired
    private DeliveryClient lgwDeliveryClient;
    @Autowired
    private FulfillmentClient lgwFulfillmentClient;
    @Autowired
    private OrderBindingService orderBindingService;
    @Autowired
    private TransportationMapper transportationMapper;
    @Autowired
    private RegisterService registerService;

    @Test
    @SuppressWarnings("all")
    @DatabaseSetup("/repository/transportation/all_kinds_of_transportation.xml")
    @DatabaseSetup("/repository/register/old_scheme_routes.xml")
    @DatabaseSetup("/repository/register/legal_info_meta_dr_sc_ds.xml")
    @DatabaseSetup("/repository/register/transportation_partner_info_dr_sc_ds.xml")
    @ExpectedDatabase(
        value = "/repository/register/after/plan_sent_to_sc_and_to_ds_new_algorithm.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void sendPlanToScAndThenToDs() throws GatewayApiException {
        clock.setFixed(Instant.parse("2020-09-06T15:00:00.0Z"), ZoneOffset.UTC);

        Transportation first = transportationMapper.getById(4L);
        Transportation second = transportationMapper.getById(5L);

        registerService.findOrCreatePlan(first.getInboundUnit().getId());
        registerService.findOrCreatePlan(second.getInboundUnit().getId());

        orderBindingService.bindAllMatchingToTransportation(
            first,
            OrderBindingType.BEFORE_OLD_REGISTER_SENDING
        );
        orderBindingService.bindAllMatchingToTransportation(
            second,
            OrderBindingType.BEFORE_OLD_REGISTER_SENDING
        );

        Register firstRegister =
            registerMapper.getByTransportationUnitIdAndType(first.getInboundUnit().getId(), RegisterType.PLAN);
        Register secondRegister =
            registerMapper.getByTransportationUnitIdAndType(second.getInboundUnit().getId(), RegisterType.PLAN);

        oldRegisterPlanService.sendOnce(first, firstRegister, true);
        oldRegisterPlanService.sendOnce(second, secondRegister, true);

        verify(lgwFulfillmentClient).createRegister(
            refEq(FulfillmentFactory.createRegister()),
            refEq(new Partner(9L)),
            any()
        );
        verify(lgwDeliveryClient).createRegister(
            refEq(DeliveryFactory.createRegister("TMR2", "1000004")),
            refEq(new Partner(10L)),
            any()
        );
    }

    @Test
    @SuppressWarnings("all")
    @DatabaseSetup("/repository/transportation/all_kinds_of_transportation.xml")
    @DatabaseSetup("/repository/register/old_scheme_routes.xml")
    @DatabaseSetup("/repository/register/legal_info_meta_dr_sc_ds.xml")
    @DatabaseSetup("/repository/register/transportation_partner_info_dr_sc_ds.xml")
    @DatabaseSetup("/repository/transportation/transportation_partner_method.xml")
    void testApiTypeInvocation() throws GatewayApiException {
        clock.setFixed(Instant.parse("2020-09-06T15:00:00.0Z"), ZoneOffset.UTC);
        Transportation transportation = transportationMapper.getById(5);
        registerService.findOrCreatePlan(transportation.getInboundUnit().getId());

        orderBindingService.bindAllMatchingToTransportation(
            transportation,
            OrderBindingType.BEFORE_OLD_REGISTER_SENDING
        );
        oldRegisterPlanService.sendOnce(
            transportation,
            registerService.findOrCreatePlan(transportation.getInboundUnit().getId()),
            true
        );

        verify(lgwFulfillmentClient).createRegister(
            refEq(FulfillmentFactory.createRegisterDifferentApiType()),
            // partner with id 10 has partnerType = DELIVERY
            refEq(new Partner(10L)),
            any()
        );
    }
}

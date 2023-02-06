package ru.yandex.market.delivery.transport_manager.facade.register;

import java.time.Instant;
import java.time.ZoneOffset;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.Transportation;
import ru.yandex.market.delivery.transport_manager.domain.entity.register.Register;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterStatus;
import ru.yandex.market.delivery.transport_manager.factory.DeliveryFactory;
import ru.yandex.market.delivery.transport_manager.factory.FulfillmentFactory;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationMapper;
import ru.yandex.market.delivery.transport_manager.service.register.RegisterService;
import ru.yandex.market.logistic.gateway.client.DeliveryClient;
import ru.yandex.market.logistic.gateway.client.FulfillmentClient;
import ru.yandex.market.logistic.gateway.client.exceptions.GatewayApiException;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;

import static com.github.springtestdbunit.assertion.DatabaseAssertionMode.NON_STRICT_UNORDERED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;

class RegisterPlanFacadeTest extends AbstractContextualTest {
    @Autowired
    private DeliveryClient lgwDeliveryClient;
    @Autowired
    private FulfillmentClient lgwFulfillmentClient;
    @Autowired
    private RegisterPlanFacade registerPlanFacade;
    @Autowired
    private TransportationMapper transportationMapper;
    @Autowired
    private RegisterService registerService;

    @Test
    @SuppressWarnings("all")
    @DatabaseSetup("/repository/transportation/all_kinds_of_transportation.xml")
    @DatabaseSetup("/repository/register/existing_register.xml")
    @DatabaseSetup("/repository/register/legal_info_meta_dr_sc_ds.xml")
    @DatabaseSetup("/repository/register/transportation_partner_info_dr_sc_ds.xml")
    @ExpectedDatabase(
        value = "/repository/register/after/after_final_sending.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void sendPlanToScAndThenToDsFinalSend() throws GatewayApiException {
        clock.setFixed(Instant.parse("2020-09-06T15:00:00.0Z"), ZoneOffset.UTC);

        registerPlanFacade.send(4L, true);
        registerPlanFacade.send(5L, true);

        verify(lgwFulfillmentClient).createRegister(
            refEq(FulfillmentFactory.createRegister()),
            refEq(new Partner(9L)),
            any()
        );
        verify(lgwDeliveryClient).createRegister(
            refEq(DeliveryFactory.createRegister("TMR3", "1000005")),
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
    @ExpectedDatabase(
        value = "/repository/register/after/plan_sent_to_sc_and_to_ds_new_algorithm_both_registers.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void sendPlanToScAndThenToDsFirstSend() throws GatewayApiException {
        clock.setFixed(Instant.parse("2020-09-06T15:00:00.0Z"), ZoneOffset.UTC);

        registerPlanFacade.send(4L, false);
        registerPlanFacade.send(5L, false);

        verify(lgwFulfillmentClient).createRegister(
            refEq(FulfillmentFactory.createRegister()),
            refEq(new Partner(9L)),
            any()
        );
        verify(lgwDeliveryClient).createRegister(
            refEq(DeliveryFactory.createRegister("TMR3", "1000004")),
            refEq(new Partner(10L)),
            any()
        );
    }

    @Test
    @SuppressWarnings("all")
    @DatabaseSetup("/repository/transportation/all_kinds_of_transportation.xml")
    @DatabaseSetup("/repository/register/after/plan_sent_to_sc_and_to_ds_new_algorithm_both_registers.xml")
    @DatabaseSetup("/repository/register/legal_info_meta_dr_sc_ds.xml")
    @DatabaseSetup("/repository/register/transportation_partner_info_dr_sc_ds.xml")
    @ExpectedDatabase(
        value = "/repository/register/after/after_final_sending_nothing_sent.xml",
        assertionMode = NON_STRICT_UNORDERED
    )
    void testNothingFinallySent() throws GatewayApiException {
        clock.setFixed(Instant.parse("2020-09-06T15:00:00.0Z"), ZoneOffset.UTC);

        registerPlanFacade.send(4L, true);
        registerPlanFacade.send(5L, true);

        verify(lgwFulfillmentClient, Mockito.times(0)).createRegister(any(), any(), any());
        verify(lgwDeliveryClient, Mockito.times(0)).createRegister(any(), any(), any());
    }

    @Test
    @SuppressWarnings("all")
    @DatabaseSetup("/repository/transportation/all_kinds_of_transportation.xml")
    @DatabaseSetup("/repository/register/old_scheme_routes.xml")
    @DatabaseSetup("/repository/register/legal_info_meta_dr_sc_ds.xml")
    @DatabaseSetup("/repository/register/transportation_partner_info_dr_sc_ds.xml")
    void resendInboundPlan() throws GatewayApiException {
        clock.setFixed(Instant.parse("2020-09-06T15:00:00.0Z"), ZoneOffset.UTC);
        Transportation transportation = transportationMapper.getById(4);
        Register inbRegister = registerService.findOrCreatePlan(transportation.getInboundUnit().getId());
        registerService.updateStatusAndDate(inbRegister.getId(), RegisterStatus.SENT_FINAL, null);
        registerService.updateStatusAndDate(inbRegister.getId(), RegisterStatus.ERROR, null);

        registerPlanFacade.resendInboundRegister(transportation);

        verify(lgwFulfillmentClient).createRegister(
            refEq(FulfillmentFactory.createRegister()),
            refEq(new Partner(9L)),
            any()
        );

        softly.assertThat(registerService.findOrCreatePlan(transportation.getInboundUnit().getId()).getStatus())
            .isEqualTo(RegisterStatus.SENT_FINAL);
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

        registerPlanFacade.send(5L, true);

        verify(lgwFulfillmentClient).createRegister(
            refEq(FulfillmentFactory.createRegisterDifferentApiType()),
            // partner with id 10 has partnerType = DELIVERY
            refEq(new Partner(10L)),
            any()
        );
    }
}

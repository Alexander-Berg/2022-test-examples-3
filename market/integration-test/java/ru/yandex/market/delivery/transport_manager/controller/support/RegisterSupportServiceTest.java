package ru.yandex.market.delivery.transport_manager.controller.support;

import java.time.Instant;
import java.util.Set;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.domain.entity.TransportationPartnerMethod;
import ru.yandex.market.delivery.transport_manager.domain.enums.RegisterStatus;
import ru.yandex.market.delivery.transport_manager.facade.register.RegisterPlanFacade;
import ru.yandex.market.delivery.transport_manager.repository.mappers.TransportationPartnerMethodMapper;
import ru.yandex.market.delivery.transport_manager.service.RegisterSupportService;
import ru.yandex.market.delivery.transport_manager.service.external.lms.dto.method.PartnerMethod;
import ru.yandex.market.delivery.transport_manager.service.register.RegisterService;

@DbUnitConfiguration(databaseConnection = {"dbUnitDatabaseConnection", "dbUnitDatabaseConnectionDbQueue"})
public class RegisterSupportServiceTest extends AbstractContextualTest {

    @Autowired
    private RegisterSupportService registerSupportService;

    @Autowired
    private RegisterService registerService;

    @Autowired
    private TransportationPartnerMethodMapper methodMapper;

    @Autowired
    private RegisterPlanFacade registerPlanFacade;

    @Test
    void sendNotExisting() {
        softly.assertThatThrownBy(() -> registerSupportService.send(1L));
    }

    @Test
    @DatabaseSetup({"/repository/register/put_outbound_register.xml"})
    void sendOutboundInvalidStatus() {
        softly.assertThatThrownBy(() -> registerSupportService.send(11L));
    }

    @Test
    @DatabaseSetup(
        value = "/repository/task/no_tasks.xml",
        type = DatabaseOperation.DELETE_ALL,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @DatabaseSetup({"/repository/register/put_outbound_register.xml"})
    @ExpectedDatabase(
        value = "/repository/register/put_outbound_register_task.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void sendOutboundNewFlow() {
        Set<TransportationPartnerMethod> data = Set.of(
            transportationPartnerMethod(98L, 5L, PartnerMethod.PUT_OUTBOUND_REGISTRY),
            transportationPartnerMethod(98L, 6L, PartnerMethod.PUT_INBOUND)
        );

        registerService.updateStatusAndDate(11L, RegisterStatus.ERROR, Instant.now());
        methodMapper.insert(data);
        registerSupportService.send(11L);
    }

    @Test
    @DatabaseSetup(
        value = "/repository/task/no_tasks.xml",
        type = DatabaseOperation.DELETE_ALL,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @DatabaseSetup({"/repository/register/put_outbound_register.xml"})
    void sendOutboundNoMethodsSupported() {
        registerService.updateStatusAndDate(11L, RegisterStatus.ERROR, Instant.now());
        softly.assertThatThrownBy(() -> registerSupportService.send(11L));
    }

    @Test
    @DatabaseSetup(
        value = "/repository/task/no_tasks.xml",
        type = DatabaseOperation.DELETE_ALL,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @DatabaseSetup({"/repository/register/put_inbound_register.xml"})
    @ExpectedDatabase(
        value = "/repository/register/put_inbound_register_task.xml",
        connection = "dbUnitDatabaseConnectionDbQueue",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void sendInboundNewFlow() {
        Set<TransportationPartnerMethod> data = Set.of(
            transportationPartnerMethod(98L, 6L, PartnerMethod.PUT_INBOUND_REGISTRY),
            transportationPartnerMethod(98L, 6L, PartnerMethod.PUT_INBOUND)
        );

        registerService.updateStatusAndDate(11L, RegisterStatus.ERROR, Instant.now());
        methodMapper.insert(data);
        registerSupportService.send(11L);
    }

    @Test
    @DatabaseSetup(
        value = "/repository/task/no_tasks.xml",
        type = DatabaseOperation.DELETE_ALL,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @DatabaseSetup({"/repository/register/put_inbound_register.xml"})
    void sendInboundNoMethodsSupported() {
        registerService.updateStatusAndDate(11L, RegisterStatus.ERROR, Instant.now());
        softly.assertThatThrownBy(() -> registerSupportService.send(11L));
    }

    @Test
    @DatabaseSetup(
        value = "/repository/task/no_tasks.xml",
        type = DatabaseOperation.DELETE_ALL,
        connection = "dbUnitDatabaseConnectionDbQueue"
    )
    @DatabaseSetup({"/repository/register/put_inbound_register.xml"})
    void sendInboundOldFlow() {
        Set<TransportationPartnerMethod> data = Set.of(
            transportationPartnerMethod(98L, 6L, PartnerMethod.CREATE_INTAKE),
            transportationPartnerMethod(98L, 6L, PartnerMethod.CREATE_REGISTER)
        );

        registerService.updateStatusAndDate(11L, RegisterStatus.SENT_FINAL, Instant.now());
        registerService.updateStatusAndDate(11L, RegisterStatus.ERROR, Instant.now());
        methodMapper.insert(data);
        registerSupportService.send(11L);
        Mockito.verify(registerPlanFacade).resendInboundRegister(Mockito.any());
    }

    private static TransportationPartnerMethod transportationPartnerMethod(
        Long transportationId,
        Long partnerId,
        PartnerMethod method
    ) {
        return new TransportationPartnerMethod()
            .setTransportationId(transportationId)
            .setMethod(method)
            .setPartnerId(partnerId);
    }
}

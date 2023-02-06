package ru.yandex.market.delivery.transport_manager.service.interwarehouse.transportation_task;

import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.delivery.transport_manager.AbstractContextualTest;
import ru.yandex.market.delivery.transport_manager.admin.dto.NewTransportationTaskDto;
import ru.yandex.market.delivery.transport_manager.admin.enums.AdminCountType;
import ru.yandex.market.delivery.transport_manager.domain.enums.ClientName;
import ru.yandex.market.delivery.transport_manager.exception.AdminValidationException;
import ru.yandex.market.delivery.transport_manager.model.dto.StockKeepingUnitDto;
import ru.yandex.market.delivery.transport_manager.model.dto.TransportationTaskDto;
import ru.yandex.market.delivery.transport_manager.model.enums.CountType;

import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;

public class TransportationTaskCreationServiceTest extends AbstractContextualTest {

    @Autowired
    private TransportationTaskCreationService transportationTaskCreationService;

    private static final TransportationTaskDto TASK_DTO =
        new TransportationTaskDto()
            .setLogisticPointTo(2L)
            .setLogisticPointFrom(1L)
            .setExternalId(1L)
            .setRegister(
                List.of(
                    new StockKeepingUnitDto().setSsku("ssku1").setSupplierId("id1").setCount(5),
                    new StockKeepingUnitDto().setSsku("ssku2").setSupplierId("id2").setCount(10),
                    new StockKeepingUnitDto()
                        .setSsku("ssku2")
                        .setSupplierId("id3")
                        .setCount(15)
                        .setRealSupplierId("real_id")
                        .setCountType(CountType.FIT)
                )
            );

    @Test
    @DatabaseSetup("/repository/transportation_task/after/after_task_creation.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/after_task_creation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void findTask() {
        softly.assertThat(transportationTaskCreationService.findOrCreate(TASK_DTO, ClientName.MBOC.getTvmServiceId()))
            .isEqualTo(1);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/after_task_creation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createTask() {
        softly.assertThat(transportationTaskCreationService.findOrCreate(TASK_DTO, ClientName.MBOC.getTvmServiceId()))
            .isEqualTo(1);
    }

    @Test
    @DatabaseSetup("/repository/transportation_task/after/after_task_creation_failed.xml")
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/after_task_creation_failed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void failCreateTask() {
        softly.assertThatThrownBy(() -> transportationTaskCreationService.findOrCreate(new TransportationTaskDto()
            .setRegister(
                List.of(
                    new StockKeepingUnitDto(),
                    new StockKeepingUnitDto().setCountType(CountType.DEFECT)
                )
            ), ClientName.MBOC.getTvmServiceId())).hasMessage("Too many count types: [FIT, DEFECT]");
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/after_manual_task_creation.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createFromFile() {
        byte[] bytes = extractFileContent("controller/admin/transportation_task/task_register.csv").getBytes();
        NewTransportationTaskDto dto = (NewTransportationTaskDto) new NewTransportationTaskDto()
            .setCountType(AdminCountType.FIT)
            .setOutboundPointId(1L)
            .setInboundPointId(2L)
            .setRegister(bytes);

        transportationTaskCreationService.createFromFile(dto);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/transportation_task/after/after_task_creation_failed.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void createFromFileSameOutboundAndInboundPoints() {
        byte[] bytes = extractFileContent("controller/admin/transportation_task/task_register.csv").getBytes();
        NewTransportationTaskDto dto = (NewTransportationTaskDto) new NewTransportationTaskDto()
            .setCountType(AdminCountType.FIT)
            .setOutboundPointId(1L)
            .setInboundPointId(1L)
            .setRegister(bytes);

        Assertions.assertThatThrownBy(() -> transportationTaskCreationService.createFromFile(dto))
            .isInstanceOf(AdminValidationException.class)
            .hasMessage("Точки отгрузки и приёмки совпадают");
    }
}

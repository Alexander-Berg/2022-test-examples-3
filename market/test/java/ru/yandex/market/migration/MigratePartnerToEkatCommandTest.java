package ru.yandex.market.migration;

import java.io.PrintWriter;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.terminal.CommandInvocation;
import ru.yandex.common.util.terminal.Terminal;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.bpmn.client.MbiBpmnClient;
import ru.yandex.market.mbi.bpmn.client.model.ProcessInstanceRequest;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartInstance;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStartResponse;
import ru.yandex.market.mbi.bpmn.client.model.ProcessStatus;
import ru.yandex.market.mbi.bpmn.client.model.ProcessType;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DbUnitDataSet(before = "MigratePartnerToEkatCommandTest.before.csv")
public class MigratePartnerToEkatCommandTest extends FunctionalTest {
    @Autowired
    private MigratePartnerToEkatCommand migratePartnerToEkatCommand;

    @Autowired
    private Terminal terminal;

    @Autowired
    private PrintWriter printWriter;

    @Autowired
    private MbiBpmnClient mbiBpmnClient;

    @BeforeEach
    void beforeEach() {
        when(terminal.getWriter()).thenReturn(printWriter);
    }

    @AfterEach
    void checkClient() {
        Mockito.verifyNoMoreInteractions(mbiBpmnClient);
    }

    @ParameterizedTest
    @DisplayName("Невалидные данные")
    @MethodSource("testInvalidArgumentsData")
    @DbUnitDataSet(after = "MigratePartnerToEkatCommandTest.before.csv")
    void testInvalidArguments(String name, Long partnerId) {
        invoke(partnerId);
    }

    private static Stream<Arguments> testInvalidArgumentsData() {
        return Stream.of(
                Arguments.of(
                        "Партнер уже в екат",
                        777L
                ),
                Arguments.of(
                        "Партнера нет в базе",
                        999L
                )
        );
    }

    @Test
    @DisplayName("Успешный запуск")
    @DbUnitDataSet(
            after = "MigratePartnerToEkatCommandTest.after.csv"
    )
    void testSuccess() {
        ProcessStartInstance instance = new ProcessStartInstance();
        instance.setStatus(ProcessStatus.ACTIVE);
        instance.setProcessInstanceId("camunda_process_id");

        var response = new ProcessStartResponse();
        response.addRecordsItem(instance);
        Mockito.when(mbiBpmnClient.postProcess(any())).thenReturn(response);

        invoke(778L);

        ProcessInstanceRequest expected = new ProcessInstanceRequest();
        expected.setProcessType(ProcessType.MIGRATE_TO_UCAT);
        expected.setParams(Map.of(
                "serviceId", "778",
                "businessId", "100",
                "operationId", "1"
        ));
        Mockito.verify(mbiBpmnClient).postProcess(expected);
    }

    private void invoke(Long partnerId) {
        CommandInvocation commandInvocation = new CommandInvocation("migrate-to-ekat",
                new String[]{String.valueOf(partnerId)},
                Collections.emptyMap());

        migratePartnerToEkatCommand.executeCommand(commandInvocation, terminal);
    }
}

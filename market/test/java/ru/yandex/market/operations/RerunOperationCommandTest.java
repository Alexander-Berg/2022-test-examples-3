package ru.yandex.market.operations;

import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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

/**
 * Тесты для {@link RerunOperationCommand}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class RerunOperationCommandTest extends FunctionalTest {

    @Autowired
    private RerunOperationCommand rerunOperationCommand;

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
    void testInvalidArguments(String name, List<Long> ids) {
        invoke(ids.toArray(new Long[0]));
    }

    private static Stream<Arguments> testInvalidArgumentsData() {
        return Stream.of(
                Arguments.of(
                        "Нет параметров",
                        List.of()
                ),
                Arguments.of(
                        "Несколько параметров",
                        List.of(123L, 345L)
                ),
                Arguments.of(
                        "Операции нет в базе",
                        List.of(999L)
                )
        );
    }

    @Test
    @DisplayName("Успешный перезапуск")
    @DbUnitDataSet(
            before = "RerunOperationCommandTest.success.before.csv",
            after = "RerunOperationCommandTest.success.after.csv"
    )
    void testSuccess() {
        ProcessStartInstance instance = new ProcessStartInstance();
        instance.setStatus(ProcessStatus.ACTIVE);
        instance.setProcessInstanceId("camunda_process_id_2");

        ProcessStartResponse response = new ProcessStartResponse();
        response.addRecordsItem(instance);
        Mockito.when(mbiBpmnClient.postProcess(any())).thenReturn(response);

        invoke(1001L);

        ProcessInstanceRequest expected = new ProcessInstanceRequest();
        expected.setProcessType(ProcessType.PARTNER_FEED_OFFER_MIGRATION);
        expected.setParams(Map.of(
                "partnerId", "777",
                "feedId", "100",
                "timestamp", "2019-12-31T21:00:00Z",
                "needHide", "true",
                "needMigrate", "true",
                "operationId", "1"
        ));
        Mockito.verify(mbiBpmnClient).postProcess(expected);
    }

    private void invoke(Long... operationIds) {
        String[] args = Arrays.stream(operationIds).map(String::valueOf).toArray(String[]::new);
        CommandInvocation commandInvocation = new CommandInvocation("rerun-operation",
                args,
                Collections.emptyMap());

        rerunOperationCommand.executeCommand(commandInvocation, terminal);
    }
}

package step;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import client.LgwClient;
import dto.responses.lgw.LgwTaskFlow;
import dto.responses.lgw.LgwTaskItem;
import dto.responses.lgw.TasksResponse;
import dto.responses.lgw.message.get_order.GetOrderRequest;
import dto.responses.lgw.message.get_order.Order;
import dto.responses.lgw.task.TaskResponse;
import io.qameta.allure.Step;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Assertions;
import toolkit.Retrier;

import ru.yandex.market.logistics.lom.model.dto.OrderDto;
import ru.yandex.market.logistics.lom.model.dto.WaybillSegmentDto;

public class LgwSteps {

    private static final LgwClient LGW_CLIENT = new LgwClient();

    @Step("Получить задачи с entityId = {entityId}")
    public TasksResponse getTasks(String entityId) {
        return LGW_CLIENT.getTasks(entityId);
    }

    @Step("Получить задачи с entityId = {entityId} и requestFlow = {lgwTaskFlow}")
    public LgwTaskItem getReadyTaskFromListWithEntityIdAndRequestFlow(
        String entityId,
        LgwTaskFlow lgwTaskFlow
    ) {
        return getTaskFromListWithEntityIdAndRequestFlow(entityId, lgwTaskFlow, "READY");
    }

    @Step("Получить задачи с entityId = {entityId} и requestFlow = {lgwTaskFlow} и статусом = {expectedStatus}")
    public LgwTaskItem getTaskFromListWithEntityIdAndRequestFlow(
        String entityId,
        LgwTaskFlow lgwTaskFlow,
        String expectedStatus
    ) {
        List<LgwTaskItem> tasks = getTasksFromListWithEntityIdAndRequestFlow(entityId, lgwTaskFlow, 1, expectedStatus);
        Assertions.assertFalse(tasks.isEmpty(), "Отсутствует таска с entityId = " + entityId +
            " и requestFlow = " + lgwTaskFlow + " и статусом " + expectedStatus);
        return tasks.get(0);
    }

    @Step("Получить задачи с entityId = {entityId} и requestFlow = {lgwTaskFlow}")
    public List<LgwTaskItem> getReadyTasksFromListWithEntityIdAndRequestFlow(
        String entityId,
        LgwTaskFlow lgwTaskFlow,
        int minSize
    ) {
        return getTasksFromListWithEntityIdAndRequestFlow(entityId, lgwTaskFlow, minSize, "READY");
    }

    @Step("Получить задачи с entityId = {entityId} и requestFlow = {lgwTaskFlow} и статусом = {expectedStatus}")
    public List<LgwTaskItem> getTasksFromListWithEntityIdAndRequestFlow(
        String entityId,
        LgwTaskFlow lgwTaskFlow,
        int minSize,
        String expectedStatus
    ) {
        return Retrier.retry(() -> {
            List<LgwTaskItem> tasks = getTasksList(entityId, lgwTaskFlow);

            org.assertj.core.api.Assertions.assertThat(tasks)
                .as("Таски " + lgwTaskFlow + " выставились не всем партнёрам. Количество тасок ЛГВ = "
                    + tasks.size())
                .hasSizeGreaterThan(minSize - 1);
            //тут делаем -1, потому что неравенство строгое

            Assertions
                .assertTrue(
                    tasks.stream().allMatch(s -> s.getValues().getStatus().equals(expectedStatus)),
                    "Задача " + lgwTaskFlow.getFlow() + " завершилась с ошибкой"
                );

            return tasks.
                stream()
                .sorted(Comparator.comparing(t -> t.getValues().getUpdated()))
                .collect(Collectors.toList());
        });
    }

    @Step("Сравнить родительские и дочерние таски с entityId = {entityId} и requestFlow = {lgwTaskFlow}")
    public boolean isParentsAndChildLgwTasksMatch(String entityId, LgwTaskFlow lgwTaskFlow) {
        return Retrier.retry(() -> {
            String stringResult = lgwTaskFlow.getFlow() + "-success";
            LgwTaskFlow lgwTaskFlowSuccess =
                Arrays.stream(LgwTaskFlow.values())
                    .filter(v -> v.getFlow().equals(stringResult))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Не найден " + stringResult));

            List<LgwTaskItem> tasks = getTasksList(entityId, lgwTaskFlow);
            List<LgwTaskItem> tasksSuccess = getTasksList(entityId, lgwTaskFlowSuccess);

            List<Integer> parentsId = tasks.stream()
                .map(s -> s.getValues().getTaskId())
                .collect(Collectors.toList());

            List<Integer> childsId = tasksSuccess.stream()
                .map(t -> t.getValues().getParentId())
                .collect(Collectors.toList());

            return parentsId.equals(childsId);
        });
    }

    @Step("Получить задачу по id = {taskId}")
    public TaskResponse getTask(Long taskId) {
        return LGW_CLIENT.getTask(taskId);
    }

    @Step("Получить задачу по id = {taskId}")
    public <T> T getTask(Long taskId, LgwTaskFlow lgwTaskFlow) {
        return LGW_CLIENT.getTask(taskId).getItem()
            .getValues()
            .getMessage()
            .getText(lgwTaskFlow);
    }

    @Step("Получаем тело запроса в указанные СД по типу таски = {lgwTaskFlow}")
    public <T> Pair<T, T> getMessagesByOrderIdAndRequestFlow(List<LgwTaskItem> tasks, LgwTaskFlow lgwTaskFlow) {

        TaskResponse firstTask = getTask(tasks.get(0).getId());
        TaskResponse secondTask = getTask(tasks.get(1).getId());

        Assertions.assertNotNull(firstTask.getItem(),
            "Не удалось получить таски  " + lgwTaskFlow);
        Assertions.assertNotNull(firstTask.getItem().getValues(),
            "Не удалось получить тело таски " + lgwTaskFlow);
        Assertions.assertNotNull(secondTask.getItem(),
            "Не удалось получить таски " + lgwTaskFlow);
        Assertions.assertNotNull(secondTask.getItem().getValues(),
            "Не удалось получить тело таски " + lgwTaskFlow);

        T messageToLastMile = firstTask.getItem().getValues().getMessage().getText(lgwTaskFlow);
        T messageToMiddleMile = secondTask.getItem().getValues().getMessage().getText(lgwTaskFlow);
        return Pair.of(messageToLastMile, messageToMiddleMile);
    }

    @Step("Обновляем дату доставки в партнере")
    public void updateOrderDeliveryDate(OrderDto order, WaybillSegmentDto waybillSegmentDto) {
        long partnerId = waybillSegmentDto.getPartnerId();
        String deliveryId = waybillSegmentDto.getExternalId();
        LocalDate dateTime = order.getDeliveryInterval().getDeliveryDateMax().plusDays(2);
        LocalTime fromTime = order.getDeliveryInterval().getFromTime();
        LocalTime endTime = order.getDeliveryInterval().getToTime();

        LGW_CLIENT.postUpdateDeliveryDate(order.getExternalId(), partnerId, deliveryId, dateTime, fromTime, endTime);
    }

    @Step("Получение заказа по DS-Api")
    public Order dsGetOrder(String orderId, Long partnerId) {
        return LGW_CLIENT.dsGetOrder(
            new GetOrderRequest()
                .setOrderId(new GetOrderRequest.OrderId().setYandexId(orderId))
                .setPartner(new GetOrderRequest.Partner().setId(partnerId))
        );
    }

    @Step("Получение всех задач с entityId = {entityId} и requestFlow = {lgwTaskFlow}")
    public List<LgwTaskItem> getTasksList(String entityId, LgwTaskFlow lgwTaskFlow) {
        return getTasks(entityId)
            .getItems()
            .stream()
            .filter(lgwTaskItem -> lgwTaskItem.getValues().getRequestFlow().equals(lgwTaskFlow.getFlow()))
            .collect(Collectors.toList());
    }
}

package ru.yandex.market.tpl.integration.tests.tests.partial_return;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ru.yandex.market.tpl.api.model.order.DetailedOrderDto;
import ru.yandex.market.tpl.api.model.order.OrderDto;
import ru.yandex.market.tpl.api.model.order.OrderItemDto;
import ru.yandex.market.tpl.api.model.order.OrderItemInstanceDto;
import ru.yandex.market.tpl.api.model.order.UpdateItemsInstancesPurchaseStatusRequestDto;
import ru.yandex.market.tpl.api.model.partial_return_order.LinkReturnableItemsInstancesWithBoxesRequestDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointDto;
import ru.yandex.market.tpl.api.model.routepoint.RoutePointType;
import ru.yandex.market.tpl.api.model.task.OrderDeliveryTaskDto;
import ru.yandex.market.tpl.integration.tests.client.ManualApiClient;
import ru.yandex.market.tpl.integration.tests.configuration.AutoTestsConfiguration;
import ru.yandex.market.tpl.integration.tests.context.AutoTestContextHolder;
import ru.yandex.market.tpl.integration.tests.exception.TplIncorrectDataTestException;
import ru.yandex.market.tpl.integration.tests.facade.ApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.CourierApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.ManualApiFacade;
import ru.yandex.market.tpl.integration.tests.facade.PublicApiFacade;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = AutoTestsConfiguration.class)
@Epic("Частичный возврат")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PartialReturnOrderTest {

    private final ManualApiFacade manualApiFacade;
    private final PublicApiFacade publicApiFacade;
    private final CourierApiFacade courierApiFacade;
    private final ApiFacade apiFacade;
    private final ManualApiClient manualApiClient;

    @BeforeEach
    void before() {
        courierApiFacade.createCourierWithSchedule();
    }

    @AfterEach
    void after() {
        manualApiFacade.deleteCourier();
        manualApiFacade.deleteOrder();
        AutoTestContextHolder.clearContext();
    }


    @Test
    @Feature("Процесс частичной выдачи заказа")
    @Stories({@Story("Создать заказ"), @Story("Забирать заказ с СЦ"),
            @Story("Указать возвращаемые позиции"), @Story("Завершить выдачу"),
            @Story("Убедиться, что следующая задача - возврат на СЦ")})
    @DisplayName(value = "Процесс частичной выдачи заказа")
    @SneakyThrows
    void testPartialReturning() {

        //Создание смены и рут поинта с заказом и заданием на доставку
        manualApiFacade.createShift();
        manualApiFacade.createUserShift();
        manualApiFacade.createRoutePointWithDeliveryTask(true);

        //Забираем заказ с СЦ
        apiFacade.pickupOrders();

        //Дозвониваемся покупателю
        publicApiFacade.successCallToRecipient();  //pickupTask -> deliverTask

        //Доезжаем до клиента
        publicApiFacade.arriveToRoutePoint();

        //Получаем данные о доставляемом заказе
        String externalOrderId = getExternalOrderId();
        DetailedOrderDto orderInfo = getOrderInfoByExtId(externalOrderId);

        //Отметить какие товары выкупаются и какие возвращаются
        UpdateItemsInstancesPurchaseStatusRequestDto requestWithPartialReturn =
                createRequestWithPartialReturn(orderInfo);
        publicApiFacade.updateItemsInstancesPurchaseStatus(requestWithPartialReturn);

        //Кнопка - выдать заказ
        publicApiFacade.giveParcel();

        //Привязываем возвратные позиции к сейф-пакетам
        publicApiFacade.createLogisticReturn(createLogisticReturnRequest(requestWithPartialReturn));

        publicApiFacade.finishLastDeliveryTask();

        RoutePointDto routePointDto = publicApiFacade.updateCurrentRoutePoint();

        //Проверяем что очередной рут-поинт - возврат на СЦ
        assertEquals(routePointDto.getType(), RoutePointType.ORDER_RETURN);

        apiFacade.finishShiftInSc();
    }

    private LinkReturnableItemsInstancesWithBoxesRequestDto createLogisticReturnRequest(UpdateItemsInstancesPurchaseStatusRequestDto requestWithPartialReturn) {

        return LinkReturnableItemsInstancesWithBoxesRequestDto
                .builder()
                .orders(requestWithPartialReturn
                        .getOrders()
                        .stream()
                        .map(this::mapToLinkDto).collect(Collectors.toList()))
                .build();
    }

    private LinkReturnableItemsInstancesWithBoxesRequestDto
            .LinkReturnableItemsInstancesWithBoxesByOrderRequestDto mapToLinkDto(
            UpdateItemsInstancesPurchaseStatusRequestDto
                    .UpdateItemsInstancesPurchaseStatusRequestByOrderDto orderRequest) {

        List<String> returnedUits = collectReturnedUits(orderRequest);

        List<String> collectBoxBarCodes = returnedUits
                .stream()
                .map(s -> s.replace("uit", ""))
                .map("FSN_RET_"::concat)
                .collect(Collectors.toList());

        return LinkReturnableItemsInstancesWithBoxesRequestDto
                .LinkReturnableItemsInstancesWithBoxesByOrderRequestDto.builder()
                .externalOrderId(orderRequest.getExternalOrderId())
                .taskId(getTaskId())
                .uits(returnedUits)
                .boxBarcodes(collectBoxBarCodes
                ).build();
    }

    private List<String> collectReturnedUits(UpdateItemsInstancesPurchaseStatusRequestDto.UpdateItemsInstancesPurchaseStatusRequestByOrderDto orderRequest) {
        return orderRequest
                .getReturnItemsInstances()
                .stream()
                .map(UpdateItemsInstancesPurchaseStatusRequestDto.ReturnItemInstanceDto::getUit)
                .collect(Collectors.toList());
    }

    private long getTaskId() {
        return AutoTestContextHolder.getContext().getRoutePoint().getTasks().iterator().next().getId();
    }

    private UpdateItemsInstancesPurchaseStatusRequestDto createRequestWithPartialReturn(DetailedOrderDto orderInfo) {
        OrderDto order = orderInfo.getOrder();

        List<String> allUits = collectAllUitsFromOrder(order);

        //Выбираем один uit на возврат - расчитываем что созданный заказ - содержит uits...

        if (allUits.isEmpty()) {
            throw new TplIncorrectDataTestException("Несоответсвие сгенерированных данных для интеграционного " +
                    "тестирования! Заказ: {} не содержит UITы", orderInfo.getOrder().getExternalOrderId());
        }
        String returningUit = allUits.remove(0);

        return UpdateItemsInstancesPurchaseStatusRequestDto.builder()
                .orders(List.of(UpdateItemsInstancesPurchaseStatusRequestDto.
                        UpdateItemsInstancesPurchaseStatusRequestByOrderDto.builder()
                        .externalOrderId(order.getExternalOrderId())
                        .returnItemsInstances(
                                List.of(UpdateItemsInstancesPurchaseStatusRequestDto
                                        .ReturnItemInstanceDto
                                        .builder()
                                        .uit(returningUit)
                                        .build()))
                        .purchaseItemsInstances(
                                allUits
                                        .stream()
                                        .map(uit -> UpdateItemsInstancesPurchaseStatusRequestDto
                                                .PurchaseItemInstanceDto.builder().uit(uit).build())
                                        .collect(Collectors.toList())
                        )
                        .build())
                ).build();
    }

    private List<String> collectAllUitsFromOrder(OrderDto order) {
        return order
                .getItems()
                .stream()
                .map(OrderItemDto::getInstances)
                .flatMap(Collection::stream)
                .map(OrderItemInstanceDto::getUit)
                .collect(Collectors.toList());
    }

    private String getExternalOrderId() {
        return ((OrderDeliveryTaskDto) AutoTestContextHolder.getContext()
                .getRoutePoint().getTasks().iterator().next()).getOrder().getExternalOrderId();
    }

    private DetailedOrderDto getOrderInfoByExtId(String externalOrderId) {
        return manualApiClient.getDetailedOrderInfoByExtId(externalOrderId);
    }
}

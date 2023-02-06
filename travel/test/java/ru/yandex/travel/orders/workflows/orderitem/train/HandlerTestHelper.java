package ru.yandex.travel.orders.workflows.orderitem.train;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ru.yandex.travel.dicts.rasp.proto.TTrainTariffInfo;
import ru.yandex.travel.orders.configurations.TrainTariffInfoDataProviderProperties;
import ru.yandex.travel.orders.services.train.tariffinfo.TrainTariffInfoDataProvider;
import ru.yandex.travel.orders.services.train.tariffinfo.TrainTariffInfoService;
import ru.yandex.travel.train.partners.im.model.ImBlankStatus;
import ru.yandex.travel.train.partners.im.model.PendingElectronicRegistration;
import ru.yandex.travel.train.partners.im.model.orderinfo.ImOperationStatus;
import ru.yandex.travel.train.partners.im.model.orderinfo.ImOperationType;
import ru.yandex.travel.train.partners.im.model.orderinfo.ImOrderItemType;
import ru.yandex.travel.train.partners.im.model.orderinfo.OrderInfoResponse;
import ru.yandex.travel.train.partners.im.model.orderinfo.OrderItemBlank;
import ru.yandex.travel.train.partners.im.model.orderinfo.OrderItemResponse;
import ru.yandex.travel.yt_lucene_index.TestLuceneIndexBuilder;

public class HandlerTestHelper {
    public static OrderInfoResponse createOrderInfoResponse(ImOperationStatus imOperationStatus, int blankId) {
        // TODO (simon-ekb): create builder
        OrderInfoResponse resp = new OrderInfoResponse();
        OrderItemResponse buyItem = new OrderItemResponse();
        buyItem.setOrderItemId(70000002);
        buyItem.setOperationType(ImOperationType.BUY);
        buyItem.setType(ImOrderItemType.RAILWAY);
        buyItem.setSimpleOperationStatus(imOperationStatus);
        buyItem.setReservationNumber("999555111");
        buyItem.setElectronicRegistrationExpirationDateTime(LocalDateTime.of(2019, 7, 30, 12, 0));

        OrderItemBlank blank = new OrderItemBlank();
        blank.setOrderItemBlankId(blankId);
        ImBlankStatus imBlankStatus = ImBlankStatus.UNKNOWN;
        if (imOperationStatus == ImOperationStatus.OK) {
            imBlankStatus = ImBlankStatus.REMOTE_CHECK_IN;
            blank.setElectronicRegistrationExpirationDateTime(LocalDateTime.of(2019, 7, 30, 12, 0));
            blank.setOnlineTicketReturnExpirationDateTime(LocalDateTime.of(2019, 7, 30, 11, 0));
        } else if (imOperationStatus == ImOperationStatus.FAILED) {
            imBlankStatus = ImBlankStatus.CANCELLED;
        }
        blank.setBlankStatus(imBlankStatus);
        blank.setPendingElectronicRegistration(PendingElectronicRegistration.NO_VALUE);
        List<OrderItemBlank> blanks = new ArrayList<>();
        blanks.add(blank);
        buyItem.setOrderItemBlanks(blanks);

        List<OrderItemResponse> orderItems = new ArrayList<>();
        orderItems.add(buyItem);
        resp.setOrderItems(orderItems);

        return resp;
    }

    public static void addTicketRefundOperation(OrderInfoResponse orderInfo, int originalBlankId,
                                                int refundOperationId, LocalDateTime confirmedAt) {
        var refund = new OrderItemResponse();
        orderInfo.getOrderItems().add(refund);
        refund.setPreviousOrderItemId(orderInfo.findBuyRailwayItems().get(0).getOrderItemId());
        refund.setOrderItemId(refundOperationId);
        refund.setOperationType(ImOperationType.REFUND);
        refund.setType(ImOrderItemType.RAILWAY);
        refund.setSimpleOperationStatus(ImOperationStatus.OK);
        refund.setReservationNumber("999555111");
        refund.setMskConfirmedAt(confirmedAt);

        OrderItemBlank blank = new OrderItemBlank();
        blank.setOrderItemBlankId(originalBlankId + 1000);
        blank.setPreviousOrderItemBlankId(originalBlankId);
        blank.setBlankStatus(ImBlankStatus.REFUNDED);
        blank.setPendingElectronicRegistration(PendingElectronicRegistration.NO_VALUE);
        blank.setAmount(BigDecimal.valueOf(1555));
        List<OrderItemBlank> blanks = new ArrayList<>();
        blanks.add(blank);
        refund.setOrderItemBlanks(blanks);
    }

    public static void addInsuranceRefundOperation(OrderInfoResponse orderInfo, int buyOperationId,
                                                   int refundOperationId) {
        var refund = new OrderItemResponse();
        orderInfo.getOrderItems().add(refund);
        refund.setOperationType(ImOperationType.REFUND);
        refund.setType(ImOrderItemType.INSURANCE);
        refund.setSimpleOperationStatus(ImOperationStatus.OK);
        refund.setReservationNumber("999555111");
        refund.setMskConfirmedAt(LocalDateTime.of(2019, 12, 19, 17, 0));
        refund.setPreviousOrderItemId(buyOperationId);
        refund.setOrderItemId(refundOperationId);
        refund.setAmount(BigDecimal.valueOf(150));
    }

    public static void addInsuranceBuyOperation(OrderInfoResponse orderInfo, int operationId) {
        var item = new OrderItemResponse();
        orderInfo.getOrderItems().add(item);
        item.setOperationType(ImOperationType.BUY);
        item.setType(ImOrderItemType.INSURANCE);
        item.setSimpleOperationStatus(ImOperationStatus.OK);
        item.setReservationNumber("999555111");
        item.setMskConfirmedAt(LocalDateTime.of(2019, 10, 19, 17, 0));
        item.setOrderItemId(operationId);
        item.setAmount(BigDecimal.valueOf(150));
    }

    public static TrainTariffInfoDataProvider createTrainTariffInfoDataProvider() {
        var tariffInfos = Arrays.asList(
                TTrainTariffInfo.newBuilder()
                        .setId(1)
                        .setCode("full")
                        .setTitleRu("Полный")
                        .setImRequestCode("Full")
                        .setImResponseCodes("Full")
                        .setWithoutPlace(false)
                        .setMinAge(0)
                        .setMinAgeIncludesBirthday(false)
                        .setMaxAge(150)
                        .setMaxAgeIncludesBirthday(false)
                        .setNeedDocument(false).build(),

                TTrainTariffInfo.newBuilder()
                        .setId(2)
                        .setCode("senior")
                        .setTitleRu("Сеньор")
                        .setImRequestCode("Senior")
                        .setImResponseCodes("Senior")
                        .setWithoutPlace(false)
                        .setMinAge(0)
                        .setMinAgeIncludesBirthday(false)
                        .setMaxAge(150)
                        .setMaxAgeIncludesBirthday(false)
                        .setNeedDocument(false).build());

        TrainTariffInfoDataProviderProperties config = new TrainTariffInfoDataProviderProperties();
        config.setTablePath("tablePath");
        config.setIndexPath("./train-tariff-index");
        config.setProxy(new ArrayList<>());

        TestLuceneIndexBuilder<TTrainTariffInfo> luceneIndexBuilder = new TestLuceneIndexBuilder<TTrainTariffInfo>()
                .setLuceneData(tariffInfos);

        return new TrainTariffInfoService(config, luceneIndexBuilder);
    }
}

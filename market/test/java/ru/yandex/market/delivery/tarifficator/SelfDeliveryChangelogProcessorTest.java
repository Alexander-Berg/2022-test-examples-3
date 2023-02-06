package ru.yandex.market.delivery.tarifficator;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.delivery.logbroker.SelfDeliveryChangelogProcessor;
import ru.yandex.market.logistics.tarifficator.shop.SelfDeliveryTariffChangelogProto;
import ru.yandex.market.logistics.tarifficator.shop.SelfDeliveryTariffChangelogProto.CourierTariffType;
import ru.yandex.market.logistics.tarifficator.shop.SelfDeliveryTariffChangelogProto.DeliveryServiceStrategy;
import ru.yandex.market.logistics.tarifficator.shop.SelfDeliveryTariffChangelogProto.RegionGroupPaymentType;
import ru.yandex.market.logistics.tarifficator.shop.SelfDeliveryTariffChangelogProto.RegionGroupStatus;
import ru.yandex.market.logistics.tarifficator.shop.SelfDeliveryTariffChangelogProto.SelfDeliveryCourierRegionGroup;
import ru.yandex.market.logistics.tarifficator.shop.SelfDeliveryTariffChangelogProto.SelfDeliveryCourierTariff;
import ru.yandex.market.logistics.tarifficator.shop.SelfDeliveryTariffChangelogProto.SelfDeliveryTariffChangelog;
import ru.yandex.market.logistics.tarifficator.shop.SelfDeliveryTariffChangelogProto.SelfDeliveryTariffChangelogEventType;
import ru.yandex.market.shop.FunctionalTest;

import static org.mockito.Mockito.mock;

/**
 * Тесты для {@link SelfDeliveryChangelogProcessor}.
 */
@ExtendWith(MockitoExtension.class)
public class SelfDeliveryChangelogProcessorTest extends FunctionalTest {

    @Mock
    private MessageBatch messageBatchMock;

    @Autowired
    SelfDeliveryChangelogProcessor selfDeliveryChangelogProcessor;

    @Test
    @DisplayName("Проверка, что сообщения успешно обрабатываются, и сохраняются только ивенты с большим currentEventMillis")
    @DbUnitDataSet(
            before = "SelfDeliveryChangelogProcessorTest.before.csv",
            after = "SelfDeliveryChangelogProcessorTest.testSuccessfulChangelogProcessing.after.csv"
    )
    public void testSuccessfulChangelogProcessing() {
        long firstShop = 100L;
        long secondShop = 200L;
        long thirdShop = 300L;

        MessageData firstMessageMock = mock(MessageData.class);
        byte[] firstMessage = createSelfDeliveryTariffChangelogMessage(
                firstShop, true, true, 777777L,
                createRegionGroup(
                        1L, true, RegionGroupStatus.SUCCESS, CourierTariffType.CATEGORY,
                        List.of(RegionGroupPaymentType.PREPAYMENT_CARD), false,
                        List.of(234L, 123L, 345L)),
                createRegionGroup(
                        2L, false, RegionGroupStatus.NEW, CourierTariffType.DEFAULT,
                        List.of(RegionGroupPaymentType.PREPAYMENT_CARD), true, List.of(1111L, 2222L))
        );
        Mockito.when(firstMessageMock.getDecompressedData()).thenReturn(firstMessage);

        MessageData secondMessageMock = mock(MessageData.class);
        byte[] secondMessage = createSelfDeliveryTariffChangelogMessage(
                secondShop, false, true, 555555L,
                createRegionGroup(3L, true, RegionGroupStatus.SUCCESS, CourierTariffType.CATEGORY_PRICE,
                        List.of(RegionGroupPaymentType.COURIER_CASH), false,
                        List.of(555L)));
        Mockito.when(secondMessageMock.getDecompressedData()).thenReturn(secondMessage);

        MessageData thirdMessageMock = mock(MessageData.class);
        byte[] thirdMessage = createSelfDeliveryTariffChangelogMessage(
                thirdShop, false, false, 111111L,
                createRegionGroup(4L, true, RegionGroupStatus.SUCCESS, CourierTariffType.CATEGORY_PRICE,
                        List.of(RegionGroupPaymentType.COURIER_CASH), false,
                        List.of(111L)));
        Mockito.when(thirdMessageMock.getDecompressedData()).thenReturn(thirdMessage);

        Mockito.when(messageBatchMock.getMessageData()).thenReturn(List.of(
                firstMessageMock, secondMessageMock, thirdMessageMock));

        selfDeliveryChangelogProcessor.process(messageBatchMock);
    }

    @Test
    @DisplayName("Проверка, что для одного магазина ченджлог с максимальным currentEventMillis")
    @DbUnitDataSet(
            before = "SelfDeliveryChangelogProcessorTest.before.csv",
            after = "SelfDeliveryChangelogProcessorTest.testOneShopChangelogCollision.after.csv"
    )
    public void testOneShopChangelogCollision() {
        long shopId = 100L;

        MessageData firstMessageMock = mock(MessageData.class);
        byte[] firstMessage = createSelfDeliveryTariffChangelogMessage(
                shopId, true, true, 777777L,
                createRegionGroup(
                        1L, true, RegionGroupStatus.SUCCESS, CourierTariffType.CATEGORY,
                        List.of(RegionGroupPaymentType.PREPAYMENT_CARD), false,
                        List.of(234L, 123L, 345L)),
                createRegionGroup(
                        2L, false, RegionGroupStatus.NEW, CourierTariffType.DEFAULT,
                        List.of(RegionGroupPaymentType.PREPAYMENT_CARD), true, List.of(1111L, 2222L))
        );
        Mockito.when(firstMessageMock.getDecompressedData()).thenReturn(firstMessage);

        MessageData secondMessageMock = mock(MessageData.class);
        byte[] secondMessage = createSelfDeliveryTariffChangelogMessage(
                shopId, false, false, 999999L,
                createRegionGroup(3L, true, RegionGroupStatus.SUCCESS, CourierTariffType.CATEGORY_PRICE,
                        List.of(RegionGroupPaymentType.COURIER_CASH), false,
                        List.of(555L, 111L)),
                createRegionGroup(4L, false, RegionGroupStatus.SUCCESS, CourierTariffType.CATEGORY_PRICE,
                        List.of(RegionGroupPaymentType.COURIER_CASH), false,
                        List.of(222L))
        );
        Mockito.when(secondMessageMock.getDecompressedData()).thenReturn(secondMessage);

        MessageData thirdMessageMock = mock(MessageData.class);
        byte[] thirdMessage = createSelfDeliveryTariffChangelogMessage(
                shopId, false, true, 111111L,
                createRegionGroup(3L, true, RegionGroupStatus.SUCCESS, CourierTariffType.CATEGORY_PRICE,
                        List.of(RegionGroupPaymentType.COURIER_CASH), false,
                        List.of(111L))
        );
        Mockito.when(thirdMessageMock.getDecompressedData()).thenReturn(thirdMessage);

        Mockito.when(messageBatchMock.getMessageData()).thenReturn(List.of(
                firstMessageMock, secondMessageMock, thirdMessageMock));

        selfDeliveryChangelogProcessor.process(messageBatchMock);
    }

    @Test
    @DisplayName("Проверка, что ничего не обновится, т.к. currentEventMillis ивентов меньше тех, которые в БД")
    @DbUnitDataSet(
            before = "SelfDeliveryChangelogProcessorTest.before.csv",
            after = "SelfDeliveryChangelogProcessorTest.testNothingToUpdate.after.csv"
    )
    public void testNothingToUpdate() {
        long shopId = 100L;

        MessageData firstMessageMock = mock(MessageData.class);
        byte[] firstMessage = createSelfDeliveryTariffChangelogMessage(
                shopId, true, true, 2L,
                createRegionGroup(
                        1L, true, RegionGroupStatus.SUCCESS, CourierTariffType.CATEGORY,
                        List.of(RegionGroupPaymentType.PREPAYMENT_CARD), false,
                        List.of(234L, 123L, 345L)),
                createRegionGroup(
                        2L, false, RegionGroupStatus.NEW, CourierTariffType.DEFAULT,
                        List.of(RegionGroupPaymentType.PREPAYMENT_CARD), true, List.of(1111L, 2222L))
        );
        Mockito.when(firstMessageMock.getDecompressedData()).thenReturn(firstMessage);

        MessageData secondMessageMock = mock(MessageData.class);
        byte[] secondMessage = createSelfDeliveryTariffChangelogMessage(
                shopId, false, false, 1L,
                createRegionGroup(3L, true, RegionGroupStatus.SUCCESS, CourierTariffType.CATEGORY_PRICE,
                        List.of(RegionGroupPaymentType.COURIER_CASH), false,
                        List.of(555L, 111L)),
                createRegionGroup(4L, false, RegionGroupStatus.SUCCESS, CourierTariffType.CATEGORY_PRICE,
                        List.of(RegionGroupPaymentType.COURIER_CASH), false,
                        List.of(222L))
        );
        Mockito.when(secondMessageMock.getDecompressedData()).thenReturn(secondMessage);

        Mockito.when(messageBatchMock.getMessageData()).thenReturn(List.of(
                firstMessageMock, secondMessageMock));

        selfDeliveryChangelogProcessor.process(messageBatchMock);
    }

    @Test
    @DisplayName("Проверка, что процессор не упадет при ошибке парсинга")
    @DbUnitDataSet(
            before = "SelfDeliveryChangelogProcessorTest.before.csv",
            after = "SelfDeliveryChangelogProcessorTest.testParsingError.after.csv"
    )
    public void testParsingError() {
        long firstShop = 100L;

        MessageData firstMessageMock = mock(MessageData.class);
        byte[] firstMessage = createSelfDeliveryTariffChangelogMessage(
                firstShop, true, true, 777777L,
                createRegionGroup(
                        1L, true, RegionGroupStatus.SUCCESS, CourierTariffType.CATEGORY,
                        List.of(RegionGroupPaymentType.PREPAYMENT_CARD), false,
                        List.of(234L, 123L, 345L))
                );
        Mockito.when(firstMessageMock.getDecompressedData()).thenReturn(firstMessage);

        MessageData secondMessageMock = mock(MessageData.class);
        byte[] secondMessage = "This is message that can't be parsed".getBytes(StandardCharsets.UTF_8);
        Mockito.when(secondMessageMock.getDecompressedData()).thenReturn(secondMessage);

        Mockito.when(messageBatchMock.getMessageData()).thenReturn(List.of(
                firstMessageMock, secondMessageMock));

        selfDeliveryChangelogProcessor.process(messageBatchMock);
    }

    @Test
    @DisplayName("Проверка, что процессор не упадет, если ничего не распарсилось")
    @DbUnitDataSet(
            before = "SelfDeliveryChangelogProcessorTest.before.csv",
            after = "SelfDeliveryChangelogProcessorTest.testParsingErrorEmptyBatch.after.csv"
    )
    public void testParsingErrorEmptyBatch() {
        MessageData firstMessageMock = mock(MessageData.class);
        byte[] firstMessage = "Bla-bla".getBytes(StandardCharsets.UTF_8);
        Mockito.when(firstMessageMock.getDecompressedData()).thenReturn(firstMessage);

        MessageData secondMessageMock = mock(MessageData.class);
        byte[] secondMessage = "Bla-bla-bla".getBytes(StandardCharsets.UTF_8);
        Mockito.when(secondMessageMock.getDecompressedData()).thenReturn(secondMessage);

        Mockito.when(messageBatchMock.getMessageData()).thenReturn(List.of(
                firstMessageMock, secondMessageMock));

        selfDeliveryChangelogProcessor.process(messageBatchMock);
    }

    @Test
    @DisplayName("Проверка пустых регионов для уже существующего магазина")
    @DbUnitDataSet(
            before = "SelfDeliveryChangelogProcessorTest.before.csv",
            after = "SelfDeliveryChangelogProcessorTest.testEmptyRegions.after.csv"
    )
    public void testEmptyRegions() {
        long firstShop = 100L;

        MessageData firstMessageMock = mock(MessageData.class);
        byte[] firstMessage = createSelfDeliveryTariffChangelogMessage(
                firstShop, true, true, 999999L,
                createRegionGroup(
                        1L, true, RegionGroupStatus.FAIL, CourierTariffType.CATEGORY,
                        List.of(RegionGroupPaymentType.COURIER_CASH), false,
                        List.of(234L, 123L, 345L)),
                createRegionGroup(
                        2L, false, RegionGroupStatus.NEW, CourierTariffType.DEFAULT,
                        List.of(RegionGroupPaymentType.PREPAYMENT_OTHER), false, List.of(1111L, 2222L))
        );

        Mockito.when(firstMessageMock.getDecompressedData()).thenReturn(firstMessage);

        Mockito.when(messageBatchMock.getMessageData()).thenReturn(List.of(firstMessageMock));

        selfDeliveryChangelogProcessor.process(messageBatchMock);
    }

    @Test
    @DisplayName("Проверка, что остаются только активные регионы")
    @DbUnitDataSet(
            before = "SelfDeliveryChangelogProcessorTest.before.csv",
            after = "SelfDeliveryChangelogProcessorTest.testFilterRegions.after.csv"
    )
    public void testFilterRegions() {
        long firstShop = 200L;

        MessageData firstMessageMock = mock(MessageData.class);
        byte[] firstMessage = createSelfDeliveryTariffChangelogMessage(
                firstShop, true, true, 999999L,
                createRegionGroup(
                        1L, true, RegionGroupStatus.SUCCESS, CourierTariffType.CATEGORY,
                        List.of(RegionGroupPaymentType.COURIER_CASH), false,
                        List.of(234L, 123L, 345L)),
                createRegionGroup(
                        2L, false, RegionGroupStatus.NEW, CourierTariffType.DEFAULT,
                        List.of(RegionGroupPaymentType.PREPAYMENT_OTHER), false, List.of(1111L, 2222L)),
                createRegionGroup(
                        3L, false, RegionGroupStatus.FAIL, CourierTariffType.DEFAULT,
                        List.of(RegionGroupPaymentType.PREPAYMENT_OTHER), false, List.of(3333L))
        );

        Mockito.when(firstMessageMock.getDecompressedData()).thenReturn(firstMessage);

        Mockito.when(messageBatchMock.getMessageData()).thenReturn(List.of(firstMessageMock));

        selfDeliveryChangelogProcessor.process(messageBatchMock);
    }


    @Test
    @DisplayName("Проверка, что процессор не упадет, если всех партнеров нет в partner")
    @DbUnitDataSet(
            before = "SelfDeliveryChangelogProcessorTest.before.csv",
            after = "SelfDeliveryChangelogProcessorTest.testNotExistingPartner.after.csv"
    )
    public void testNotExistingPartner() {
        long firstShop = 500;

        MessageData firstMessageMock = mock(MessageData.class);
        byte[] firstMessage = createSelfDeliveryTariffChangelogMessage(
                firstShop, true, true, 999999L,
                createRegionGroup(
                        1L, true, RegionGroupStatus.SUCCESS, CourierTariffType.CATEGORY,
                        List.of(RegionGroupPaymentType.COURIER_CASH), false,
                        List.of(234L, 123L, 345L))
        );

        Mockito.when(firstMessageMock.getDecompressedData()).thenReturn(firstMessage);

        Mockito.when(messageBatchMock.getMessageData()).thenReturn(List.of(firstMessageMock));

        selfDeliveryChangelogProcessor.process(messageBatchMock);
    }

    @Test
    @DisplayName("Проверка, что несуществующий партнер не бует обрабатываться")
    @DbUnitDataSet(
            before = "SelfDeliveryChangelogProcessorTest.before.csv",
            after = "SelfDeliveryChangelogProcessorTest.testSkipNotExistingPartner.after.csv"
    )
    public void testSkipNotExistingPartner() {
        long firstShop = 100L;
        long secondShop = 500;

        MessageData firstMessageMock = mock(MessageData.class);
        byte[] firstMessage = createSelfDeliveryTariffChangelogMessage(
                firstShop, true, true, 777777L,
                createRegionGroup(
                        1L, true, RegionGroupStatus.SUCCESS, CourierTariffType.CATEGORY,
                        List.of(RegionGroupPaymentType.PREPAYMENT_CARD), false,
                        List.of(234L, 123L, 345L)),
                createRegionGroup(
                        2L, false, RegionGroupStatus.NEW, CourierTariffType.DEFAULT,
                        List.of(RegionGroupPaymentType.PREPAYMENT_CARD), true, List.of(1111L, 2222L))
        );
        Mockito.when(firstMessageMock.getDecompressedData()).thenReturn(firstMessage);

        MessageData secondMessageMock = mock(MessageData.class);
        byte[] secondMessage = createSelfDeliveryTariffChangelogMessage(
                secondShop, false, true, 555555L,
                createRegionGroup(3L, true, RegionGroupStatus.SUCCESS, CourierTariffType.CATEGORY_PRICE,
                        List.of(RegionGroupPaymentType.COURIER_CASH), false,
                        List.of(555L)));
        Mockito.when(secondMessageMock.getDecompressedData()).thenReturn(secondMessage);

        Mockito.when(messageBatchMock.getMessageData()).thenReturn(List.of(
                firstMessageMock, secondMessageMock));

        selfDeliveryChangelogProcessor.process(messageBatchMock);
    }

    @Test
    @DisplayName("Проверка, что пустой список обрааботается нормально")
    @DbUnitDataSet(
            before = "SelfDeliveryChangelogProcessorTest.before.csv",
            after = "SelfDeliveryChangelogProcessorTest.testParsingErrorEmptyBatch.after.csv"
    )
    public void testEmptyMessageBatch() {

        Mockito.when(messageBatchMock.getMessageData()).thenReturn(Collections.emptyList());

        selfDeliveryChangelogProcessor.process(messageBatchMock);
    }

    private byte[] createSelfDeliveryTariffChangelogMessage(
            long shopId,
            boolean hasPickupDelivery,
            boolean hasCourierDelivery,
            long eventMillis,
            SelfDeliveryCourierRegionGroup...regionGroups
    ) {
        SelfDeliveryCourierTariff tariff = SelfDeliveryCourierTariff.newBuilder()
                .addAllRegionGroups(Arrays.asList(regionGroups))
                .build();

        SelfDeliveryTariffChangelog selfDeliveryTariffChangelog = SelfDeliveryTariffChangelog.newBuilder()
                .setShopId(shopId)
                .setHasPickupDelivery(hasPickupDelivery)
                .setHasCourierDelivery(hasCourierDelivery)
                .setEventMillis(eventMillis)
                .setActualCourierTariff(tariff)
                .setEventId(1L)
                .setEventType(SelfDeliveryTariffChangelogEventType.REGION_GROUP_UPDATE)
                .setNeedModeration(false)
                .build();

        return selfDeliveryTariffChangelog.toByteArray();
    }

    private SelfDeliveryCourierRegionGroup createRegionGroup(
            Long id,
            boolean isSelfRegion,
            RegionGroupStatus status,
            CourierTariffType tariffType,
            List<RegionGroupPaymentType> paymentTypes,
            boolean shouldHaveServices,
            List<Long> regionIds
    ) {
        SelfDeliveryCourierRegionGroup.Builder builder = SelfDeliveryCourierRegionGroup
                .newBuilder()
                .setId(id)
                .setIsSelfRegion(isSelfRegion)
                .setStatus(status)
                .setCurrency("RUR")
                .setTariffType(tariffType)
                .addAllRegionIds(regionIds)
                .addAllPaymentTypes(paymentTypes);

        if (shouldHaveServices) {
            builder.addDeliveryServices(
                    SelfDeliveryTariffChangelogProto.DeliveryService.newBuilder()
                            .setCourierDeliveryStrategy(DeliveryServiceStrategy.AUTO_CALCULATED)
                            .setPickupDeliveryStrategy(DeliveryServiceStrategy.NO_DELIVERY)
                            .setDeliveryServiceId(100L)
                            .build()
                    )
                    .build();
        }

        return builder.build();
    }
}

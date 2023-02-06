package ru.yandex.market.wms.servicebus.api.external.trassir.client;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.test.context.SpringIntegrationTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;

import ru.yandex.market.wms.common.model.enums.InventoryHoldStatus;
import ru.yandex.market.wms.common.spring.dto.subs.EventTagDto;
import ru.yandex.market.wms.common.spring.dto.subs.SubtitlesDto;
import ru.yandex.market.wms.common.spring.dto.subs.packing.ScannedPackagedItemTagDto;
import ru.yandex.market.wms.common.spring.dto.subs.receiving.ReceiptAnomalyTagDto;
import ru.yandex.market.wms.common.spring.dto.subs.receiving.ReceiptFitTagDto;
import ru.yandex.market.wms.common.spring.dto.subs.receiving.ReceiptReturnTagDto;
import ru.yandex.market.wms.common.spring.utils.FileContentUtils;
import ru.yandex.market.wms.servicebus.IntegrationTest;
import ru.yandex.market.wms.servicebus.configuration.TcpSocketConfiguration;

@SpringIntegrationTest(noAutoStartup = {"tcpClientForTrassir"})
public class TrassirClientTest extends IntegrationTest {

    @Autowired
    private TrassirClient trassirClient;

    @Autowired
    @Qualifier(TcpSocketConfiguration.MESSAGE_CHANNEL_NAME)
    private SubscribableChannel messageChannel;

    @Mock
    private MessageHandler messageHandlerMock;

    @MockBean
    @Autowired
    // заменяем реальный бин на мок => в тесте мы не будем пытаться делать tcp запрос,
    // а остановимся только на отправке субтитров в messageChannel
    private IntegrationFlow tcpClientForTrassir;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        messageChannel.subscribe(messageHandlerMock);
    }

    @AfterEach
    public void tearDown() {
        messageChannel.unsubscribe(messageHandlerMock);
        Mockito.reset(messageHandlerMock);
    }

    /**
     * Набор параметров для проверки корректности формата отправляемых в trassir объектов {@link EventTagDto}.
     * Для добавления нового формата в проверку, необходимо в стрим добавить {@link Arguments} содержащий {@link EventTagDto}
     * и файл с ожидаемым результатом сериализации в формате .xml
     *
     * @return стрим входных параметров для теста trassirSubtitlesPushing
     */
    private static Stream<Arguments> eventItemTagArguments() {
        return Stream.of(
                Arguments.of(buildFakeReceiptFitTagDto(),
                        "api/external/trassir/client/trassir-request-receipt-fit.xml"),
                Arguments.of(buildFakeReceiptAnomalyTagDto(),
                        "api/external/trassir/client/trassir-request-receipt-anomaly.xml"),
                Arguments.of(buildFakeReceiptReturnTagDto(),
                        "api/external/trassir/client/trassir-request-receipt-return.xml"),
                Arguments.of(buildFakeScannedPackagedItemTagDto(),
                        "api/external/trassir/client/trassir-packing-scanned-item.xml"));
    }

    @ParameterizedTest
    @MethodSource("eventItemTagArguments")
    public void trassirSubtitlesDtoHaveExpectedFormat(EventTagDto tagDto, String expectedResultFileName) {
        SubtitlesDto subtitlesDto = SubtitlesDto.builder()
                .timestamp(LocalDateTime.of(2021, Month.MARCH, 8, 13, 0, 0))
                .tags(Collections.singletonList(tagDto))
                .build();

        ArgumentCaptor<Message<String>> messageArgumentCaptor = ArgumentCaptor.forClass(Message.class);
        trassirClient.pushSubtitlesToTrassir(subtitlesDto);

        Mockito.verify(messageHandlerMock).handleMessage(messageArgumentCaptor.capture());
        String expectedTrassirRequest =
                FileContentUtils.getFileContent(expectedResultFileName);
        Assertions.assertEquals(expectedTrassirRequest, messageArgumentCaptor.getValue().getPayload());
    }

    private static ReceiptFitTagDto buildFakeReceiptFitTagDto() {
        return ReceiptFitTagDto.builder()
                .receiptKey("00001")
                .lot("001")
                .sku("ROV123")
                .uit("470010000000")
                .storer("storer")
                .user("user")
                .location("STAGE01")
                .manufacturerSku("ROV123")
                .receiptType("1")
                .build();
    }

    private static ReceiptAnomalyTagDto buildFakeReceiptAnomalyTagDto() {
        return ReceiptAnomalyTagDto.builder()
                .receiptKey("00001")
                .anomalyType("DAMAGED")
                .sku("ROV123")
                .container("ANO123")
                .storer("storer")
                .user("user")
                .location("STAGE01")
                .manufacturerSku("ROV123")
                .altSku("barcode123")
                .quantity(15)
                .build();
    }

    private static ReceiptReturnTagDto buildFakeReceiptReturnTagDto() {
        return ReceiptReturnTagDto.builder()
                .receiptKey("00001")
                .lot("001")
                .sku("ROV123")
                .uit("470010000000")
                .storer("storer")
                .user("user")
                .location("STAGE01")
                .manufacturerSku("ROV123")
                .inventoryHoldStatuses(new LinkedHashSet<>(
                        List.of(InventoryHoldStatus.RETURN, InventoryHoldStatus.DAMAGE)))
                .orderId("42")
                .returnId("12")
                .receiptType("15")
                .build();
    }

    private static ScannedPackagedItemTagDto buildFakeScannedPackagedItemTagDto() {
        return ScannedPackagedItemTagDto.builder()
                .ticketId("00001")
                .order("0001000001")
                .sku("ROV123")
                .uit("470010000000")
                .storer("storer")
                .user("user")
                .location("STAGE01")
                .build();
    }
}

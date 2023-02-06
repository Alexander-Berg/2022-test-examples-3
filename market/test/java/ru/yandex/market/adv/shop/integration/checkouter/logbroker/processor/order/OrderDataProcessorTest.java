package ru.yandex.market.adv.shop.integration.checkouter.logbroker.processor.order;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.kikimr.persqueue.compression.CompressionCodec;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageBatch;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageData;
import ru.yandex.kikimr.persqueue.consumer.transport.message.inbound.data.MessageMeta;
import ru.yandex.market.adv.loader.file.FileLoader;
import ru.yandex.market.adv.shop.integration.AbstractShopIntegrationTest;
import ru.yandex.market.adv.shop.integration.checkouter.logbroker.properties.LogbrokerCheckouterOrderTopicProperties;
import ru.yandex.market.adv.shop.integration.checkouter.yt.entity.order.MarketOrderItemEntity;
import ru.yandex.market.adv.yt.test.annotation.YtUnitDataSet;
import ru.yandex.market.adv.yt.test.annotation.YtUnitScheme;

/**
 * Date: 26.05.2022
 * Project: adv-shop-integration
 *
 * @author alexminakov
 */
@ParametersAreNonnullByDefault
class OrderDataProcessorTest extends AbstractShopIntegrationTest {

    @Autowired
    private OrderDataProcessor orderDataProcessor;
    @Autowired
    private FileLoader fileLoader;
    @Autowired
    private LogbrokerCheckouterOrderTopicProperties properties;

    @DisplayName("Обработка очереди заказов завершилась сохранением информации в БД.")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemEntity.class,
                    path = "//tmp/process_allCorrect_saveInYt_market_order_item"
            ),
            before = "OrderDataProcessor/json/yt/process_allCorrect_saveInYt.before.json",
            after = "OrderDataProcessor/json/yt/process_allCorrect_saveInYt.after.json"
    )
    @Test
    void process_allCorrect_saveInYt() {
        run("process_allCorrect_saveInYt_",
                () -> orderDataProcessor.process(
                        List.of(
                                creatMessageBatch(
                                        List.of(
                                                createMessageData("1", 5),
                                                createMessageData("2", 6),
                                                createMessageData("3", 7),
                                                createMessageData("4", 8),
                                                createMessageData("5", 9)
                                        )
                                ),
                                creatMessageBatch(
                                        List.of(
                                                createMessageData("6", 10),
                                                createMessageData("7", 11),
                                                createMessageData("8", 12),
                                                createMessageData("9", 13),
                                                createMessageData("10", 14)
                                        )
                                )
                        )
                )
        );
    }

    @DisplayName("Обработка очереди заказов была пропущена.")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemEntity.class,
                    path = "//tmp/process_skipData_nothing_market_order_item"
            ),
            before = "OrderDataProcessor/json/yt/process_skipData_nothing.before.json"
    )
    @Test
    void process_skipData_nothing() {
        try {
            properties.setSkip(true);

            run("process_skipData_nothing_",
                    () -> orderDataProcessor.process(
                            List.of(
                                    creatMessageBatch(
                                            List.of(
                                                    createMessageData("1", 5),
                                                    createMessageData("2", 6)
                                            )
                                    )
                            )
                    )
            );
        } finally {
            properties.setSkip(false);
        }
    }

    @DisplayName("Обработка очереди заказов провалилась.")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemEntity.class,
                    path = "//tmp/process_wrongData_exception_market_order_item"
            ),
            before = "OrderDataProcessor/json/yt/process_wrongData_exception.before.json"
    )
    @Test
    void process_wrongData_exception() {
        run("process_wrongData_exception_",
                () -> Assertions.assertThatThrownBy(
                                () -> orderDataProcessor.process(
                                        List.of(
                                                creatMessageBatch(
                                                        List.of(
                                                                createMessageData("incorrect", 5)
                                                        )
                                                )
                                        )
                                )
                        )
                        .isInstanceOf(UncheckedIOException.class)
        );
    }

    @DisplayName("Обработка очереди заказов не содержала данных для сохранения.")
    @YtUnitDataSet(
            scheme = @YtUnitScheme(
                    model = MarketOrderItemEntity.class,
                    path = "//tmp/process_emptyData_nothing_market_order_item"
            ),
            before = "OrderDataProcessor/json/yt/process_emptyData_nothing.before.json"
    )
    @Test
    void process_emptyData_nothing() {
        run("process_emptyData_nothing_",
                () -> orderDataProcessor.process(
                        List.of(
                                creatMessageBatch(
                                        List.of(
                                                createMessageData("1", 5),
                                                createMessageData("2", 6),
                                                createMessageData("3", 7),
                                                createMessageData("4", 8)
                                        )
                                ),
                                creatMessageBatch(
                                        List.of(
                                                createMessageData("5", 9)
                                        )
                                )
                        )
                )
        );
    }

    @Nonnull
    private MessageBatch creatMessageBatch(List<MessageData> messageDataList) {
        return new MessageBatch(
                "checkouter-order-event-log",
                1,
                messageDataList
        );
    }

    @Nonnull
    private MessageData createMessageData(String order, long offset) {
        return new MessageData(
                fileLoader.loadFileBinary(
                        "OrderDataProcessor/json/logbroker/" + order + ".json",
                        this.getClass()
                ),
                offset,
                new MessageMeta(
                        new byte[10],
                        1,
                        1,
                        1,
                        "1.1.1.0",
                        CompressionCodec.RAW,
                        Map.of()
                )
        );
    }
}

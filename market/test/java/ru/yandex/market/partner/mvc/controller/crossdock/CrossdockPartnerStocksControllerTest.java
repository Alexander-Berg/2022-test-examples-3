package ru.yandex.market.partner.mvc.controller.crossdock;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.datacamp.feed.FeedProcessorUpdateRequestEvent;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.feed.processor.parsing.FeedUpdateTaskOuterClass;
import ru.yandex.market.partner.test.context.FunctionalTest;
import ru.yandex.market.partner.util.FunctionalTestHelper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Тесты для {@link CrossdockPartnerStocksController}
 */
@DbUnitDataSet(before = "CrossdockPartnerStocksControllerTest.before.csv")
class CrossdockPartnerStocksControllerTest extends FunctionalTest {

    @Autowired
    private LogbrokerEventPublisher<FeedProcessorUpdateRequestEvent> feedProcessorUpdateLogbrokerEventPublisher;

    @DisplayName("Обновление стоков кроссдок-партнёра со стоками по пуш-схеме при выключенном расширении фида на все " +
            "склады")
    @Test
    void updateStocksPushSchemeNoFeedExpansion() {
        FunctionalTestHelper.put(baseUrl + "/crossdock/1005628/stocks?upload_id=1", null);

        var messageArgumentCaptor =
                ArgumentCaptor.forClass(FeedProcessorUpdateRequestEvent.class);
        //расширения на склады нет - ожидаем одно взаимодействие с моком
        Mockito.verify(feedProcessorUpdateLogbrokerEventPublisher).publishEvent(messageArgumentCaptor.capture());
        var event = messageArgumentCaptor.getValue();
        assertThat(event.getPayload())
                .extracting(e -> e.getFeed().getShopId(), e -> e.getFeed().getFeedId(),
                        FeedUpdateTaskOuterClass.FeedUpdateTask::getFeedParsingType, e -> e.getWarehouses(0).getWarehouseId())
                .containsExactly(5476L, 7953L, FeedUpdateTaskOuterClass.FeedUpdateTask.FeedParsingType.COMPLETE, 111L);
    }

    @DisplayName("Обновление стоков только для кроссдок-партнеров")
    @Test
    void notCrossdock() {
        HttpClientErrorException.BadRequest notCrossdock =
                Assertions.assertThrows(HttpClientErrorException.BadRequest.class, () ->
                        FunctionalTestHelper.put(baseUrl + "/crossdock/1005629/stocks", null));
        Assertions.assertEquals(notCrossdock.getStatusCode(), HttpStatus.BAD_REQUEST);
    }

    @DisplayName("Обновление стоков только для кроссдок-партнеров с параметром CPA_IS_PARTNER_INTERFACE")
    @Test
    void lackOfParam() {
        HttpClientErrorException.BadRequest lackOfParam =
                Assertions.assertThrows(HttpClientErrorException.BadRequest.class, () ->
                        FunctionalTestHelper.put(baseUrl + "/crossdock/1005630/stocks", null));
        Assertions.assertEquals(lackOfParam.getStatusCode(), HttpStatus.BAD_REQUEST);
    }

    @DisplayName("У партнеров с push схемой должен быть передан upload_id")
    @Test
    void pushWithoutUploadId() {
        HttpClientErrorException.BadRequest pushWithoutUploadId =
                Assertions.assertThrows(HttpClientErrorException.BadRequest.class, () ->
                        FunctionalTestHelper.put(baseUrl + "/crossdock/1005631/stocks", null));
        Assertions.assertEquals(pushWithoutUploadId.getStatusCode(), HttpStatus.BAD_REQUEST);
    }

    @DisplayName("Для обновления стоков у партнера должен быть активный фид")
    @Test
    void noFeedPresentForSupplier() {
        assertThatExceptionOfType(HttpClientErrorException.BadRequest.class)
                .isThrownBy(() -> FunctionalTestHelper.put(baseUrl + "/crossdock/1005633/stocks?upload_id=1", null))
                .matches(error -> error.getResponseBodyAsString().contains("No active feed was found for supplier " +
                        "with id: 5481"));
    }
}

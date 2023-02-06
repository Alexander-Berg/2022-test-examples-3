package ru.yandex.market.mbi.api.controller.supplier;

import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.google.protobuf.InvalidProtocolBufferException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.spring.FunctionalTestHelper;
import ru.yandex.market.core.datacamp.feed.FeedProcessorUpdateRequestEvent;
import ru.yandex.market.logbroker.LogbrokerEventPublisher;
import ru.yandex.market.mbi.api.client.entity.supplier.SupplierFeedDTO;
import ru.yandex.market.mbi.api.config.FunctionalTest;
import ru.yandex.market.mbi.feed.processor.parsing.FeedUpdateTaskOuterClass;
import ru.yandex.market.mbi.util.MbiAsserts;
import ru.yandex.market.yt.samovar.SamovarContextOuterClass;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Функциональные тесты для {@link SupplierFeedController}.
 *
 * @author fbokovikov
 */
@DbUnitDataSet(before = "SupplierFeedControllerFunctionalTest.csv")
class SupplierFeedControllerFunctionalTest extends FunctionalTest {

    @Autowired
    private LogbrokerEventPublisher<FeedProcessorUpdateRequestEvent> feedProcessorUpdateLogbrokerEventPublisher;

    @Test
    @DisplayName("Фид поставщика не найден")
    void feedNotFound() {
        Assertions.assertThatThrownBy(() -> mbiApiClient.getSupplierFeedInfo(10L))
                .isInstanceOf(HttpClientErrorException.class);
    }

    @Test
    @DisplayName("Фид поставщика найден")
    void feedFound() {
        long supplierId = 100L;
        SupplierFeedDTO supplierFeedInfo = mbiApiClient.getSupplierFeedInfo(supplierId);
        Assertions.assertThat(supplierFeedInfo)
                .isEqualTo(new SupplierFeedDTO(supplierId, 1L,
                        LocalDate.of(2017, Month.JANUARY, 1)
                                .atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime()));
    }

    @Test
    @DisplayName("Фиксируем xml-ответ для ручки получения фида поставщика")
    void xmlView() {
        String supplierFeedUrl = "http://localhost:" + port + "/suppliers/{supplierId}/feeds";
        ResponseEntity<String> response = FunctionalTestHelper.get(supplierFeedUrl, 100L);
        OffsetDateTime d = LocalDate.of(2017, Month.JANUARY, 1)
                .atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        String s = d.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        MbiAsserts.assertXmlEquals( //language=xml
                "<supplier-feed><supplier-id>100</supplier-id><feed-id>1</feed-id> " +
                        "<updated-at>" + s + "</updated-at></supplier-feed>",
                response.getBody()
        );
    }

    @SuppressWarnings("unused")
    @ParameterizedTest(name = "{0}")
    @DisplayName("Проверка обновления фида")
    @CsvSource({
            "ASSORTMENT,105,http://shop.ru/101",
            "ASSORTMENT,6,http://test.feed.url/"
    })
    void refreshFeed(SamovarContextOuterClass.FeedInfo.FeedType feedType,
                     long feedId, String url) throws InvalidProtocolBufferException {
        doNothing().
                when(feedProcessorUpdateLogbrokerEventPublisher)
                .publishEvent(any());

        mbiApiClient.refreshFeed(feedId);

        var samovarEventCaptor = ArgumentCaptor.forClass(FeedProcessorUpdateRequestEvent.class);
        verify(feedProcessorUpdateLogbrokerEventPublisher, times(1))
                .publishEvent(samovarEventCaptor.capture());
        List<FeedProcessorUpdateRequestEvent> logbrokerEvents = samovarEventCaptor.getAllValues();
        Assertions.assertThat(logbrokerEvents)
                .hasSize(1);

        FeedUpdateTaskOuterClass.FeedUpdateTask payload = logbrokerEvents.get(0)
                .getPayload();
        Assertions.assertThat(payload.getFeed().getUrl())
                .isEqualTo(url);
        Assertions.assertThat(payload.getFeed().getFeedType())
                .isEqualTo(feedType);
    }
}

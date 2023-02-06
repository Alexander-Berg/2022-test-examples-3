package ru.yandex.market.ff.i18n;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.NoSuchMessageException;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.enums.RequestStatus;
import ru.yandex.market.ff.client.enums.RequestType;
import ru.yandex.market.ff.client.enums.StockType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Функциональные тесты для {@link RequestExportMessages}.
 *
 * @author avetokhin 27.08.18.
 */
class RequestExportMessagesTest extends IntegrationTest {

    @Autowired
    private RequestExportMessages messages;

    @Autowired
    @Qualifier("allowedRequestStatusMoves")
    private Map<RequestType, Map<RequestStatus, List<RequestStatus>>> allowedStatusMoves;

    /**
     * Проверяет, что для всех типов стоков возвращается название стока.
     */
    @Test
    void getStockLabel() {
        for (StockType stockType : StockType.values()) {
            final String label = messages.getStockLabel(stockType);
            assertThat(label, notNullValue());
        }
    }

    /**
     * Проверяет, что для всех типов заявок возвращается название типа.
     */
    @Test
    void getRequestTypeLabel() {
        for (RequestType type : RequestType.REAL_TYPES) {
            final String label = messages.getRequestTypeLabel(type);
            assertThat(label, notNullValue());
        }
    }

    @Test
    void getStatusLabel() {
        for (RequestType type : RequestType.REAL_TYPES) {
            Map<RequestStatus, List<RequestStatus>> allowedMoves = allowedStatusMoves.get(type);
            Set<RequestStatus> allowedStatuses = new HashSet<>();
            allowedMoves.forEach((status, requestStatuses) -> {
                allowedStatuses.add(status);
                allowedStatuses.addAll(requestStatuses);
            });
            for (RequestStatus status : allowedStatuses) {
                String label;
                try {
                    label = messages.getStatusLabel(status, type);
                } catch (NoSuchMessageException e) {
                    label = null;
                }
                assertions.assertThat(label).as(type + "." + status).isNotNull();
            }
        }
    }

}

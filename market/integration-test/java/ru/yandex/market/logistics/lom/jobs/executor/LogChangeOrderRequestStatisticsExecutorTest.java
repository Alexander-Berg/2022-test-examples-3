package ru.yandex.market.logistics.lom.jobs.executor;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;

@DisplayName("Тесты логирования статистики заявок изменение заказа")
public class LogChangeOrderRequestStatisticsExecutorTest extends AbstractContextualTest {

    @Autowired
    private LogChangeOrderRequestStatisticsExecutor logChangeOrderRequestStatisticsExecutor;

    @Test
    @DisplayName("Проверка записи статистики в лог по новым заявкам для заказов в доставке")
    @DatabaseSetup("/jobs/executor/logChangeOrderRequestStatisticsExecutor/log_created_for_order_in_delivery.xml")
    void logCreatedChangeRequestForOrderInDeliveryTest() {
        logChangeOrderRequestStatisticsExecutor.doJob(null);

        softly.assertThat(backLogCaptor.getResults().size()).isEqualTo(3);
        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=CREATED_FOR_ORDER_IN_DELIVERY\t" +
                "payload=ORDER_CHANGED_BY_PARTNER/1/CREATED_FOR_ORDER_IN_DELIVERY/CREATED\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=ORDER_CHANGE_REQUEST_STATS\t" +
                "extra_keys=updatedTime,requestType,requestId,createdTime,status\t" +
                "extra_values=1604323800,ORDER_CHANGED_BY_PARTNER,1,1604322000,CREATED\n",
            "level=INFO\t" +
                "format=plain\t" +
                "code=CREATED_FOR_ORDER_IN_DELIVERY\t" +
                "payload=ITEM_NOT_FOUND/2/CREATED_FOR_ORDER_IN_DELIVERY/CREATED\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=ORDER_CHANGE_REQUEST_STATS\t" +
                "extra_keys=updatedTime,requestType,requestId,createdTime,status\t" +
                "extra_values=1604323800,ITEM_NOT_FOUND,2,1604322000,CREATED\n"
        );
    }

    @Test
    @DisplayName("Проверка записи статистики в лог по активным заявкам для завершенных заказов")
    @DatabaseSetup("/jobs/executor/logChangeOrderRequestStatisticsExecutor/log_active_for_finalized_orders.xml")
    void logActiveChangeRequestForFinalizedOrderTest() {
        logChangeOrderRequestStatisticsExecutor.doJob(null);

        softly.assertThat(backLogCaptor.getResults().size()).isEqualTo(7);
        softly.assertThat(backLogCaptor.getResults().toString()).contains(
            "level=INFO\t" +
                "format=plain\t" +
                "code=ACTIVE_FOR_FINALIZED_ORDER\t" +
                "payload=ORDER_CHANGED_BY_PARTNER/1/ACTIVE_FOR_FINALIZED_ORDER/CREATED\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=ORDER_CHANGE_REQUEST_STATS\t" +
                "extra_keys=updatedTime,requestType,requestId,createdTime,status\t" +
                "extra_values=1604323800,ORDER_CHANGED_BY_PARTNER,1,1604322000,CREATED\n",
            "level=INFO\t" +
                "format=plain\t" +
                "code=ACTIVE_FOR_FINALIZED_ORDER\t" +
                "payload=ORDER_CHANGED_BY_PARTNER/2/ACTIVE_FOR_FINALIZED_ORDER/INFO_RECEIVED\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=ORDER_CHANGE_REQUEST_STATS\t" +
                "extra_keys=updatedTime,requestType,requestId,createdTime,status\t" +
                "extra_values=1604323800,ORDER_CHANGED_BY_PARTNER,2,1604322000,INFO_RECEIVED\n",
            "level=INFO\t" +
                "format=plain\t" +
                "code=ACTIVE_FOR_FINALIZED_ORDER\t" +
                "payload=ORDER_CHANGED_BY_PARTNER/3/ACTIVE_FOR_FINALIZED_ORDER/PROCESSING\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=ORDER_CHANGE_REQUEST_STATS\t" +
                "extra_keys=updatedTime,requestType,requestId,createdTime,status\t" +
                "extra_values=1604323800,ORDER_CHANGED_BY_PARTNER,3,1604322000,PROCESSING\n",
            "level=INFO\t" +
                "format=plain\t" +
                "code=ACTIVE_FOR_FINALIZED_ORDER\t" +
                "payload=ITEM_NOT_FOUND/4/ACTIVE_FOR_FINALIZED_ORDER/CREATED\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=ORDER_CHANGE_REQUEST_STATS\t" +
                "extra_keys=updatedTime,requestType,requestId,createdTime,status\t" +
                "extra_values=1604323800,ITEM_NOT_FOUND,4,1604322000,CREATED\n",
            "level=INFO\t" +
                "format=plain\t" +
                "code=ACTIVE_FOR_FINALIZED_ORDER\t" +
                "payload=ITEM_NOT_FOUND/5/ACTIVE_FOR_FINALIZED_ORDER/INFO_RECEIVED\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=ORDER_CHANGE_REQUEST_STATS\t" +
                "extra_keys=updatedTime,requestType,requestId,createdTime,status\t" +
                "extra_values=1604323800,ITEM_NOT_FOUND,5,1604322000,INFO_RECEIVED\n",
            "level=INFO\t" +
                "format=plain\t" +
                "code=ACTIVE_FOR_FINALIZED_ORDER\t" +
                "payload=ITEM_NOT_FOUND/6/ACTIVE_FOR_FINALIZED_ORDER/PROCESSING\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=ORDER_CHANGE_REQUEST_STATS\t" +
                "extra_keys=updatedTime,requestType,requestId,createdTime,status\t" +
                "extra_values=1604323800,ITEM_NOT_FOUND,6,1604322000,PROCESSING\n"
        );
    }


}

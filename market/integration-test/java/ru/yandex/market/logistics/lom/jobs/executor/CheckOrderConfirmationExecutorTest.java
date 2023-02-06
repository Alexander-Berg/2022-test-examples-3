package ru.yandex.market.logistics.lom.jobs.executor;

import java.time.Instant;
import java.time.ZoneId;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.lom.AbstractContextualTest;

import static org.mockito.Mockito.mock;

@DatabaseSetup("/controller/order/search/orders.xml")
@DatabaseSetup(
    value = "/jobs/executor/checkOrderConfirmation/check_order_confirmation.xml",
    type = DatabaseOperation.INSERT
)
class CheckOrderConfirmationExecutorTest extends AbstractContextualTest {

    @Autowired
    private CheckOrderConfirmationExecutor checkOrderConfirmation;

    private final JobExecutionContext jobContext = mock(JobExecutionContext.class);

    @Test
    @DisplayName("Заказ долго ждет ответа от LGW")
    void foundWaitingOrder() {
        clock.setFixed(Instant.parse("2019-08-01T19:00:00.01Z"), ZoneId.systemDefault());

        checkOrderConfirmation.doJob(jobContext);

        String log = backLogCaptor.getResults().toString();

        softly.assertThat(log)
            .contains("level=ERROR\t" +
                "format=plain\t" +
                "code=CONFIRMATION_ORDER_TIMEOUT_EXPIRED\t" +
                "payload=Expired the confirmation of the segment in status 'STARTED'\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=BUSINESS_ORDER_EVENT\t" +
                "entity_types=order,lom_order,sender,partner,platform\t" +
                "entity_values=order:1003,lom_order:7,sender:3,partner:3,platform:YANDEX_DELIVERY\n")
            .contains("level=ERROR\t" +
                "format=plain\t" +
                "code=CONFIRMATION_ORDER_TIMEOUT_EXPIRED\t" +
                "payload=Expired the confirmation of the segment in status 'STARTED'\t" +
                "request_id=1000000000000/abcdabcdabcdabcdabcdabcdabcdabcd\t" +
                "tags=BUSINESS_ORDER_EVENT\t" +
                "entity_types=order,lom_order,sender,partner,platform\t" +
                "entity_values=order:1002,lom_order:6,sender:2,partner:2,platform:YANDEX_DELIVERY\n");
    }

    @Test
    @DisplayName("Нет долго ждущих ответа от LGW заказов")
    void notFoundWaitingOrder() {
        clock.setFixed(Instant.parse("2019-08-01T19:05:00.01Z"), ZoneId.systemDefault());

        checkOrderConfirmation.doJob(jobContext);

        softly.assertThat(backLogCaptor.getResults()).hasSize(1);
    }
}

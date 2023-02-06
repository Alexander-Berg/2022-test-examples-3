package ru.yandex.market.tpl.core.domain.order.producer;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.tpl.api.model.order.OrderFlowStatus;
import ru.yandex.market.tpl.core.CoreTest;
import ru.yandex.market.tpl.core.adapter.ConfigurationProviderAdapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.SEND_MESSAGE_UPDATE_LOCKER_ORDER_STATUS;

@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class LockerOrderFlowStatusUpdatedProducerTest {

    private final LockerOrderFlowStatusUpdatedProducer producer;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    @MockBean
    private ConfigurationProviderAdapter configurationProviderAdapter;


    @Test
    void produce() throws IOException {
        when(configurationProviderAdapter.isBooleanEnabled(SEND_MESSAGE_UPDATE_LOCKER_ORDER_STATUS)).thenReturn(true);
        producer.produce("123", OrderFlowStatus.TRANSPORTATION_RECIPIENT);
        List<Map<String, Object>> queryForList = jdbcTemplate.queryForList("select queue_name, task from queue_task");
        LockerOrderFlowStatusUpdatedPayload payload = objectMapper.readValue(queryForList.get(0).get("task").toString(),
                LockerOrderFlowStatusUpdatedPayload.class);
        assertThat(payload.getStatus()).isEqualTo(OrderFlowStatus.TRANSPORTATION_RECIPIENT);
        assertThat(payload.getStatus().getCode()).isEqualTo(48);
        assertThat(payload.getExternalOrderId()).isEqualTo("123");
        assertThat(payload.getEntityId()).isEqualTo("123");
        assertThat(queryForList.get(0).get("queue_name")).isEqualTo("LOCKER_ORDER_FLOW_STATUS_UPDATED");
    }

}

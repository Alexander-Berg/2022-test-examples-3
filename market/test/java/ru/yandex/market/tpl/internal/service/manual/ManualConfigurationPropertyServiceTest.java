package ru.yandex.market.tpl.internal.service.manual;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

import ru.yandex.market.tpl.common.db.test.DbQueueTestUtil;
import ru.yandex.market.tpl.core.adapter.ConfigurationServiceAdapter;
import ru.yandex.market.tpl.core.dbqueue.model.QueueType;
import ru.yandex.market.tpl.internal.TplIntAbstractTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.configuration.ConfigurationProperties.YARD_FOR_EQUEUE_DISABLED;

@RequiredArgsConstructor
class ManualConfigurationPropertyServiceTest extends TplIntAbstractTest {

    private final ManualConfigurationPropertyService subject;
    private final ConfigurationServiceAdapter configurationServiceAdapter;
    private final DbQueueTestUtil dbQueueTestUtil;

    @Test
    void enableYardForEqueue() {
        configurationServiceAdapter.mergeValue(YARD_FOR_EQUEUE_DISABLED, true);

        subject.setStateOfYardForEqueue(true);
        Optional<Boolean> result = configurationServiceAdapter.getValue(YARD_FOR_EQUEUE_DISABLED, Boolean.class);
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get()).isFalse();

        dbQueueTestUtil.assertQueueHasSize(QueueType.EQUEUE_PUSH_COURIERS_TO_SC, 0);
    }

    @Test
    void disableYardForEqueue() {
        configurationServiceAdapter.mergeValue(YARD_FOR_EQUEUE_DISABLED, false);

        subject.setStateOfYardForEqueue(false);
        Optional<Boolean> result2 = configurationServiceAdapter.getValue(YARD_FOR_EQUEUE_DISABLED, Boolean.class);
        assertThat(result2.isPresent()).isTrue();
        assertThat(result2.get()).isTrue();

        dbQueueTestUtil.assertQueueHasSize(QueueType.EQUEUE_PUSH_COURIERS_TO_SC, 1);
    }

}

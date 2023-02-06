package ru.yandex.market.logistics.nesu.jobs;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.jobs.executor.VirtualServicesModifierUpdateExecutor;
import ru.yandex.market.logistics.nesu.jobs.producer.ModifierUploadTaskProducer;
import ru.yandex.market.logistics.nesu.service.modifier.DeliveryOptionModifierService;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DatabaseSetup("/jobs/executors/sender_modifier_update_setup.xml")
@DisplayName("Обновление модификаторов виртуальных служб в КД")
class VirtualServicesModifierUpdateExecutorTest extends AbstractContextualTest {

    @Autowired
    private ModifierUploadTaskProducer producer;

    @Autowired
    private DeliveryOptionModifierService deliveryOptionModifierService;

    @Autowired
    private ModifierUploadTaskProducer modifierUploadTaskProducer;

    private VirtualServicesModifierUpdateExecutor executor;

    @BeforeEach
    void setup() {
        doNothing().when(producer).produceTask(anyLong());
        executor = new VirtualServicesModifierUpdateExecutor(deliveryOptionModifierService, modifierUploadTaskProducer);
    }

    @AfterEach
    void checkMocks() {
        verifyNoMoreInteractions(producer);
    }

    @Test
    @DisplayName("Обновление настроек для всех сендеров с виртуальными службами")
    void allSendersWillProduce() {
        executor.doJob(null);
        verify(producer).produceTask(1L);
        verify(producer).produceTask(3L);
    }

}

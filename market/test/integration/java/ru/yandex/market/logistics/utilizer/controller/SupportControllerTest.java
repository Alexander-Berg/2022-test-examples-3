package ru.yandex.market.logistics.utilizer.controller;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import ru.yandex.market.logistics.utilizer.base.AbstractContextualTest;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.SendUtilizationCycleFinalizationEmailPayload;
import ru.yandex.market.logistics.utilizer.dbqueue.task.events.transfer.CreateTransferDbqueuePayload;
import ru.yandex.money.common.dbqueue.api.EnqueueParams;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SupportControllerTest extends AbstractContextualTest {

    @Test
    @DatabaseSetup(value = "classpath:fixtures/controller/support/finalize-cycle/before.xml")
    public void finalizeCycleForVendorWithoutCreatedCycles() throws Exception {
        mockMvc.perform(put("/support/finalize-cycle/100501"))
                .andExpect(status().isBadRequest());
        Mockito.verifyZeroInteractions(sendUtilizationCycleFinalizationEmailProducer);
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/controller/support/finalize-cycle/before.xml")
    public void finalizeCycleForVendorWithCreatedCycle() throws Exception {
        mockMvc.perform(put("/support/finalize-cycle/100500"))
                .andExpect(status().isOk());
        ArgumentCaptor<EnqueueParams<SendUtilizationCycleFinalizationEmailPayload>> captor =
                ArgumentCaptor.forClass(EnqueueParams.class);
        Mockito.verify(sendUtilizationCycleFinalizationEmailProducer).enqueue(captor.capture());
        softly.assertThat(captor.getValue().getPayload().getUtilizationCycleId()).isEqualTo(1);
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/controller/support/create-transfer/before.xml")
    public void createTransferForVendorWithoutFinalizedCycles() throws Exception {
        mockMvc.perform(put("/support/create-transfer/100501"))
                .andExpect(status().isBadRequest());
        Mockito.verifyZeroInteractions(createTransferDbqueueProducer);
    }

    @Test
    @DatabaseSetup(value = "classpath:fixtures/controller/support/create-transfer/before.xml")
    public void createTransferForEnabledVendorWithFinalizedCycles() throws Exception {
        mockMvc.perform(put("/support/create-transfer/100500"))
                .andExpect(status().isOk());
        ArgumentCaptor<EnqueueParams<CreateTransferDbqueuePayload>> captor =
                ArgumentCaptor.forClass(EnqueueParams.class);
        Mockito.verify(createTransferDbqueueProducer).enqueue(captor.capture());
        softly.assertThat(captor.getValue().getPayload().getUtilizationCycleId()).isEqualTo(5);
    }
}

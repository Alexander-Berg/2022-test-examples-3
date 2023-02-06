package ru.yandex.market.wms.api.config;

import java.util.HashMap;

import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.MessageHeaders;

import ru.yandex.market.wms.api.model.dto.FixLostWriteOffDTO;
import ru.yandex.market.wms.api.service.async.LostWriteOffAsyncService;
import ru.yandex.market.wms.common.spring.service.CycleInventoryService;

import static org.mockito.Mockito.doAnswer;

@TestConfiguration
public class LostWriteOffAsyncConfig {

    @Bean
    public LostWriteOffAsyncService lostWriteOffAsyncService(
            JmsTemplate jms, CycleInventoryService cycleInventoryService
    ) {
        LostWriteOffAsyncService origin = new LostWriteOffAsyncService(jms, cycleInventoryService);
        LostWriteOffAsyncService mock = Mockito.spy(origin);
        doAnswer((Answer<Void>) invocation -> {
            Object[] args = invocation.getArguments();
            FixLostWriteOffDTO obj = (FixLostWriteOffDTO) args[0];
            mock.onWriteOff(obj, new MessageHeaders(new HashMap<>()));
            return null;
        }).when(mock).writeOffFixLost(ArgumentMatchers.any());
        return mock;
    }
}

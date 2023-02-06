package ru.yandex.direct.communication.config;

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import ru.yandex.direct.communication.CommunicationChannelRepository;
import ru.yandex.direct.communication.inventory.CommunicationInventoryClient;
import ru.yandex.direct.core.entity.communication.repository.CommunicationEventVersionsRepository;

@Configuration
@Import({CommunicationConfiguration.class})
public class CommunicationTestingConfiguration {

    @MockBean(name = CommunicationConfiguration.COMMUNICATION_INVENTORY_CLIENT)
    public CommunicationInventoryClient communicationInventoryClient;

    @MockBean()
    public CommunicationChannelRepository communicationChannelRepository;

    @MockBean()
    public CommunicationEventVersionsRepository communicationEventVersionsRepository;
}

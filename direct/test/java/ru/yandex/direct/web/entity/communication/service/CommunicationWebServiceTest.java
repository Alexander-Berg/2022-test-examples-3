package ru.yandex.direct.web.entity.communication.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.ads.bsyeti.libs.communications.EChannel;
import ru.yandex.ads.bsyeti.libs.communications.ECommunicationType;
import ru.yandex.ads.bsyeti.libs.communications.EMessageStatus;
import ru.yandex.direct.communication.CommunicationChannelRepository;
import ru.yandex.direct.communication.container.CommunicationChannelItem;
import ru.yandex.direct.communication.container.web.Message;
import ru.yandex.direct.communication.container.web.WebMessage;
import ru.yandex.direct.core.entity.communication.repository.CommunicationEventVersionsRepository;
import ru.yandex.direct.web.entity.communication.model.response.WebGenerateMessageResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static ru.yandex.ads.bsyeti.libs.communications.TDirectWebUIData.EWebUIMessageType.RECOMMENDATION;

@RunWith(MockitoJUnitRunner.class)
public class CommunicationWebServiceTest {

    @Mock
    private CommunicationChannelRepository repository;

    @Mock
    private CommunicationEventVersionsRepository versionsRepository;

    @InjectMocks
    private CommunicationWebService webService;

    @Before
    public void init() {
        when(versionsRepository.getVersionsByStatuses(anyList())).thenReturn(List.of());
    }

    @Test
    public void getUserMessages_success() {
        Message message = new Message().withId("12345");
        var item = getChannelItem(message, Integer.MAX_VALUE, EChannel.DIRECT_WEB_UI,
                List.of(EMessageStatus.NEW), 0);

        when(repository.getMessagesByUid(anyLong(), anyList())).thenReturn(List.of(item));
        var response = (WebGenerateMessageResult) webService.getUserMessages(0L,
                new HashMap<>());

        assertThat(response)
                .extracting(WebGenerateMessageResult::getMessages)
                .asList()
                .containsOnly(message);
    }

    @Test
    public void getUserMessages_alreadyExpired() {
        Message message = new Message().withId("12345");
        var item = getChannelItem(message, 0, EChannel.DIRECT_WEB_UI,
                List.of(EMessageStatus.NEW), 0);

        when(repository.getMessagesByUid(anyLong(), anyList())).thenReturn(List.of(item));
        var response = (WebGenerateMessageResult) webService.getUserMessages(0L,
                new HashMap<>());

        assertThat(response)
                .extracting(WebGenerateMessageResult::getMessages)
                .asList()
                .isEmpty();
    }

    @Test
    public void getUserMessages_incorrectChannel() {
        Message message = new Message().withId("12345");
        var item = getChannelItem(message, Integer.MAX_VALUE, EChannel.MAIL,
                List.of(EMessageStatus.NEW), 0);

        when(repository.getMessagesByUid(anyLong(), anyList())).thenReturn(List.of(item));
        var response = (WebGenerateMessageResult) webService.getUserMessages(0L,
                new HashMap<>());

        assertThat(response)
                .extracting(WebGenerateMessageResult::getMessages)
                .asList()
                .isEmpty();
    }

    @Test
    public void getUserMessages_emptyStatuses() {
        Message message = new Message().withId("12345");
        var item = getChannelItem(message, Integer.MAX_VALUE, EChannel.DIRECT_WEB_UI,
                null, 0);

        when(repository.getMessagesByUid(anyLong(), anyList())).thenReturn(List.of(item));
        var response = (WebGenerateMessageResult) webService.getUserMessages(0L,
                new HashMap<>());

        assertThat(response)
                .extracting(WebGenerateMessageResult::getMessages)
                .asList()
                .isEmpty();
    }

    @Test
    public void getUserMessages_incorrectStatuses() {
        Message message = new Message().withId("12345");
        var item = getChannelItem(message, Integer.MAX_VALUE, EChannel.DIRECT_WEB_UI,
                List.of(EMessageStatus.DELIVERED, EMessageStatus.APPLY), 0);

        when(repository.getMessagesByUid(anyLong(), anyList())).thenReturn(List.of(item));
        var response = (WebGenerateMessageResult) webService.getUserMessages(0L,
                new HashMap<>());

        assertThat(response)
                .extracting(WebGenerateMessageResult::getMessages)
                .asList()
                .isEmpty();
    }

    @Test
    public void getUserMessages_alreadyProcessed() {
        Message message = new Message().withId("12345");
        var item = getChannelItem(message, Integer.MAX_VALUE, EChannel.DIRECT_WEB_UI,
                List.of(EMessageStatus.NEW), 0);

        when(repository.getMessagesByUid(anyLong(), anyList())).thenReturn(List.of(item));
        Map<Long, Set<EMessageStatus>> removedStatuses = new HashMap<>();
        removedStatuses.put(111L, Set.of(EMessageStatus.NEW));
        var response = (WebGenerateMessageResult) webService.getUserMessages(0L,
                removedStatuses);

        assertThat(response)
                .extracting(WebGenerateMessageResult::getMessages)
                .asList()
                .isEmpty();
    }

    @Test
    public void getUserMessages_showLater() {
        Message message = new Message().withId("12345");
        var item = getChannelItem(message, Integer.MAX_VALUE, EChannel.DIRECT_WEB_UI,
                List.of(EMessageStatus.NEW), Integer.MAX_VALUE);

        when(repository.getMessagesByUid(anyLong(), anyList())).thenReturn(List.of(item));
        var response = (WebGenerateMessageResult) webService.getUserMessages(0L,
                new HashMap<>());

        assertThat(response)
                .extracting(WebGenerateMessageResult::getMessages)
                .asList()
                .isEmpty();
    }

    @Test
    public void getUserMessages_emptyMessage() {
        var item = getChannelItem(null, Integer.MAX_VALUE, EChannel.DIRECT_WEB_UI,
                List.of(EMessageStatus.NEW), 0);

        when(repository.getMessagesByUid(anyLong(), anyList())).thenReturn(List.of(item));
        var response = (WebGenerateMessageResult) webService.getUserMessages(0L,
                new HashMap<>());

        assertThat(response)
                .extracting(WebGenerateMessageResult::getMessages)
                .asList()
                .isEmpty();
    }

    private CommunicationChannelItem getChannelItem(Message message, int expirationTime, EChannel channel,
                                                    List<EMessageStatus> statuses, int showAfter) {
        return new CommunicationChannelItem(channel.getNumber(), 111L, 0, 11111L,
                expirationTime, "SOURCE", 123L, statuses, ECommunicationType.MARKETING,
                new WebMessage(message, showAfter, RECOMMENDATION, 0));
    }
}

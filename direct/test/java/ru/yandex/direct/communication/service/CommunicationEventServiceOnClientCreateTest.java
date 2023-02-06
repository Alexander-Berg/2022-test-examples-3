package ru.yandex.direct.communication.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.common.db.PpcProperty;
import ru.yandex.direct.common.db.PpcPropertyName;
import ru.yandex.direct.communication.container.AdditionalInfoContainer;
import ru.yandex.direct.communication.facade.CommunicationEventVersionProcessingFacade;
import ru.yandex.direct.communication.facade.CommunicationEventVersionProcessingFacadeBuilder;
import ru.yandex.direct.communication.facade.CommunicationEventVersionsProcessor;
import ru.yandex.direct.core.entity.communication.model.CommunicationEventVersion;
import ru.yandex.direct.core.entity.communication.model.CommunicationEventVersionStatus;
import ru.yandex.direct.core.entity.communication.repository.CommunicationEventVersionsRepository;
import ru.yandex.direct.dbutil.model.ClientId;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

public class CommunicationEventServiceOnClientCreateTest {

    @Mock
    private CommunicationEventVersionsRepository repository;

    private CommunicationEventService service;

    @Mock
    private PpcPropertiesSupport propertiesSupport;

    private CommunicationChannelService channelService;

    @Spy
    private MockedProcessor processor = new MockedProcessor("PROCESSOR_A");

    @Spy
    private MockedProcessor anotherProcessor = new MockedProcessor("PROCESSOR_B");

    private CommunicationEventVersionProcessingFacade processingFacade;

    private final ClientId clientId = ClientId.fromLong(123L);

    @Before
    public void setUp() {
        openMocks(this);
        PpcProperty<Boolean> ppcProp = Mockito.mock(PpcProperty.class);
        when(ppcProp.getOrDefault(anyBoolean())).thenReturn(false);
        when(propertiesSupport.get(any(PpcPropertyName.class), any(Duration.class)))
                .thenReturn(ppcProp);
        channelService = new CommunicationChannelService(
                null, propertiesSupport, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        processingFacade = new CommunicationEventVersionProcessingFacadeBuilder()
                .withProcessors(List.of(processor, anotherProcessor))
                .build();
    }

    @Test
    public void processOnClientCreateEvents_noEventsForProcessing() {
        when(repository.getVersionsByStatuses(anyList()))
                .thenReturn(Collections.emptyList());
        service = new CommunicationEventService(repository, null, processingFacade, channelService);
        service.processOnClientCreateEvents(clientId);
        verify(processor, never()).process(any(), any(), any());
        verify(anotherProcessor, never()).process(any(), any(), any());
    }

    @Test
    public void processOnClientCreateEvents_eventsWithEmptyOnClientCreate() {
        when(repository.getVersionsByStatuses(anyList()))
                .thenReturn(List.of(
                        new CommunicationEventVersion()
                                .withEventId(1L)
                                .withStatus(CommunicationEventVersionStatus.ACTIVE),
                        new CommunicationEventVersion()
                                .withEventId(2L)
                                .withStatus(CommunicationEventVersionStatus.APPROVED),
                        new CommunicationEventVersion()
                                .withEventId(3L)
                                .withStatus(CommunicationEventVersionStatus.ARCHIVED),
                        new CommunicationEventVersion()
                                .withEventId(4L)
                                .withStatus(CommunicationEventVersionStatus.READY)
                ));

        service = new CommunicationEventService(repository, null, processingFacade, channelService);
        service.processOnClientCreateEvents(clientId);
        verify(processor, never()).process(any(), any(), any());
        verify(anotherProcessor, never()).process(any(), any(), any());
    }

    @Test
    public void processOnClientCreateEvents_eventsWithActiveStatusesOnly() {
        var activeEvents =
                List.of(new CommunicationEventVersion()
                                .withEventId(1L)
                                .withStatus(CommunicationEventVersionStatus.ACTIVE)
                                .withOnClientCreate(processor.name),
                        new CommunicationEventVersion()
                                .withEventId(2L)
                                .withStatus(CommunicationEventVersionStatus.ACTIVE)
                                .withOnClientCreate(processor.name)
                );

        when(repository.getVersionsByStatuses(List.of(CommunicationEventVersionStatus.ACTIVE)))
                .thenReturn(activeEvents);

        service = new CommunicationEventService(repository, null, processingFacade, channelService);
        service.processOnClientCreateEvents(clientId);
        verify(processor, times(1)).process(
                any(AdditionalInfoContainer.class),
                eq(activeEvents),
                eq(CommunicationEventVersionProcessingFacade.ON_CLIENT_CREATE_TRIGGER));
        verify(anotherProcessor, never()).process(any(), any(), any());
    }

    @Test
    public void processOnClientCreateEvents_activeEventsWithSameOnClientCreate() {
        var events = List.of(
                new CommunicationEventVersion()
                        .withEventId(1L)
                        .withStatus(CommunicationEventVersionStatus.ACTIVE)
                        .withOnClientCreate(processor.name),
                new CommunicationEventVersion()
                        .withEventId(2L)
                        .withStatus(CommunicationEventVersionStatus.ACTIVE)
                        .withOnClientCreate(processor.name),
                new CommunicationEventVersion()
                        .withEventId(3L)
                        .withStatus(CommunicationEventVersionStatus.ACTIVE)
                        .withOnClientCreate(processor.name),
                new CommunicationEventVersion()
                        .withEventId(4L)
                        .withStatus(CommunicationEventVersionStatus.ACTIVE)
                        .withOnClientCreate(processor.name)
        );

        when(repository.getVersionsByStatuses(anyList()))
                .thenReturn(events);

        service = new CommunicationEventService(repository, null, processingFacade, channelService);
        service.processOnClientCreateEvents(clientId);
        verify(processor, times(1)).process(
                any(AdditionalInfoContainer.class),
                eq(events),
                eq(CommunicationEventVersionProcessingFacade.ON_CLIENT_CREATE_TRIGGER));
        verify(anotherProcessor, never()).process(any(), any(), any());
    }

    @Test
    public void processOnClientCreateEvents_activeEventsWithDifferentOnClientCreate() {
        var eventList = new ArrayList<CommunicationEventVersion>();
        eventList.add(
                new CommunicationEventVersion()
                        .withEventId(1L)
                        .withStatus(CommunicationEventVersionStatus.ACTIVE)
                        .withOnClientCreate(processor.name));
        eventList.add(
                new CommunicationEventVersion()
                        .withEventId(2L)
                        .withStatus(CommunicationEventVersionStatus.ACTIVE)
                        .withOnClientCreate(processor.name));

        var anotherEventList = new ArrayList<CommunicationEventVersion>();
        anotherEventList.add(
                new CommunicationEventVersion()
                        .withEventId(3L)
                        .withStatus(CommunicationEventVersionStatus.ACTIVE)
                        .withOnClientCreate(anotherProcessor.name));
        anotherEventList.add(
                new CommunicationEventVersion()
                        .withEventId(4L)
                        .withStatus(CommunicationEventVersionStatus.ACTIVE)
                        .withOnClientCreate(anotherProcessor.name));

        var fullEventList = new ArrayList<>(eventList);
        fullEventList.addAll(anotherEventList);

        when(repository.getVersionsByStatuses(anyList()))
                .thenReturn(fullEventList);

        service = new CommunicationEventService(repository, null, processingFacade, channelService);
        service.processOnClientCreateEvents(clientId);
        verify(processor, times(1)).process(
                any(AdditionalInfoContainer.class),
                eq(eventList),
                eq(CommunicationEventVersionProcessingFacade.ON_CLIENT_CREATE_TRIGGER));

        verify(anotherProcessor, times(1)).process(
                any(AdditionalInfoContainer.class),
                eq(anotherEventList),
                eq(CommunicationEventVersionProcessingFacade.ON_CLIENT_CREATE_TRIGGER));
    }

    private static class MockedProcessor implements CommunicationEventVersionsProcessor {

        private final String name;

        public MockedProcessor(String name) {
            this.name = name;
        }

        @Override
        public void process(AdditionalInfoContainer target, List<CommunicationEventVersion> eventVersions,
                            String trigger) {
            //do nothing
        }

        @Override
        public String getName() {
            return name;
        }
    }
}

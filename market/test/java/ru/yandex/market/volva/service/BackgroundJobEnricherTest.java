package ru.yandex.market.volva.service;

import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.mockito.Mockito;

import ru.yandex.market.volva.dao.PostProcessingQueueDao;
import ru.yandex.market.volva.entity.EdgeEvent;
import ru.yandex.market.volva.entity.EventCollection;
import ru.yandex.market.volva.entity.IdType;
import ru.yandex.market.volva.entity.Node;
import ru.yandex.market.volva.entity.Source;
import ru.yandex.market.volva.entity.crypta.PostProcessingQueueEntry;

import static java.util.stream.Collectors.toSet;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;

/**
 * @author dzvyagin
 */
public class BackgroundJobEnricherTest {

    private PostProcessingQueueDao postProcessingQueueDao;

    @Test
    public void process() {
        postProcessingQueueDao = mock(PostProcessingQueueDao.class);
        BackgroundJobEnricher backgroundJobEnricher = new BackgroundJobEnricher(postProcessingQueueDao);
        EventCollection events = EventCollection.create(List.of(
            EdgeEvent.addTrusted(new Node("123", IdType.PUID), new Node("222", IdType.UUID)),
            EdgeEvent.addTrusted(new Node("124", IdType.PUID), new Node("223", IdType.YANDEXUID)),
            EdgeEvent.addTrusted(new Node("125", IdType.PUID), new Node("224", IdType.DEVICE_ID))
        ), Source.CHECKOUTER);
        backgroundJobEnricher.process(events);
        Mockito.verify(postProcessingQueueDao)
            .saveEntries(argThat(entries ->
                entries.stream()
                    .map(PostProcessingQueueEntry::getNode)
                    .collect(toSet())
                    .equals(Set.of(
                        new Node("123", IdType.PUID),
                        new Node("124", IdType.PUID),
                        new Node("125", IdType.PUID),
                        new Node("222", IdType.UUID),
                        new Node("223", IdType.YANDEXUID),
                        new Node("224", IdType.DEVICE_ID)
                    ))
            ));
    }

}

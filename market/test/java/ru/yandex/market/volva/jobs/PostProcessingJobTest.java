package ru.yandex.market.volva.jobs;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;

import ru.yandex.market.mstat.graphs.impl.SimpleGraphEdge;
import ru.yandex.market.volva.config.ConfigurationService;
import ru.yandex.market.volva.dao.CryptaYtDao;
import ru.yandex.market.volva.dao.PhonesYtDao;
import ru.yandex.market.volva.dao.PostProcessingQueueDao;
import ru.yandex.market.volva.entity.EventCollection;
import ru.yandex.market.volva.entity.IdType;
import ru.yandex.market.volva.entity.Node;
import ru.yandex.market.volva.entity.crypta.PostProcessingQueueEntry;
import ru.yandex.market.volva.entity.crypta.PostProcessingRequestStatus;
import ru.yandex.market.volva.logbroker.MessageConsumer;
import ru.yandex.market.volva.service.YtReaderService;
import ru.yandex.market.volva.yt.YtEdge;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author dzvyagin
 */
public class PostProcessingJobTest {

    @Test
    public void jobTest() {
        JobService jobService = mock(JobService.class);
        PostProcessingQueueDao postProcessingQueueDao = mock(PostProcessingQueueDao.class);
        CryptaYtDao cryptaYtDao = mock(CryptaYtDao.class);
        PhonesYtDao phonesYtDao = mock(PhonesYtDao.class);
        ConfigurationService configurationService = mock(ConfigurationService.class);
        //noinspection unchecked
        MessageConsumer<EventCollection> consumer = mock(MessageConsumer.class);
        YtReaderService ytReaderService = mock(YtReaderService.class);
        when(jobService.jobStarted(any(Job.class))).thenReturn(JobEntity.builder().id(1L).jobName("name").startedAt(Instant.now()).status(JobStatus.RUNNING).build());
        when(jobService.isJobRunAllowed(any(Job.class))).thenReturn(true);
        when(postProcessingQueueDao.getEntries(any(PostProcessingRequestStatus.class), anyString(), anyInt())).thenReturn(List.of(
            PostProcessingQueueEntry.builder().id(1L).status(PostProcessingRequestStatus.QUEUED).nodeValue("node1").nodeType(IdType.PUID).build(),
            PostProcessingQueueEntry.builder().id(2L).status(PostProcessingRequestStatus.QUEUED).nodeValue("node2").nodeType(IdType.CARD).build()
        ));
        when(ytReaderService.getGluedEdges(any(), anyCollection())).thenReturn(CompletableFuture.completedFuture(
            List.of(
                YtEdge.fromNodes(new Node("node1", IdType.PUID), new Node("1", IdType.CRYPTA_ID), "1"),
                YtEdge.fromNodes(new Node("node1", IdType.PUID), new Node("1", IdType.PHONE), "1"),
                YtEdge.fromNodes(new Node("node2", IdType.CARD), new Node("3", IdType.CRYPTA_ID), "2"),
                YtEdge.fromNodes(new Node("node2", IdType.CARD), new Node("3", IdType.PHONE), "2")
            )));
        when(cryptaYtDao.getCryptaEdgesForNodes(any())).thenReturn(List.of(
            new SimpleGraphEdge<>(new Node("1", IdType.PUID), new Node("1", IdType.CRYPTA_ID)),
            new SimpleGraphEdge<>(new Node("2", IdType.PUID), new Node("2", IdType.CRYPTA_ID))
        ));
        when(phonesYtDao.getPhoneEdgesForNodes(any())).thenReturn(List.of(
            new SimpleGraphEdge<>(new Node("3", IdType.PUID), new Node("3", IdType.PHONE)),
            new SimpleGraphEdge<>(new Node("4", IdType.PUID), new Node("4", IdType.PHONE))
        ));
        PostProcessingJob job = new PostProcessingJob(jobService, postProcessingQueueDao, cryptaYtDao, phonesYtDao, consumer, ytReaderService, configurationService);
        job.run();
        verify(consumer, times(2)).accept(any(EventCollection.class));
        verify(postProcessingQueueDao).updateStatuses(anyList(), eq(PostProcessingRequestStatus.PROCESSED));
        verify(postProcessingQueueDao).removeOldQueries(eq(PostProcessingRequestStatus.PROCESSED), any(Instant.class));
    }
}

package ru.yandex.chemodan.app.telemost.ugcLive;

import org.joda.time.Duration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.telemost.ugcLive.model.StreamAction;
import ru.yandex.chemodan.app.telemost.ugcLive.model.StreamState;
import ru.yandex.chemodan.util.test.AbstractTest;
import ru.yandex.misc.thread.ThreadUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class UgcLiveStreamPublisherTest extends AbstractTest {

    @Mock
    private UgcLiveClient client;
    private UgcLiveStreamPublisher publisher;

    @Before
    public void before() {
        publisher = new UgcLiveStreamPublisherImpl(client, Duration.millis(10));
    }

    @Test
    public void testPublishing() {
        doReturn(Option.of(StreamState.OFFLINE),
                Option.of(StreamState.PREPARING),
                Option.of(StreamState.READY)
        ).when(client).getStreamState(any());

        publisher.submitPublishing("XXX");

        ThreadUtils.sleep(Duration.standardSeconds(1));

        verify(client, times(3)).getStreamState(anyString());
        verify(client, times(1)).performStreamAction(anyString(), eq(StreamAction.PUBLISH));
    }
}

package ru.yandex.market.pers.author.takeout;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.pers.author.PersAuthorTest;
import ru.yandex.market.pers.author.takeout.model.TakeoutParam;
import ru.yandex.market.pers.author.takeout.model.TakeoutRequest;
import ru.yandex.market.pers.author.takeout.model.TakeoutState;
import ru.yandex.market.pers.author.takeout.model.TakeoutType;
import ru.yandex.market.pers.author.yt.YtHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 18.11.2021
 */
public class TakeoutCleanupJobTest extends PersAuthorTest {

    @Autowired
    private TakeoutService takeoutService;

    @Autowired
    private TakeoutCleanupJob cleanupJob;

    @Autowired
    private YtHelper ytHelper;

    @Test
    public void testCleanupJob() {
        takeoutService.saveTakeoutRequest(TakeoutRequest.generate("refNew", TakeoutType.UID).withData(Map.of()));

        takeoutService.saveTakeoutRequest(TakeoutRequest.generate("refYtReady", TakeoutType.UID).withData(Map.of()));


        takeoutService.saveTakeoutRequest(TakeoutRequest.generate("refCancel", TakeoutType.UID).withData(Map.of()));


        takeoutService.saveTakeoutRequest(TakeoutRequest.generate("refReady", TakeoutType.UID).withData(Map.of()));
        takeoutService.markReady("refReady", "resultUrl");

        List<String> ytTables = List.of(
            prepareRequest("refNew", TakeoutState.NEW).getId(),
            prepareRequest("refYtReady", TakeoutState.YT_READY).getId(),
            prepareRequest("refCancel", TakeoutState.CANCELLED).getId(),
            prepareRequest("refReady", TakeoutState.READY).getId(),
            -1 // non-found request
        ).stream().map(String::valueOf).collect(Collectors.toList());


        when(ytHelper.getYtClient().list(any())).thenReturn(ytTables);

        cleanupJob.takeoutCleanupYt();

        ArgumentCaptor<YPath> removeCaptor = ArgumentCaptor.forClass(YPath.class);
        verify(ytHelper.getYtClient(), times(3)).remove(removeCaptor.capture());

        assertEquals(
            Set.of(
                ytHelper.getTakeoutPath().child(ytTables.get(2)),
                ytHelper.getTakeoutPath().child(ytTables.get(3)),
                ytHelper.getTakeoutPath().child("-1")
            ),
            new HashSet<>(removeCaptor.getAllValues())
        );
    }

    private TakeoutRequest prepareRequest(String ref, TakeoutState state) {
        takeoutService.saveTakeoutRequest(TakeoutRequest.generate("refNew", TakeoutType.UID).withData(Map.of(
            TakeoutParam.USER_ID.getCode(), "123123123"
        )));
        return takeoutService.changeStateSafe(ref, state);
    }
}

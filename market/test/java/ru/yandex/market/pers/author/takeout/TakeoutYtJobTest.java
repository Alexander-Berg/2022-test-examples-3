package ru.yandex.market.pers.author.takeout;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.author.PersAuthorTest;
import ru.yandex.market.pers.author.takeout.model.TakeoutParam;
import ru.yandex.market.pers.author.takeout.model.TakeoutRequest;
import ru.yandex.market.pers.author.takeout.model.TakeoutState;
import ru.yandex.market.pers.author.takeout.model.TakeoutType;
import ru.yandex.market.pers.author.yt.YtHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 15.11.2021
 */
public class TakeoutYtJobTest extends PersAuthorTest {

    @Autowired
    private TakeoutYtJob takeoutYtJob;

    @Autowired
    private TakeoutService takeoutService;

    @Autowired
    private YtHelper ytHelper;

    @Test
    public void testPrepareYql() {
        TakeoutRequest request = TakeoutRequest.generate("ref", TakeoutType.UID)
            .withData(Map.of(TakeoutParam.USER_ID.getCode(), "1234567890"));

        String result = TakeoutYtJob.prepareYql("Some code\n$uid_filter = 0; --UID_FILTER;\nOther code", request);
        assertEquals("Some code\n$uid_filter = 1234567890;\nOther code", result);
    }

    @Test
    public void testCheckRequest() {
        TakeoutRequest request = TakeoutRequest.generate("ref", TakeoutType.UID)
            .withData(Map.of(TakeoutParam.USER_ID.getCode(), "1234567890"));

        TakeoutRequest invalidRequest = TakeoutRequest.generate("ref", TakeoutType.MODEL);
        invalidRequest.setData(Map.of(TakeoutParam.USER_ID.getCode(), "1234567890"));

        assertTrue(TakeoutYtJob.checkParams(request));
        assertFalse(TakeoutYtJob.checkParams(invalidRequest));
    }

    @Test
    public void testProcessFineUid() {
        takeoutService.saveTakeoutRequest(TakeoutRequest
            .generate("ref", TakeoutType.UID)
            .withData(Map.of(TakeoutParam.USER_ID.getCode(), "1234567890"))
        );

        takeoutService.saveTakeoutRequest(TakeoutRequest
            .generate("ref2", TakeoutType.UID)
            .withData(Map.of(TakeoutParam.USER_ID.getCode(), "43"))
        );

        takeoutYtJob.takeoutToYt();

        TakeoutRequest statusOk = takeoutService.getStatus("ref");
        assertEquals(TakeoutState.YT_READY, statusOk.getState());

        TakeoutRequest statusNew = takeoutService.getStatus("ref2");
        assertEquals(TakeoutState.NEW, statusNew.getState());

        // check jdbcTemplate was called properly
        ArgumentCaptor<String> yqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(ytHelper.getYqlJdbcTemplate(), times(1)).update(yqlCaptor.capture());

        assertTrue(yqlCaptor.getValue().contains("$uid_filter = 1234567890;"));
        assertTrue(yqlCaptor.getValue().contains("select * from $uid_result"));
        assertTrue(yqlCaptor.getValue().contains("insert into `" + ytHelper.getTakeoutPath() + "/" + statusOk.getId() + "`"));
    }

    @Test
    public void testProcessWithParamFailUid() {
        takeoutService.saveTakeoutRequest(TakeoutRequest
            .generate("ref", TakeoutType.UID)
            .withData(Map.of(TakeoutParam.MODEL_ID.getCode(), "1234567890"))
        );

        takeoutService.saveTakeoutRequest(TakeoutRequest
            .generate("ref2", TakeoutType.UID)
            .withData(Map.of(TakeoutParam.USER_ID.getCode(), "43"))
        );

        takeoutYtJob.takeoutToYt();

        TakeoutRequest statusFail = takeoutService.getStatus("ref");
        assertEquals(TakeoutState.FAILED, statusFail.getState());
        assertEquals("Illegal data", statusFail.getData().get(TakeoutService.KEY_FAIL_REASON));

        TakeoutRequest statusNew = takeoutService.getStatus("ref2");
        assertEquals(TakeoutState.NEW, statusNew.getState());
    }

    @Test
    public void testIgnoreProcessingFreshUid() {
        takeoutService.saveTakeoutRequest(TakeoutRequest
            .generate("ref", TakeoutType.UID)
            .withData(Map.of(TakeoutParam.USER_ID.getCode(), "1234567890"))
        );

        takeoutService.changeStateSafe("ref", TakeoutState.PROCESSING);

        takeoutYtJob.takeoutToYt();

        TakeoutRequest statusOk = takeoutService.getStatus("ref");
        assertEquals(TakeoutState.PROCESSING, statusOk.getState());

        verify(ytHelper.getYqlJdbcTemplate(), times(0)).update(anyString());
    }

    @Test
    public void testProcessProcessingOldUid() {
        takeoutService.saveTakeoutRequest(TakeoutRequest
            .generate("ref", TakeoutType.UID)
            .withData(Map.of(TakeoutParam.USER_ID.getCode(), "1234567890"))
        );

        takeoutService.changeStateSafe("ref", TakeoutState.PROCESSING);

        jdbcTemplate.update(
            "update pers.takeout set upd_time = now() - make_interval(hours := ?)",
            TakeoutService.LOCK_TO_PROCESS_TIMEOUT_HOUR + 1
        );

        takeoutYtJob.takeoutToYt();
        verify(ytHelper.getYqlJdbcTemplate(), times(0)).update(anyString());

        takeoutYtJob.takeoutResetOldProcessing();
        takeoutYtJob.takeoutToYt();

        TakeoutRequest statusOk = takeoutService.getStatus("ref");
        assertEquals(TakeoutState.YT_READY, statusOk.getState());

        verify(ytHelper.getYqlJdbcTemplate(), times(1)).update(anyString());
    }

    @Test
    public void testIgnoreAnyOtherStateOldUid() {
        for (TakeoutState state : TakeoutState.values()) {
            if (state == TakeoutState.NEW || state == TakeoutState.PROCESSING) {
                continue;
            }

            takeoutService.saveTakeoutRequest(TakeoutRequest
                .generate("ref-" + state.name(), TakeoutType.UID)
                .withData(Map.of(TakeoutParam.USER_ID.getCode(), "1234567890"))
            );

            takeoutService.changeStateSafe("ref-" + state.name(), state);
        }

        jdbcTemplate.update(
            "update pers.takeout set upd_time = now() - make_interval(hours := ?)",
            TakeoutService.LOCK_TO_PROCESS_TIMEOUT_HOUR + 1
        );

        takeoutYtJob.takeoutResetOldProcessing();
        takeoutYtJob.takeoutToYt();

        for (TakeoutState state : TakeoutState.values()) {
            if (state == TakeoutState.NEW || state == TakeoutState.PROCESSING) {
                continue;
            }

            TakeoutRequest statusOk = takeoutService.getStatus("ref-" + state.name());
            assertEquals(state, statusOk.getState());
        }

        verify(ytHelper.getYqlJdbcTemplate(), times(0)).update(anyString());
    }

    @Test
    public void testProcessOverloaded() {
        takeoutService.saveTakeoutRequest(TakeoutRequest
            .generate("ref", TakeoutType.UID)
            .withData(Map.of(TakeoutParam.USER_ID.getCode(), "1234567890"))
        );

        takeoutYtJob.getQueue().add(() -> TakeoutRequest.unknown("no way"));

        takeoutYtJob.takeoutToYt();

        assertEquals(TakeoutState.NEW, takeoutService.getStatus("ref").getState());
        verify(ytHelper.getYqlJdbcTemplate(), times(0)).update(anyString());

        // clear queue - should work fine now
        takeoutYtJob.getQueue().clear();

        takeoutYtJob.takeoutToYt();

        assertEquals(TakeoutState.YT_READY, takeoutService.getStatus("ref").getState());
        verify(ytHelper.getYqlJdbcTemplate(), times(1)).update(anyString());
    }

}

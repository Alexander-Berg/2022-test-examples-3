package ru.yandex.market.pers.author.takeout;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.author.PersAuthorTest;
import ru.yandex.market.pers.author.mock.mvc.TakeoutPassportControllerMvcMocks;
import ru.yandex.market.pers.author.takeout.model.TakeoutParam;
import ru.yandex.market.pers.author.takeout.model.TakeoutState;
import ru.yandex.market.pers.author.takeout.model.TakeoutStatusDto;
import ru.yandex.market.pers.tvm.TvmChecker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.isNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 29.10.2021
 */
public class TakeoutPassportControllerTest extends PersAuthorTest {
    public static final int UID = 3987713;
    @Autowired
    private TakeoutPassportControllerMvcMocks takeoutMvc;

    @Autowired
    private TvmChecker tvmChecker;

    @Autowired
    private TakeoutService takeoutService;

    @BeforeEach
    private void init() {
        Mockito.doThrow(new IllegalArgumentException())
            .when(tvmChecker).checkTvm(isNull(), anyList());
    }

    @Test
    public void testTvmUsage() {
        assertTrue(takeoutMvc.getTakeoutStatusWithToken("ref", null, status().is4xxClientError())
            .contains("TVM check failed"));
        takeoutMvc.getTakeoutStatusWithToken("ref", "any", status().is2xxSuccessful());
    }

    @Test
    public void testStatusUnexpected() {
        TakeoutStatusDto status = takeoutMvc.getTakeoutStatus("ref");
        assertEquals(status.getState(), TakeoutState.UNKNOWN.toString());
        assertEquals(status.getRef(), "ref");
        assertTrue(status.isFinished());
        assertFalse(status.isReady());

        // try to cancel - fails, since record is not known to context
        assertEquals(TakeoutState.UNKNOWN.toString(), takeoutMvc.cancelTakeout("ref").getState());
    }

    @Test
    public void testUidTakeoutWithRef() {
        TakeoutStatusDto created = takeoutMvc.startUidTakeout(UID, "ref");
        assertEquals(TakeoutState.NEW.toString(), created.getState());
        assertEquals("ref", created.getRef());
        assertFalse(created.isFinished());
        assertFalse(created.isReady());


        TakeoutStatusDto status = takeoutMvc.getTakeoutStatus("ref");
        assertEquals(status.getState(), TakeoutState.NEW.toString());
        assertEquals(status.getRef(), "ref");
        assertFalse(status.isFinished());
        assertFalse(status.isReady());

        // try to cancel - works fine
        assertEquals(TakeoutState.CANCELLED.toString(), takeoutMvc.cancelTakeout("ref").getState());

        // try to cancel a cancel - does not work
        takeoutService.changeStateSafe("ref", TakeoutState.PROCESSING);
        assertEquals(TakeoutState.CANCELLED, takeoutService.getStatus("ref").getState());
    }

    @Test
    public void testUidTakeoutWithNoRef() {
        TakeoutStatusDto created = takeoutMvc.startUidTakeout(UID, null);
        assertEquals(TakeoutState.NEW.toString(), created.getState());
        assertNotNull(created.getRef());
        assertFalse(created.isFinished());
        assertFalse(created.isReady());


        TakeoutStatusDto status = takeoutMvc.getTakeoutStatus(created.getRef());
        assertEquals(status.getState(), TakeoutState.NEW.toString());
        assertEquals(status.getRef(), created.getRef());
        assertFalse(status.isFinished());
        assertFalse(status.isReady());

        // try to cancel - works fine
        assertEquals(TakeoutState.CANCELLED.toString(), takeoutMvc.cancelTakeout(created.getRef()).getState());
    }

    @Test
    public void testUidTakeoutTwice() {
        TakeoutStatusDto created = takeoutMvc.startUidTakeout(UID, "ref");
        assertEquals(TakeoutState.NEW.toString(), created.getState());
        takeoutService.changeStateSafe("ref", TakeoutState.PROCESSING);

        TakeoutStatusDto createdAgain = takeoutMvc.startUidTakeout(UID, "ref");
        assertEquals(TakeoutState.PROCESSING.toString(), createdAgain.getState());

        TakeoutStatusDto createdOther = takeoutMvc.startUidTakeout(UID, "ref2");
        assertEquals(TakeoutState.NEW.toString(), createdOther.getState());

        List<String> allRefs = jdbcTemplate.queryForList("select ref from pers.takeout t order by ref", String.class);

        assertEquals(List.of("ref", "ref2"), allRefs);
        assertEquals(TakeoutState.PROCESSING, takeoutService.getStatus("ref").getState());
        assertEquals(TakeoutState.NEW, takeoutService.getStatus("ref2").getState());
    }

    @Test
    public void testLinkTakeoutWithRef() {
        TakeoutStatusDto created = takeoutMvc.startTakeoutByLink("ref", "http://market.yandex.ru/product/12321");
        assertEquals(TakeoutState.NEW.toString(), created.getState());
        assertEquals("ref", created.getRef());
        assertFalse(created.isFinished());
        assertFalse(created.isReady());

        TakeoutStatusDto status = takeoutMvc.getTakeoutStatus("ref");
        assertEquals(status.getState(), TakeoutState.NEW.toString());
        assertEquals(status.getRef(), "ref");
        assertFalse(status.isFinished());
        assertFalse(status.isReady());
        assertEquals(Map.of(TakeoutParam.MODEL_ID.getCode(), "12321"), takeoutService.getStatus("ref").getData());
    }

    @Test
    public void testLinkTakeoutInvalid() {
        TakeoutStatusDto created = takeoutMvc.startTakeoutByLink("ref", "http://market.yandex.ru/product/");
        assertEquals(TakeoutState.INVALID_PARAMS.toString(), created.getState());
        assertEquals("ref", created.getRef());
        assertTrue(created.isFinished());
        assertFalse(created.isReady());

        TakeoutStatusDto status = takeoutMvc.getTakeoutStatus("ref");
        assertEquals(status.getState(), TakeoutState.UNKNOWN.toString());
        assertEquals(status.getRef(), "ref");
        assertTrue(status.isFinished());
        assertFalse(status.isReady());
    }

}

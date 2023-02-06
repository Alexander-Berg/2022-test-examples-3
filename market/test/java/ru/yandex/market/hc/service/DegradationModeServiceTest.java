package ru.yandex.market.hc.service;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.hc.entity.DegradationConfig;
import ru.yandex.market.hc.entity.DegradationModes;
import ru.yandex.market.hc.entity.KeyEntity;
import ru.yandex.market.hc.entity.KeyType;
import ru.yandex.market.hc.entity.StateEntry;
import ru.yandex.market.hc.entity.Status;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Created by aproskriakov on 10/5/21
 */
@RunWith(SpringJUnit4ClassRunner.class)
public class DegradationModeServiceTest {

    @MockBean
    private MemcachedService memcachedService;
    @MockBean
    private DegradationConfigService degradationConfigService;
    private DegradationModeService dmService;
    private DegradationModes degradationModes;

    private final static String TEST_KEY = "testCheck";
    private final static int UPDATE_DEGR_MODE_PERIOD = 300;


    @Before
    public void setUp() {
        Map <KeyEntity, Integer> degradationModesMap = new ConcurrentHashMap<>();
        KeyEntity keyEntity = KeyEntity.builder()
                .name(TEST_KEY)
                .type(KeyType.FIXED)
                .build();
        degradationModesMap.put(keyEntity, 0);
        degradationModes = new DegradationModes(degradationModesMap);
        dmService = new DegradationModeService(memcachedService, degradationModes, degradationConfigService);
    }


    @Test
    public void testUpdateDm_Crit_OneTime() throws IOException {
        Status entryStatus = Status.CRIT;
        long entryTimestamp = Instant.now().getEpochSecond();
        int dcUpdatePeriod = UPDATE_DEGR_MODE_PERIOD;
        Set<Integer> dcDegrModes = Stream.of(10, 30, 90).collect(Collectors.toSet());

        testUpdateDM(entryStatus, 0, entryTimestamp, dcUpdatePeriod, dcDegrModes, 10);
        testUpdateDM(entryStatus, 10, entryTimestamp, dcUpdatePeriod, dcDegrModes, 30);
        testUpdateDM(entryStatus, 30, entryTimestamp, dcUpdatePeriod, dcDegrModes, 90);
        testUpdateDM(entryStatus, 60, entryTimestamp, dcUpdatePeriod, dcDegrModes, 90);
        testUpdateDM(entryStatus, 90, entryTimestamp, dcUpdatePeriod, dcDegrModes, 90);

        dcDegrModes = Stream.of(10, 30, 60, 90).collect(Collectors.toSet());
        testUpdateDM(entryStatus, -20, entryTimestamp, dcUpdatePeriod, dcDegrModes, 10);
        testUpdateDM(entryStatus, 0, entryTimestamp, dcUpdatePeriod, dcDegrModes, 10);
        testUpdateDM(entryStatus, 10, entryTimestamp, dcUpdatePeriod, dcDegrModes, 30);
        testUpdateDM(entryStatus, 30, entryTimestamp, dcUpdatePeriod, dcDegrModes, 60);
        testUpdateDM(entryStatus, 60, entryTimestamp, dcUpdatePeriod, dcDegrModes, 90);
        testUpdateDM(entryStatus, 50, entryTimestamp, dcUpdatePeriod, dcDegrModes, 90);
        testUpdateDM(entryStatus, 90, entryTimestamp, dcUpdatePeriod, dcDegrModes, 90);
        testUpdateDM(entryStatus, 300, entryTimestamp, dcUpdatePeriod, dcDegrModes, 90);
    }

    @Test
    public void testUpdateDm_Crit_TwoTimes() throws IOException {
        Status entryStatus = Status.CRIT;
        long entryTimestamp = Instant.now().getEpochSecond() - UPDATE_DEGR_MODE_PERIOD;
        int dcUpdatePeriod = UPDATE_DEGR_MODE_PERIOD;
        Set<Integer> dcDegrModes = Stream.of(10, 30, 90).collect(Collectors.toSet());


        testUpdateDM(entryStatus, 0, entryTimestamp, dcUpdatePeriod, dcDegrModes, 30);
        testUpdateDM(entryStatus, 10, entryTimestamp, dcUpdatePeriod, dcDegrModes, 90);
        testUpdateDM(entryStatus, 30, entryTimestamp, dcUpdatePeriod, dcDegrModes, 90);
        testUpdateDM(entryStatus, 90, entryTimestamp, dcUpdatePeriod, dcDegrModes, 90);
    }

    @Test
    public void testUpdateDm_Crit_ThreeTimes() throws IOException {
        Status entryStatus = Status.CRIT;
        long entryTimestamp = Instant.now().getEpochSecond() - 2 * UPDATE_DEGR_MODE_PERIOD;
        int dcUpdatePeriod = UPDATE_DEGR_MODE_PERIOD;
        Set<Integer> dcDegrModes = Stream.of(10, 30, 90).collect(Collectors.toSet());


        testUpdateDM(entryStatus, 0, entryTimestamp, dcUpdatePeriod, dcDegrModes, 90);
        testUpdateDM(entryStatus, 10, entryTimestamp, dcUpdatePeriod, dcDegrModes, 90);

        dcDegrModes = Stream.of(10, 30, 60, 90).collect(Collectors.toSet());
        testUpdateDM(entryStatus, 0, entryTimestamp, dcUpdatePeriod, dcDegrModes, 60);
        testUpdateDM(entryStatus, 4, entryTimestamp, dcUpdatePeriod, dcDegrModes, 60);
        testUpdateDM(entryStatus, 10, entryTimestamp, dcUpdatePeriod, dcDegrModes, 90);
        testUpdateDM(entryStatus, 30, entryTimestamp, dcUpdatePeriod, dcDegrModes, 90);
    }

    @Test
    public void testUpdateDm_Crit_FourTimes() throws IOException {
        Status entryStatus = Status.CRIT;
        long entryTimestamp = Instant.now().getEpochSecond() - 3 * UPDATE_DEGR_MODE_PERIOD;
        int dcUpdatePeriod = UPDATE_DEGR_MODE_PERIOD;
        Set<Integer> dcDegrModes = Stream.of(10, 30, 90).collect(Collectors.toSet());


        testUpdateDM(entryStatus, 0, entryTimestamp, dcUpdatePeriod, dcDegrModes, 90);
        testUpdateDM(entryStatus, 10, entryTimestamp, dcUpdatePeriod, dcDegrModes, 90);
    }

    @Test
    public void testUpdateDm_Ok_OneTimes() throws IOException {
        Status entryStatus = Status.OK;
        long entryTimestamp = Instant.now().getEpochSecond();
        int dcUpdatePeriod = UPDATE_DEGR_MODE_PERIOD;
        Set<Integer> dcDegrModes = Stream.of(10, 30, 90).collect(Collectors.toSet());


        testUpdateDM(entryStatus, 90, entryTimestamp, dcUpdatePeriod, dcDegrModes, 30);
        testUpdateDM(entryStatus, 30, entryTimestamp, dcUpdatePeriod, dcDegrModes, 10);
        testUpdateDM(entryStatus, 10, entryTimestamp, dcUpdatePeriod, dcDegrModes, 0);
        testUpdateDM(entryStatus, 5, entryTimestamp, dcUpdatePeriod, dcDegrModes, 0);
        testUpdateDM(entryStatus, 0, entryTimestamp, dcUpdatePeriod, dcDegrModes, 0);

        dcDegrModes = Stream.of(10, 30, 60, 90).collect(Collectors.toSet());
        testUpdateDM(entryStatus, 90, entryTimestamp, dcUpdatePeriod, dcDegrModes, 60);
        testUpdateDM(entryStatus, 60, entryTimestamp, dcUpdatePeriod, dcDegrModes, 30);
        testUpdateDM(entryStatus, 70, entryTimestamp, dcUpdatePeriod, dcDegrModes, 30);
        testUpdateDM(entryStatus, 30, entryTimestamp, dcUpdatePeriod, dcDegrModes, 10);
        testUpdateDM(entryStatus, 5, entryTimestamp, dcUpdatePeriod, dcDegrModes, 0);
        testUpdateDM(entryStatus, 0, entryTimestamp, dcUpdatePeriod, dcDegrModes, 0);
    }

    @Test
    public void testUpdateDm_Ok_TwoTimes() throws IOException {
        Status entryStatus = Status.OK;
        long entryTimestamp = Instant.now().getEpochSecond() - UPDATE_DEGR_MODE_PERIOD;
        int dcUpdatePeriod = UPDATE_DEGR_MODE_PERIOD;
        Set<Integer> dcDegrModes = Stream.of(10, 30, 90).collect(Collectors.toSet());


        testUpdateDM(entryStatus, 90, entryTimestamp, dcUpdatePeriod, dcDegrModes, 10);
        testUpdateDM(entryStatus, 30, entryTimestamp, dcUpdatePeriod, dcDegrModes, 0);
        testUpdateDM(entryStatus, 10, entryTimestamp, dcUpdatePeriod, dcDegrModes, 0);
    }

    @Test
    public void testUpdateDm_Ok_ThreeTimes() throws IOException {
        Status entryStatus = Status.OK;
        long entryTimestamp = Instant.now().getEpochSecond() - 2 * UPDATE_DEGR_MODE_PERIOD;
        int dcUpdatePeriod = UPDATE_DEGR_MODE_PERIOD;
        Set<Integer> dcDegrModes = Stream.of(10, 30, 90).collect(Collectors.toSet());

        testUpdateDM(entryStatus, 90, entryTimestamp, dcUpdatePeriod, dcDegrModes, 0);
        testUpdateDM(entryStatus, 30, entryTimestamp, dcUpdatePeriod, dcDegrModes, 0);
        testUpdateDM(entryStatus, 10, entryTimestamp, dcUpdatePeriod, dcDegrModes, 0);

        dcDegrModes = Stream.of(10, 30, 60, 90).collect(Collectors.toSet());
        testUpdateDM(entryStatus, 90, entryTimestamp, dcUpdatePeriod, dcDegrModes, 10);
        testUpdateDM(entryStatus, 80, entryTimestamp, dcUpdatePeriod, dcDegrModes, 10);
        testUpdateDM(entryStatus, 60, entryTimestamp, dcUpdatePeriod, dcDegrModes, 0);
        testUpdateDM(entryStatus, 50, entryTimestamp, dcUpdatePeriod, dcDegrModes, 0);
    }

    @Test
    public void testUpdateDm_Ok_FourTimes() throws IOException {
        Status entryStatus = Status.OK;
        long entryTimestamp = Instant.now().getEpochSecond() - 3 * UPDATE_DEGR_MODE_PERIOD;
        int dcUpdatePeriod = UPDATE_DEGR_MODE_PERIOD;
        Set<Integer> dcDegrModes = Stream.of(10, 30, 90).collect(Collectors.toSet());

        testUpdateDM(entryStatus, 90, entryTimestamp, dcUpdatePeriod, dcDegrModes,0);
        testUpdateDM(entryStatus, 30, entryTimestamp, dcUpdatePeriod, dcDegrModes, 0);
        testUpdateDM(entryStatus, 10, entryTimestamp, dcUpdatePeriod, dcDegrModes, 0);
    }

    @Test
    public void testUpdateDm_ManualOn() throws IOException {
        Status entryStatus = Status.OK;
        long entryTimestamp = Instant.now().getEpochSecond();
        int dcUpdatePeriod = UPDATE_DEGR_MODE_PERIOD;
        Set<Integer> dcDegrModes = Stream.of(10, 30, 90).collect(Collectors.toSet());

        testUpdateDM(entryStatus, 90, entryTimestamp, dcUpdatePeriod, dcDegrModes,0, 0);
        testUpdateDM(entryStatus, 90, entryTimestamp, dcUpdatePeriod, dcDegrModes,40, 40);
        testUpdateDM(entryStatus, 90, entryTimestamp, dcUpdatePeriod, dcDegrModes,100, 100);
    }

    @Test
    public void testUpdateDm_ManualOff() throws IOException {
        Status entryStatus = Status.OK;
        long entryTimestamp = Instant.now().getEpochSecond();
        int dcUpdatePeriod = UPDATE_DEGR_MODE_PERIOD;
        Set<Integer> dcDegrModes = Stream.of(10, 30, 90).collect(Collectors.toSet());

        testUpdateDM(entryStatus, 90, entryTimestamp, dcUpdatePeriod, dcDegrModes, -1, 30);
        testUpdateDM(entryStatus, 30, entryTimestamp, dcUpdatePeriod, dcDegrModes, -1, 10);
        testUpdateDM(entryStatus, 10, entryTimestamp, dcUpdatePeriod, dcDegrModes, -1, 0);

        testUpdateDM(entryStatus, 90, entryTimestamp, dcUpdatePeriod, dcDegrModes, null, 30);
        testUpdateDM(entryStatus, 30, entryTimestamp, dcUpdatePeriod, dcDegrModes, null, 10);
        testUpdateDM(entryStatus, 10, entryTimestamp, dcUpdatePeriod, dcDegrModes, null, 0);
    }

    private void testUpdateDM(Status entryStatus, int entryDM, long entryTimestamp, int dcUpdatePeriod,
                              Set<Integer> dcDegrModes, int expectedDm) throws IOException {
        testUpdateDM(entryStatus, entryDM, entryTimestamp, dcUpdatePeriod, dcDegrModes, -1, expectedDm);
    }

    private void testUpdateDM(Status entryStatus, int entryDM, long entryTimestamp, int dcUpdatePeriod,
                              Set<Integer> dcDegrModes, Integer manualDm, int expectedDm) throws IOException {
        StateEntry stateEntry = StateEntry.builder()
                .status(entryStatus)
                .degradationMode(entryDM)
                .timestamp(entryTimestamp)
                .build();
        DegradationConfig dc = DegradationConfig.builder()
                .updatePeriod(dcUpdatePeriod)
                .degradationModes(dcDegrModes)
                .manualDegradationMode(manualDm)
                .build();
        when(memcachedService.getStateEntry(TEST_KEY)).thenReturn(stateEntry);
        when(degradationConfigService.getOrDefault(TEST_KEY)).thenReturn(dc);

        dmService.updateDegradationModes();

        assertEquals(expectedDm, degradationModes.get(TEST_KEY).intValue());
    }
}

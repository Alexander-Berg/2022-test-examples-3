package ru.yandex.market.wms.autostart.autostartlogic.dao;

import java.util.List;
import java.util.Map;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.wms.autostart.autostartlogic.dao.entitiy.WaveLogEntity;
import ru.yandex.market.wms.common.spring.IntegrationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class WaveLogDaoTest extends IntegrationTest {
    @Autowired
    private WaveLogDao waveLogDao;

    @Test
    @DatabaseSetup("/controller/log/get-last-waves/db/before.xml")
    public void testOffsetLimit() {
        Map<Integer, Integer> expectedId = Map.of(0, 4, 1, 3, 2, 2, 3, 1);
        int currentPage = 0;
        assertEquals(4, waveLogDao.getNumberOfRows());
        for (; currentPage < 4; currentPage++) {
            List<WaveLogEntity> actualWaves = waveLogDao.getLastWaveLogs(1, currentPage);
            assertEquals(1, actualWaves.size());
            assertEquals(expectedId.get(currentPage), actualWaves.get(0).getId());
        }
        List<WaveLogEntity> actualWaves = waveLogDao.getLastWaveLogs(1, currentPage);
        assertEquals(0, actualWaves.size());
    }
}

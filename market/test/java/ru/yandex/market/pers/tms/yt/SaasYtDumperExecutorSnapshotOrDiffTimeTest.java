package ru.yandex.market.pers.tms.yt;

import java.sql.Timestamp;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.pers.tms.yt.saas.SaasIndexDumperService;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class SaasYtDumperExecutorSnapshotOrDiffTimeTest extends MockedPersTmsTest {

    @Autowired
    SaasIndexDumperService saasIndexDumperService;
    SaasIndexDumperService saasIndexDumperServiceSpy;

    @Before
    public void setUp() throws Exception {
        saasIndexDumperServiceSpy = spy(saasIndexDumperService);
    }

    @Test
    public void testIsSnapshotTime() throws Exception {
        assertIsSnapshotTime("2018-04-26 18:30:10.0", 24 * 60 * 60 * 1000, true);
        assertIsSnapshotTime("2018-04-26 18:30:10.0", 19 * 60 * 60 * 1000, true);
        assertIsSnapshotTime("2018-04-26 18:30:10.0", 18 * 60 * 60 * 1000, false);
        assertIsSnapshotTime("2018-04-26 01:30:10.0", 2 * 60 * 60 * 1000, true);
        assertIsSnapshotTime("2018-04-26 01:30:10.0", 1 * 60 * 60 * 1000, false);
    }

    public void assertIsSnapshotTime(String currentTd, long deltaFromLastSnapshot, boolean isSnapshot) throws Exception {
        long current = Timestamp.valueOf(currentTd).getTime();
        long lastIndex = current - deltaFromLastSnapshot;

        doReturn(current).when(saasIndexDumperServiceSpy).getCurrentTime();
        doReturn(lastIndex).when(saasIndexDumperServiceSpy).getLastTimeIndex();

        assertEquals(saasIndexDumperServiceSpy.isSnapshot(), isSnapshot);
    }

}

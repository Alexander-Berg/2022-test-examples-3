package ru.yandex.market.tsum.pipelines.ott.jobs;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import com.google.common.io.CharStreams;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import ru.yandex.market.tsum.pipelines.ott.resources.OttReleaseChangeLog;
import ru.yandex.market.tsum.pipelines.ott.resources.ReleaseBranchInfo;
import ru.yandex.market.tsum.pipelines.ott.resources.StartReleaseInfo;
import ru.yandex.market.tsum.pipelines.ott.resources.TicketDeployInfo;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OttUpdateChangeLogJobTest {
    @InjectMocks
    OttUpdateChangeLogJob job = new OttUpdateChangeLogJob();

    @Mock
    private OttReleaseChangeLog releaseChangeLog;

    @Mock
    private ReleaseBranchInfo releaseBranchInfo;

    @Mock
    private StartReleaseInfo startReleaseInfo;

    @Before
    public void setUp() {
        when(releaseChangeLog.getTicketReleaseInfos()).thenReturn(List.of(
            new TicketDeployInfo("OTT-11111", "title 1", "OTT-77777", 0, Set.of(), Set.of(), Set.of()),
            new TicketDeployInfo("OTT-22222", "title 2", "OTT-77777", 0, Set.of(), Set.of(), Set.of()),
            new TicketDeployInfo("OTT-33333", "title 3", "OTT-77777", 0, Set.of(), Set.of(), Set.of())
        ));

        ReflectionTestUtils.setField(job, "startrekUrl", "https://st.yandex-team.ru");
    }

    @Test
    public void releaseChangeLogTest() {
        when(releaseBranchInfo.getReleaseBuildName()).thenReturn("release_22220101_OTT-77777");

        String initial = loadFile("resources/initialChangelog.md");
        String expected = loadFile("resources/releaseChangelog.md");
        String updated = job.updateChangeLog(initial);
        Assert.assertEquals(expected, updated);
    }

    @Test
    public void previousHotfixChangeLogTest() {
        when(startReleaseInfo.getReleaseTicket()).thenReturn("OTT-11017");
        when(startReleaseInfo.isHotfix()).thenReturn(Boolean.TRUE);

        when(releaseBranchInfo.getReleaseBuildName()).thenReturn("release_20200512_OTT-11017_hf3");

        String initial = loadFile("resources/initialChangelog.md");
        String expected = loadFile("resources/prevHotfixChangelog.md");
        String updated = job.updateChangeLog(initial);
        Assert.assertEquals(expected, updated);
    }

    @Test
    public void lastHotfixChangeLogTest() {
        when(startReleaseInfo.getReleaseTicket()).thenReturn("OTT-11436");
        when(startReleaseInfo.isHotfix()).thenReturn(Boolean.TRUE);

        when(releaseBranchInfo.getReleaseBuildName()).thenReturn("release_20200526_OTT-11436_hf1");

        String initial = loadFile("resources/initialChangelog.md");
        String expected = loadFile("resources/lastHotfixChangelog.md");
        String updated = job.updateChangeLog(initial);
        Assert.assertEquals(expected, updated);
    }

    private static String loadFile(String path) {
        try {
            return CharStreams.toString(
                new InputStreamReader(OttUpdateChangeLogJobTest.class.getResourceAsStream(path), StandardCharsets.UTF_8)
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

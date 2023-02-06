package ru.yandex.market.tsum.pipelines.ott.jobs;

import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNLogEntry;

import ru.yandex.market.tsum.clients.arcadia.RootArcadiaClient;
import ru.yandex.market.tsum.pipelines.ott.resources.ReleaseBranchInfo;
import ru.yandex.market.tsum.pipelines.ott.resources.StartReleaseInfo;
import ru.yandex.market.tsum.pipelines.startrek.config.SvnCopyConfig;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OttArcadiaSvnReleaseBranchJobTest {
    @InjectMocks
    OttArcadiaSvnReleaseBranchJob job = new OttArcadiaSvnReleaseBranchJob();

    @Mock
    private StartReleaseInfo startReleaseInfo;

    @Mock
    private SvnCopyConfig svnCopyConfig;

    @Mock
    private RootArcadiaClient rootArcadiaClient;

    @Mock
    private SVNLogEntry svnLogEntry;

    @Before
    public void setUp() {
        when(svnCopyConfig.getReleaseBranchesPath()).thenReturn("/branches/ott/backend/releases");
        when(startReleaseInfo.getReleaseTicket()).thenReturn("OTT-8541");
        when(svnLogEntry.getRevision()).thenReturn(123456L);
        when(rootArcadiaClient.getHead(anyString())).thenReturn(svnLogEntry);
        Collection<SVNDirEntry> entries = List.of(
            mockedEntry("release_20191211_OTT-9074"),
            mockedEntry("release_20191213_OTT-9114"),
            mockedEntry("release_20191213_OTT-9114_hf1")
        );
        when(rootArcadiaClient.getDirEntries(anyString())).thenReturn(entries);
    }

    @Test
    public void newReleaseBranchTest() {
        ReleaseBranchInfo releaseBranchInfo = job.buildReleaseBranch(
            startReleaseInfo.getReleaseTicket(),
            "20200207"
        );

        Assert.assertEquals(
            "/branches/ott/backend/releases/release_20200207_OTT-8541",
            releaseBranchInfo.getReleaseBranch()
        );
        Assert.assertEquals(
            "/trunk",
            releaseBranchInfo.getPreviousBranch()
        );
        Assert.assertEquals(
            "release_20200207_OTT-8541",
            releaseBranchInfo.getReleaseBuildName()
        );
        Assert.assertEquals(
            "OTT-8541",
            releaseBranchInfo.getReleaseHumanName()
        );
    }

    @Test
    public void firstHotfixTest() {
        Collection<SVNDirEntry> entries = List.of(
            mockedEntry("release_20191211_OTT-9074"),
            mockedEntry("release_20191213_OTT-9114"),
            mockedEntry("release_20191213_OTT-9114_hf1"),
            mockedEntry("release_20200207_OTT-8541")
        );
        when(rootArcadiaClient.getDirEntries(anyString())).thenReturn(entries);

        ReleaseBranchInfo releaseBranchInfo = job.buildHotfixBranch(
            startReleaseInfo.getReleaseTicket(),
            startReleaseInfo.isRestart()
        );

        Assert.assertEquals(
            "/branches/ott/backend/releases/release_20200207_OTT-8541_hf1",
            releaseBranchInfo.getReleaseBranch()
        );
        Assert.assertEquals(
            "/branches/ott/backend/releases/release_20200207_OTT-8541",
            releaseBranchInfo.getPreviousBranch()
        );
        Assert.assertEquals(
            "release_20200207_OTT-8541_hf1",
            releaseBranchInfo.getReleaseBuildName()
        );
        Assert.assertEquals(
            "OTT-8541 hf1",
            releaseBranchInfo.getReleaseHumanName()
        );
    }

    @Test
    public void thirdHotfixTest() {
        Collection<SVNDirEntry> entries = List.of(
            mockedEntry("release_20191211_OTT-9074"),
            mockedEntry("release_20191213_OTT-9114"),
            mockedEntry("release_20191213_OTT-9114_hf1"),
            mockedEntry("release_20200207_OTT-8541"),
            mockedEntry("release_20200207_OTT-8541_hf1"),
            mockedEntry("release_20200207_OTT-8541_hf2")
        );
        when(rootArcadiaClient.getDirEntries(anyString())).thenReturn(entries);

        ReleaseBranchInfo releaseBranchInfo = job.buildHotfixBranch(
            startReleaseInfo.getReleaseTicket(),
            startReleaseInfo.isRestart()
        );

        Assert.assertEquals(
            "/branches/ott/backend/releases/release_20200207_OTT-8541_hf3",
            releaseBranchInfo.getReleaseBranch()
        );
        Assert.assertEquals(
            "/branches/ott/backend/releases/release_20200207_OTT-8541_hf2",
            releaseBranchInfo.getPreviousBranch()
        );
        Assert.assertEquals(
            "release_20200207_OTT-8541_hf3",
            releaseBranchInfo.getReleaseBuildName()
        );
        Assert.assertEquals(
            "OTT-8541 hf3",
            releaseBranchInfo.getReleaseHumanName()
        );
    }

    @Test
    public void testRestartRelease() {
        when(startReleaseInfo.isRestart()).thenReturn(true);

        Collection<SVNDirEntry> entries = List.of(
            mockedEntry("release_20191211_OTT-9074"),
            mockedEntry("release_20191213_OTT-9114"),
            mockedEntry("release_20191213_OTT-9114_hf1"),
            mockedEntry("release_20200207_OTT-8541")
        );
        when(rootArcadiaClient.getDirEntries(anyString())).thenReturn(entries);

        ReleaseBranchInfo releaseBranchInfo = job.buildReleaseBranchInfo(
            startReleaseInfo.getReleaseTicket(),
            startReleaseInfo.isHotfix(),
            startReleaseInfo.isRestart()
        );

        Assert.assertEquals(
            "/branches/ott/backend/releases/release_20200207_OTT-8541_rn1",
            releaseBranchInfo.getReleaseBranch()
        );
        Assert.assertEquals(
            "/branches/ott/backend/releases/release_20200207_OTT-8541",
            releaseBranchInfo.getPreviousBranch()
        );
        Assert.assertEquals(
            "release_20200207_OTT-8541_rn1",
            releaseBranchInfo.getReleaseBuildName()
        );
        Assert.assertEquals(
            "OTT-8541 rn1",
            releaseBranchInfo.getReleaseHumanName()
        );
    }

    @Test
    public void testDoubleRestartRelease() {
        when(startReleaseInfo.isRestart()).thenReturn(true);

        Collection<SVNDirEntry> entries = List.of(
            mockedEntry("release_20191211_OTT-9074"),
            mockedEntry("release_20191213_OTT-9114"),
            mockedEntry("release_20191213_OTT-9114_hf1"),
            mockedEntry("release_20200207_OTT-8541"),
            mockedEntry("release_20200207_OTT-8541_rn1")
        );
        when(rootArcadiaClient.getDirEntries(anyString())).thenReturn(entries);

        ReleaseBranchInfo releaseBranchInfo = job.buildReleaseBranchInfo(
            startReleaseInfo.getReleaseTicket(),
            startReleaseInfo.isHotfix(),
            startReleaseInfo.isRestart()
        );

        Assert.assertEquals(
            "/branches/ott/backend/releases/release_20200207_OTT-8541_rn2",
            releaseBranchInfo.getReleaseBranch()
        );
        Assert.assertEquals(
            "/branches/ott/backend/releases/release_20200207_OTT-8541_rn1",
            releaseBranchInfo.getPreviousBranch()
        );
        Assert.assertEquals(
            "release_20200207_OTT-8541_rn2",
            releaseBranchInfo.getReleaseBuildName()
        );
        Assert.assertEquals(
            "OTT-8541 rn2",
            releaseBranchInfo.getReleaseHumanName()
        );
    }

    @Test
    public void testRestartFirstHotfix() {
        when(startReleaseInfo.isHotfix()).thenReturn(true);
        when(startReleaseInfo.isRestart()).thenReturn(true);

        Collection<SVNDirEntry> entries = List.of(
            mockedEntry("release_20191211_OTT-9074"),
            mockedEntry("release_20191213_OTT-9114"),
            mockedEntry("release_20191213_OTT-9114_hf1"),
            mockedEntry("release_20200207_OTT-8541"),
            mockedEntry("release_20200207_OTT-8541_hf1")
        );
        when(rootArcadiaClient.getDirEntries(anyString())).thenReturn(entries);

        ReleaseBranchInfo releaseBranchInfo = job.buildReleaseBranchInfo(
            startReleaseInfo.getReleaseTicket(),
            startReleaseInfo.isHotfix(),
            startReleaseInfo.isRestart()
        );

        Assert.assertEquals(
            "/branches/ott/backend/releases/release_20200207_OTT-8541_hf1_rn1",
            releaseBranchInfo.getReleaseBranch()
        );
        Assert.assertEquals(
            "/branches/ott/backend/releases/release_20200207_OTT-8541_hf1",
            releaseBranchInfo.getPreviousBranch()
        );
        Assert.assertEquals(
            "release_20200207_OTT-8541_hf1_rn1",
            releaseBranchInfo.getReleaseBuildName()
        );
        Assert.assertEquals(
            "OTT-8541 hf1 rn1",
            releaseBranchInfo.getReleaseHumanName()
        );
    }

    @Test
    public void testRestartThirdHotfix() {
        when(startReleaseInfo.isHotfix()).thenReturn(true);
        when(startReleaseInfo.isRestart()).thenReturn(true);

        Collection<SVNDirEntry> entries = List.of(
            mockedEntry("release_20191211_OTT-9074"),
            mockedEntry("release_20191213_OTT-9114"),
            mockedEntry("release_20191213_OTT-9114_hf1"),
            mockedEntry("release_20200207_OTT-8541"),
            mockedEntry("release_20200207_OTT-8541_hf1"),
            mockedEntry("release_20200207_OTT-8541_hf2"),
            mockedEntry("release_20200207_OTT-8541_hf3")
        );
        when(rootArcadiaClient.getDirEntries(anyString())).thenReturn(entries);

        ReleaseBranchInfo releaseBranchInfo = job.buildReleaseBranchInfo(
            startReleaseInfo.getReleaseTicket(),
            startReleaseInfo.isHotfix(),
            startReleaseInfo.isRestart()
        );

        Assert.assertEquals(
            "/branches/ott/backend/releases/release_20200207_OTT-8541_hf3_rn1",
            releaseBranchInfo.getReleaseBranch()
        );
        Assert.assertEquals(
            "/branches/ott/backend/releases/release_20200207_OTT-8541_hf3",
            releaseBranchInfo.getPreviousBranch()
        );
        Assert.assertEquals(
            "release_20200207_OTT-8541_hf3_rn1",
            releaseBranchInfo.getReleaseBuildName()
        );
        Assert.assertEquals(
            "OTT-8541 hf3 rn1",
            releaseBranchInfo.getReleaseHumanName()
        );
    }

    @Test
    public void testDoubleRestartHotfix() {
        when(startReleaseInfo.isHotfix()).thenReturn(true);
        when(startReleaseInfo.isRestart()).thenReturn(true);

        Collection<SVNDirEntry> entries = List.of(
            mockedEntry("release_20191211_OTT-9074"),
            mockedEntry("release_20191213_OTT-9114"),
            mockedEntry("release_20191213_OTT-9114_hf1"),
            mockedEntry("release_20200207_OTT-8541"),
            mockedEntry("release_20200207_OTT-8541_hf1"),
            mockedEntry("release_20200207_OTT-8541_hf2"),
            mockedEntry("release_20200207_OTT-8541_hf3_rn1")
        );
        when(rootArcadiaClient.getDirEntries(anyString())).thenReturn(entries);

        ReleaseBranchInfo releaseBranchInfo = job.buildReleaseBranchInfo(
            startReleaseInfo.getReleaseTicket(),
            startReleaseInfo.isHotfix(),
            startReleaseInfo.isRestart()
        );

        Assert.assertEquals(
            "/branches/ott/backend/releases/release_20200207_OTT-8541_hf3_rn2",
            releaseBranchInfo.getReleaseBranch()
        );
        Assert.assertEquals(
            "/branches/ott/backend/releases/release_20200207_OTT-8541_hf3_rn1",
            releaseBranchInfo.getPreviousBranch()
        );
        Assert.assertEquals(
            "release_20200207_OTT-8541_hf3_rn2",
            releaseBranchInfo.getReleaseBuildName()
        );
        Assert.assertEquals(
            "OTT-8541 hf3 rn2",
            releaseBranchInfo.getReleaseHumanName()
        );
    }

    private static SVNDirEntry mockedEntry(String name) {
        SVNDirEntry svnDirEntry = Mockito.mock(SVNDirEntry.class);
        when(svnDirEntry.getName()).thenReturn(name);
        return svnDirEntry;
    }
}

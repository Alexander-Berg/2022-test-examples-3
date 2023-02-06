package ru.yandex.market.tsum.pipelines.common.jobs.arcadia;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.io.SVNRepository;

import ru.yandex.market.tsum.pipe.engine.definition.context.JobContext;
import ru.yandex.market.tsum.pipe.engine.runtime.helpers.TestJobContext;
import ru.yandex.market.tsum.pipelines.arcadia.ArcadiaStartReleaseJob;
import ru.yandex.market.tsum.pipelines.arcadia.resources.ArcadiaStartReleaseJobConfig;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 23.10.17
 */
public class ArcadiaStartReleaseJobTest {

    @Test
    public void getPreviousBranch() throws SVNException {
        String branchName = "2017.8.13";
        String alreadyCreatedBranch = "2017.8.15";

        JobContext context = new TestJobContext();
        String pipelineId = context.getPipeLaunch().getPipeId();
        String branchPath = "/arc/branches/market/idx/" + pipelineId + "/";

        ArcadiaStartReleaseJob arcadiaStartReleaseJob = new ArcadiaStartReleaseJob();
        arcadiaStartReleaseJob.setArcadiaStartReleaseJobConfig(
            new ArcadiaStartReleaseJobConfig(branchPath)
        );

        Optional<String> previousBranch = arcadiaStartReleaseJob.getPreviousBranch(
            Arrays.asList(
                dirEntry("2017.8.11", SVNNodeKind.DIR, "commit 1"),
                dirEntry(alreadyCreatedBranch, SVNNodeKind.DIR, ArcadiaStartReleaseJob.getCommitMessage(context)),
                dirEntry("2017.8.14", SVNNodeKind.FILE, "commit 2"),
                dirEntry(branchName, SVNNodeKind.DIR, "commit 3"),
                dirEntry("2017.8.17", SVNNodeKind.DIR, "commit 4")
            ), branchPath + alreadyCreatedBranch
        );

        Assert.assertEquals(branchPath + branchName, previousBranch.get());
    }

    @Test
    public void getPreviousBranchWithoutCreated() throws SVNException {
        String branchName = "2017.8.15";

        JobContext context = new TestJobContext();
        String pipelineId = context.getPipeLaunch().getPipeId();
        String branchPath = "/arc/branches/market/idx/" + pipelineId + "/";

        ArcadiaStartReleaseJob arcadiaStartReleaseJob = new ArcadiaStartReleaseJob();
        arcadiaStartReleaseJob.setArcadiaStartReleaseJobConfig(
            new ArcadiaStartReleaseJobConfig(branchPath)
        );

        Optional<String> previousBranch = arcadiaStartReleaseJob.getPreviousBranch(
            Arrays.asList(
                dirEntry("2017.8.11", SVNNodeKind.DIR, "commit 1"),
                dirEntry(branchName, SVNNodeKind.DIR, "commit 2"),
                dirEntry("2017.8.14", SVNNodeKind.FILE, "commit 3"),
                dirEntry("2017.8.13", SVNNodeKind.DIR, "commit 4")
            ), null
        );

        Assert.assertEquals(branchPath + branchName, previousBranch.get());
    }

    @Test
    public void getNewVersionName() {
        Assert.assertEquals(
            "2018.3.1",
            ArcadiaStartReleaseJob.getNewVersionName("2018.2.3",
                new GregorianCalendar(2018, 6, 1))
        );

        Assert.assertEquals(
            "2018.2.4",
            ArcadiaStartReleaseJob.getNewVersionName("2018.2.3",
                new GregorianCalendar(2018, 5, 1))
        );

        Assert.assertEquals(
            "2018.1.1",
            ArcadiaStartReleaseJob.getNewVersionName("2017.1.3",
                new GregorianCalendar(2018, 1, 1))
        );

        Assert.assertEquals(
            "2018.1.4",
            ArcadiaStartReleaseJob.getNewVersionName("2018.1.3 version description",
                new GregorianCalendar(2018, 1, 1))
        );
    }

    @Test
    public void getAlreadyCreatedBranch() throws SVNException {
        String alreadyCreatedBranch = "2017.8.15";
        String branchPath = "/arc/branches/market/idx/publisher";

        TestJobContext context = new TestJobContext();
        String commitMessage = ArcadiaStartReleaseJob.getCommitMessage(context);

        SVNRepository repository = Mockito.mock(SVNRepository.class);
        Mockito.when(repository.log(new String[]{alreadyCreatedBranch}, null, 0, -1, false, true))
            .thenReturn(Arrays.asList(
                logEntry(new Date(3), "this is not the message you are looking for"),
                logEntry(new Date(1), commitMessage),
                logEntry(new Date(2), "this is not the message you are looking for")
            ));
        Mockito.when(repository.log(new String[]{"2017.8.11"}, null, 0, -1, false, true))
            .thenReturn(Arrays.asList(
                logEntry(new Date(3), "this is not the message you are looking for"),
                logEntry(new Date(2), "this is not the message you are looking for"),
                logEntry(new Date(1), "this is not the message you are looking for")
            ));
        Mockito.when(repository.log(new String[]{"2017.8.14"}, null, 0, -1, false, true))
            .thenReturn(Collections.emptyList());

        ArcadiaStartReleaseJob arcadiaStartReleaseJob = new ArcadiaStartReleaseJob();
        arcadiaStartReleaseJob.setArcadiaStartReleaseJobConfig(
            new ArcadiaStartReleaseJobConfig(branchPath)
        );

        String gotCreatedBranch = arcadiaStartReleaseJob.getAlreadyCreatedBranch(
            Arrays.asList(
                dirEntry("2017.8.11", SVNNodeKind.DIR, "commit 1"),
                dirEntry("2017.8.14", SVNNodeKind.FILE, "commit 2"),
                dirEntry(alreadyCreatedBranch, SVNNodeKind.DIR, "this is not the message you are looking for")
            ),
            context,
            repository
        );

        Assert.assertEquals(branchPath + "/" + alreadyCreatedBranch, gotCreatedBranch);
    }

    private SVNLogEntry logEntry(Date date, String message) {
        return new SVNLogEntry(null, -1, "user42", date, message);
    }

    private SVNDirEntry dirEntry(String name, SVNNodeKind nodeKind, String commitMessage) {
        return new SVNDirEntry(null, null, name, nodeKind, 0, false, 0, null, null, commitMessage);
    }
}

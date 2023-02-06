package ru.yandex.market.tsum.sox.deploy;

import com.google.protobuf.Timestamp;
import nanny.tickets.Releases;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.impl.DefaultListF;
import ru.yandex.market.tsum.clients.conductor.ConductorPackageDetails;
import ru.yandex.market.tsum.clients.conductor.ConductorTask;
import ru.yandex.market.tsum.clients.sandbox.SandboxReleaseType;
import ru.yandex.startrek.client.model.Comment;
import ru.yandex.startrek.client.model.Issue;
import ru.yandex.startrek.client.model.UserRef;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 31/05/2017
 */
public class SoxDeployValidationServiceTest {
    @Test
    public void createConductorTaskDescription() {
        ConductorTask task = new ConductorTask();
        task.setBranch(ConductorTask.Branch.TESTING);
        task.setTicket(123);
        task.addAllPackages(Arrays.asList(
            new ConductorPackageDetails("package1", "1"),
            new ConductorPackageDetails("package2", "2"),
            new ConductorPackageDetails("soxPackage1", "1"),
            new ConductorPackageDetails("package3", "3")
        ));

        List<String> problems = new ArrayList<>();
        problems.add("problem1");
        problems.add("problem2");

        Set<String> soxPackages = new HashSet<>();
        soxPackages.add("soxPackage1");
        soxPackages.add("soxPackage2");

        String description = SoxDeployValidationService.createDescription(new ConductorTaskValidationParameters(task, soxPackages), problems);

        Assert.assertTrue(description.contains("В релизе в окружения testing через задачу в Conductor 123 (https://c.yandex-team.ru/tickets/123)"));
        Assert.assertTrue(description.contains("problem1") && description.contains("problem2"));
        Assert.assertTrue(
            description.contains("package1\t1 (Не Sox)") &&
                description.contains("package2\t2 (Не Sox)") &&
                description.contains("soxPackage1\t1 (Под Sox)") &&
                description.contains("package3\t3 (Не Sox)")
        );
    }

    @Test
    public void createSandboxTaskDescription() {
        List<Releases.SandboxResource> resources = Arrays.asList(
            Releases.SandboxResource.newBuilder().setType("RESOURCE_1").setId("1").build(),
            Releases.SandboxResource.newBuilder().setType("RESOURCE_2").setId("2").build(),
            Releases.SandboxResource.newBuilder().setType("SOX_RESOURCE_1").setId("3").build(),
            Releases.SandboxResource.newBuilder().setType("RESOURCE_3").setId("4").build()
        );

        Releases.SandboxRelease release = Releases.SandboxRelease.newBuilder()
            .setTaskId("456")
            .setCreationTime(Timestamp.newBuilder().setSeconds(new Date().getTime()).build())
            .setTitle("Cool title")
            .setReleaseType(SandboxReleaseType.STABLE.getSandboxName())
            .addAllResources(resources)
            .build();

        List<String> problems = new ArrayList<>();
        problems.add("problem1");
        problems.add("problem2");

        Set<String> soxResources = new HashSet<>();
        soxResources.add("SOX_RESOURCE_1");
        soxResources.add("SOX_RESOURCE_2");

        List<String> services = Arrays.asList("production_service_vla", "production_service_iva");

        String description = SoxDeployValidationService.createDescription(new NannyValidationParameters(release, services, soxResources, Collections.emptySet()), problems);

        Assert.assertTrue(description.contains("В релизе в окружения production_service_vla, production_service_iva через задачу в Sandbox 456 (https://sandbox.yandex-team.ru/task/456)"));
        Assert.assertTrue(description.contains("problem1") && description.contains("problem2"));
        Assert.assertTrue(
            description.contains("RESOURCE_1\t1 (Не Sox)") &&
                description.contains("RESOURCE_2\t2 (Не Sox)") &&
                description.contains("SOX_RESOURCE_1\t3 (Под Sox)") &&
                description.contains("RESOURCE_3\t4 (Не Sox)")
        );
    }

    @Test
    public void getApproversReasonsTest() throws Exception {
        SoxDeployValidationService soxDeployValidationService = Mockito.mock(SoxDeployValidationService.class);

        Field approveString = SoxDeployValidationService.class.getDeclaredField("approveString");
        approveString.setAccessible(true);
        approveString.set(soxDeployValidationService, "!SOXOK");

        Method getApprovedReasonsMethod = SoxDeployValidationService.class
            .getDeclaredMethod("getApprovedReasons", Issue.class);
        getApprovedReasonsMethod.setAccessible(true);


        String userLogin = "testUser";
        UserRef userRef = Mockito.mock(UserRef.class);
        Mockito.when(userRef.getLogin()).thenReturn(userLogin);

        String robotLogin = "robot-market-infra";
        UserRef robotRef = Mockito.mock(UserRef.class);
        Mockito.when(robotRef.getLogin()).thenReturn(robotLogin);

        String badRobotLogin = "teamcity";
        UserRef badRobotRef = Mockito.mock(UserRef.class);
        Mockito.when(badRobotRef.getLogin()).thenReturn(badRobotLogin);

        List<Comment> testCommentsList = new ArrayList<>();

        Comment commentByUser = Mockito.mock(Comment.class);
        Mockito.when(commentByUser.getCreatedBy()).thenReturn(userRef);
        Mockito.when(commentByUser.getText()).thenReturn(Option.of("!SOXOK"));
        testCommentsList.add(commentByUser);

        Comment commentRobotByRM = Mockito.mock(Comment.class);
        Mockito.when(commentRobotByRM.getCreatedBy()).thenReturn(robotRef);
        String releaseMasterLogin = "myReleaseMaster";
        Mockito.when(commentRobotByRM.getText()).thenReturn(Option.of("!SOXOK by release master кто:" + releaseMasterLogin));
        testCommentsList.add(commentRobotByRM);

        Comment commentRobotByAutotests = Mockito.mock(Comment.class);
        Mockito.when(commentRobotByAutotests.getCreatedBy()).thenReturn(robotRef);
        Mockito.when(commentRobotByAutotests.getText()).thenReturn(Option.of("!SOXOK by autotests"));
        testCommentsList.add(commentRobotByAutotests);

        Comment commentRobotByJugglerChecks = Mockito.mock(Comment.class);
        Mockito.when(commentRobotByJugglerChecks.getCreatedBy()).thenReturn(robotRef);
        Mockito.when(commentRobotByJugglerChecks.getText()).thenReturn(Option.of("!SOXOK by juggler checks"));
        testCommentsList.add(commentRobotByJugglerChecks);

        Comment commentRobotByReviewers = Mockito.mock(Comment.class);
        Mockito.when(commentRobotByReviewers.getCreatedBy()).thenReturn(robotRef);
        Mockito.when(commentRobotByReviewers.getText()).thenReturn(Option.of("!SOXOK by reviewers кто:user1, кто:user2, кто:user3"));
        testCommentsList.add(commentRobotByReviewers);

        Comment commentByBadRobot = Mockito.mock(Comment.class);
        Mockito.when(commentByBadRobot.getCreatedBy()).thenReturn(badRobotRef);
        String badUserLogin = "user4";
        Mockito.when(commentByBadRobot.getText()).thenReturn(Option.of("!SOXOK by release master кто:" + badUserLogin));
        testCommentsList.add(commentByBadRobot);

        Issue issue = Mockito.mock(Issue.class);
        ListF<Comment> comments = new DefaultListF(testCommentsList);
        Mockito.when(issue.getComments()).thenReturn(comments.iterator());

        Set<String> approvedUsers = (Set<String>) getApprovedReasonsMethod.invoke(soxDeployValidationService, issue);

        Assert.assertTrue(approvedUsers.contains("by " + userLogin));
        Assert.assertTrue(approvedUsers.contains("by " + releaseMasterLogin));
        Assert.assertTrue(approvedUsers.contains("by reviewers"));
        Assert.assertTrue(approvedUsers.contains("by autotests"));
        Assert.assertTrue(approvedUsers.contains("by juggler checks"));
        Assert.assertFalse(approvedUsers.contains("by " + badUserLogin));
        Assert.assertFalse(approvedUsers.contains("by " + badRobotLogin));

    }
}
package ru.yandex.market.tsum.clients.conductor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.tsum.clients.TestConductor;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 02.11.16
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestConductor.class})
@Ignore
public class ConductorClientTest {

    @Autowired
    private ConductorClient client;

    @Test
    public void getDoneTasks() throws Exception {
        List<ConductorTask> tasks = client.getDoneTasks(null, null, "cs", 3).get();
        tasks.forEach(System.out::println);
    }

    @Test
    public void getLastVersion() throws ExecutionException, InterruptedException {
        String versionName = client.getLastDeployedVersion("yandex-logshatter", ConductorBranch.STABLE, "ape");
        System.out.println(versionName);
    }

    @Test
    public void getInWorkTasks() throws Exception {
        List<ConductorTask> tasks = client.getInWorkTasks("cs").get();
        tasks.forEach(System.out::println);
    }

    @Test
    public void getTask() throws ExecutionException, InterruptedException {
        ConductorTask task = client.getTask("CS-984072-1").get();
        System.out.println(task);
    }

    @Test
    public void getPackagesOnHost() throws ExecutionException, InterruptedException {
        List<ConductorPackageOnHost> packages = client.getPackagesOnHost("mbo01h.market.yandex.net").get();
        System.out.println(packages);
    }

    @Test
    public void addTicket() throws ExecutionException, InterruptedException {
        ConductorTicket createdTicket = client.addTicket(
            Arrays.asList(
                new ConductorPackageDetails("test-pipeline-foo", "1.15"),
                new ConductorPackageDetails("test-pipeline-bar", "1.15"),
                new ConductorPackageDetails("test-pipeline-baz", "1.15")
            ),
            ConductorBranch.UNSTABLE,
            "ConductorClientTest выкладывает этот тикет",
            Collections.singletonList("sid-hugo@yandex-team.ru"),
            Collections.emptyList(),
            false
        ).get();
        System.out.println(
            "Ticket id: " + createdTicket.getTicket() + "\n" +
                "Ticket status: " + createdTicket.getStatus() + "\n" +
                "Ticket url: " + createdTicket.getUrl() + "\n" +
                "Ticket packages: " + createdTicket.getPackages().stream()
                        .map(ConductorPackageDetails::getPackageName)
                        .collect(Collectors.joining(", ")) + "\n" +
                "Ticket branch: " + createdTicket.getConductorBranch().name() + "\n"
        );

        ListenableFuture<ConductorTicketV1> ticket = client.getTicketV1(createdTicket.getTicket());
        System.out.println(ticket.get());
    }

    @Test
    public void getTicketsWithoutPackages() throws ExecutionException, InterruptedException {
        List<ConductorTicket> tickets = client.getTicketsWithoutPackages(new ConductorPackageDetails("yandex" +
            "-logshatter", "2.25")).get();
        for (ConductorTicket ticket : tickets) {
            System.out.println(
                "Ticket id: " + ticket.getTicket() + "\n" +
                    "Ticket status: " + ticket.getStatus() + "\n" +
                    "Ticket url: " + ticket.getUrl() + "\n" +
                    "Ticket branch: " + ticket.getConductorBranch().name() + "\n" +
                    "Ticket comment: " + ticket.getComment() + "\n" +
                    "===================="
            );
        }
    }

    @Test
    @Ignore(value = "integration test to check api works")
    public void getGroupsToHostsTest() throws ExecutionException, InterruptedException {
        List<String> hostnames = client.getGroupsToHosts("cs_all").get();
        hostnames.forEach(System.out::println);
    }
}

package ru.yandex.market.tsum.clients.conductor;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.io.Resources;
import org.junit.Assert;
import org.junit.Test;

import static ru.yandex.market.tsum.clients.conductor.ConductorBranch.FALLBACK;
import static ru.yandex.market.tsum.clients.conductor.ConductorBranch.HOTFIX;
import static ru.yandex.market.tsum.clients.conductor.ConductorBranch.STABLE;
import static ru.yandex.market.tsum.clients.conductor.ConductorBranch.TESTING;
import static ru.yandex.market.tsum.clients.conductor.ConductorTicket.TicketStatus.DONE;
import static ru.yandex.market.tsum.clients.conductor.ConductorTicket.TicketStatus.IN_WORK;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 14.04.17
 */
public class ConductorParserTest {
    @Test
    public void getTicketV1() throws Exception {
        String json = "{\"type\":\"ticket\",\"id\":1118043,\"self\":\"/tickets/1118043\"," +
            "\"web_path\":\"/tickets/1118043\",\"value\":{\"status\":\"done\",\"comment\":\"ConductorClientTest " +
            "выкладывает этот тикет\",\"branch\":\"unstable\",\"created_at\":\"2017-04-14T16:21:20+03:00\"," +
            "\"updated_at\":\"2017-04-14T16:22:30+03:00\"," +
            "\"packages_with_versions\":[{\"name\":\"test-pipeline-foo\",\"version\":\"1.15\"}," +
            "{\"name\":\"test-pipeline-bar\",\"version\":\"1.15\"},{\"name\":\"test-pipeline-baz\",\"version\":\"1" +
            ".15\"}],\"author\":{\"type\":\"user\",\"id\":8739,\"self\":\"/users/8739\",\"web_path\":\"/users/8739\"," +
            "\"value\":{\"login\":\"robot-mrk-infra-tst\",\"email\":\"robot-mrk-infra-tst@yandex-team.ru\"," +
            "\"role\":\"audience\"}}}}";

        ConductorTicketV1 ticket = ConductorParser.getTicketV1(json);

        Assert.assertEquals(1118043, ticket.getId());
        Assert.assertEquals("/tickets/1118043", ticket.getWebPath());
        Assert.assertEquals("done", ticket.getStatus());
        Assert.assertEquals("unstable", ticket.getBranch().toLowerCase());
        Assert.assertEquals("ConductorClientTest выкладывает этот тикет", ticket.getComment());
        List<ConductorPackageDetails> packages = ticket.getPackagesWithVersions();
        Assert.assertEquals(3, packages.size());

        assertPackage(packages, "test-pipeline-foo", "1.15");
        assertPackage(packages, "test-pipeline-bar", "1.15");
        assertPackage(packages, "test-pipeline-baz", "1.15");

        Assert.assertEquals("robot-mrk-infra-tst", ticket.getAuthor().getLogin());
    }

    @Test
    public void getTicketsFromPackagesResponse() {
        String json = "{\"data\":[],\"included\":[{\"id\":\"1117323\",\"attributes\":{\"status\":\"done\"," +
            "\"comment\":\"Some comment 1\",\"branch\":\"prestable\",\"mailcc\":\"\",\"skip_restart\":false," +
            "\"do_not_autoinstall\":false,\"url\":\"https://c.yandex-team.ru/tickets/1117323\"," +
            "\"created_at\":\"2017-04-13T17:27:11+03:00\",\"updated_at\":\"2017-04-13T17:28:25+03:00\"}}," +
            "{\"id\":\"1118025\",\"attributes\":{\"status\":\"done\",\"comment\":\"Some comment 2\"," +
            "\"branch\":\"stable\",\"mailcc\":\"\",\"skip_restart\":false,\"do_not_autoinstall\":false," +
            "\"url\":\"https://c.yandex-team.ru/tickets/1118025\",\"created_at\":\"2017-04-14T16:06:51+03:00\"," +
            "\"updated_at\":\"2017-04-17T09:45:21+03:00\"}}]}";
        List<ConductorTicket> tickets = ConductorParser.getTicketsFromPackagesResponse(json);

        Assert.assertEquals(1117323, tickets.get(0).getTicket());
        Assert.assertEquals(DONE, tickets.get(0).getStatus());
        Assert.assertEquals("Some comment 1", tickets.get(0).getComment());
        Assert.assertEquals("prestable", tickets.get(0).getConductorBranch().toLowerCase());
        Assert.assertEquals("https://c.yandex-team.ru/tickets/1117323", tickets.get(0).getUrl());

        Assert.assertEquals(1118025, tickets.get(1).getTicket());
        Assert.assertEquals(DONE, tickets.get(1).getStatus());
        Assert.assertEquals("Some comment 2", tickets.get(1).getComment());
        Assert.assertEquals("stable", tickets.get(1).getConductorBranch().toLowerCase());
        Assert.assertEquals("https://c.yandex-team.ru/tickets/1118025", tickets.get(1).getUrl());
    }

    @Test
    public void getTicketWithTasksResponse() {
        String json = "{\"data\":{\"id\":\"1188150\",\"type\":\"tickets\",\"links\":{\"self\":\"http://c.yandex-team" +
            ".ru/api/v2/tickets/1188150\"},\"attributes\":{\"status\":\"in_work\",\"comment\":\"Some comment\"," +
            "\"branch\":\"stable\",\"mailcc\":null,\"skip_restart\":false,\"ticket_packages\":[{\"id\":2260393," +
            "\"ticket_id\":1188150,\"package_id\":32772,\"version\":\"2.62\",\"remove\":false,\"downgrade\":false}," +
            "{\"id\":2260392,\"ticket_id\":1188150,\"package_id\":35944,\"version\":\"2.76\",\"remove\":false," +
            "\"downgrade\":false}],\"filter\":null,\"do_not_autoinstall\":false,\"url\":\"http://c.yandex-team" +
            ".ru/tickets/1188150\",\"created_at\":\"2017-07-11T10:36:39+03:00\"," +
            "\"updated_at\":\"2017-07-11T10:36:54+03:00\"}," +
            "\"relationships\":{\"author\":{\"links\":{\"self\":\"http://c.yandex-team" +
            ".ru/api/v2/tickets/1188150/relationships/author\",\"related\":\"http://c.yandex-team" +
            ".ru/api/v2/tickets/1188150/author\"}},\"tasks\":{\"links\":{\"self\":\"http://c.yandex-team" +
            ".ru/api/v2/tickets/1188150/relationships/tasks\",\"related\":\"http://c.yandex-team" +
            ".ru/api/v2/tickets/1188150/tasks\"},\"data\":[{\"type\":\"tasks\",\"id\":\"2958594\"}," +
            "{\"type\":\"tasks\",\"id\":\"2958595\"},{\"type\":\"tasks\",\"id\":\"2958596\"}]}," +
            "\"packages\":{\"links\":{\"self\":\"http://c.yandex-team" +
            ".ru/api/v2/tickets/1188150/relationships/packages\",\"related\":\"http://c.yandex-team" +
            ".ru/api/v2/tickets/1188150/packages\"}},\"subscribers\":{\"links\":{\"self\":\"http://c.yandex-team" +
            ".ru/api/v2/tickets/1188150/relationships/subscribers\",\"related\":\"http://c.yandex-team" +
            ".ru/api/v2/tickets/1188150/subscribers\"}}}},\"included\":[{\"id\":\"2\",\"type\":\"projects\"," +
            "\"links\":{\"self\":\"http://c.yandex-team.ru/api/v2/projects/2\"},\"attributes\":{\"name\":\"cs\"," +
            "\"description\":\"\",\"frozen_notifications\":false,\"mailto\":\"cs-admin@yandex-team.ru\"," +
            "\"root_email\":\"market-root@yandex-team.ru\",\"testing_mail\":\"market-testing@yandex-team.ru\"," +
            "\"url\":\"http://c.yandex-team.ru/projects/cs\",\"cauth_notify\":\"\",\"cauth_queue\":\"\"," +
            "\"abc_service_id\":969,\"created_at\":\"2009-11-09T19:53:07+03:00\"," +
            "\"updated_at\":\"2017-05-25T18:44:46+03:00\"}," +
            "\"relationships\":{\"groups\":{\"links\":{\"self\":\"http://c.yandex-team" +
            ".ru/api/v2/projects/2/relationships/groups\",\"related\":\"http://c.yandex-team" +
            ".ru/api/v2/projects/2/groups\"}},\"deploy_groups\":{\"links\":{\"self\":\"http://c.yandex-team" +
            ".ru/api/v2/projects/2/relationships/deploy_groups\",\"related\":\"http://c.yandex-team" +
            ".ru/api/v2/projects/2/deploy_groups\"}},\"workflows\":{\"links\":{\"self\":\"http://c.yandex-team" +
            ".ru/api/v2/projects/2/relationships/workflows\",\"related\":\"http://c.yandex-team" +
            ".ru/api/v2/projects/2/workflows\"}},\"tags\":{\"links\":{\"self\":\"http://c.yandex-team" +
            ".ru/api/v2/projects/2/relationships/tags\",\"related\":\"http://c.yandex-team" +
            ".ru/api/v2/projects/2/tags\"}},\"deploy_settings\":{\"links\":{\"self\":\"http://c.yandex-team" +
            ".ru/api/v2/projects/2/relationships/deploy_settings\",\"related\":\"http://c.yandex-team" +
            ".ru/api/v2/projects/2/deploy_settings\"}},\"admins\":{\"links\":{\"self\":\"http://c.yandex-team" +
            ".ru/api/v2/projects/2/relationships/admins\",\"related\":\"http://c.yandex-team" +
            ".ru/api/v2/projects/2/admins\"}}}},{\"id\":\"118\",\"type\":\"projects\",\"links\":{\"self\":\"http://c" +
            ".yandex-team.ru/api/v2/projects/118\"},\"attributes\":{\"name\":\"ape\",\"description\":\"cocaine cloud " +
            "application engine\",\"frozen_notifications\":false,\"mailto\":\"cocaine-admin@yandex-team.ru\"," +
            "\"root_email\":\"ape-root@yandex-team.ru\",\"testing_mail\":\"testing,broom,linux0id\"," +
            "\"url\":\"http://c.yandex-team.ru/projects/ape\",\"cauth_notify\":\"\",\"cauth_queue\":\"\"," +
            "\"abc_service_id\":542,\"created_at\":\"2012-06-24T21:08:30+04:00\"," +
            "\"updated_at\":\"2017-05-25T18:44:47+03:00\"}," +
            "\"relationships\":{\"groups\":{\"links\":{\"self\":\"http://c.yandex-team" +
            ".ru/api/v2/projects/118/relationships/groups\",\"related\":\"http://c.yandex-team" +
            ".ru/api/v2/projects/118/groups\"}},\"deploy_groups\":{\"links\":{\"self\":\"http://c.yandex-team" +
            ".ru/api/v2/projects/118/relationships/deploy_groups\",\"related\":\"http://c.yandex-team" +
            ".ru/api/v2/projects/118/deploy_groups\"}},\"workflows\":{\"links\":{\"self\":\"http://c.yandex-team" +
            ".ru/api/v2/projects/118/relationships/workflows\",\"related\":\"http://c.yandex-team" +
            ".ru/api/v2/projects/118/workflows\"}},\"tags\":{\"links\":{\"self\":\"http://c.yandex-team" +
            ".ru/api/v2/projects/118/relationships/tags\",\"related\":\"http://c.yandex-team" +
            ".ru/api/v2/projects/118/tags\"}},\"deploy_settings\":{\"links\":{\"self\":\"http://c.yandex-team" +
            ".ru/api/v2/projects/118/relationships/deploy_settings\",\"related\":\"http://c.yandex-team" +
            ".ru/api/v2/projects/118/deploy_settings\"}},\"admins\":{\"links\":{\"self\":\"http://c.yandex-team" +
            ".ru/api/v2/projects/118/relationships/admins\",\"related\":\"http://c.yandex-team" +
            ".ru/api/v2/projects/118/admins\"}}}},{\"id\":\"2958594\",\"type\":\"tasks\"," +
            "\"links\":{\"self\":\"http://c.yandex-team.ru/api/v2/tasks/2958594\"}," +
            "\"attributes\":{\"name\":\"CS-1188150-1\",\"status\":\"done\",\"message\":null,\"paused\":false," +
            "\"number\":1,\"deploy_scope\":1,\"done_at\":\"2017-07-11T10:37:35+03:00\"," +
            "\"created_at\":\"2017-07-11T10:36:39+03:00\",\"updated_at\":\"2017-07-11T10:37:35+03:00\"}," +
            "\"relationships\":{\"ticket\":{\"links\":{\"self\":\"http://c.yandex-team" +
            ".ru/api/v2/tasks/2958594/relationships/ticket\",\"related\":\"http://c.yandex-team" +
            ".ru/api/v2/tasks/2958594/ticket\"}},\"project\":{\"links\":{\"self\":\"http://c.yandex-team" +
            ".ru/api/v2/tasks/2958594/relationships/project\",\"related\":\"http://c.yandex-team" +
            ".ru/api/v2/tasks/2958594/project\"},\"data\":{\"type\":\"projects\",\"id\":\"2\"}}," +
            "\"packages\":{\"links\":{\"self\":\"http://c.yandex-team" +
            ".ru/api/v2/tasks/2958594/relationships/packages\",\"related\":\"http://c.yandex-team" +
            ".ru/api/v2/tasks/2958594/packages\"}}}},{\"id\":\"2958595\",\"type\":\"tasks\"," +
            "\"links\":{\"self\":\"http://c.yandex-team.ru/api/v2/tasks/2958595\"}," +
            "\"attributes\":{\"name\":\"CS-1188150-2\",\"status\":\"done\",\"message\":null,\"paused\":false," +
            "\"number\":2,\"deploy_scope\":1,\"done_at\":\"2017-07-11T10:37:30+03:00\"," +
            "\"created_at\":\"2017-07-11T10:36:39+03:00\",\"updated_at\":\"2017-07-11T10:37:30+03:00\"}," +
            "\"relationships\":{\"ticket\":{\"links\":{\"self\":\"http://c.yandex-team" +
            ".ru/api/v2/tasks/2958595/relationships/ticket\",\"related\":\"http://c.yandex-team" +
            ".ru/api/v2/tasks/2958595/ticket\"}},\"project\":{\"links\":{\"self\":\"http://c.yandex-team" +
            ".ru/api/v2/tasks/2958595/relationships/project\",\"related\":\"http://c.yandex-team" +
            ".ru/api/v2/tasks/2958595/project\"},\"data\":{\"type\":\"projects\",\"id\":\"2\"}}," +
            "\"packages\":{\"links\":{\"self\":\"http://c.yandex-team" +
            ".ru/api/v2/tasks/2958595/relationships/packages\",\"related\":\"http://c.yandex-team" +
            ".ru/api/v2/tasks/2958595/packages\"}}}},{\"id\":\"2958596\",\"type\":\"tasks\"," +
            "\"links\":{\"self\":\"http://c.yandex-team.ru/api/v2/tasks/2958596\"}," +
            "\"attributes\":{\"name\":\"APE-1188150-3\",\"status\":\"new\",\"message\":null,\"paused\":false," +
            "\"number\":3,\"deploy_scope\":1,\"done_at\":null,\"created_at\":\"2017-07-11T10:36:39+03:00\"," +
            "\"updated_at\":\"2017-07-11T10:36:39+03:00\"}," +
            "\"relationships\":{\"ticket\":{\"links\":{\"self\":\"http://c.yandex-team" +
            ".ru/api/v2/tasks/2958596/relationships/ticket\",\"related\":\"http://c.yandex-team" +
            ".ru/api/v2/tasks/2958596/ticket\"}},\"project\":{\"links\":{\"self\":\"http://c.yandex-team" +
            ".ru/api/v2/tasks/2958596/relationships/project\",\"related\":\"http://c.yandex-team" +
            ".ru/api/v2/tasks/2958596/project\"},\"data\":{\"type\":\"projects\",\"id\":\"118\"}}," +
            "\"packages\":{\"links\":{\"self\":\"http://c.yandex-team" +
            ".ru/api/v2/tasks/2958596/relationships/packages\",\"related\":\"http://c.yandex-team" +
            ".ru/api/v2/tasks/2958596/packages\"}}}}]}";
        ConductorTicket ticket = ConductorParser.getTicketWithTasks(json);

        Assert.assertEquals(1188150, ticket.getTicket());
        Assert.assertEquals(IN_WORK, ticket.getStatus());
        Assert.assertEquals("Some comment", ticket.getComment());
        Assert.assertEquals("stable", ticket.getConductorBranch().toLowerCase());
        Assert.assertEquals("http://c.yandex-team.ru/tickets/1188150", ticket.getUrl());

        Assert.assertTrue(ticket.getTasks().size() == 3);

        Assert.assertEquals("CS-1188150-2", ticket.getTasks().get(0).getName());
        Assert.assertEquals("done", ticket.getTasks().get(0).getStatus());
        Assert.assertEquals("cs", ticket.getTasks().get(0).getProject());
    }

    @Test
    public void getTicketsFromTicketPackages() throws Exception {
        String json = "{\"data\":[{\"id\":\"2267123\",\"type\":\"ticket_packages\",\"links\":{\"self\":\"https://c" +
            ".yandex-team.ru/api/v2/ticket_packages/2267123\"},\"attributes\":{\"version\":\"1.76\"," +
            "\"downgrade\":false},\"relationships\":{\"ticket\":{\"links\":{\"self\":\"https://c.yandex-team" +
            ".ru/api/v2/ticket_packages/2267123/relationships/ticket\",\"related\":\"https://c.yandex-team" +
            ".ru/api/v2/ticket_packages/2267123/ticket\"},\"data\":{\"type\":\"tickets\",\"id\":\"1191075\"}}," +
            "\"package\":{\"links\":{\"self\":\"https://c.yandex-team" +
            ".ru/api/v2/ticket_packages/2267123/relationships/package\",\"related\":\"https://c.yandex-team" +
            ".ru/api/v2/ticket_packages/2267123/package\"}}}},{\"id\":\"2267301\",\"type\":\"ticket_packages\"," +
            "\"links\":{\"self\":\"https://c.yandex-team.ru/api/v2/ticket_packages/2267301\"}," +
            "\"attributes\":{\"version\":\"1.76\",\"downgrade\":false}," +
            "\"relationships\":{\"ticket\":{\"links\":{\"self\":\"https://c.yandex-team" +
            ".ru/api/v2/ticket_packages/2267301/relationships/ticket\",\"related\":\"https://c.yandex-team" +
            ".ru/api/v2/ticket_packages/2267301/ticket\"},\"data\":{\"type\":\"tickets\",\"id\":\"1191152\"}}," +
            "\"package\":{\"links\":{\"self\":\"https://c.yandex-team" +
            ".ru/api/v2/ticket_packages/2267301/relationships/package\",\"related\":\"https://c.yandex-team" +
            ".ru/api/v2/ticket_packages/2267301/package\"}}}},{\"id\":\"2267327\",\"type\":\"ticket_packages\"," +
            "\"links\":{\"self\":\"https://c.yandex-team.ru/api/v2/ticket_packages/2267327\"}," +
            "\"attributes\":{\"version\":\"1.76\",\"downgrade\":false}," +
            "\"relationships\":{\"ticket\":{\"links\":{\"self\":\"https://c.yandex-team" +
            ".ru/api/v2/ticket_packages/2267327/relationships/ticket\",\"related\":\"https://c.yandex-team" +
            ".ru/api/v2/ticket_packages/2267327/ticket\"},\"data\":{\"type\":\"tickets\",\"id\":\"1191163\"}}," +
            "\"package\":{\"links\":{\"self\":\"https://c.yandex-team" +
            ".ru/api/v2/ticket_packages/2267327/relationships/package\",\"related\":\"https://c.yandex-team" +
            ".ru/api/v2/ticket_packages/2267327/package\"}}}}],\"included\":[{\"id\":\"1191075\"," +
            "\"type\":\"tickets\",\"links\":{\"self\":\"https://c.yandex-team.ru/api/v2/tickets/1191075\"}," +
            "\"attributes\":{\"status\":\"done\",\"comment\":\"Тикет создан автоматически с помощью релизного " +
            "пайплайна.\\nСсылка на пайплайн: https://tsum.yandex-team" +
            ".ru/pipe/launch/596757247c0e90a227ee9f1f\\nСсылка на задачу: https://tsum.yandex-team" +
            ".ru/pipe/launch/596757247c0e90a227ee9f1f/job/ConductorDeployJob1/1\\n\\nИзменения:\\n* blacksmith g to v" +
            " fix\\n* Merge pull request #434 from market-infra/bracket-fix\\n* Потерялось при " +
            "перемерживании\\n\\n\\nPipe job id: 596757247c0e90a227ee9f1f:ConductorDeployJob1:1\"," +
            "\"branch\":\"testing\",\"mailcc\":null,\"skip_restart\":false,\"_ticket_packages\":[{\"id\":2267125," +
            "\"ticket_id\":1191075,\"package_id\":45248,\"version\":\"1.131\",\"remove\":false,\"downgrade\":false}," +
            "{\"id\":2267123,\"ticket_id\":1191075,\"package_id\":45997,\"version\":\"1.76\",\"remove\":false," +
            "\"downgrade\":false},{\"id\":2267124,\"ticket_id\":1191075,\"package_id\":46784,\"version\":\"1.119\"," +
            "\"remove\":false,\"downgrade\":false}],\"filter\":null,\"do_not_autoinstall\":false,\"url\":\"https://c" +
            ".yandex-team.ru/tickets/1191075\",\"created_at\":\"2017-07-13T15:35:01+03:00\"," +
            "\"updated_at\":\"2017-07-13T15:42:20+03:00\"}," +
            "\"relationships\":{\"author\":{\"links\":{\"self\":\"https://c.yandex-team" +
            ".ru/api/v2/tickets/1191075/relationships/author\",\"related\":\"https://c.yandex-team" +
            ".ru/api/v2/tickets/1191075/author\"}},\"tasks\":{\"links\":{\"self\":\"https://c.yandex-team" +
            ".ru/api/v2/tickets/1191075/relationships/tasks\",\"related\":\"https://c.yandex-team" +
            ".ru/api/v2/tickets/1191075/tasks\"}},\"packages\":{\"links\":{\"self\":\"https://c.yandex-team" +
            ".ru/api/v2/tickets/1191075/relationships/packages\",\"related\":\"https://c.yandex-team" +
            ".ru/api/v2/tickets/1191075/packages\"}},\"ticket_packages\":{\"links\":{\"self\":\"https://c.yandex-team" +
            ".ru/api/v2/tickets/1191075/relationships/ticket_packages\",\"related\":\"https://c.yandex-team" +
            ".ru/api/v2/tickets/1191075/ticket_packages\"}},\"subscribers\":{\"links\":{\"self\":\"https://c" +
            ".yandex-team.ru/api/v2/tickets/1191075/relationships/subscribers\",\"related\":\"https://c.yandex-team" +
            ".ru/api/v2/tickets/1191075/subscribers\"}}}},{\"id\":\"1191152\",\"type\":\"tickets\"," +
            "\"links\":{\"self\":\"https://c.yandex-team.ru/api/v2/tickets/1191152\"}," +
            "\"attributes\":{\"status\":\"done\",\"comment\":\"Тикет создан автоматически с помощью релизного " +
            "пайплайна.\\nСсылка на пайплайн: https://tsum.yandex-team" +
            ".ru/pipe/launch/596757247c0e90a227ee9f1f\\nСсылка на задачу: https://tsum.yandex-team" +
            ".ru/pipe/launch/596757247c0e90a227ee9f1f/job/ConductorDeployJob2/1\\n\\nИзменения:\\n* Merge pull " +
            "request #434 from market-infra/bracket-fix\\n* blacksmith g to v fix\\n* Потерялось при " +
            "перемерживании\\n\\n\\nPipe job id: 596757247c0e90a227ee9f1f:ConductorDeployJob2:1\"," +
            "\"branch\":\"stable\",\"mailcc\":null,\"skip_restart\":false,\"_ticket_packages\":[{\"id\":2267300," +
            "\"ticket_id\":1191152,\"package_id\":45248,\"version\":\"1.131\",\"remove\":false,\"downgrade\":false}," +
            "{\"id\":2267301,\"ticket_id\":1191152,\"package_id\":45997,\"version\":\"1.76\",\"remove\":false," +
            "\"downgrade\":false},{\"id\":2267302,\"ticket_id\":1191152,\"package_id\":46784,\"version\":\"1.119\"," +
            "\"remove\":false,\"downgrade\":false}],\"filter\":null,\"do_not_autoinstall\":false,\"url\":\"https://c" +
            ".yandex-team.ru/tickets/1191152\",\"created_at\":\"2017-07-13T16:04:32+03:00\"," +
            "\"updated_at\":\"2017-07-13T16:09:20+03:00\"}," +
            "\"relationships\":{\"author\":{\"links\":{\"self\":\"https://c.yandex-team" +
            ".ru/api/v2/tickets/1191152/relationships/author\",\"related\":\"https://c.yandex-team" +
            ".ru/api/v2/tickets/1191152/author\"}},\"tasks\":{\"links\":{\"self\":\"https://c.yandex-team" +
            ".ru/api/v2/tickets/1191152/relationships/tasks\",\"related\":\"https://c.yandex-team" +
            ".ru/api/v2/tickets/1191152/tasks\"}},\"packages\":{\"links\":{\"self\":\"https://c.yandex-team" +
            ".ru/api/v2/tickets/1191152/relationships/packages\",\"related\":\"https://c.yandex-team" +
            ".ru/api/v2/tickets/1191152/packages\"}},\"ticket_packages\":{\"links\":{\"self\":\"https://c.yandex-team" +
            ".ru/api/v2/tickets/1191152/relationships/ticket_packages\",\"related\":\"https://c.yandex-team" +
            ".ru/api/v2/tickets/1191152/ticket_packages\"}},\"subscribers\":{\"links\":{\"self\":\"https://c" +
            ".yandex-team.ru/api/v2/tickets/1191152/relationships/subscribers\",\"related\":\"https://c.yandex-team" +
            ".ru/api/v2/tickets/1191152/subscribers\"}}}},{\"id\":\"1191163\",\"type\":\"tickets\"," +
            "\"links\":{\"self\":\"https://c.yandex-team.ru/api/v2/tickets/1191163\"}," +
            "\"attributes\":{\"status\":\"done\",\"comment\":\"Тикет создан автоматически с помощью релизного " +
            "пайплайна.\\nСсылка на пайплайн: https://tsum.yandex-team" +
            ".ru/pipe/launch/596757247c0e90a227ee9f1f\\nСсылка на задачу: https://tsum.yandex-team" +
            ".ru/pipe/launch/596757247c0e90a227ee9f1f/job/ConductorDeployJob2/1\\n\\nИзменения:\\n* blacksmith g to v" +
            " fix\\n* Merge pull request #434 from market-infra/bracket-fix\\n* Потерялось при " +
            "перемерживании\\n\\n\\nPipe job id: 596757247c0e90a227ee9f1f:ConductorDeployJob2:1\"," +
            "\"branch\":\"stable\",\"mailcc\":null,\"skip_restart\":false,\"_ticket_packages\":[{\"id\":2267329," +
            "\"ticket_id\":1191163,\"package_id\":45248,\"version\":\"1.131\",\"remove\":false,\"downgrade\":false}," +
            "{\"id\":2267327,\"ticket_id\":1191163,\"package_id\":45997,\"version\":\"1.76\",\"remove\":false," +
            "\"downgrade\":false},{\"id\":2267328,\"ticket_id\":1191163,\"package_id\":46784,\"version\":\"1.119\"," +
            "\"remove\":false,\"downgrade\":false}],\"filter\":null,\"do_not_autoinstall\":false,\"url\":\"https://c" +
            ".yandex-team.ru/tickets/1191163\",\"created_at\":\"2017-07-13T16:09:04+03:00\"," +
            "\"updated_at\":\"2017-07-13T16:11:54+03:00\"}," +
            "\"relationships\":{\"author\":{\"links\":{\"self\":\"https://c.yandex-team" +
            ".ru/api/v2/tickets/1191163/relationships/author\",\"related\":\"https://c.yandex-team" +
            ".ru/api/v2/tickets/1191163/author\"}},\"tasks\":{\"links\":{\"self\":\"https://c.yandex-team" +
            ".ru/api/v2/tickets/1191163/relationships/tasks\",\"related\":\"https://c.yandex-team" +
            ".ru/api/v2/tickets/1191163/tasks\"}},\"packages\":{\"links\":{\"self\":\"https://c.yandex-team" +
            ".ru/api/v2/tickets/1191163/relationships/packages\",\"related\":\"https://c.yandex-team" +
            ".ru/api/v2/tickets/1191163/packages\"}},\"ticket_packages\":{\"links\":{\"self\":\"https://c.yandex-team" +
            ".ru/api/v2/tickets/1191163/relationships/ticket_packages\",\"related\":\"https://c.yandex-team" +
            ".ru/api/v2/tickets/1191163/ticket_packages\"}},\"subscribers\":{\"links\":{\"self\":\"https://c" +
            ".yandex-team.ru/api/v2/tickets/1191163/relationships/subscribers\",\"related\":\"https://c.yandex-team" +
            ".ru/api/v2/tickets/1191163/subscribers\"}}}}],\"meta\":{\"page[size]\":10,\"page[number]\":1," +
            "\"filters\":{\"package_name\":\"yandex-market-tsum-api\",\"version\":\"1.76\"},\"page_count\":1}," +
            "\"links\":{\"first\":\"https://c.yandex-team" +
            ".ru/api/v2/ticket_packages?filter%5Bpackage_name%5D=yandex-market-tsum-api&filter%5Bversion%5D=1" +
            ".76&include=ticket&page%5Bnumber%5D=1&page%5Bsize%5D=10\",\"last\":\"https://c.yandex-team" +
            ".ru/api/v2/ticket_packages?filter%5Bpackage_name%5D=yandex-market-tsum-api&filter%5Bversion%5D=1" +
            ".76&include=ticket&page%5Bnumber%5D=1&page%5Bsize%5D=10\"}}";

        List<ConductorTicket> tickets = ConductorParser.getTicketsFromTicketPackages(json);

        Assert.assertEquals(3, tickets.size());

        Assert.assertEquals(1191075, tickets.get(0).getTicket());
        Assert.assertEquals("https://c.yandex-team.ru/tickets/1191075", tickets.get(0).getUrl());
        Assert.assertEquals(DONE, tickets.get(0).getStatus());
        Assert.assertEquals(TESTING, tickets.get(0).getConductorBranch());
    }

    private void assertPackage(List<ConductorPackageDetails> packages, String packageName, String packageVersion) {
        ConductorPackageDetails pkg = packages.stream()
            .filter(p -> p.getPackageName().equals(packageName))
            .findFirst()
            .orElse(null);

        Assert.assertNotNull(pkg);
        Assert.assertEquals(packageVersion, pkg.getVersion());
    }

    @Test
    public void searchResulToPackagesList() throws Exception {
        URL resultFile = Resources.getResource("conductor/package-search-result.json");
        String searchResult = Resources.toString(resultFile, Charset.defaultCharset());
        var conductorPackages = ConductorParser.parseDataResult(searchResult, ConductorPackageInfo.class);

        Assert.assertEquals(1, conductorPackages.size());

        ConductorPackageInfo conductorPackage = conductorPackages.get(0);
        Assert.assertEquals("45248", conductorPackage.getId());
        Assert.assertEquals("yandex-market-tsum-ui", conductorPackage.getName());
    }

    @Test
    public void searchResulToHostList() throws Exception {
        URL resultFile = Resources.getResource("conductor/hosts-search-result.json");
        String searchResult = Resources.toString(resultFile, Charset.defaultCharset());
        List<ConductorHost> hosts = ConductorParser.parseDataResult(searchResult, ConductorHost.class);

        Assert.assertEquals(2, hosts.size());

        ConductorHost host1 = hosts.get(0);
        Assert.assertEquals("11974522", host1.getId());
        Assert.assertEquals("wms-load-app02e.market.yandex.net", host1.getFqdn());
        Assert.assertEquals("wms-load-app02e.market", host1.getShortName());

        ConductorHost host2 = hosts.get(1);
        Assert.assertEquals("11974523", host2.getId());
        Assert.assertEquals("wms-load-app03e.market.yandex.net", host2.getFqdn());
        Assert.assertEquals("wms-load-app03e.market", host2.getShortName());
    }

    @Test
    public void parseMeta() throws Exception {
        URL resultFile = Resources.getResource("conductor/hosts-search-result.json");
        String searchResult = Resources.toString(resultFile, Charset.defaultCharset());
        ConductorMeta meta = ConductorParser.parseMeta(searchResult);

        Assert.assertEquals(1, meta.getPageCount());
        Assert.assertEquals(2, meta.getPageNumber());
        Assert.assertEquals(10, meta.getPageSize());
    }

    @Test
    public void getLastDeployedVersion() throws IOException {
        testLastDeployedVersion(null, Collections.singletonList(STABLE));
        testLastDeployedVersion("2017.09.23.1", Arrays.asList(STABLE, HOTFIX, FALLBACK), "cs");
        testLastDeployedVersion("2017.09.23.1", Arrays.asList(STABLE, HOTFIX, FALLBACK), "cs", "advq");
        testLastDeployedVersion(null, Arrays.asList(STABLE, HOTFIX, FALLBACK), "cs", "notExistingProject");
        testLastDeployedVersion(null, Arrays.asList(STABLE, HOTFIX, FALLBACK), "notExistingProject");
        testLastDeployedVersion(null, Arrays.asList(STABLE, HOTFIX, FALLBACK), "cs", "search_suggest");
        testLastDeployedVersion(null, Collections.singletonList(TESTING), "cs");
        testLastDeployedVersionNever(null, Arrays.asList(STABLE, HOTFIX, FALLBACK));
    }

    private void testLastDeployedVersion(String expectedVersion, List<ConductorBranch> branches, String... project)
        throws IOException {
        URL resultFile = Resources.getResource("conductor/lastDeployedVersionStable.json");
        String responseBody = Resources.toString(resultFile, Charset.defaultCharset());
        Assert.assertEquals(
            expectedVersion,
            ConductorParser.getLastDeployedVersion(responseBody, "yandex-cs-torrent-client3", branches,
                Arrays.asList(project))
        );
    }

    private void testLastDeployedVersionNever(String expectedVersion, List<ConductorBranch> branches)
        throws IOException {
        URL resultFile = Resources.getResource("conductor/lastDeployedVersionNever.json");
        String responseBody = Resources.toString(resultFile, Charset.defaultCharset());
        Assert.assertEquals(
            expectedVersion,
            ConductorParser.getLastDeployedVersion(responseBody, "yandex-cs-torrent-client3", branches,
                Collections.emptyList())
        );
    }
}

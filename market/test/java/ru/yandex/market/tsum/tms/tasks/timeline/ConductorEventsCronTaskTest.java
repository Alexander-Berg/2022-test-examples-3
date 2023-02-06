package ru.yandex.market.tsum.tms.tasks.timeline;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.tsum.clients.conductor.ConductorBranch;
import ru.yandex.market.tsum.clients.conductor.ConductorTicketV1;
import ru.yandex.market.tsum.pipelines.common.resources.ConductorPackage;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 11.07.17
 */
public class ConductorEventsCronTaskTest {
    @Test
    public void createTskvRecord() {
        String fullComment = "Тикет создан автоматически с помощью релизного пайплайна.\n" +
            "Ссылка на пайплайн: https://tsum.yandex-team.ru/pipe/projects\n" +
            "Ссылка на задачу: https://tsum.yandex-team.ru/pipe/projects\n" +
            "\n" +
            "Изменения:\n" +
            "MARKETINFRA-42 forty two changes\n" +
            "\n" +
            "\n" +
            "Pipe job id: 595fa2ea8252fсb17a2c132f:ConductorDeployJob1:1\r\n" +
            "Pipeline id: test-teamcity-conductor";

        String incompleteComment = "Тикет создан автоматически с помощью релизного пайплайна.\n" +
            "Ссылка на пайплайн: https://tsum.yandex-team.ru/pipe/projects\n" +
            "Ссылка на задачу: https://tsum.yandex-team.ru/pipe/projects\n" +
            "\n" +
            "Изменения:\n" +
            "MARKETINFRA-42 forty two changes\n" +
            "\n" +
            "\n" +
            "Pipe job id: 595fa2ea8252fсb17a2c132f:ConductorDeployJob1:1";

        List<String> taskNames = Arrays.asList("CS-42-1", "CS-42-2");

        ConductorTicketV1 fullTicket = new ConductorTicketV1(
            42,
            new ConductorTicketV1.TicketValue(
                "done",
                fullComment,
                ConductorBranch.TESTING,
                new ConductorTicketV1.Author(null, 123, "user42", null, null, null),
                new Date(0),
                new Date(10),
                Arrays.asList(new ConductorPackage("package0", "0").toDetails(), new ConductorPackage("package1",
                    "1").toDetails())
            ),
            null
        );

        ConductorTicketV1 incompleteTicket = new ConductorTicketV1(
            42,
            new ConductorTicketV1.TicketValue(
                "ObSoLeTe",
                incompleteComment,
                ConductorBranch.STABLE,
                new ConductorTicketV1.Author(null, 123, "user42", null, null, null),
                new Date(0),
                new Date(10),
                Arrays.asList(new ConductorPackage("package0", "0").toDetails())
            ),
            null
        );

        String tskvRecord = ConductorEventsCronTask.createTskvRecord(fullTicket, taskNames);
        Assert.assertEquals(
            "ticket_id=42\tbranch=testing\tstatus=done\tpackages=package0,package1\tversions=0,1\ttasks=CS-42-1,CS-42-2\tjob_id=595fa2ea8252fсb17a2c132f:ConductorDeployJob1:1\tpipe_id=test-teamcity-conductor\tcreate_date=0\tend_date=10\tauthor=user42",
            // substring for skipping date field
            tskvRecord.substring(tskvRecord.indexOf("ticket_id"))
        );
        tskvRecord = ConductorEventsCronTask.createTskvRecord(incompleteTicket, taskNames);
        Assert.assertEquals(
            "ticket_id=42\tbranch=stable\tstatus=obsolete\tpackages=package0\tversions=0\ttasks=CS-42-1,CS-42-2\tjob_id=595fa2ea8252fсb17a2c132f:ConductorDeployJob1:1\tpipe_id=\tcreate_date=0\tend_date=10\tauthor=user42",
            // substring for skipping date field
            tskvRecord.substring(tskvRecord.indexOf("ticket_id"))
        );
    }
}
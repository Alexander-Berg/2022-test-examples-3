package ru.yandex.market.tsum.core.agent;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.tsum.agent.AgentHostReport;
import ru.yandex.market.tsum.agent.AgentPackage;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 14/12/2016
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {TestAgentConfig.class})
public class AgentMongoDaoTest {

    @Autowired
    private AgentMongoDao dao;

    @Test
    public void testProto() throws Exception {
        AgentHostReport report = AgentHostReport.newBuilder().setEnviroment("ascas").setHost("host1")
            .addPackages(AgentPackage.newBuilder().setName("aba").build())
            .addPackages(AgentPackage.getDefaultInstance())
            .addPackages(AgentPackage.getDefaultInstance())
            .build();

        dao.saveReport(report);

        AgentMongoDao.ReportWrapper.AgentHostReportEntity report2 = dao.getReport("host1").get();

        Assert.assertEquals(new AgentMongoDao.ReportWrapper.AgentHostReportEntity(report), report2);

    }

}
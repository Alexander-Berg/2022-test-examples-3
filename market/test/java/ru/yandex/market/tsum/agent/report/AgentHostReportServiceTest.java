package ru.yandex.market.tsum.agent.report;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.tsum.agent.DebsumsMismatch;
import ru.yandex.market.tsum.agent.MismatchStatus;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 09/12/2016
 */
public class AgentHostReportServiceTest {
    @Test
    public void parseDpkgPackage() throws Exception {
        Assert.assertEquals(
            new DpkgPackage("ca-certificates", "ii", "20160104ubuntu0.14.04.1"),
            AgentHostReportService.parseDpkgPackage("ii  ca-certificates                           20160104ubuntu0.14.04.1           all          Common CA certificates   ")
        );
    }

    @Test
    public void parseDebsumsMismatch() throws Exception {
        Assert.assertEquals(
            DebsumsMismatch.newBuilder()
                .setDebPackage("config-nginx-backends")
                .setFile("/etc/nginx/sites-available/235-market-gurudaemon.market_slb-testing.conf")
                .setStatus(MismatchStatus.MISSING)
                .build(),
            AgentHostReportService.parseDebsumsMismatch("debsums: missing file /etc/nginx/sites-available/235-market-gurudaemon.market_slb-testing.conf (from config-nginx-backends package)")
        );

        Assert.assertEquals(
            DebsumsMismatch.newBuilder()
                .setDebPackage("docker-engine")
                .setFile("/etc/default/docker")
                .setStatus(MismatchStatus.CHANGED)
                .build(),
            AgentHostReportService.parseDebsumsMismatch("debsums: changed file /etc/default/docker (from docker-engine package)")
        );

        Assert.assertNull(AgentHostReportService.parseDebsumsMismatch("debsums: no md5sums for some package bla"));
        Assert.assertNull(AgentHostReportService.parseDebsumsMismatch("dpkg-query: warning: parsing file '/var"));
        Assert.assertNull(AgentHostReportService.parseDebsumsMismatch(" missing description"));
        Assert.assertNull(AgentHostReportService.parseDebsumsMismatch("dpkg-divert: warning: parsing file '/var"));
    }
}
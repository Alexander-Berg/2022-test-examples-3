package ru.yandex.market.tsum.pipelines.lcmp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.tsum.clients.gencfg.GenCfgCType;
import ru.yandex.market.tsum.clients.gencfg.GenCfgLocation;
import ru.yandex.market.tsum.clients.nanny.NannyUtils;
import ru.yandex.market.tsum.pipelines.common.resources.StartrekTicket;
import ru.yandex.market.tsum.pipelines.sre.resources.BalancerEnvironment;
import ru.yandex.market.tsum.pipelines.sre.resources.BalancerFlavour;
import ru.yandex.market.tsum.pipelines.sre.resources.BalancerType;
import ru.yandex.market.tsum.pipelines.sre.resources.IPVersion;
import ru.yandex.market.tsum.pipelines.sre.resources.balancer.BalancerInfo;

import static ru.yandex.market.tsum.core.TestResourceLoader.getTestResourceAsString;

public class BalancerRequestTicketTest {
    private static BalancerInfo balancerInfo;

    @BeforeClass
    public static void prepareBalancerInfo() {
        BalancerInfo.Builder builder = BalancerInfo.Builder.create()
            .withBalancerType(BalancerType.INTERNAL)
            .withEnvironment(BalancerEnvironment.PRODUCTION)
            .withBalancerFlavour(BalancerFlavour.MSLB)
            .withStartrekTicket(new StartrekTicket("CSADMIN-24875"))
            .withDescription("API для поддержки компонента встраиваемого редактора MBO CMS Editor")
            .withFqdn("mbo-cms-editor-api.tst.vs.market.yandex.net")
            .withHttpPort(80)
            .withHttpsPort(443)
            .withRedirectToHttps(true)
            .withRealServers(Arrays.asList("testing_market_mbo_cms_editor_api_sas",
                "testing_market_mbo_cms_editor_api_vla"))
            .withHealthCheckUrl("/ping")
            .withHealthCheckType(BalancerInfo.HealthCheckType.RESPONSE_CODE)
            .withEnableGrpc(true)
            .withEnableGrpcSsl(true)
            .withGrpcOffsetPort(3)
            .withRps("1")
            .withIpVersion(IPVersion.IPV6)
            .withTypeOfBackends(BalancerInfo.TypeOfBackends.NANNY_SERVICE)
            .withEnableHttp2(false)
            .withSslBackends(false)
            .withSslExternalCa(false)
            .withMachineAccess(Arrays.asList(""))
            .withHumanAccess(Arrays.asList("Служба Эксплуатации Маркета", "Ещё одна группа"))
            .withSslBackends(false)
            .withSslExternalCa(false)
            .withHealthCheckText("None");

        balancerInfo = builder.build();
    }

    @Test
    public void testRenderTemplateMethod() throws IOException {
        String expectedTicketDescription = getTestResourceAsString(this.getClass(), "expectedTicketDescription.txt");
        String ticketDescription = BalancerRequestTicket.renderDescription(balancerInfo);

        Assert.assertEquals(expectedTicketDescription, ticketDescription);
    }

    @Test
    @Ignore
    public void realServers() {
        String appName = "erp-health";
        List<GenCfgLocation> locations = new ArrayList<GenCfgLocation>(Arrays.asList(
                GenCfgLocation.VLA,
                GenCfgLocation.SAS
        ));

        List<String> realServers = locations
                .stream()
                .map(genCfgLocation -> {
                    return NannyUtils.getServiceName(
                            appName,
                            genCfgLocation,
                            GenCfgCType.PRODUCTION
                    );
                })
                .collect(Collectors.toList());

        System.out.println(realServers);
    }
}

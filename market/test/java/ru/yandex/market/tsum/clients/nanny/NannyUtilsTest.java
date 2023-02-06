package ru.yandex.market.tsum.clients.nanny;

import org.junit.Test;

import ru.yandex.market.tsum.clients.gencfg.GenCfgCType;
import ru.yandex.market.tsum.clients.gencfg.GenCfgLocation;
import ru.yandex.market.tsum.clients.nanny.service.ServiceEnvironment;

import static org.junit.Assert.assertEquals;

/**
 * @author Andrey Trubachev <a href="mailto:d3rp@yandex-team.ru"></a>
 * @date 08/08/2017
 */
public class NannyUtilsTest {
    @Test
    public void getServiceName() throws Exception {
        assertEquals(
            "testing_market_my_service_sas",
            NannyUtils.getServiceName("my_service", GenCfgLocation.SAS, GenCfgCType.TESTING)
        );
        assertEquals(
            "prestable_market_my_service_vla",
            NannyUtils.getServiceName("my_service", GenCfgLocation.VLA, GenCfgCType.PRESTABLE)
        );
        assertEquals(
            "production_market_my_service_iva",
            NannyUtils.getServiceName("my_service", GenCfgLocation.IVA, GenCfgCType.PRODUCTION)
        );
    }

    @Test
    public void getEnvFromServiceId() throws Exception {
        assertEquals(ServiceEnvironment.PRODUCTION, NannyUtils.getEnvFromServiceId("production_pricechart_gen_vla"));
        assertEquals(ServiceEnvironment.PRESTABLE, NannyUtils.getEnvFromServiceId("prestable_market_front_vendors_iva"
        ));
        assertEquals(ServiceEnvironment.TESTING, NannyUtils.getEnvFromServiceId("testing_market_stock_storage_sas"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void serviceIdWithoutEnvironment() throws Exception {
        NannyUtils.getEnvFromServiceId("my-cool-service");
    }

    @Test(expected = IllegalArgumentException.class)
    public void serviceIdWithUnknownEnvironment() throws Exception {
        NannyUtils.getEnvFromServiceId("my_env_market_srv_fil");
    }

    @Test
    public void extractTaskNumFromReleaseId() throws Exception {
        assertEquals(160942923L, NannyUtils.getSandboxTaskNumFromReleaseId("SANDBOX_RELEASE-160942923-STABLE"));
        assertEquals(178160247L, NannyUtils.getSandboxTaskNumFromReleaseId("SANDBOX_RELEASE-178160247-STABLE"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nannyTaskExtractorWithoutDashes() throws Exception {
        NannyUtils.getSandboxTaskNumFromReleaseId("SANDBOX_RELEASE160942923STABLE");
    }

    @Test(expected = IllegalArgumentException.class)
    public void nannyTaskExtractorWithoutSecondDash() throws Exception {
        NannyUtils.getSandboxTaskNumFromReleaseId("SANDBOX_RELEASE-160942923STABLE");
    }

    @Test(expected = IllegalArgumentException.class)
    public void nannyTaskExtractorWithTwoDashes() throws Exception {
        NannyUtils.getSandboxTaskNumFromReleaseId("SANDBOX_RELEASE--160942923-STABLE");
    }
}

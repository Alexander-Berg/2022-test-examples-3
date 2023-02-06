package ru.yandex.market.tsum.pipelines.sre.resources;

import java.util.Collections;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RtcServiceSpecTest {
    private RtcServiceSpec rtcServiceSpec;

    @Before
    public void initRtcServiceSpec() {
        rtcServiceSpec = new RtcServiceSpec(
            "name",
            "description",
            "serviceCategoryPath",
            "itype",
            "prj",
            new NannyAuthAttrsOwners(Collections.emptyList(), Collections.emptyList(), Collections.emptyList())
        );
    }

    @Test
    public void testGetDashboardName() {
        Assert.assertEquals(rtcServiceSpec.getDashboardName(), "prj_name");
        rtcServiceSpec.setDashboardName("custom_dashboard_name");
        Assert.assertEquals(rtcServiceSpec.getDashboardName(), "custom_dashboard_name");
    }
}

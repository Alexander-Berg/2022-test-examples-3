package ru.yandex.market.tsum.pipelines.common.jobs.hbf_close_dc;

import java.text.SimpleDateFormat;
import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.tsum.clients.dclocation.DcLocation;
import ru.yandex.market.tsum.pipelines.sre.resources.HBFCloseDCEvent;

public class NannyServicesCheckAllUnavailableTest {

    @Test
    public void validateServiceList() throws Exception {
        Instant fromInstant =
            new SimpleDateFormat("hh:mm dd.MM.yyyy z").parse("14:00 22.12.2022 GMT+05:00").toInstant();
        Instant toInstant = new SimpleDateFormat("hh:mm dd.MM.yyyy z").parse("15:00 22.12.2022 GMT+05:00").toInstant();
        HBFCloseDCEvent closeDCEvent = new HBFCloseDCEvent(fromInstant, toInstant, DcLocation.SAS,
            "Subj", "Description", "", "", "Close");

        String dc = closeDCEvent.getDc().getCanonicName().toLowerCase();
        String[] src = new String[]{"a_sas", "a_vla", "a_iva", "a_sas_sas_sas", "asas", "a_sas_vla"};
        String[] expected = new String[]{"a_sas", "a_sas_sas_sas"};
        String[] result = NannyServicesCheckAllUnavailable.validateServiceList(src, dc);

        Assert.assertArrayEquals(expected, result);
    }

}

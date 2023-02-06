package ru.yandex.market.mcrp_request.util;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mcrp_request.DAO.RTCResources;
import ru.yandex.market.mcrp_request.DAO.Request;
import ru.yandex.market.mcrp_request.dto.forecaster.ForecasterDto;
import ru.yandex.market.mcrp_request.dto.forecaster.RTCItem;
import ru.yandex.market.mcrp_request.util.forecaster.RTCForecasterParser;

public class RTCForecasterParserTest {

    RTCForecasterParser rtcForecasterParser = new RTCForecasterParser();
    ObjectMapper mapper = new ObjectMapper();

    @Test
    public void parseResurcesTest() throws IOException {
        String jsonStr = IOUtils.toString(RTCForecasterParserTest.class.getResourceAsStream("RTCForecaster.json"));
        ForecasterDto<RTCItem> dto = mapper.readValue(jsonStr, new TypeReference<ForecasterDto<RTCItem>>() {
        });
        Request request = rtcForecasterParser.parseRequest(dto);

        Assert.assertEquals("REPORT", request.getAbc());
        Assert.assertEquals("RTC", request.getCloud());
        Assert.assertEquals(List.of("aahms"), request.getResps());
        Assert.assertEquals("EVENLY", request.getDeadline());
        Assert.assertEquals("DAU", request.getReason());

        Assert.assertEquals("RTC", request.getResources().get(dto.getDeadline()).getKind());
        Assert.assertTrue(request.getResources().get(dto.getDeadline()) instanceof RTCResources);
        RTCResources resources = (RTCResources) request.getResources().get(dto.getDeadline());
        Assert.assertEquals(Set.of("VLA", "SAS", "MAN"), resources.getDCs());
        Map<String, List<RTCResources.RTCResource>> locations = resources.getLocations();

        Assert.assertEquals(1, locations.get("VLA").size());
        RTCResources.RTCResource rtcResource = locations.get("VLA").get(0);
        Assert.assertEquals(1680, rtcResource.getCPUCores(), 10e-16);
        Assert.assertEquals(0, rtcResource.getHDDGb(), 10e-16);
        Assert.assertEquals(0, rtcResource.getRAMGb(), 10e-16);
        Assert.assertEquals(0, rtcResource.getSSDGb(), 10e-16);
        Assert.assertEquals("", rtcResource.getServiceName());

        Assert.assertEquals(1, locations.get("SAS").size());
        rtcResource = locations.get("SAS").get(0);
        Assert.assertEquals(1680, rtcResource.getCPUCores(), 10e-16);
        Assert.assertEquals(0, rtcResource.getHDDGb(), 10e-16);
        Assert.assertEquals(0, rtcResource.getRAMGb(), 10e-16);
        Assert.assertEquals(0, rtcResource.getSSDGb(), 10e-16);
        Assert.assertEquals("", rtcResource.getServiceName());

        Assert.assertEquals(1, locations.get("MAN").size());
        rtcResource = locations.get("MAN").get(0);
        Assert.assertEquals(1680, rtcResource.getCPUCores(), 10e-16);
        Assert.assertEquals(0, rtcResource.getHDDGb(), 10e-16);
        Assert.assertEquals(0, rtcResource.getRAMGb(), 10e-16);
        Assert.assertEquals(0, rtcResource.getSSDGb(), 10e-16);
        Assert.assertEquals("", rtcResource.getServiceName());
    }
}

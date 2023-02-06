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

import ru.yandex.market.mcrp_request.DAO.Request;
import ru.yandex.market.mcrp_request.DAO.SAASResources;
import ru.yandex.market.mcrp_request.dto.forecaster.ForecasterDto;
import ru.yandex.market.mcrp_request.dto.forecaster.SaaSItem;
import ru.yandex.market.mcrp_request.util.forecaster.SaaSForecasterParser;

public class SaaSForecasterParserTest {

    SaaSForecasterParser saaSForecasterParser = new SaaSForecasterParser();
    ObjectMapper mapper = new ObjectMapper();

    @Test
    public void parseResurcesTest() throws IOException {
        String jsonStr = IOUtils.toString(SaaSForecasterParserTest.class.getResourceAsStream("SaaSForecaster.json"));
        ForecasterDto<SaaSItem> dto = mapper.readValue(jsonStr, new TypeReference<ForecasterDto<SaaSItem>>() {
        });
        Request request = saaSForecasterParser.parseRequest(dto);

        Assert.assertEquals("REPORT", request.getAbc());
        Assert.assertEquals("SAAS", request.getCloud());
        Assert.assertEquals(List.of("yuraaka", "zizu"), request.getResps());
        Assert.assertEquals("EVENLY", request.getDeadline());
        Assert.assertEquals("CUMULATIVE MARKETPREORDER-21", request.getReason());

        Assert.assertEquals("SAAS", request.getResources().get(dto.getDeadline()).getKind());
        Assert.assertTrue(request.getResources().get(dto.getDeadline()) instanceof SAASResources);
        SAASResources resources = (SAASResources) request.getResources().get(dto.getDeadline());
        Assert.assertEquals(Set.of("VLA", "SAS", "MAN"), resources.getDCs());
        Map<String, List<SAASResources.SAASResource>> locations = resources.getLocations();

        Assert.assertEquals(1, locations.get("VLA").size());
        SAASResources.SAASResource saasResource = locations.get("VLA").get(0);
        Assert.assertEquals(128000, saasResource.getCPUCores(), 10e-16);
        Assert.assertEquals(5944234737664.0, saasResource.getHDDGb(), 10e-16);
        Assert.assertEquals(858993459200.0, saasResource.getRAMGb(), 10e-16);
        Assert.assertEquals(4123168604160.0, saasResource.getSSDGb(), 10e-16);
        Assert.assertEquals("market_snippet", saasResource.getServiceName());

        Assert.assertEquals(1, locations.get("SAS").size());
        saasResource = locations.get("SAS").get(0);
        Assert.assertEquals(128000, saasResource.getCPUCores(), 10e-16);
        Assert.assertEquals(5944234737664.0, saasResource.getHDDGb(), 10e-16);
        Assert.assertEquals(858993459200.0, saasResource.getRAMGb(), 10e-16);
        Assert.assertEquals(4123168604160.0, saasResource.getSSDGb(), 10e-16);
        Assert.assertEquals("market_snippet", saasResource.getServiceName());

        Assert.assertEquals(1, locations.get("MAN").size());
        saasResource = locations.get("MAN").get(0);
        Assert.assertEquals(128000, saasResource.getCPUCores(), 10e-16);
        Assert.assertEquals(5944234737664.0, saasResource.getHDDGb(), 10e-16);
        Assert.assertEquals(858993459200.0, saasResource.getRAMGb(), 10e-16);
        Assert.assertEquals(4123168604160.0, saasResource.getSSDGb(), 10e-16);
        Assert.assertEquals("market_snippet", saasResource.getServiceName());
    }
}

package ru.yandex.market.mcrp_request.util;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mcrp_request.DAO.MDSResources;
import ru.yandex.market.mcrp_request.DAO.Request;
import ru.yandex.market.mcrp_request.dto.forecaster.ForecasterDto;
import ru.yandex.market.mcrp_request.dto.forecaster.MDSItem;
import ru.yandex.market.mcrp_request.util.forecaster.MDSForecasterParser;

public class MDSForecasterParserTest {
    MDSForecasterParser mdsForecasterParser = new MDSForecasterParser();
    ObjectMapper mapper = new ObjectMapper();

    @Test
    public void parseResurcesTest() throws IOException {
        String jsonStr = IOUtils.toString(MDBForecasterParserTest.class.getResourceAsStream("MDSForecaster.json"));
        ForecasterDto<MDSItem> dto = mapper.readValue(jsonStr, new TypeReference<ForecasterDto<MDSItem>>() {
        });
        Request request = mdsForecasterParser.parseRequest(dto);

        Assert.assertEquals("DELIVERY", request.getAbc());
        Assert.assertEquals("MDS", request.getCloud());
        Assert.assertEquals(List.of("ndolganov"), request.getResps());
        Assert.assertEquals("DEC", request.getDeadline());
        Assert.assertEquals("NEWSERVICE MARKETPREORDER-74", request.getReason());

        Assert.assertEquals("MDS", request.getResources().get(dto.getDeadline()).getKind());
        Assert.assertTrue(request.getResources().get(dto.getDeadline()) instanceof MDSResources);
        MDSResources resources = (MDSResources) request.getResources().get(dto.getDeadline());
        Assert.assertEquals(0.5, resources.getLocations().get("MDS").get(0).getStorageGb(), 10e-16);
    }
}

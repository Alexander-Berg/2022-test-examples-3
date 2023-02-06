package ru.yandex.market.mcrp_request.util;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.mcrp_request.DAO.MDBResources;
import ru.yandex.market.mcrp_request.DAO.Request;
import ru.yandex.market.mcrp_request.dto.forecaster.ForecasterDto;
import ru.yandex.market.mcrp_request.dto.forecaster.MDBItem;
import ru.yandex.market.mcrp_request.util.forecaster.MDBForecasterParser;

public class MDBForecasterParserTest {
    MDBForecasterParser mdbForecasterParser = new MDBForecasterParser();
    ObjectMapper mapper = new ObjectMapper();

    @Test
    public void parseResurcesTest() throws IOException {
        String jsonStr = IOUtils.toString(MDBForecasterParserTest.class.getResourceAsStream("MDBForecaster.json"));
        ForecasterDto<MDBItem> dto = mapper.readValue(jsonStr, new TypeReference<ForecasterDto<MDBItem>>() {
        });
        Request request = mdbForecasterParser.parseRequest(dto);

        Assert.assertEquals("DELIVERY", request.getAbc());
        Assert.assertEquals("PGAAS", request.getCloud());
        Assert.assertEquals(List.of("aahms"), request.getResps());
        Assert.assertEquals("EVENLY", request.getDeadline());
        Assert.assertEquals("CUMULATIVE MARKETPREORDER-35", request.getReason());

        Assert.assertEquals("MDB", request.getResources().get(dto.getDeadline()).getKind());
        Assert.assertTrue(request.getResources().get(dto.getDeadline()) instanceof MDBResources);
        MDBResources resources = (MDBResources) request.getResources().get(dto.getDeadline());
        Assert.assertEquals(3, resources.getLocations().get("PGAAS").get(0).getCPUCores(), 10e-16);
        Assert.assertEquals(0, resources.getLocations().get("PGAAS").get(0).getHDDGb(), 10e-16);
        Assert.assertEquals(0, resources.getLocations().get("PGAAS").get(0).getRAMGb(), 10e-16);
        Assert.assertEquals(184320, resources.getLocations().get("PGAAS").get(0).getSSDGb(), 10e-16);
    }
}

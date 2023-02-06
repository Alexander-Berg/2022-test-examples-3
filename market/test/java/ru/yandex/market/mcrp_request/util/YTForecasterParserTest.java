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
import ru.yandex.market.mcrp_request.DAO.YTResources;
import ru.yandex.market.mcrp_request.dto.forecaster.ForecasterDto;
import ru.yandex.market.mcrp_request.dto.forecaster.YTItem;
import ru.yandex.market.mcrp_request.util.forecaster.YTForecasterParser;

public class YTForecasterParserTest {
    YTForecasterParser ytForecasterParser = new YTForecasterParser();
    ObjectMapper mapper = new ObjectMapper();

    @Test
    public void parseResurcesTest() throws IOException {
        String jsonStr = IOUtils.toString(YTForecasterParserTest.class.getResourceAsStream("YTForecaster.json"));
        ForecasterDto<YTItem> dto = mapper.readValue(jsonStr, new TypeReference<ForecasterDto<YTItem>>() {
        });
        Request request = ytForecasterParser.parseRequest(dto);

        Assert.assertEquals("ABO", request.getAbc());
        Assert.assertEquals("YT", request.getCloud());
        Assert.assertEquals(List.of("aahms"), request.getResps());
        Assert.assertEquals("EVENLY", request.getDeadline());
        Assert.assertEquals(
                "CUMULATIVE https://a.yandex-team.ru/arc/trunk/arcadia/market/sre/tools/capacity/preorder/v2020apr/additional/YT/calculations/other.ipynb",
                request.getReason());

        Assert.assertEquals("YT", request.getResources().get(dto.getDeadline()).getKind());
        Assert.assertTrue(request.getResources().get(dto.getDeadline()) instanceof YTResources);
        YTResources resources = (YTResources) request.getResources().get(dto.getDeadline());
        Assert.assertEquals(Set.of("ARNOLD"), resources.getClusters());
        Map<String, List<YTResources.YTResource>> locations = resources.getLocations();

        Assert.assertEquals(1, locations.get("ARNOLD").size());
        YTResources.YTResource resource = locations.get("ARNOLD").get(0);
        Assert.assertEquals(10, resource.getCPUCores(), 10e-16);
        Assert.assertEquals("pool_name", resource.getPools());
        Assert.assertEquals(0, resource.getDinTableRAMGb(), 10e-16);
        Assert.assertEquals("market-abo-development", resource.getAccounts());
        Assert.assertEquals(1024, resource.getHDDGb(), 10e-16);
        Assert.assertEquals(0, resource.getSSDGb(), 10e-16);
    }
}

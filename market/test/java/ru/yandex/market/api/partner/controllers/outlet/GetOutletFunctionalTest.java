package ru.yandex.market.api.partner.controllers.outlet;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.api.partner.controllers.outlet.model.OutletDTO;
import ru.yandex.market.api.partner.controllers.outlet.model.OutletsPagerWrapperDTO;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.jaxb.jackson.ApiObjectMapperFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Проверяет пейджинг аутлетов.
 */
@DbUnitDataSet(before = "GetOutletFunctionalTest.before.csv")
class GetOutletFunctionalTest extends FunctionalTest {

    @Test
    void testGetOutletsPaging() throws IOException {
        ObjectMapper jsonMapper = new ApiObjectMapperFactory().createJsonMapper();

        //первая страница
        OutletsPagerWrapperDTO pagerWrapperDTO = sendRequestGetOutletsWrapper(jsonMapper, "");

        String nextToken = pagerWrapperDTO.getPaging().getNextPageToken();
        assertNull( pagerWrapperDTO.getPaging().getPrevPageToken());
        assertEquals("MjU", nextToken);
        assertEquals(25, pagerWrapperDTO.getOutlets().size());

        //вторая страница
        OutletsPagerWrapperDTO pagerWrapperDTO1 = sendRequestGetOutletsWrapper(jsonMapper, nextToken);

        String nextToken1 = pagerWrapperDTO1.getPaging().getNextPageToken();
        assertEquals("NTA", nextToken1);
        assertEquals("MA", pagerWrapperDTO1.getPaging().getPrevPageToken());
        assertEquals(25, pagerWrapperDTO1.getOutlets().size());

        //третья страница
        OutletsPagerWrapperDTO pagerWrapperDTO2 = sendRequestGetOutletsWrapper(jsonMapper, nextToken1);
        String nextToken2 = pagerWrapperDTO2.getPaging().getNextPageToken();
        assertEquals("NzU",nextToken2);
        assertEquals("MjU",pagerWrapperDTO2.getPaging().getPrevPageToken());
        assertEquals(25, pagerWrapperDTO2.getOutlets().size());

        //четвертая страница
        OutletsPagerWrapperDTO pagerWrapperDTO3 = sendRequestGetOutletsWrapper(jsonMapper, nextToken2);
        String nextToken3 = pagerWrapperDTO3.getPaging().getNextPageToken();
        assertNull(nextToken3);
        assertEquals("NTA",pagerWrapperDTO3.getPaging().getPrevPageToken());
        assertEquals(1, pagerWrapperDTO3.getOutlets().size());

        // вернулись на третю страницу
        OutletsPagerWrapperDTO pagerWrapperDTO5 = sendRequestGetOutletsWrapper(jsonMapper, pagerWrapperDTO3.getPaging().getPrevPageToken());

        List<OutletDTO> outletsResult = pagerWrapperDTO5.getOutlets();
        assertEquals(25, outletsResult.size());
        assertTrue(pagerWrapperDTO2.getOutlets().containsAll(outletsResult));
    }

    private OutletsPagerWrapperDTO sendRequestGetOutletsWrapper(ObjectMapper jsonMapper,
                                                                String pageToken) throws IOException {
        ResponseEntity<String> response = FunctionalTestHelper
                .makeRequest(
                        outletsUrl("?limit=25" + (pageToken.isEmpty() ? "" : "&page_token=" + pageToken)),
                        HttpMethod.GET, Format.JSON);

        return jsonMapper.readValue(response.getBody(), OutletsPagerWrapperDTO.class);
    }


    private String outletsUrl(String token) {
        return String.format("%s/campaigns/%d/outlets%s", urlBasePrefix, 10776L, token);
    }

}

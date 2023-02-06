package ru.yandex.market.loyalty.back.controller;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.gdpr.RebindMapping;
import ru.yandex.market.loyalty.core.gdpr.RebindMappingDao;
import ru.yandex.market.loyalty.test.TestFor;

import java.util.List;

import static org.junit.Assert.assertEquals;

@TestFor(GdprDataDeleteController.class)
public class GdprDataDeleteControllerTest extends MarketLoyaltyBackMockedDbTestBase {

    @Autowired
    RebindMappingDao rebindMappingDao;
    @Autowired
    MockMvc mockMvc;


    @Test
    public void shouldSaveGdprRebinding() throws Exception {
        ensureGdprRebindSaved(0);
        mockMvc
                .perform(MockMvcRequestBuilders.post("/gdpr/rebind").content(
                        "{" +
                                "  \"rebindList\": [" +
                                "    {\"puid\": 1, \"muid\":  2}," +
                                "    {\"puid\": 3, \"muid\":  4}," +
                                "    {\"puid\": 5, \"muid\":  6}" +
                                "  ]\n" +
                                "}"
                ).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(MockMvcResultMatchers.status().isOk());
        ensureGdprRebindSaved(3);
        checkIdentifiers(new int[]{1, 3, 5}, new int[]{2, 4, 6});
    }

    private void checkIdentifiers(int[] puids, int[] muids) {
        List<RebindMapping> rebindMappings = rebindMappingDao.getUnprocessedMappings();
        for (int i = 0; i < puids.length; i++) {
            assertEquals(puids[i], rebindMappings.get(i).getFrom());
            assertEquals(muids[i], rebindMappings.get(i).getTo());
        }
    }

    private void ensureGdprRebindSaved(long count) {
        assertEquals(
                count,
                rebindMappingDao.getUnprocessedMappings().size()
        );
    }

}

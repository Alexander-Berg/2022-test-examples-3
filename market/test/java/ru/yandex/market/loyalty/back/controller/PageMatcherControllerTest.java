package ru.yandex.market.loyalty.back.controller;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.health.factories.AbstractAlertFactory;
import ru.yandex.market.health.model.Alert;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PageMatcherControllerTest extends MarketLoyaltyBackMockedDbTestBase {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testSmoke() throws Exception {
        mockMvc.perform(get("/pagematch"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void testAlerts() throws Exception {
        mockMvc.perform(get("/health/solomon"))
                .andExpect(status().is2xxSuccessful());
    }

    @Test
    public void testAlertIdNotExceed64CharAndUnique() throws Exception {
        String content = mockMvc.perform(get("/health/solomon"))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();
        List<Alert> value = objectMapper.readValue(content, new TypeReference<>() {
        });
        var set = new HashSet<String>();
        var wrongIds = value.stream()
                .filter(e -> {
                    if (set.contains(e.getId())) {
                        return true;
                    } else {
                        set.add(e.getId());
                        return false;
                    }
                })
                .map(Alert::getId)
                .collect(Collectors.toSet());
        Assert.assertTrue("found not unique ids: " + String.join(", ", wrongIds), wrongIds.isEmpty());
        String longIds = value.stream()
                .map(Alert::getId)
                .filter(id -> id.length() > AbstractAlertFactory.MAX_ID_LENGTH)
                .collect(Collectors.joining(","));
        Assert.assertEquals("found ids that exceeds 64 chars", "", longIds);
    }

    @Test
    public void testPlaybookListRequestHandler() throws Exception {
        mockMvc.perform(get("/health/juggler/alerts").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().is2xxSuccessful());
    }
}

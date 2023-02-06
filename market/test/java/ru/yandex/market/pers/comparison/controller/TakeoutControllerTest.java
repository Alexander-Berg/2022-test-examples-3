package ru.yandex.market.pers.comparison.controller;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.comparator.CustomComparator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.pers.comparison.PersComparisonTest;
import ru.yandex.market.pers.comparison.model.ComparisonItem;
import ru.yandex.market.pers.comparison.model.UserType;
import ru.yandex.market.pers.comparison.service.ComparisonServiceNew;

import static org.springframework.test.web.servlet.ResultMatcher.matchAll;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TakeoutControllerTest extends PersComparisonTest {
    private final static String EMPTY_UID = "971449290";
    private final static String NON_EMPTY_UID = "837814830";

    private final static String NOT_EMPTY_STATUS_STR = "{\"types\": [\"comparisons\"]}";
    private final static String EMPTY_STATUS_STR = "{\"types\": []}";
    private final static ComparisonItem item = new ComparisonItem(1, "2");
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ComparisonServiceNew comparisonServicePg;

    @Test
    public void testGetEmptyData() throws Exception {
        mockMvc.perform(get(String.format("/takeout/data"))
                .param("uid", EMPTY_UID))
                .andExpect(matchAll(
                        status().isOk(),
                        content().json("{\"comparison\":[]}", true)));
    }

    @Test()
    public void testGetNotEmptyData() throws Exception {
        saveItem(NON_EMPTY_UID);
        mockMvc.perform(get(String.format("/takeout/data"))
                .param("uid", NON_EMPTY_UID))
                .andExpect(matchAll(
                        status().isOk(),
                        result -> {
                            String contentAsString = result.getResponse().getContentAsString();
                            JSONAssert.assertEquals(
                                    "{\"comparison\":[{\"id\":null,\"categoryId\":1," +
                                            "\"productId\":\"2\",\"sku\":null,\"regionId\":null," +
                                            "\"creationTime\":null}]}",
                                    contentAsString,
                                    new CustomComparator(
                                            JSONCompareMode.STRICT,
                                            new Customization("comparison[*].creationTime", (o1, o2) -> true),
                                            new Customization("comparison[*].id", (o1, o2) -> true))
                            );
                        }));
    }

    @Test
    public void testGetEmptyStatus() throws Exception {
        getStatusByUidAndAssert(EMPTY_UID, EMPTY_STATUS_STR);
    }

    @Test
    public void testGetNotEmptyStatus() throws Exception {
        saveItem(NON_EMPTY_UID);
        getStatusByUidAndAssert(NON_EMPTY_UID, NOT_EMPTY_STATUS_STR);
    }

    @Test
    public void testDeleteNothing() throws Exception {
        deleteByUid(EMPTY_UID);
    }


    @Test
    public void testDeleteItem() throws Exception {
        saveItem(NON_EMPTY_UID);
        getStatusByUidAndAssert(NON_EMPTY_UID, NOT_EMPTY_STATUS_STR);
        deleteByUid(NON_EMPTY_UID);
        getStatusByUidAndAssert(NON_EMPTY_UID, EMPTY_STATUS_STR);

    }

    private void saveItem(String uid) {
        comparisonServicePg.saveItem(UserType.UID, uid, item);
    }

    private void getStatusByUidAndAssert(String uid, String expectedContent) throws Exception {
        mockMvc.perform(get(String.format("/takeout/status"))
                .param("uid", uid))
                .andExpect(matchAll(
                        status().isOk(),
                        content().json(expectedContent, true)));
    }

    private void deleteByUid(String uid) throws Exception {
        mockMvc.perform(post(String.format("/takeout/delete"))
                .param("uid", uid)
                .param("types", TakeoutController.COMPARISON_TYPE))
                .andExpect(matchAll(
                        status().isOk(),
                        content().string("")));
    }
}

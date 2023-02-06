package ru.yandex.market.wms.autostart.controller;

import java.util.stream.Stream;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.wms.autostart.AutostartIntegrationTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.wms.common.spring.utils.FileContentUtils.getFileContent;

public class PickDetailsControllerTest extends AutostartIntegrationTest {

    @Test
    @DatabaseSetup("/controller/pickdetails/db/immutable-state.xml")
    public void listPickDetailsReturnsLastPageOffsetEqualToWavesCount() throws Exception {
        ResultActions result = mockMvc.perform(get("/pick-details")
                .param("offset", "3")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/pickdetails/response/last-page-with-offset-equal-to-pickdetails-count.json")));
    }

    @Test
    @DatabaseSetup("/controller/pickdetails/db/immutable-state.xml")
    public void listPickDetailsReturnsLastPageOffsetGreaterThanoWavesCount() throws Exception {
        ResultActions result = mockMvc.perform(get("/pick-details")
                .param("offset", "30")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/pickdetails/response/last-page-with-offset-greater-than-pickdetails-count.json")));
    }

    @Test
    @DatabaseSetup("/controller/pickdetails/db/immutable-state.xml")
    public void testListPickDetailsSequentialPageLoadsFromFirstPage() throws Exception {
        ResultActions result = mockMvc.perform(get("/pick-details")
                .param("limit", "1")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/pickdetails/response/first-page-offset-not-set.json")));
    }

    @Test
    @DatabaseSetup("/controller/pickdetails/db/immutable-state.xml")
    public void testListPickDetailsSequentialPageLoadsNextPage() throws Exception {
        ResultActions result = mockMvc.perform(get("/pick-details")
                .param("offset", "1")
                .param("limit", "1")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/pickdetails/response/next-page-offset-set.json")));
    }

    @Test
    @DatabaseSetup("/controller/pickdetails/db/immutable-state.xml")
    public void testListPickDetailsSequentialPageLoadsFromFirstPageWithDescSortByStatus() throws Exception {
        ResultActions result = mockMvc.perform(get("/pick-details")
                .param("limit", "1")
                .param("sort", "status")
                .param("order", "desc")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/pickdetails/response/order-by-status-desc.json")));
    }

    @ParameterizedTest
    @MethodSource("testListPickDetailsSequentialPageLoadsFromFirstPageWithFilterByStatusArgs")
    @DatabaseSetup("/controller/pickdetails/db/immutable-state.xml")
    public void testListPickDetailsSequentialPageLoadsFromFirstPageWithFilterByStatus(
            String filter,
            String resultFile
    ) throws Exception {
        ResultActions result = mockMvc.perform(get("/pick-details")
                .param("limit", "5")
                .param("filter", filter)
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/pickdetails/response/" + resultFile)));
    }

    @Test
    @DatabaseSetup("/controller/pickdetails/db/immutable-state.xml")
    public void testListPickDetailsSequentialPageLoadsFromFirstPageWithFilterByEditDate() throws Exception {
        ResultActions result = mockMvc.perform(get("/pick-details")
                .param("limit", "5")
                .param("filter", "editDate=='2021-07-07 15:00:00'")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/pickdetails/response/filter-by-edit-date.json")));
    }

    @Test
    @DatabaseSetup("/controller/pickdetails/db/immutable-state.xml")
    public void testListPickDetailsSequentialPageLoadsFromFirstPageWithFilterByEditDateRange() throws Exception {
        ResultActions result = mockMvc.perform(get("/pick-details")
                .param("limit", "5")
                .param("filter", "editDate=ge='2021-07-07 15:00:00';editDate=le='2021-07-07 16:00:59'")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/pickdetails/response/filter-by-edit-date-range.json")));
    }

    @Test
    @DatabaseSetup("/controller/pickdetails/db/immutable-state.xml")
    public void testListPickDetailsSequentialPageLoadsFromFirstPageWithFilterByAddDate() throws Exception {
        ResultActions result = mockMvc.perform(get("/pick-details")
                .param("limit", "5")
                .param("filter", "addDate=='2021-07-06 15:00:00'")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/pickdetails/response/filter-by-add-date.json")));
    }

    @Test
    @DatabaseSetup("/controller/pickdetails/db/immutable-state.xml")
    public void testListPickDetailsSequentialPageLoadsFromFirstPageWithFilterByAddDateRange() throws Exception {
        ResultActions result = mockMvc.perform(get("/pick-details")
                .param("limit", "5")
                .param("filter", "addDate=ge='2021-07-06 15:00:00';addDate=le='2021-07-06 16:00:59'")
                .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk())
                .andExpect(content().json(getFileContent(
                        "controller/pickdetails/response/filter-by-add-date-range.json")));
    }

    @Test
    @DatabaseSetup("/controller/pickdetails/db/reserve-no-lost-pickdetails.xml")
    public void testReserveNothingToReserve() throws Exception {
        ResultActions result = mockMvc.perform(
                post("/pick-details/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(getFileContent(
                                "controller/pickdetails/request/reserve-no-lost-pickdetails.json")
                        )
        );
        result.andExpect(status().is4xxClientError())
                .andExpect(content().json(getFileContent(
                        "controller/pickdetails/response/reserve-no-lost-pickdetails.json")));
    }

    private static Stream<Arguments> testListPickDetailsSequentialPageLoadsFromFirstPageWithFilterByStatusArgs() {
        return Stream.of(
                Arguments.of(
                        "status=gt=NORMAL",
                        "filter-by-status.json"
                ),
                Arguments.of(
                        "status=lt=SORTED_BY_DELIVERY_SERVICE",
                        "filter-by-status-2.json"
                )
        );
    }
}

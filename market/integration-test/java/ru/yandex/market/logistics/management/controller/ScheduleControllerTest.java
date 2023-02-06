package ru.yandex.market.logistics.management.controller;

import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.entity.request.schedule.CourierScheduleFilter;
import ru.yandex.market.logistics.management.entity.request.schedule.ScheduleDayFilter;
import ru.yandex.market.logistics.management.service.geobase.GeoBaseRegionsService;
import ru.yandex.market.logistics.management.util.CleanDatabase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@CleanDatabase
@Sql("/data/controller/schedule/prepare_data.sql")
class ScheduleControllerTest extends AbstractContextualTest {
    private static final String URI = "/externalApi/schedule";
    private static final String COURIER_URI = URI + "/courier";

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GeoBaseRegionsService geoBaseRegionsService;

    @Test
    void getByIdSuccessful() throws Exception {
        mockMvc.perform(get(URI + "/1"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/schedule/got_by_id.json"));
    }

    @Test
    void getByNonexistentId() throws Exception {
        mockMvc.perform(get(URI + "/123"))
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.content().string("null"));
    }

    @Test
    void getByIds() throws Exception {
        mockMvc.perform(put(URI)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(objectMapper.writeValueAsBytes(new ScheduleDayFilter(ImmutableSet.of(1L, 2L)))))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/schedule/got_by_ids.json"));
    }

    @Test
    void getByNullIds() throws Exception {
        mockMvc.perform(put(URI)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(objectMapper.writeValueAsBytes(new ScheduleDayFilter(Sets.newHashSet(1L, null)))))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getByNull() throws Exception {
        mockMvc.perform(put(URI)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(objectMapper.writeValueAsBytes(null)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getByEmptyCollection() throws Exception {
        mockMvc.perform(put(URI)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(objectMapper.writeValueAsBytes(new ScheduleDayFilter(ImmutableSet.of()))))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/empty_entities.json"));
    }

    @Test
    void searchCourier() throws Exception {
        geoBaseRegionsService.syncRegions();

        searchCourierSchedule(CourierScheduleFilter.newBuilder()
            .partnerIds(ImmutableSet.of(10L, 14L))
            .locationIds(Set.of(213, 1))
            .build())
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/schedule/courier_by_ids.json"));
    }

    @Test
    void searchCourierValidation() throws Exception {
        searchCourierSchedule(CourierScheduleFilter.newBuilder().build())
            .andExpect(status().isBadRequest());
    }

    @Test
    void searchCourierValidationEmptyIds() throws Exception {
        searchCourierSchedule(CourierScheduleFilter.newBuilder().partnerIds(ImmutableSet.of()).build())
            .andExpect(status().isBadRequest());
    }

    @Test
    void searchCourierValidationNullIds() throws Exception {
        searchCourierSchedule(CourierScheduleFilter.newBuilder().partnerIds(Sets.newHashSet((Long) null)).build())
            .andExpect(status().isBadRequest());
    }

    @Test
    void searchCourierNoFilter() throws Exception {
        mockMvc.perform(put(COURIER_URI)
            .contentType(MediaType.APPLICATION_JSON_UTF8))
            .andExpect(status().isBadRequest());
    }

    @Test
    void searchCourierBadLocationId() throws Exception {
        geoBaseRegionsService.syncRegions();

        searchCourierSchedule(CourierScheduleFilter.newBuilder()
            .partnerIds(ImmutableSet.of(10L))
            .locationIds(Set.of(21))
            .build())
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/empty_entities.json"));
    }

    @Test
    void searchCourierEmpty() throws Exception {
        searchCourierSchedule(CourierScheduleFilter.newBuilder()
            .partnerIds(ImmutableSet.of(11L))
            .locationIds(Set.of(213))
            .build())
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/empty_entities.json"));
    }

    private ResultActions searchCourierSchedule(CourierScheduleFilter filter) throws Exception {
        return mockMvc.perform(put(COURIER_URI)
            .contentType(MediaType.APPLICATION_JSON_UTF8)
            .content(objectMapper.writeValueAsBytes(filter)));
    }
}

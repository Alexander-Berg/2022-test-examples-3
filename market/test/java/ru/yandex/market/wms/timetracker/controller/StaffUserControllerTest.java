package ru.yandex.market.wms.timetracker.controller;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import ru.yandex.market.wms.timetracker.config.NoAuthTest;
import ru.yandex.market.wms.timetracker.dto.StaffUserDto;

import ru.yandex.market.wms.timetracker.service.StaffUserService;

@WebMvcTest(StaffUserController.class)
@ActiveProfiles("test")
@Import(NoAuthTest.class)
public class StaffUserControllerTest {

    @MockBean
    private StaffUserService staffUserService;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    public static class TestConfig {
        @Bean
        public StaffUserService staffUserService() {
            return Mockito.mock(StaffUserService.class);
        }
    }

    @Test
    public void getStaffUsers() throws Exception {
        final List<StaffUserDto> contentExpected = List.of(
                StaffUserDto.builder()
                        .staffLogin("test")
                        .wmsLogin("sof-test")
                        .build(),
                StaffUserDto.builder()
                        .staffLogin("test2")
                        .wmsLogin("sof-test2")
                        .build()
        );

        Mockito.when(staffUserService.getUsersStaffLogin(ArgumentMatchers.eq("SOF"),
                        ArgumentMatchers.anyList()))
                .thenReturn(contentExpected);

        final String jsonModel = mapper.writeValueAsString(contentExpected);

        mockMvc
                .perform(
                        MockMvcRequestBuilders.get("/employee-staff/SOF?logins=test1,test2")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json(jsonModel));
    }

    @Test
    public void getStaffUsersWhenEmpty() throws Exception {
        mockMvc
                .perform(
                        MockMvcRequestBuilders.get("/employee-staff/SOF?logins=")
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().json("[]"));
    }
}

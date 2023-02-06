package ru.yandex.market.loyalty.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.loyalty.admin.config.MarketLoyaltyAdmin;
import ru.yandex.market.loyalty.admin.controller.dto.UpdateTmsScheduleDto;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.admin.tms.repository.Shedlock;
import ru.yandex.market.loyalty.admin.tms.repository.ShedlockDao;
import ru.yandex.market.loyalty.admin.tms.repository.ShedlockHistory;
import ru.yandex.market.loyalty.admin.tms.repository.ShedlockHistoryRepository;
import ru.yandex.market.loyalty.test.TestFor;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.log;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestFor(AdminTmsController.class)
public class AdminTmsControllerTest extends MarketLoyaltyAdminMockedDbTest {

    private static final String SHEDLOCK_NAME = "SHEDLOCK_NAME_TMS_TEST";
    private static final String SHEDLOCK_HISTORY_NAME = "SHEDLOCK_HISTORY_NAME";
    private static final String SHEDLOCK_HISTORY_NAME2 = "SHEDLOCK_HISTORY_NAME2";

    @Autowired
    private MockMvc mockMvc;

    @MarketLoyaltyAdmin
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShedlockDao shedlockDao;

    @Autowired
    private ShedlockHistoryRepository shedlockHistoryRepository;

    @Before
    public void prepareData() {
        try {
            shedlockDao.insert(new Shedlock(SHEDLOCK_NAME, new Date(), new Date(), "lock", null));
        } catch (DuplicateKeyException ignore) {
        }
        try {
            ShedlockHistory shedlockHistory = ShedlockHistory.ShedlockHistoryBuilder.builder()
                    .withName(SHEDLOCK_HISTORY_NAME)
                    .build();
            ShedlockHistory shedlockHistory2 = ShedlockHistory.ShedlockHistoryBuilder.builder()
                    .withName(SHEDLOCK_HISTORY_NAME2)
                    .build();
            shedlockHistoryRepository.save(shedlockHistory);
            shedlockHistoryRepository.save(shedlockHistory2);
        } catch (DuplicateKeyException ignore) {
        }
    }

    @Test
    public void getShedlockHistory() throws Exception {
        String contentAsString = mockMvc
                .perform(
                        get("/api/tms/history?page=1&size=10")
                                .with(csrf())
                )
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn()
                .getResponse()
                .getContentAsString();


        assertTrue(StringUtils.isNoneBlank(contentAsString));
        assertTrue(contentAsString.contains(SHEDLOCK_HISTORY_NAME));
        assertTrue(contentAsString.contains(SHEDLOCK_HISTORY_NAME2));
    }

    @Test
    public void getShedlockHistoryByName() throws Exception {
        String contentAsString = mockMvc
                .perform(
                        get("/api/tms/history?name=2&page=1&size=10")
                                .with(csrf())
                )
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn()
                .getResponse()
                .getContentAsString();


        assertTrue(StringUtils.isNoneBlank(contentAsString));
        assertTrue(contentAsString.contains(SHEDLOCK_HISTORY_NAME2));
    }

    @Test
    public void getShedlock() throws Exception {
        String contentAsString = mockMvc
                .perform(
                        get("/api/tms")
                                .with(csrf())
                )
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertTrue(StringUtils.isNoneBlank(contentAsString));
        assertTrue(contentAsString.contains(SHEDLOCK_NAME));
    }

    @Test
    public void test_updateDisabledUntil() throws Exception {
        Date date = new Date();
        UpdateTmsScheduleDto dto = new UpdateTmsScheduleDto(SHEDLOCK_NAME, date, "Some message");
        mockMvc
                .perform(
                        put("/api/tms")
                                .content(objectMapper.writeValueAsString(dto))
                                .contentType(MediaType.APPLICATION_JSON_VALUE)
                                .with(csrf())
                )
                .andDo(log())
                .andExpect(status().is(HttpStatus.OK.value()));

        Shedlock shedlock = shedlockDao.find(SHEDLOCK_NAME);

        assertEquals(date, shedlock.getDisabledUntil());
    }
}

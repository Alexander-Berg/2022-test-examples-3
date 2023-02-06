package ru.yandex.market.sc.api.controller;

import java.time.Clock;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.api.test.ScApiControllerTest;
import ru.yandex.market.sc.core.domain.order.model.ApiOrderStatus;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.stock.repository.StockLogEntity;
import ru.yandex.market.sc.core.domain.stock.repository.StockLogRepository;
import ru.yandex.market.sc.core.test.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author valter
 */
@ScApiControllerTest
class StockControllerTest {

    private static final long UID = 123L;

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestFactory testFactory;
    @Autowired
    StockLogRepository stockLogRepository;
    @Autowired
    Clock clock;

    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.storedUser(sortingCenter, UID);
    }

    @Test
    void writeStockLogFull() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/stock/log")
                        .header("Authorization", "OAuth uid-" + UID)
                        .contentType("application/json")
                        .content(fullStockLogEntityJson())
        )
                .andDo(print())
                .andExpect(status().isOk());
        assertThat(stockLogRepository.findAllBySortingCenterId(sortingCenter.getId()))
                .usingElementComparatorIgnoringFields("id", "createdAt", "updatedAt")
                .isEqualTo(List.of(fullStockLogEntity()));
    }

    @Test
    void writeStockLogEmpty() throws Exception {
        mockMvc.perform(
                MockMvcRequestBuilders.post("/api/stock/log")
                        .header("Authorization", "OAuth uid-" + UID)
                        .contentType("application/json")
                        .content(emptyStockLogEntityJson())
        )
                .andDo(print())
                .andExpect(status().isOk());
        assertThat(stockLogRepository.findAllBySortingCenterId(sortingCenter.getId()))
                .usingElementComparatorIgnoringFields("id", "createdAt", "updatedAt")
                .isEqualTo(List.of(emptyStockLogEntity()));
    }

    private StockLogEntity fullStockLogEntity() {
        return new StockLogEntity(
                clock.instant(),
                sortingCenter.getId(),
                "123", "123-1", 2L, 1L, 3L,
                ApiOrderStatus.OK,
                ApiOrderStatus.OK
        );
    }

    private String fullStockLogEntityJson() {
        return "{ " +
                "  \"order\": { " +
                "    \"externalId\": \"123\", " +
                "    \"status\": \"OK\", " +
                "    \"place\": { " +
                "      \"externalId\": \"123-1\", " +
                "      \"status\": \"OK\", " +
                "      \"cell\":{ " +
                "        \"id\":1 " +
                "      } " +
                "    }, " +
                "    \"cell\":{ " +
                "      \"id\":2 " +
                "    } " +
                "  }, " +
                "  \"cell\": { " +
                "    \"id\":3 " +
                "  } " +
                "}";
    }

    private StockLogEntity emptyStockLogEntity() {
        return new StockLogEntity(
                clock.instant(),
                sortingCenter.getId(),
                "123", null, null, null, 3,
                ApiOrderStatus.ERROR
        );
    }

    private String emptyStockLogEntityJson() {
        return "{ " +
                "  \"order\": { " +
                "    \"externalId\": \"123\", " +
                "    \"status\": \"ERROR\" " +
                "  }, " +
                "  \"cell\": { " +
                "    \"id\":3 " +
                "  } " +
                "}";
    }

}

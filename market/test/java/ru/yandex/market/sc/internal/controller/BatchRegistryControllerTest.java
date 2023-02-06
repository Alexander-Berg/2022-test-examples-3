package ru.yandex.market.sc.internal.controller;

import java.nio.file.Files;
import java.time.Clock;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import ru.yandex.market.sc.core.domain.lot.repository.LotStatus;
import ru.yandex.market.sc.core.domain.order.repository.ScOrder;
import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.sortable.lot.SortableLot;
import ru.yandex.market.sc.core.domain.sortable.model.enums.SortableType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.domain.user.repository.User;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.test.TestFactory.order;

@ScIntControllerTest
class BatchRegistryControllerTest {

    private static final long UID = 123L;

    @Autowired
    MockMvc mockMvc;
    @Autowired
    TestFactory testFactory;
    @Autowired
    ScOrderRepository scOrderRepository;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    ResourceLoader resourceLoader;
    @MockBean
    Clock clock;
    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
        testFactory.setSortingCenterProperty(
                sortingCenter.getId(), SortingCenterPropertiesKey.SUPPORTS_SORT_LOTS_WITHOUT_CELL, "true");
        testFactory.storedUser(sortingCenter, UID);
        testFactory.setupMockClock(clock);
    }

    @Test
    @SneakyThrows
    void getBatchRegistries() {
        String batchRegister = "br-0001";
        User user = testFactory.getOrCreateStoredUser(sortingCenter);
        ScOrder o1 = testFactory.createForToday(order(sortingCenter).externalId("o1").build())
                .accept().sort().get();

        ScOrder o2 = testFactory.createForToday(order(sortingCenter).externalId("o2").build())
                .accept().sort().get();

        ScOrder o3 = testFactory.createForToday(
                        order(sortingCenter).externalId("o3").places("o3-1", "o3-2", "o3-3").build())
                .acceptPlaces("o3-1", "o3-2", "o3-3").sortPlaces("o3-1", "o3-2", "o3-3").get();

        ScOrder o4 = testFactory.createForToday(order(sortingCenter).externalId("o4").build())
                .accept().sort().get();

        jdbcTemplate.update("update orders set batch_register = ? where id in (?,?,?,?)",
                batchRegister, o1.getId(), o2.getId(), o3.getId(), o4.getId());

        SortableLot l1 = testFactory.storedLot(sortingCenter, SortableType.PALLET, testFactory.orderPlace(o1).getCell(), LotStatus.CREATED, false, null);
        testFactory.sortPlaceToLot(testFactory.orderPlace(o1), l1, user);
        SortableLot l2 = testFactory.storedLot(sortingCenter, SortableType.PALLET, testFactory.orderPlace(o2).getCell(), LotStatus.CREATED, false, null);
        testFactory.sortPlaceToLot(testFactory.orderPlace(o2), l2, user);
        SortableLot l3 = testFactory.storedLot(sortingCenter, SortableType.PALLET, testFactory.anyOrderPlace(o3).getCell(), LotStatus.CREATED, false, null);
        testFactory.sortPlaceToLot(testFactory.orderPlace(o3, "o3-1"), l3, user);
        testFactory.sortPlaceToLot(testFactory.orderPlace(o3, "o3-2"), l3, user);
        testFactory.sortPlaceToLot(testFactory.orderPlace(o3, "o3-3"), l3, user);
        testFactory.sortPlaceToLot(testFactory.orderPlace(o4), l3, user);

        String jsonExpected = new String(
                Files.readAllBytes(
                        resourceLoader.getResource("classpath:response/batch_registry" +
                                "/BatchRegistryResponse.json").getFile().toPath())
        );

        mockMvc.perform(
                get("/internal/batchRegistry/" + batchRegister)
                        .header("Authorization", "OAuth uid-" + UID)
        )
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(content().json(jsonExpected));
    }

    @Test
    @SneakyThrows
    void batchRegistryIsEmpty() {
        String batchRegister = "br-0002";

        mockMvc.perform(
                get("/internal/batchRegistry/" + batchRegister)
                        .header("Authorization", "OAuth uid-" + UID)
        )
                .andExpect(status().isOk())
                .andExpect(content().json("{\"batches\":[]}"));
    }

}

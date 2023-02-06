package ru.yandex.market.sc.internal.controller;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.core.domain.order.repository.ScOrderRepository;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.warehouse.repository.Warehouse;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;

/**
 * @author valter
 */
@ScIntControllerTest
@Disabled("long")
public class MultithreadingCreateOrderTest {

    @Autowired
    TestFactory testFactory;
    @Autowired
    ScOrderRepository scOrderRepository;
    @Autowired
    MockMvc mockMvc;

    @Test
    void createManyOrders() {
        SortingCenter sortingCenter = testFactory.storedSortingCenter();
        testFactory.storedCell(sortingCenter);
        Warehouse warehouse = testFactory.storedWarehouse();

        ExecutorService pool = Executors.newFixedThreadPool(20);
        AtomicLong externalId = new AtomicLong(0L);
        long courierId = 1000L;
        String warehouseId = warehouse.getYandexId();
        AtomicLong errorCount = new AtomicLong(0L);
        for (int i = 0; i < 10; i++) {
            courierId += i * 10;
            for (int j = 0; j < 100; j++) {
                long currentCourier = courierId + (j % 10);
                pool.submit(() -> {
                    try {
                        createOrder(sortingCenter.getToken(), externalId.getAndIncrement(), currentCourier, warehouseId);
                    } catch (AssertionError e) {
                        errorCount.incrementAndGet();
                    }
                });
            }
        }
        pool.shutdown();
        while (true) {
            try {
                if (pool.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    break;
                }
            } catch (InterruptedException ignored) {
            }
        }

        if (errorCount.get() > 0) {
            throw new AssertionError("Errors in " + errorCount.get() + " requests");
        }

        assertThat(scOrderRepository.findAll().size()).isEqualTo(1000);
    }

    @SneakyThrows
    private void createOrder(String token, long externalId, long courierId, String warehouseId) {
        String createXml = IOUtils.toString(
                Objects.requireNonNull(
                        TestFactory.class.getClassLoader().getResourceAsStream("ff_create_custom_order.xml")
                ),
                StandardCharsets.UTF_8
        );
        ffApiRequest(String.format(createXml,
                token, externalId, courierId, warehouseId, warehouseId, warehouseId, warehouseId));
    }

    @SneakyThrows
    private void ffApiRequest(String body) {
        mockMvc.perform(
                MockMvcRequestBuilders.post("/fulfillment/query-gateway")
                        .contentType(MediaType.TEXT_XML)
                        .content(body)
        )
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(xpath("/root/requestState/isError").string("false"));
    }

}

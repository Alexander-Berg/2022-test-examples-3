package ru.yandex.market.sc.internal.ff;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.xpath;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author valter
 */
@ScIntControllerTest
class FFApiUpdateOrderProcessorTest {

    @Autowired
    TestFactory testFactory;
    @Autowired
    MockMvc mockMvc;

    private SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    @SneakyThrows
    void testUpdateTransactional() {
        var orderWithPlaces = testFactory.createForToday(
                        order(sortingCenter).externalId("o1").places("p1", "p2").build())
                .acceptPlaces("p1", "p2").sortPlaces("p1", "p2").shipPlaces("p1", "p2").acceptPlaces("p1")
                .getOrderWithPlaces();

        assertThat(orderWithPlaces.order().getCourier()).isNotNull();
        assertThat(orderWithPlaces.place("p1").getCourier()).isNotNull();
        assertThat(orderWithPlaces.place("p2").getCourier()).isNotNull();

        String body = String.format(
                fileContent("ff_update_order_empty_courier.xml"),
                orderWithPlaces.order().getSortingCenter().getToken(),
                orderWithPlaces.order().getExternalId());
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/fulfillment/query-gateway")
                                .contentType(MediaType.TEXT_XML)
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(xpath("/root/requestState/isError").string("true"));

        orderWithPlaces = testFactory.getOrderWithPlaces(orderWithPlaces.order().getId());
        assertThat(orderWithPlaces.order().getCourier()).isNotNull();
        assertThat(orderWithPlaces.place("p1").getCourier()).isNotNull();
        assertThat(orderWithPlaces.place("p2").getCourier()).isNotNull();
    }

    @SneakyThrows
    private String fileContent(@SuppressWarnings("SameParameterValue") String fileName) {
        return IOUtils.toString(
                Objects.requireNonNull(
                        TestFactory.class.getClassLoader().getResourceAsStream(fileName)),
                StandardCharsets.UTF_8);
    }

}

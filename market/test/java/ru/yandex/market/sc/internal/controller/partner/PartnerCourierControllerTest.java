package ru.yandex.market.sc.internal.controller.partner;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import ru.yandex.market.common.test.mockito.MemCachedClientFactoryMock;
import ru.yandex.market.sc.core.configuration.ConfigurationProperties;
import ru.yandex.market.sc.core.domain.courier.TplCourierService;
import ru.yandex.market.sc.core.domain.courier.model.PartnerCourierDto;
import ru.yandex.market.sc.core.domain.courier.repository.Courier;
import ru.yandex.market.sc.core.domain.delivery_service.model.DeliveryServiceType;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenter;
import ru.yandex.market.sc.core.domain.sorting_center.repository.SortingCenterPropertySource;
import ru.yandex.market.sc.core.domain.sorting_center.repository.enums.SortingCenterPropertiesKey;
import ru.yandex.market.sc.core.external.memcached.MemcachedService;
import ru.yandex.market.sc.core.test.TestFactory;
import ru.yandex.market.sc.internal.test.ScIntControllerTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.sc.core.external.memcached.MemcachedService.partnerCourierListKey;
import static ru.yandex.market.sc.core.test.TestFactory.order;

/**
 * @author hardlight
 */
@ScIntControllerTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class PartnerCourierControllerTest {

    private final MockMvc mockMvc;
    private final TestFactory testFactory;
    private final SortingCenterPropertySource sortingCenterPropertySource;
    private final MemcachedService memcachedService;
    private final ApplicationContext context;
    @MockBean
    TplCourierService tplCourierService;

    SortingCenter sortingCenter;

    @BeforeEach
    void init() {
        sortingCenter = testFactory.storedSortingCenter();
    }

    @Test
    void getCouriers() throws Exception {
        doReturn(List.of(
                new PartnerCourierDto(1L, "А", new PartnerCourierDto.CourierCompany("Рога и копыта")),
                new PartnerCourierDto(2L, "Б", new PartnerCourierDto.CourierCompany("Рога и копыта"))
        ))
                .when(tplCourierService).getCouriers(eq(sortingCenter.getId()), eq(sortingCenter.getToken()));
        assertThat(sortingCenterPropertySource.isDropoff(sortingCenter.getId())).isFalse();
        assertThat(context.getBean("memCachedClientFactoryMock", MemCachedClientFactoryMock.class).getCaches())
                .isEqualTo(Map.of("sc-api", Collections.emptyMap(), "sc-int", Collections.emptyMap()));
        assertThat(memcachedService.getFromScApiCacheOrSet(
                partnerCourierListKey(sortingCenter),
                () -> new ArrayList<>(tplCourierService.getCouriers(sortingCenter.getId(),
                        sortingCenter.getToken())),
                Duration.ofSeconds(0)
        )).hasSize(2);
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/couriers")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "couriers":[
                                {
                                    "id":1,
                                    "name":"А",
                                    "company":{
                                        "name":"Рога и копыта"
                                    }
                                },
                                {
                                    "id":2,
                                    "name":"Б",
                                    "company":{
                                        "name":"Рога и копыта"
                                    }
                                }
                            ]
                        }
                        """, true));
    }

    @Test
    void getCouriersHasLimit() throws Exception {
        doReturn(List.of(
                new PartnerCourierDto(1L, "А", new PartnerCourierDto.CourierCompany("Рога и копыта")),
                new PartnerCourierDto(2L, "Б", new PartnerCourierDto.CourierCompany("Рога и копыта"))
        ))
                .when(tplCourierService).getCouriers(eq(sortingCenter.getId()), eq(sortingCenter.getToken()));
        testFactory.setConfiguration(ConfigurationProperties.MAX_COURIER_SUGGEST_SIZE_ROUTES_PAGE, 1);
        assertThat(sortingCenterPropertySource.isDropoff(sortingCenter.getId())).isFalse();
        assertThat(context.getBean("memCachedClientFactoryMock", MemCachedClientFactoryMock.class).getCaches())
                .isEqualTo(Map.of("sc-api", Collections.emptyMap(), "sc-int", Collections.emptyMap()));
        assertThat(memcachedService.getFromScApiCacheOrSet(
                partnerCourierListKey(sortingCenter),
                () -> new ArrayList<>(tplCourierService.getCouriers(sortingCenter.getId(),
                        sortingCenter.getToken())),
                Duration.ofSeconds(0)
        )).hasSize(2);
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/couriers")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "couriers":[
                                {
                                    "id":1,
                                    "name":"А",
                                    "company":{
                                        "name":"Рога и копыта"
                                    }
                                }
                            ]
                        }
                        """, true));
    }

    @Test
    void getCouriersHasCourierNameFilter() throws Exception {
        doReturn(List.of(
                new PartnerCourierDto(1L, "Первый", new PartnerCourierDto.CourierCompany("Рога и копыта")),
                new PartnerCourierDto(2L, "Второй", new PartnerCourierDto.CourierCompany("Рога и копыта"))
        ))
                .when(tplCourierService).getCouriers(eq(sortingCenter.getId()), eq(sortingCenter.getToken()));
        assertThat(sortingCenterPropertySource.isDropoff(sortingCenter.getId())).isFalse();
        assertThat(context.getBean("memCachedClientFactoryMock", MemCachedClientFactoryMock.class).getCaches())
                .isEqualTo(Map.of("sc-api", Collections.emptyMap(), "sc-int", Collections.emptyMap()));
        assertThat(memcachedService.getFromScApiCacheOrSet(
                partnerCourierListKey(sortingCenter),
                () -> new ArrayList<>(tplCourierService.getCouriers(sortingCenter.getId(),
                        sortingCenter.getToken())),
                Duration.ofSeconds(0)
        )).hasSize(2);
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/couriers?name=тор")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                            "couriers":[
                                {
                                    "id":2,
                                    "name":"Второй",
                                    "company":{
                                        "name":"Рога и копыта"
                                    }
                                }
                            ]
                        }
                        """, true));
    }

    @Test
    void getMidMilesCouriers() throws Exception {
        testFactory.storedCourier(1L);
        var midMileCourier = testFactory.storedCourier(2L, 1L);
        testFactory.createOrderForToday(sortingCenter)
                .updateCourier(midMileCourier)
                .get();

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/couriers/midMiles")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{\"couriers\":[" + courierJson(midMileCourier) + "]}", true));
    }

    @Test
    void getCouriersForDropoffEmpty() throws Exception {
        testFactory.setSortingCenterProperty(sortingCenter, SortingCenterPropertiesKey.IS_DROPOFF, true);
        doReturn(List.of(
                new PartnerCourierDto(1L, "Первый", new PartnerCourierDto.CourierCompany("Рога и копыта"))
        ))
                .when(tplCourierService).getCouriers(eq(sortingCenter.getId()), eq(sortingCenter.getToken()));
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/couriers")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{\"couriers\":[]}", true));
    }

    @Test
    void getEmptyMidMilesCouriers() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.get("/internal/partners/" + sortingCenter.getPartnerId()
                                + "/couriers/midMiles")
                )
                .andExpect(status().isOk())
                .andExpect(content().json("{\"couriers\":[]}", true));
    }

    private String courierJson(Courier courier) {
        return "{\"id\":" + courier.getId() + "," +
                "\"name\":\"" + courier.getName() + "\"" +
                "}";
    }

    @Test
    @SneakyThrows
    void getAllCouriers() {
        var lastMileCourier = testFactory.storedCourier(12L);
        var deliveryService = testFactory.storedDeliveryService("23");
        var o1 = testFactory.createForToday(order(sortingCenter, "o1")
                        .dsType(DeliveryServiceType.TRANSIT)
                        .deliveryService(deliveryService)
                        .build())
                .get();
        var o2 = testFactory.createForToday(order(sortingCenter, "o2").build())
                .updateCourier(lastMileCourier)
                .get();

        mockMvc.perform(MockMvcRequestBuilders.get("/internal/partners/{partner_id}/couriers/all",
                        sortingCenter.getPartnerId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$.[*].id", containsInAnyOrder(12, 1100000000000023L)));
    }
}

package ru.yandex.market.ff.service.implementation;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.model.entity.FulfillmentInfo;
import ru.yandex.market.ff.service.FulfillmentInfoService;

class FulfillmentInfoServiceTest extends IntegrationTest {

    @Autowired
    private FulfillmentInfoService fulfillmentInfoService;


    /**
     * Тест №1
     * <p>
     * Входные данные:
     * В fulfillment_service пусто
     * В warehouse-ids-allowed-for-requests-creation пусто
     * <p>
     * Ожидаем:
     * Вернется пустой список
     */
    @Test
    @DatabaseSetup("classpath:service/fulfillment-info/all-active-fulfillment-partners-returns-empty.xml")
    void allActiveFulfillmentPartnersReturnsEmpty() {

        Set<FulfillmentInfo> fulfillmentInfos = fulfillmentInfoService.allActiveFulfillmentPartners();
        assertions.assertThat(fulfillmentInfos).isEmpty();
    }

    /**
     * Тест №2
     * <p>
     * Входные данные:
     * В fulfillment_service 3 записи с типом FULFILLMENT.
     * 145, 147 - активны
     * 333 - выключен
     * В warehouse-ids-allowed-for-requests-creation 145, 147, 333
     * <p>
     * Ожидаем:
     * Вернется список из 145, 147
     */
    @Test
    @DatabaseSetup("classpath:service/fulfillment-info/" +
            "all-active-fulfillment-partners-filtered-non-active-partners.xml")
    void allActiveFulfillmentPartnersFilteredNonActiveParners() {

        List<Long> expected = List.of(145L, 147L);
        Set<FulfillmentInfo> fulfillmentInfos = fulfillmentInfoService.allActiveFulfillmentPartners();
        List<Long> actualIds = fulfillmentInfos.stream().map(FulfillmentInfo::getId).collect(Collectors.toList());

        assertions.assertThat(actualIds).isEqualTo(expected);
    }

    /**
     * Тест №3
     * <p>
     * Входные данные:
     * В fulfillment_service 3 записи с типом FULFILLMENT.
     * 172, 173 - активны
     * 333 - выключен
     * 123, 456 - с типами отличными от FULFILLMENT
     * В fulfillment_service 2 записи с другими типами
     * В warehouse-ids-allowed-for-requests-creation 172, 173, 333, 123, 456
     * <p>
     * Ожидаем:
     * Вернется список из 172, 173
     */
    @Test
    @DatabaseSetup("classpath:service/fulfillment-info/all-active-fulfillment-partners-filtered-with-other-types.xml")
    void allActiveFulfillmentPartnersFilteredPartnersWithOtherTypes() {

        List<Long> expected = List.of(172L, 173L);
        Set<FulfillmentInfo> fulfillmentInfos = fulfillmentInfoService.allActiveFulfillmentPartners();
        List<Long> actualIds = fulfillmentInfos.stream().map(FulfillmentInfo::getId).collect(Collectors.toList());

        assertions.assertThat(actualIds).isEqualTo(expected);
    }

    /**
     * Тест №4
     * <p>
     * Входные данные:
     * В fulfillment_service 5 записей с типом FULFILLMENT.
     * 172, 173, 123, 456 - активны
     * В warehouse-ids-allowed-for-requests-creation: 172, 173
     * <p>
     * Ожидаем:
     * Вернется список из 172, 173
     */
    @Test
    @DatabaseSetup("classpath:service/fulfillment-info/" +
            "all-active-fulfillment-partners-filtered-with-disabled-warehouses.xml")
    void allActiveFulfillmentPartnersFilteredPartnersWithDisabledWarehouses() {

        List<Long> expected = List.of(172L, 173L);
        Set<FulfillmentInfo> fulfillmentInfos = fulfillmentInfoService.allActiveFulfillmentPartners();
        List<Long> actualIds = fulfillmentInfos.stream().map(FulfillmentInfo::getId).collect(Collectors.toList());

        assertions.assertThat(actualIds).isEqualTo(expected);
    }
}

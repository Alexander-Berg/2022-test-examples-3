package ru.yandex.market.tpl.core.domain.partner;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import ru.yandex.market.tpl.core.CoreTest;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.tpl.core.domain.partner.SortingCenterService.PVZ_SORTING_CENTER_ID;

/**
 * @author valter
 */
@CoreTest
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
class SortingCenterServiceTest {

    private static final long BETA_PRO_SORTING_CENTER_ID = 47819L;

    private final SortingCenterService sortingCenterService;

    @Test
    void findSortCenterForDs() {
        assertThat(sortingCenterService.findSortCenterForDs(198).getName()).isEqualTo("Маркет ПВЗ");
        assertThat(sortingCenterService.findSortCenterForDs(239).getName()).isEqualTo("Маркет Курьер");
    }

    @Test
    void findDsForSortingCenter() {
        assertThat(sortingCenterService.findDsForSortingCenter(PVZ_SORTING_CENTER_ID).get(0).getName())
                .isEqualTo("Маркет ПВЗ");
        assertThat(sortingCenterService.findDsForSortingCenter(BETA_PRO_SORTING_CENTER_ID).get(0).getName())
                .isEqualTo("Маркет Курьер");
    }

    @Test
    @Sql("classpath:mockPartner/deliveryServiceWithSCWithTokens.sql")
    void findScTokenByDsToken() {
        assertThat(sortingCenterService.getScTokenByDsTokenOrThrow("ds_token"))
                .isEqualTo("sc_token");
    }

    @Test
    @Sql("classpath:mockPartner/deliveryServiceWithSchedule.sql")
    void getScheduleForDeliveryService() {
        DeliveryService deliveryService = sortingCenterService.findDsById(100500);

        assertThat(deliveryService.getSchedule()).isNotNull();
        assertThat(deliveryService.getSchedule().getIntervals()).hasSize(6);
    }

}

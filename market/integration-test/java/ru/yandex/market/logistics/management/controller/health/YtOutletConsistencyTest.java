package ru.yandex.market.logistics.management.controller.health;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.repository.LogisticsPointCountRepository;
import ru.yandex.market.logistics.management.repository.YtOutletCountRepository;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Тесты проверки выгрузки ПВЗ на консистентность")
@DatabaseSetup("/data/controller/health/outlet/prepare.xml")
class YtOutletConsistencyTest extends AbstractContextualTest {
    private static final String ERROR_PREFIX = "2;Tables logistics_point and yt_outlet aren't consistent: ";
    @Autowired
    private LogisticsPointCountRepository logisticsPointCountRepository;

    @Autowired
    private YtOutletCountRepository ytOutletCountRepository;

    @Test
    @DisplayName("Число ПВЗ в базе и выгрузке совпадает")
    void numberOfLogisticsPointAreEqualToNumberOfOutlets() throws Exception {
        checkYtOutletConsistency("0;OK");
    }

    @Test
    @DisplayName("Результат успешной проверки кэшируется")
    void successResultIsCached() throws Exception {
        checkYtOutletConsistency("0;OK");
        checkYtOutletConsistency("0;OK");
        verify(logisticsPointCountRepository).countAllGroupByPartnerIdAndActive();
        verify(ytOutletCountRepository).countAllGroupByPartnerIdAndActive();
    }

    @Test
    @DisplayName("У партнёра есть активные ПВЗ в базе, но нет в выгрузке")
    @DatabaseSetup(value = "/data/controller/health/outlet/delete_outlet_3.xml", type = DatabaseOperation.DELETE)
    void noActiveOutletsForPartner() throws Exception {
        checkYtOutletConsistency(ERROR_PREFIX + "Partner 2 has no active outlets");
    }

    @Test
    @DisplayName("У партнёра есть ПВЗ в выгрузке, но нет в базе")
    @DatabaseSetup(
        value = "/data/controller/health/outlet/insert_outlet_4_partner_3.xml",
        type = DatabaseOperation.INSERT
    )
    void noActiveLogisticsPointsForPartner() throws Exception {
        checkYtOutletConsistency(ERROR_PREFIX + "Partner 3 has no active pickup points");
    }

    @Test
    @DisplayName("У партнёра выгружаются не все активные ПВЗ")
    @DatabaseSetup(value = "/data/controller/health/outlet/delete_outlet_2.xml", type = DatabaseOperation.DELETE)
    void numberOfActivePickupPointsGreaterThanNumberOfActiveOutlets() throws Exception {
        checkYtOutletConsistency(
            ERROR_PREFIX +
                "Partner 1 has different number of active pickup points (2) and outlets (1)"
        );
    }

    @Test
    @DisplayName("У партнёра выгружаются лишние активные ПВЗ")
    @DatabaseSetup(
        value = "/data/controller/health/outlet/insert_outlet_4_partner_2.xml",
        type = DatabaseOperation.INSERT
    )
    void numberOfActiveLogisticsPointLessThanNumberOfActiveOutlets() throws Exception {
        checkYtOutletConsistency(
            ERROR_PREFIX +
                "Partner 2 has different number of active pickup points (1) and outlets (2)"
        );
    }

    @Test
    @DisplayName("У партнёра есть неактивные ПВЗ в базе, но нет в выгрузке")
    @DatabaseSetup(value = "/data/controller/health/outlet/deactivate_points.xml", type = DatabaseOperation.REFRESH)
    @DatabaseSetup(value = "/data/controller/health/outlet/delete_outlet_3.xml", type = DatabaseOperation.DELETE)
    void noInactiveOutletsForPartner() throws Exception {
        checkYtOutletConsistency(ERROR_PREFIX + "Partner 2 has no inactive outlets");
    }

    @Test
    @DisplayName("У партнёра ест неактивные ПВЗ в выгрузке, но нет в базе")
    @DatabaseSetup(value = "/data/controller/health/outlet/deactivate_points.xml", type = DatabaseOperation.REFRESH)
    @DatabaseSetup(
        value = "/data/controller/health/outlet/insert_outlet_4_partner_3_inactive.xml",
        type = DatabaseOperation.INSERT
    )
    void noInactivePickupPointsForPartner() throws Exception {
        checkYtOutletConsistency(ERROR_PREFIX + "Partner 3 has no inactive pickup points");
    }

    @Test
    @DisplayName("У партнёра выгружаются не все неактивные ПВЗ")
    @DatabaseSetup(value = "/data/controller/health/outlet/deactivate_points.xml", type = DatabaseOperation.REFRESH)
    @DatabaseSetup(value = "/data/controller/health/outlet/delete_outlet_2.xml", type = DatabaseOperation.DELETE)
    void numberOfInactivePickupPointsGreaterThanNumberOfInactiveOutlets() throws Exception {
        checkYtOutletConsistency(
            ERROR_PREFIX +
                "Partner 1 has different number of inactive pickup points (2) and outlets (1)"
        );
    }

    @Test
    @DisplayName("У партнёра выгружаются лишние неактивные ПВЗ")
    @DatabaseSetup(value = "/data/controller/health/outlet/deactivate_points.xml", type = DatabaseOperation.REFRESH)
    @DatabaseSetup(
        value = "/data/controller/health/outlet/insert_outlet_4_partner_2_inactive.xml",
        type = DatabaseOperation.INSERT
    )
    void numberOfInactiveLogisticsPointLessThanNumberOfInactiveOutlets() throws Exception {
        checkYtOutletConsistency(
            ERROR_PREFIX +
                "Partner 2 has different number of inactive pickup points (1) and outlets (2)"
        );
    }

    @Test
    @DisplayName("Результат неуспешной проверки не кэшируется")
    @DatabaseSetup(value = "/data/controller/health/outlet/delete_outlet_3.xml", type = DatabaseOperation.DELETE)
    void failedResultIsNotCached() throws Exception {
        checkYtOutletConsistency(ERROR_PREFIX + "Partner 2 has no active outlets");
        checkYtOutletConsistency(ERROR_PREFIX + "Partner 2 has no active outlets");
        verify(logisticsPointCountRepository, times(2)).countAllGroupByPartnerIdAndActive();
        verify(ytOutletCountRepository, times(2)).countAllGroupByPartnerIdAndActive();
    }

    private void checkYtOutletConsistency(String expectedMessage) throws Exception {
        mockMvc.perform(get("/health/checkYtOutletConsistency"))
            .andExpect(status().isOk())
            .andExpect(content().string(expectedMessage));
    }
}

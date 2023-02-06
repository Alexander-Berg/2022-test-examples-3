package ru.yandex.market.tpl.billing.service.courier;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.tariffs.client.model.TariffFindQuery;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.model.exception.TariffsException;
import ru.yandex.market.tpl.billing.service.tariff.TariffService;
import ru.yandex.market.tpl.billing.util.DateTimeUtil;

import static java.time.Month.MAY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.tpl.billing.utils.TariffsUtil.mockTariffResponse;

/**
 * Тесты для {@link CourierSurchargeBillingService}
 */
@DbUnitDataSet(
        before = "/database/service/courier/surcharge/couriersurchargebillingservice/before/userAndCompanies.csv"
)
public class CourierSurchargeBillingServiceTest extends AbstractFunctionalTest {
    private static final LocalDate MAY_20_2022 = LocalDate.of(2022, MAY, 20);

    @Autowired
    private CourierSurchargeBillingService courierSurchargeBillingService;

    @Autowired
    private TestableClock clock;

    @Autowired
    private TariffService tariffService;

    @BeforeEach
    void setClock() {
        clock.setFixed(
                LocalDateTime.of(LocalDate.of(2022, MAY, 31), LocalTime.NOON).toInstant(DateTimeUtil.DEFAULT_ZONE_ID),
                DateTimeUtil.DEFAULT_ZONE_ID
        );
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/courier/surcharge/couriersurchargebillingservice/before/billWithManyTariffsFound.csv"
    )
    @DisplayName("Нашлось несколько подходящих тарифов - бросилась ошибка (в тарифнице есть чек на это, но все же)")
    void testBillWithManyTariffsFound() {
        mockTariffResponse(tariffService, "/request/service/courier/couriersurchargebillingservice/manyTariffsFound.json");
        TariffsException exception = assertThrows(
                TariffsException.class,
                () -> courierSurchargeBillingService.processSurcharges(MAY_20_2022)
        );
        assertThat(exception.getMessage(), startsWith("For surcharge [test1] found [2] matched tariffs : "));
    }

    @Test
    @DbUnitDataSet(
            before = "/database/service/courier/surcharge/couriersurchargebillingservice/before/billWithNoneTariffsFound.csv"
    )
    @DisplayName("Не нашлось подходящего тарифа - бросилась ошибка")
    void testBillWithNoneTariffsFound() {
        mockTariffResponse(tariffService, "/request/service/courier/couriersurchargebillingservice/noneTariffsFound.json");
        TariffsException exception = assertThrows(
                TariffsException.class,
                () -> courierSurchargeBillingService.processSurcharges(MAY_20_2022)
        );
        assertThat(exception.getMessage(), startsWith("Tariff for surcharge [test1] is not found"));
    }

    @Test
    @DisplayName("Тест на правильный биллинг")
    @DbUnitDataSet(
            before = "/database/service/courier/surcharge/couriersurchargebillingservice/before/goodBilling.csv",
            after = "/database/service/courier/surcharge/couriersurchargebillingservice/after/goodBilling.csv"
    )
    void testGoodBilling() {
        mockTariffResponse(tariffService, "/request/service/courier/couriersurchargebillingservice/goodTariffs.json");
        courierSurchargeBillingService.processSurcharges(MAY_20_2022);

        verify(tariffService, times(2)).findTariffs(any(TariffFindQuery.class));
    }

    @Test
    @DisplayName("Тест на то, что дропофы билятся как КГТ")
    @DbUnitDataSet(
            before = "/database/service/courier/surcharge/couriersurchargebillingservice/before/dropoffBilledAsMgt.csv",
            after = "/database/service/courier/surcharge/couriersurchargebillingservice/after/dropoffBilledAsMgt.csv"
    )
    void testBillDropoffAsMgt() {
        mockTariffResponse(tariffService, "/request/service/courier/couriersurchargebillingservice/dropoffBilledAsMgt.json");
        courierSurchargeBillingService.processSurcharges(MAY_20_2022);
    }

    @Test
    @DisplayName("Тест что прокидывается user_shift_id")
    @DbUnitDataSet(
            before = "/database/service/courier/surcharge/couriersurchargebillingservice/before/userShiftIdNotNull.csv",
            after = "/database/service/courier/surcharge/couriersurchargebillingservice/after/userShiftNotNull.csv"
    )
    void testUserShiftIdIsNotNull() {
        mockTariffResponse(tariffService, "/request/service/courier/couriersurchargebillingservice/goodTariffs.json");
        courierSurchargeBillingService.processSurcharges(MAY_20_2022);
    }
}

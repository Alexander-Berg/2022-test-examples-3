package ru.yandex.market.tpl.billing.service.courier.surcharge;


import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.client.billing.BillingClient;
import ru.yandex.market.tpl.client.billing.dto.BillingSurchargeDto;
import ru.yandex.market.tpl.client.billing.dto.BillingSurchargePageDto;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Тесты для {@link HttpSurchargeImportService}
 */
public class HttpSurchargeImportServiceTest extends AbstractFunctionalTest {

    private static final LocalDate IMPORT_DATE = LocalDate.of(2022, Month.MAY, 19);

    @Autowired
    private HttpSurchargeImportService httpSurchargeImportService;

    @Autowired
    private BillingClient billingClient;

    @Test
    @DisplayName("Интеграционный тест на импорт данных по штрафам/начислениям из по курьерки")
    @DbUnitDataSet(
            before = "/database/service/courier/surcharge/httpsurchargeimportservice/before/doImport.csv",
            after = "/database/service/courier/surcharge/httpsurchargeimportservice/after/doImport.csv")
    void testDoImport() {
        Mockito.doAnswer(invocation -> BillingSurchargePageDto.builder()
                .last(true)
                .first(true)
                .totalPages(1)
                .totalElements(2)
                .number(0)
                .content(List.of(
                        BillingSurchargeDto.builder()
                                .id("id1")
                                .createdAt(Instant.now())
                                .eventDate(IMPORT_DATE)
                                .resolution("COMMIT")
                                .type("ANY_TYPE")
                                .cargoType("KGT")
                                .companyId(1L)
                                .companyDsmId("dsm-1L")
                                .scId(10L)
                                .userId(100L)
                                .userDsmId("dsm-100")
                                .amount(BigDecimal.valueOf(100_00L)) //100 рублей, из ручки приходит в копейках
                                .multiplier(3)
                                .build(),
                        BillingSurchargeDto.builder()
                                .id("id2")
                                .createdAt(Instant.now())
                                .eventDate(IMPORT_DATE)
                                .resolution("REJECT")
                                .type("ANY_ANOTHER_TYPE")
                                .cargoType("MGT")
                                .companyId(2L)
                                .companyDsmId("dsm-2L")
                                .scId(11L)
                                .multiplier(1)
                                .build(),
                        BillingSurchargeDto.builder()
                                .id("id3")
                                .createdAt(Instant.now())
                                .eventDate(IMPORT_DATE)
                                .resolution("COMMIT")
                                .type("ANY_TYPE")
                                .cargoType("KGT")
                                .companyId(1L)
                                .companyDsmId("dsm-1L")
                                .scId(10L)
                                .userId(10L)
                                .userDsmId("dsm-10")
                                .amount(BigDecimal.valueOf(100_00L))
                                .userShiftId(1L)
                                .multiplier(1)
                                .build()
                ))
                .build()).when(billingClient).findUserSurcharges(eq(IMPORT_DATE), anyInt(), anyInt());

        httpSurchargeImportService.doImport(IMPORT_DATE);
    }
}

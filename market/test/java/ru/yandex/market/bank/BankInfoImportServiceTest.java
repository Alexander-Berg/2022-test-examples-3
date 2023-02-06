package ru.yandex.market.bank;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.bank.model.BankInfoDTO;
import ru.yandex.market.billing.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.bank.BankInfoService;

class BankInfoImportServiceTest extends FunctionalTest {
    @Autowired
    private BankInfoClient bankInfoClient;

    @Autowired
    private BankInfoService bankInfoService;

    @Test
    @DisplayName("Базовый случай работы сервиса")
    @DbUnitDataSet(
            before = "csv/BankInfoImportService.base_case.before.csv",
            after = "csv/BankInfoImportService.base_case.after.csv"
    )
    void testBaseCase() {
        final BankInfoDTO bankInfoDTO2 = new BankInfoDTO(
                "042222222",
                "new_name2",
                "new_place2",
                false,
                false);
        final BankInfoDTO bankInfoDTO3 = new BankInfoDTO(
                "043333333",
                "new_name3",
                "new_place3",
                false,
                false);

        Mockito.doReturn(Arrays.asList(bankInfoDTO2, bankInfoDTO3))
                .when(bankInfoClient).downloadBankInfo();

        new BankInfoImportService(bankInfoClient, bankInfoService).importBankInfo();
    }
}

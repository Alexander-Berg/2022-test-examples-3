package ru.yandex.market.tpl.billing.dao;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.dao.view.VMoneyFlowTransactionsDao;
import ru.yandex.market.tpl.billing.model.yt.YtMoneyFlowTransactionDto;

public class VMoneyFlowTransactionsDaoTest extends AbstractFunctionalTest {

    private static final LocalDate TARGET_DATE = LocalDate.of(2022, 4, 7);

    @Autowired
    private VMoneyFlowTransactionsDao vMoneyFlowTransactionsDao;

    @Test
    @DbUnitDataSet(before = "/database/dao/v_money_flow_transaction_dao/before/payments_export.csv")
    @DisplayName("Проверка, что из базы достаются нужные платежи")
    void testPaymentsExport() {
        List<YtMoneyFlowTransactionDto> actualData = vMoneyFlowTransactionsDao.getYtData(TARGET_DATE);
        actualData.sort(Comparator.comparingLong(YtMoneyFlowTransactionDto::getId));

        Assertions.assertEquals(getExpectedData(), actualData);
    }

    private List<YtMoneyFlowTransactionDto> getExpectedData() {
        return List.of(
                YtMoneyFlowTransactionDto.builder()
                        .id(2L)
                        .userShiftId(1L)
                        .eventTime("2022-04-06")
                        .trantime("2022-04-07")
                        .partnerId(1L)
                        .paymentType("PAYMENT")
                        .transactionType("payments")
                        .productType("MGT")
                        .amount(300000L)
                        .currency("RUB")
                        .isCorrection(false)
                        .orgId(64554L)
                        .clientId("user_client_id")
                        .contractId("user_contract_id")
                        .personId("user_person_id")
                        .userType("PARTNER")
                        .build(),
                YtMoneyFlowTransactionDto.builder()
                        .id(3L)
                        .userShiftId(2L)
                        .eventTime("2022-04-05")
                        .trantime("2022-04-07")
                        .partnerId(1L)
                        .paymentType("PAYMENT")
                        .transactionType("payments")
                        .productType("MGT")
                        .amount(300000L)
                        .currency("RUB")
                        .isCorrection(false)
                        .orgId(64554L)
                        .clientId("company_client_id")
                        .contractId("company_contract_id")
                        .personId("company_person_id")
                        .userType("SELF_EMPLOYED")
                        .build()
        );
    }
}

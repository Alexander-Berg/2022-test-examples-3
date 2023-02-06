package ru.yandex.market.billing.payment.services;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.inside.yt.kosher.Yt;
import ru.yandex.inside.yt.kosher.cypress.Cypress;
import ru.yandex.inside.yt.kosher.cypress.YPath;
import ru.yandex.market.FunctionalTest;
import ru.yandex.market.billing.service.environment.EnvironmentService;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.util.DateTimes;
import ru.yandex.market.yql_test.annotation.YqlTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

class PaymentOrderExistInYtServiceTest extends FunctionalTest {

    private static final LocalDateTime TEST_UPDATE_TIME = LocalDateTime.of(2022, 3, 8, 10, 30, 0);

    private static final Clock CLOCK = Clock.fixed(DateTimes.toInstantAtDefaultTz(TEST_UPDATE_TIME),
            ZoneId.systemDefault());

    private PaymentOrderExistInYtService paymentOrderExistInYtService;

    @Autowired
    private Yt hahnYt;

    @Mock
    private Cypress cypress;

    @Autowired
    private PaymentOrderYtDao paymentOrderYtDao;

    @Autowired
    private EnvironmentService environmentService;

    @BeforeEach
    void init() {
        when(hahnYt.cypress()).thenReturn(cypress);
        doAnswer(invocation -> true).when(cypress).exists(any(YPath.class));
        paymentOrderExistInYtService = new PaymentOrderExistInYtService(
                paymentOrderYtDao,
                environmentService,
                CLOCK
        );
    }

    @Test
    @YqlTest(
            schemasDir = "/yt/schemas",
            schemas = {
                    "//home/market/testing/billing/tlog/payouts/payments/2022-03-08",
                    "//home/market/testing/billing/tlog/payouts/expenses/2022-03-08"
            },
            csv = "PaymentOrderExistInYtServiceTest.processNotExists.yt.csv",
            yqlMock = "PaymentOrderExistInYtServiceTest.processNotExists.yt.mock"
    )
    @DbUnitDataSet(
            before = "PaymentOrderExistInYtServiceTest.processNotExists.before.csv",
            after = "PaymentOrderExistInYtServiceTest.processNotExists.after.csv"
    )
    void processNotExists() {
        paymentOrderExistInYtService.process(LocalDate.now());
    }
}

package ru.yandex.direct.core.entity.autooverdraft;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.configuration.CoreConfiguration;
import ru.yandex.direct.core.entity.balance.container.PaymentMethodInfo;
import ru.yandex.direct.core.entity.balance.container.PersonPaymentMethodInfo;
import ru.yandex.direct.core.service.integration.balance.BalanceService;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.utils.JsonUtils;

import static org.junit.Assume.assumeFalse;

@ContextConfiguration(classes = CoreConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
// если вдруг захотелось запустить, нужно закомментировать @Ignore
@Ignore("Для запуска вручную. Нужен запущенный докер и, возможно, что-то ещё.")
public class BalanceIntegrationTest {
    private static final long WALLET_CID = 10444173L;
    private static final ClientId CLIENT_ID = ClientId.fromLong(312545L);
    private static final long OPERATOR_UID = 149511L;
    private static final BigDecimal CLIENT_LIMIT = BigDecimal.valueOf(15960);
    private static final String CURRENCY_CODE = "RUB";

    @Autowired
    private BalanceService balanceService;

    @Test
    public void getPaymentMethodsSuccess() {
        List<PersonPaymentMethodInfo> paymentOptions =
                balanceService.getPaymentOptions(OPERATOR_UID, CLIENT_ID, WALLET_CID);
        System.out.println(JsonUtils.toJson(paymentOptions));
    }

    @Test
    public void setOverdraftParamsSuccess() {
        List<PersonPaymentMethodInfo> paymentOptions =
                balanceService.getPaymentOptions(OPERATOR_UID, CLIENT_ID, WALLET_CID);

        assumeFalse(paymentOptions.isEmpty());
        PersonPaymentMethodInfo ppm = paymentOptions.stream().findFirst().get();
        assumeFalse(ppm.getPaymentMethods().isEmpty());
        PaymentMethodInfo pm = ppm.getPaymentMethods().stream().findFirst().get();

        balanceService
                .setOverdraftParams(ppm.getPersonInfo().getId().longValue(), pm.getCode(), CURRENCY_CODE, CLIENT_LIMIT);
    }
}

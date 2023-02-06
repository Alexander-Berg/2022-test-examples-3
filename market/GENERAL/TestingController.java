package ru.yandex.market.pers.pay.controller;

import java.util.List;
import java.util.Optional;

import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ru.yandex.market.pers.pay.client.PersPayConstants;
import ru.yandex.market.pers.pay.model.PersPayEntity;
import ru.yandex.market.pers.pay.model.PersPayEntityType;
import ru.yandex.market.pers.pay.model.PersPayOfferState;
import ru.yandex.market.pers.pay.model.PersPayState;
import ru.yandex.market.pers.pay.model.PersPayUser;
import ru.yandex.market.pers.pay.model.PersPayUserType;
import ru.yandex.market.pers.pay.model.PersPayment;
import ru.yandex.market.pers.pay.model.PersPaymentBillingEventType;
import ru.yandex.market.pers.pay.model.PersPaymentFilter;
import ru.yandex.market.pers.pay.service.PaymentService;
import ru.yandex.market.pers.pay.service.TmsPaymentService;
import ru.yandex.market.pers.tvm.spring.TvmProtected;

import static ru.yandex.market.pers.pay.client.PersPayConstants.MODEL_ID_KEY;
import static ru.yandex.market.pers.pay.client.PersPayConstants.USER_ID_KEY;

/**
 * Test api only for testing environment to use by dev/test teams.
 *
 * @author Ilya Kislitsyn / ilyakis@ / 24.03.2021
 */
@Profile({"local", "test", "testing"})
@RestController
@TvmProtected
@RequestMapping("/test")
public class TestingController {

    public static final int LIMIT = 100;
    private final PaymentService paymentService;
    private final TmsPaymentService tmsPaymentService;
    private final JdbcTemplate jdbcTemplate;

    public TestingController(PaymentService paymentService,
                             TmsPaymentService tmsPaymentService,
                             @Qualifier("payJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.paymentService = paymentService;
        this.tmsPaymentService = tmsPaymentService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @ApiOperation("Ручное удаление платежа. Только для целей тестирования. " +
        "Даёт возможность создать новый платёж по той же модели ещё раз. Грубо прерывает пайплайн обработки платежа.")
    @DeleteMapping(value = "/payment/UID/{" + USER_ID_KEY + "}")
    public void resetPaymentForTests(@PathVariable(USER_ID_KEY) long userId,
                                     @RequestParam(value = MODEL_ID_KEY) long modelId) {
        String payKey = PersPayConstants.buildPayKey(
            new PersPayUser(PersPayUserType.UID, userId),
            new PersPayEntity(PersPayEntityType.MODEL_GRADE, modelId)
        );

        // expire all offers
        jdbcTemplate.update(
            "update pay.payment_offer \n" +
                "set state = ? \n" +
                "where state = ? \n" +
                "  and pay_key = ?",
            PersPayOfferState.EXPIRED.getValue(),
            PersPayOfferState.FRESH.getValue(),
            payKey);

        Optional<PersPayment> paymentOpt = paymentService.getPayment(new PersPaymentFilter().payKey(payKey));

        if (paymentOpt.isEmpty()) {
            return;
        }

        PersPayment payment = paymentOpt.get();

        // log to find errors if this controller ever got published to prod and used (never should, but anyway)
        // stops events processing for this payment
        tmsPaymentService.changeState(payment.getId(), PersPayState.CANCELED, "Removed manually from API");
        // cancel hold to be sure there are no hanging holds
        tmsPaymentService.savePayerEvents(List.of(payment.getId()), PersPaymentBillingEventType.CANCEL);

        //change pay_key to unique value to ensure new payment could be created
        jdbcTemplate.update("update pay.payment set pay_key = pay_key||'+'||id::text where id = ?", payment.getId());
    }

    @ApiOperation("Проверка последних 100 платежей по пользователю. Только для тестов")
    @GetMapping(value = "/payment/UID/{" + USER_ID_KEY + "}/info")
    public List<PersPayment> getUserPaymentsForTests(@PathVariable(USER_ID_KEY) long userId) {
        return paymentService.getPayments(new PersPaymentFilter()
         .userType(PersPayUserType.UID)
            .userId(String.valueOf(userId))
            .limit(LIMIT)
            .sortDesc()
        );
    }


}

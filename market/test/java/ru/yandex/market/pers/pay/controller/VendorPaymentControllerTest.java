package ru.yandex.market.pers.pay.controller;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.pay.PersPayTest;
import ru.yandex.market.pers.pay.model.PersPayUser;
import ru.yandex.market.pers.pay.model.PersPayerType;
import ru.yandex.market.pers.pay.model.PersPayment;
import ru.yandex.market.pers.pay.model.dto.PaymentCounterDto;
import ru.yandex.market.pers.pay.mvc.VendorPaymentMvcMocks;
import ru.yandex.market.pers.pay.service.PaymentService;
import ru.yandex.market.pers.pay.service.TmsPaymentService;
import ru.yandex.market.util.ListUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pers.pay.model.PersPayEntityType.MODEL_GRADE;
import static ru.yandex.market.pers.pay.model.PersPayState.NEW;
import static ru.yandex.market.pers.pay.model.PersPayState.PAYED;
import static ru.yandex.market.pers.pay.model.PersPayUserType.UID;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 11.03.2021
 */
public class VendorPaymentControllerTest extends PersPayTest {
    public static final long USER_ID = 9364172;
    public static final PersPayUser USER = new PersPayUser(UID, USER_ID);
    public static final long MODEL_ID = 41434;
    public static final long BRAND_ID = 524524;
    public static final String DATASOURCE_ID = "4952098";

    @Autowired
    private VendorPaymentMvcMocks vendorPaymentMvc;
    @Autowired
    private PaymentService paymentService;
    @Autowired
    private TmsPaymentService tmsPaymentService;

    @Test
    public void testStat() {
        createPayment(DATASOURCE_ID, USER_ID, MODEL_ID);
        createPayment(DATASOURCE_ID + 1, USER_ID + 1, MODEL_ID);
        createPayment(DATASOURCE_ID, USER_ID, MODEL_ID + 1);

        List<PersPayment> payments = tmsPaymentService.getPaymentsToProcess(NEW, null);
        tmsPaymentService.changeState(ListUtils.toList(payments, PersPayment::getId), PAYED, Map.of());

        // invalid amount
        List<Long> allModels = List.of(MODEL_ID, MODEL_ID + 1, MODEL_ID + 2);

        // check without filter
        assertEquals(List.of(
            new PaymentCounterDto(MODEL_GRADE, String.valueOf(MODEL_ID), 2),
            new PaymentCounterDto(MODEL_GRADE, String.valueOf(MODEL_ID + 1), 1),
            new PaymentCounterDto(MODEL_GRADE, String.valueOf(MODEL_ID + 2), 0)),
            vendorPaymentMvc.getPaymentStat(BRAND_ID, USER_ID, allModels, null));

        // check with filter
        assertEquals(List.of(
            new PaymentCounterDto(MODEL_GRADE, String.valueOf(MODEL_ID), 1),
            new PaymentCounterDto(MODEL_GRADE, String.valueOf(MODEL_ID + 1), 1),
            new PaymentCounterDto(MODEL_GRADE, String.valueOf(MODEL_ID + 2), 0)),
            vendorPaymentMvc.getPaymentStat(BRAND_ID, USER_ID, allModels, DATASOURCE_ID));
    }

    private long createPayment(String datasourceId, long userId, long modelId) {
        return paymentService.savePaymentForTests(PersPayment.builderModelGradeUid(modelId, userId)
            .payer(PersPayerType.VENDOR, datasourceId)
            .amount(1)
        );
    }


}

package ru.yandex.direct.intapi.entity.balanceclient.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import org.junit.Test;

import ru.yandex.direct.core.entity.promocodes.model.PromocodeClientDomain;
import ru.yandex.direct.core.entity.promocodes.service.PromocodesAntiFraudService;
import ru.yandex.direct.core.entity.promocodes.service.PromocodesAntiFraudServiceBuilder;
import ru.yandex.direct.intapi.entity.balanceclient.model.BalancePromocodeInfo;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NotifyPromocodeServiceIsApplicablePromocodeTest {

    private static final LocalDateTime LONG_TIME_AGO = LocalDateTime.of(2017, 1, 1, 0, 0);
    private static final LocalDateTime BORDER = LocalDateTime.of(2019, 1, 10, 0, 0);
    private static final String CODE = "AAA";

    private final NotifyPromocodeService service;

    @SuppressWarnings("ConstantConditions")
    public NotifyPromocodeServiceIsApplicablePromocodeTest() {
        PromocodesAntiFraudService antiFraudService = new PromocodesAntiFraudServiceBuilder().build();
        service = new NotifyPromocodeService(null, antiFraudService, null, null, null, null, null);
    }

    @Test
    public void nonUniqueUrlPromocodeActivatedInPastIsNotApplicable() {
        BalancePromocodeInfo info = new BalancePromocodeInfo()
                .withUniqueUrlNeeded(false)
                .withCode(CODE)
                .withAvailableQty(BigDecimal.ONE)
                .withInvoiceEnabledAt(LONG_TIME_AGO);

        assertFalse(service.isApplicablePromocode(info, Map.of()));
    }

    @Test
    public void nonUniqueUrlPromocodeActivatedInFutureIsNotApplicable() {
        BalancePromocodeInfo info = new BalancePromocodeInfo()
                .withUniqueUrlNeeded(false)
                .withCode(CODE)
                .withAvailableQty(BigDecimal.ONE)
                .withInvoiceEnabledAt(LocalDateTime.now().plusMonths(4));

        assertFalse(service.isApplicablePromocode(info, Map.of()));
    }

    @Test
    public void nonUniqueUrlKnownDomainPromocodeActivatedAfterDefaultDateIsApplicable() {
        BalancePromocodeInfo info = new BalancePromocodeInfo()
                .withUniqueUrlNeeded(false)
                .withCode(CODE.toLowerCase() + " ")
                .withAvailableQty(BigDecimal.ONE)
                .withInvoiceEnabledAt(BORDER.plusSeconds(1));

        assertTrue(service.isApplicablePromocode(info, Map.of(CODE, new PromocodeClientDomain())));
    }

    @Test
    public void uniqueUrlPromocodeActivatedInDefaultDateIsNotApplicable() {
        BalancePromocodeInfo info = new BalancePromocodeInfo()
                .withUniqueUrlNeeded(true)
                .withCode(CODE)
                .withAvailableQty(BigDecimal.ONE)
                .withInvoiceEnabledAt(BORDER);

        assertFalse(service.isApplicablePromocode(info, Map.of()));
    }

    @Test
    public void uniqueUrlPromocodeActivatedAfterDefaultDateIsApplicable() {
        BalancePromocodeInfo info = new BalancePromocodeInfo()
                .withUniqueUrlNeeded(true)
                .withCode(CODE)
                .withAvailableQty(BigDecimal.ONE)
                .withInvoiceEnabledAt(BORDER.plusSeconds(1));

        assertTrue(service.isApplicablePromocode(info, Map.of()));
    }

    @Test
    public void uniqueUrlPromocodeActivatedAfterDefaultDateWithoutAvailableQtyIsNotApplicable() {
        BalancePromocodeInfo info = new BalancePromocodeInfo()
                .withUniqueUrlNeeded(true)
                .withCode(CODE)
                .withAvailableQty(BigDecimal.ZERO)
                .withInvoiceEnabledAt(BORDER.plusSeconds(1));

        assertFalse(service.isApplicablePromocode(info, Map.of()));
    }

    @Test
    public void nonUniqueUrlWithoutAvailableQtyPromocodeActivatedInFutureIsNotApplicable() {
        BalancePromocodeInfo info = new BalancePromocodeInfo()
                .withUniqueUrlNeeded(false)
                .withCode(CODE)
                .withAvailableQty(BigDecimal.ZERO)
                .withInvoiceEnabledAt(LocalDateTime.now().plusMonths(4));

        assertFalse(service.isApplicablePromocode(info, Map.of()));
    }
}

package ru.yandex.market.checkout.checkouter.actualization.multicart.processor;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.checkout.application.AbstractWebTestBase;
import ru.yandex.market.checkout.checkouter.actualization.flow.context.MultiCartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartParameters;
import ru.yandex.market.checkout.checkouter.actualization.model.MultiCartContext;
import ru.yandex.market.checkout.checkouter.installments.InstallmentsInfo;
import ru.yandex.market.checkout.checkouter.installments.InstallmentsOption;
import ru.yandex.market.checkout.checkouter.installments.MonthlyPayment;
import ru.yandex.market.checkout.checkouter.pay.validation.InstallmentsValidator;
import ru.yandex.market.checkout.checkouter.pay.validation.InstallmentsValidatorDecision;

import static ru.yandex.market.checkout.checkouter.pay.validation.InstallmentsValidatorDecision.InstallmentsValidatorDecisionType.OK;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;

class InstallmentsPaymentOptionsProcessorTest extends AbstractWebTestBase {

    private static final List<InstallmentsOption> INSTALLMENTS_OPTION_LIST =
            List.of(
                    new InstallmentsOption("6", new MonthlyPayment(Currency.RUR, "1600")),
                    new InstallmentsOption("12", new MonthlyPayment(Currency.RUR, "363")),
                    new InstallmentsOption("24", new MonthlyPayment(Currency.RUR, "233"))
            );

    @Autowired
    private InstallmentsValidator installmentsValidator;

    private InstallmentsPaymentOptionsProcessor processor;

    @BeforeEach
    void setUp() {
        InstallmentsValidator mockValidator = Mockito.spy(installmentsValidator);
        Mockito.doReturn(
                new InstallmentsValidatorDecision(
                        new InstallmentsInfo(
                                INSTALLMENTS_OPTION_LIST,
                                INSTALLMENTS_OPTION_LIST.get(INSTALLMENTS_OPTION_LIST.size() - 1)
                        ),
                        OK
                )
        ).when(mockValidator)
                .availableForMultiCart(Mockito.any(), Mockito.any());
        processor = new InstallmentsPaymentOptionsProcessor(mockValidator);
    }

    @Test
    void testSelectedOption() {
        var multiCart = orderCreateHelper.cart(defaultBlueOrderParameters()
                .addOrder(defaultBlueOrderParameters()));
        multiCart.setInstallmentsInfo(
                new InstallmentsInfo(
                        INSTALLMENTS_OPTION_LIST,
                        new InstallmentsOption("6", new MonthlyPayment(Currency.RUR, "1 600"))
                )
        );
        var multiCartParameters = ImmutableMultiCartParameters.builder()
                .withBuyerRegionId(123L)
                .withShowInstallments(true)
                .build();
        var multiCartContext = MultiCartContext.createBy(multiCartParameters, Map.of());
        processor.process(multiCart, MultiCartFetchingContext.of(multiCartContext, multiCart));

        Assertions.assertEquals("6", multiCart.getInstallmentsInfo().getSelected().getTerm());
        multiCart.getCarts().forEach(cart -> Assertions.assertFalse(cart.hasErrors()));
    }

    @Test
    void testInvalidOption() {
        var multiCart = orderCreateHelper.cart(defaultBlueOrderParameters()
                .addOrder(defaultBlueOrderParameters()));
        multiCart.setInstallmentsInfo(
                new InstallmentsInfo(
                        INSTALLMENTS_OPTION_LIST,
                        new InstallmentsOption("146", new MonthlyPayment(Currency.RUR, "100000"))
                )
        );
        var multiCartParameters = ImmutableMultiCartParameters.builder()
                .withBuyerRegionId(123L)
                .withShowInstallments(true)
                .build();
        var multiCartContext = MultiCartContext.createBy(multiCartParameters, Map.of());
        processor.process(multiCart, MultiCartFetchingContext.of(multiCartContext, multiCart));

        multiCart.getCarts().forEach(cart -> Assertions.assertTrue(cart.hasErrors()));
    }
}

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
import ru.yandex.market.checkout.checkouter.credit.CreditInformation;
import ru.yandex.market.checkout.checkouter.credit.CreditOption;
import ru.yandex.market.checkout.checkouter.installments.MonthlyPayment;
import ru.yandex.market.checkout.checkouter.pay.validation.CreditValidator;
import ru.yandex.market.checkout.checkouter.pay.validation.CreditValidatorDecision;
import ru.yandex.market.checkout.checkouter.util.CheckouterProperties;

import static ru.yandex.market.checkout.checkouter.feature.type.common.BooleanFeatureType.ENABLE_CREDIT_BROKER;
import static ru.yandex.market.checkout.checkouter.pay.validation.CreditValidatorDecision.CreditValidatorDecisionType.OK;
import static ru.yandex.market.checkout.providers.BlueParametersProvider.defaultBlueOrderParameters;


class CreditPaymentOptionsProcessorTest extends AbstractWebTestBase {

    private static final List<CreditOption> CREDIT_OPTION_LIST =
            List.of(
                    new CreditOption("6", new MonthlyPayment(Currency.RUR, "1600")),
                    new CreditOption("12", new MonthlyPayment(Currency.RUR, "363")),
                    new CreditOption("24", new MonthlyPayment(Currency.RUR, "233"))
            );

    @Autowired
    CheckouterProperties checkouterProperties;

    @Autowired
    private CreditValidator creditValidator;

    private CreditPaymentOptionsProcessor processor;

    @BeforeEach
    void setUp() {
        checkouterFeatureWriter.writeValue(ENABLE_CREDIT_BROKER, true);

        CreditValidator mockValidator = Mockito.spy(creditValidator);

        CreditInformation creditInformation = new CreditInformation();
        creditInformation.setOptions(CREDIT_OPTION_LIST);
        creditInformation.setSelected(CREDIT_OPTION_LIST.get(CREDIT_OPTION_LIST.size() - 1));
        Mockito.doReturn(
                new CreditValidatorDecision(creditInformation, OK)
        ).when(mockValidator)
                .availableForMultiCart(Mockito.any(), Mockito.any());
        processor = new CreditPaymentOptionsProcessor(checkouterProperties, mockValidator, checkouterFeatureReader);
    }

    @Test
    void testSelectedOption() {
        var multiCart = orderCreateHelper.cart(defaultBlueOrderParameters()
                .addOrder(defaultBlueOrderParameters()));

        CreditInformation creditInformation = new CreditInformation();
        creditInformation.setSelected(new CreditOption("6", new MonthlyPayment(Currency.RUR, "1 600")));
        multiCart.setCreditInformation(creditInformation);
        var multiCartParameters = ImmutableMultiCartParameters.builder()
                .withBuyerRegionId(123L)
                .withShowCredits(true)
                .withShowCreditBroker(true)
                .build();
        var multiCartContext = MultiCartContext.createBy(multiCartParameters, Map.of());
        processor.process(multiCart, MultiCartFetchingContext.of(multiCartContext, multiCart));

        Assertions.assertEquals("6", multiCart.getCreditInformation().getSelected().getTerm());
        multiCart.getCarts().forEach(cart -> Assertions.assertFalse(cart.hasErrors()));
    }

    @Test
    void testInvalidOption() {
        var multiCart = orderCreateHelper.cart(defaultBlueOrderParameters()
                .addOrder(defaultBlueOrderParameters()));

        CreditInformation creditInformation = new CreditInformation();
        creditInformation.setSelected(new CreditOption("666", new MonthlyPayment(Currency.RUR, "19000")));
        multiCart.setCreditInformation(creditInformation);
        var multiCartParameters = ImmutableMultiCartParameters.builder()
                .withBuyerRegionId(123L)
                .withShowCredits(true)
                .withShowCreditBroker(true)
                .build();
        var multiCartContext = MultiCartContext.createBy(multiCartParameters, Map.of());
        processor.process(multiCart, MultiCartFetchingContext.of(multiCartContext, multiCart));

        multiCart.getCarts().forEach(cart -> Assertions.assertTrue(cart.hasErrors()));
    }

    @Test
    void testInvalidOptionShouldBeIgnoredIfItIsNotCreditBroker() {
        var multiCart = orderCreateHelper.cart(defaultBlueOrderParameters()
                .addOrder(defaultBlueOrderParameters()));

        CreditInformation creditInformation = new CreditInformation();
        creditInformation.setSelected(new CreditOption("666", new MonthlyPayment(Currency.RUR, "19000")));
        multiCart.setCreditInformation(creditInformation);
        var multiCartParameters = ImmutableMultiCartParameters.builder()
                .withBuyerRegionId(123L)
                .withShowCredits(true)
                .withShowCreditBroker(false)
                .build();
        var multiCartContext = MultiCartContext.createBy(multiCartParameters, Map.of());
        processor.process(multiCart, MultiCartFetchingContext.of(multiCartContext, multiCart));

        multiCart.getCarts().forEach(cart -> Assertions.assertFalse(cart.hasErrors()));
    }
}

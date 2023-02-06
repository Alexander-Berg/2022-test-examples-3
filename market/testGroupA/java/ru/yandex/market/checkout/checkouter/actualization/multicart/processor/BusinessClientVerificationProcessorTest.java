package ru.yandex.market.checkout.checkouter.actualization.multicart.processor;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.util.CollectionUtils;

import ru.yandex.market.checkout.checkouter.actualization.flow.context.MultiCartFetchingContext;
import ru.yandex.market.checkout.checkouter.actualization.model.BusinessClientVerificationResult;
import ru.yandex.market.checkout.checkouter.actualization.model.MultiCartContext;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.trace.CheckoutContextHolder;
import ru.yandex.market.checkout.checkouter.validation.ValidationResult;
import ru.yandex.market.checkout.providers.B2bCustomersTestProvider;
import ru.yandex.market.checkout.test.providers.BuyerProvider;

import static java.util.Optional.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BusinessClientVerificationProcessorTest {

    private BusinessClientVerificationProcessor processor = new BusinessClientVerificationProcessor();

    @BeforeEach
    public void beforeEach() {
        CheckoutContextHolder.setCheckoutOperation(true);
    }

    @AfterEach
    public void afterEach() {
        CheckoutContextHolder.setCheckoutOperation(false);
    }

    @Test
    public void ifNotCheckoutOperationThenNoError() {
        // Given
        CheckoutContextHolder.setCheckoutOperation(false);

        MultiCart multiCart = new MultiCart();
        multiCart.addCart(order(true));

        MultiCartFetchingContext context = Mockito.mock(MultiCartFetchingContext.class);

        // When
        processor.process(multiCart, context);

        // Then
        assertTrue(CollectionUtils.isEmpty(multiCart.getValidationErrors()));
    }

    private Order order(boolean isBusinessClient) {
        Buyer buyer = new Buyer();
        buyer.setUid(BuyerProvider.UID);
        buyer.setBusinessBalanceId(isBusinessClient ? B2bCustomersTestProvider.BUSINESS_BALANCE_ID : null);

        Order order = new Order();
        order.setBuyer(buyer);

        return order;
    }

    @Test
    public void ifNotBusinessClientThenNoError() {
        // Given
        CheckoutContextHolder.setCheckoutOperation(true);

        MultiCart multiCart = new MultiCart();
        multiCart.addCart(order(false));

        MultiCartFetchingContext context = Mockito.mock(MultiCartFetchingContext.class);

        // When
        processor.process(multiCart, context);

        // Then
        assertTrue(CollectionUtils.isEmpty(multiCart.getValidationErrors()));
    }

    @Test
    public void ifNoVerificationResultThenError() throws Throwable {
        // Given
        CheckoutContextHolder.setCheckoutOperation(true);

        MultiCart multiCart = new MultiCart();
        multiCart.addCart(order(true));

        MultiCartFetchingContext context = Mockito.mock(MultiCartFetchingContext.class);
        MultiCartContext multiCartContext = Mockito.mock(MultiCartContext.class);
        Mockito.doReturn(multiCartContext).when(context).getMultiCartContext();
        Mockito.doReturn(empty()).when(multiCartContext).getBusinessClientVerification();

        // When
        processor.process(multiCart, context);

        // Then
        List<ValidationResult> errors = multiCart.getValidationErrors();
        assertEquals(1, errors.size());
        assertEquals(ValidationResult.Severity.ERROR, errors.get(0).getSeverity());
        assertEquals("BUSINESS_CLIENT_VERIFICATION_FAILED", errors.get(0).getCode());
    }

    @Test
    public void ifResultIsTrueThenNoError() throws Throwable {
        // Given
        CheckoutContextHolder.setCheckoutOperation(true);

        MultiCart multiCart = new MultiCart();
        multiCart.addCart(order(true));

        BusinessClientVerificationResult result = new BusinessClientVerificationResult(true);
        MultiCartFetchingContext context = Mockito.mock(MultiCartFetchingContext.class);
        MultiCartContext multiCartContext = Mockito.mock(MultiCartContext.class);
        Mockito.doReturn(multiCartContext).when(context).getMultiCartContext();
        Mockito.doReturn(Optional.of(result)).when(multiCartContext).getBusinessClientVerification();

        // When
        processor.process(multiCart, context);

        // Then
        assertTrue(CollectionUtils.isEmpty(multiCart.getValidationErrors()));
    }

    @Test
    public void ifResultIsFalseThenError() throws Throwable {
        // Given
        CheckoutContextHolder.setCheckoutOperation(true);

        MultiCart multiCart = new MultiCart();
        multiCart.addCart(order(true));

        MultiCartFetchingContext context = Mockito.mock(MultiCartFetchingContext.class);
        MultiCartContext multiCartContext = Mockito.mock(MultiCartContext.class);
        Mockito.doReturn(multiCartContext).when(context).getMultiCartContext();
        Mockito.doReturn(empty()).when(multiCartContext).getBusinessClientVerification();

        // When
        processor.process(multiCart, context);

        // Then
        List<ValidationResult> errors = multiCart.getValidationErrors();
        assertEquals(1, errors.size());
        assertEquals(ValidationResult.Severity.ERROR, errors.get(0).getSeverity());
        assertEquals("BUSINESS_CLIENT_VERIFICATION_FAILED", errors.get(0).getCode());
    }

    @Test
    public void ifExceptionThenNoPropagation() throws Throwable {
        // Given
        CheckoutContextHolder.setCheckoutOperation(true);

        MultiCart multiCart = new MultiCart();
        multiCart.addCart(order(true));

        MultiCartFetchingContext context = Mockito.mock(MultiCartFetchingContext.class);
        MultiCartContext multiCartContext = Mockito.mock(MultiCartContext.class);
        Mockito.doReturn(multiCartContext).when(context).getMultiCartContext();
        Mockito.doReturn(empty()).when(multiCartContext).getBusinessClientVerification();

        // When + Then
        Assertions.assertDoesNotThrow(() -> processor.process(multiCart, context));
    }
}

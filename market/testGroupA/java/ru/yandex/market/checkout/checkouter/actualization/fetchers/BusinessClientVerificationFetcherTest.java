package ru.yandex.market.checkout.checkouter.actualization.fetchers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.checkout.checkouter.actualization.model.BusinessClientVerificationResult;
import ru.yandex.market.checkout.checkouter.actualization.model.ImmutableMultiCartContext;
import ru.yandex.market.checkout.checkouter.b2b.B2bCustomersApi;
import ru.yandex.market.checkout.checkouter.b2b.B2bCustomersClient;
import ru.yandex.market.checkout.checkouter.cart.MultiCart;
import ru.yandex.market.checkout.checkouter.order.Buyer;
import ru.yandex.market.checkout.checkouter.order.immutable.ImmutableMultiCart;
import ru.yandex.market.checkout.checkouter.trace.CheckoutContextHolder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class BusinessClientVerificationFetcherTest {

    @Mock
    private B2bCustomersClient b2bCustomersClient;

    @InjectMocks
    private BusinessClientVerificationFetcher fetcher;

    @BeforeEach
    public void beforeEach() {
        CheckoutContextHolder.setCheckoutOperation(true);
    }

    @AfterEach
    public void afterEach() {
        CheckoutContextHolder.setCheckoutOperation(false);
    }

    @Test
    public void isNotACheckoutOperationThenNoCallsToApiAndReturnsNull() {
        CheckoutContextHolder.setCheckoutOperation(false);
        ImmutableMultiCartContext context = Mockito.mock(ImmutableMultiCartContext.class);

        BusinessClientVerificationResult result = fetcher.fetch(context);

        Assertions.assertNull(result);
        verifyNoInteractions(b2bCustomersClient);
    }

    private ImmutableMultiCartContext setUpBuyer(Buyer buyer) {
        ImmutableMultiCartContext context = Mockito.mock(ImmutableMultiCartContext.class);
        MultiCart multiCart = Mockito.mock(MultiCart.class);
        Mockito.doReturn(buyer).when(multiCart).getBuyer();
        ImmutableMultiCart immutableMultiCart = ImmutableMultiCart.from(multiCart);
        Mockito.doReturn(immutableMultiCart).when(context).getMultiCart();
        return context;
    }

    @Test
    public void ifNoBuyerThenNoCallsToApiAndReturnsNull() {
        Buyer buyer = null;
        ImmutableMultiCartContext context = setUpBuyer(buyer);

        BusinessClientVerificationResult result = fetcher.fetch(context);

        Assertions.assertNull(result);
        verifyNoInteractions(b2bCustomersClient);
    }

    @Test
    public void ifNoUidThenNoCallsToApiAndReturnsNull() {
        Buyer buyer = new Buyer();
        buyer.setUid(null);
        ImmutableMultiCartContext context = setUpBuyer(buyer);

        BusinessClientVerificationResult result = fetcher.fetch(context);

        Assertions.assertNull(result);
        verifyNoInteractions(b2bCustomersClient);
    }

    @Test
    public void ifTypeIsNotBusinessThenNoCallsToApiAndReturnsNull() {
        Buyer buyer = new Buyer();
        buyer.setUid(1L);
        buyer.setBusinessBalanceId(null);  // leads to Person type
        ImmutableMultiCartContext context = setUpBuyer(buyer);

        BusinessClientVerificationResult result = fetcher.fetch(context);

        Assertions.assertNull(result);
        verifyNoInteractions(b2bCustomersClient);
    }

    @Test
    public void ifTypeIsBusinessThenCallsToApi() {
        Buyer buyer = new Buyer();
        buyer.setUid(1L);
        buyer.setBusinessBalanceId(2L);
        ImmutableMultiCartContext context = setUpBuyer(buyer);

        fetcher.fetch(context);

        verify(b2bCustomersClient).isClientCanOrder(buyer.getUid(), buyer.getBusinessBalanceId());
    }

    @Test
    public void ifApiResponseIsNullThenCanOrderIsFalse() {
        Buyer buyer = new Buyer();
        buyer.setUid(1L);
        buyer.setBusinessBalanceId(2L);
        ImmutableMultiCartContext context = setUpBuyer(buyer);
        Mockito.doReturn(null).when(b2bCustomersClient)
                .isClientCanOrder(buyer.getUid(), buyer.getBusinessBalanceId());

        BusinessClientVerificationResult result = fetcher.fetch(context);

        assertFalse(result.isCanOrder());
    }

    @Test
    public void ifApiResponseIsFalseThenCanOrderIsFalse() {
        Buyer buyer = new Buyer();
        buyer.setUid(1L);
        buyer.setBusinessBalanceId(2L);
        ImmutableMultiCartContext context = setUpBuyer(buyer);
        B2bCustomersApi.ClientCanOrderResponse response = new B2bCustomersApi.ClientCanOrderResponse(
                String.valueOf(buyer.getUid()),
                false
        );
        Mockito.doReturn(response).when(b2bCustomersClient)
                .isClientCanOrder(buyer.getUid(), buyer.getBusinessBalanceId());

        BusinessClientVerificationResult result = fetcher.fetch(context);

        assertFalse(result.isCanOrder());
    }

    @Test
    public void ifApiResponseIsTrueThenCanOrderIsTrue() {
        Buyer buyer = new Buyer();
        buyer.setUid(1L);
        buyer.setBusinessBalanceId(2L);
        ImmutableMultiCartContext context = setUpBuyer(buyer);
        B2bCustomersApi.ClientCanOrderResponse response = new B2bCustomersApi.ClientCanOrderResponse(
                String.valueOf(buyer.getUid()),
                true
        );
        Mockito.doReturn(response).when(b2bCustomersClient)
                .isClientCanOrder(buyer.getUid(), buyer.getBusinessBalanceId());

        BusinessClientVerificationResult result = fetcher.fetch(context);

        assertTrue(result.isCanOrder());
    }
}

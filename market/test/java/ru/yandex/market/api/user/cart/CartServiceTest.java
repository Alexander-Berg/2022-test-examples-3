package ru.yandex.market.api.user.cart;

import java.util.Collections;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.market.api.domain.OfferId;
import ru.yandex.market.api.domain.OfferIdEncodingService;
import ru.yandex.market.api.domain.v2.OfferPriceV2;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.error.InvalidParameterValueException;
import ru.yandex.market.api.error.validation.IncorrectParameterValueError;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.internal.carter.CartServiceImpl;
import ru.yandex.market.api.internal.carter.CarterClient;
import ru.yandex.market.api.internal.carter.MarketClickerService;
import ru.yandex.market.api.internal.carter.domain.AddItemResult;
import ru.yandex.market.api.internal.carter.domain.GetCartResult;
import ru.yandex.market.api.internal.carter.domain.RemoveItemResult;
import ru.yandex.market.api.internal.common.GenericParams;
import ru.yandex.market.api.offer.OfferService;
import ru.yandex.market.api.server.sec.User;
import ru.yandex.market.api.test.ExceptionMatcher;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithMocks;
import ru.yandex.market.api.util.concurrent.Pipelines;
import ru.yandex.market.api.util.httpclient.HttpClientFactory;
import ru.yandex.market.http.HttpClient;
import ru.yandex.market.http.HttpClientBuilder;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by apershukov on 28.09.16.
 */

@WithMocks
public class CartServiceTest extends UnitTestBase {

    private static final String WARE_MD5 = "offer_id";
    private static final String FEE_SHOW = "feeShow";
    private static final String OFFER_ID = WARE_MD5 + ":" + FEE_SHOW;

    @Mock
    private CarterClient carterClient;
    @Mock
    private OfferService offerService;
    @Mock
    private OfferIdEncodingService offerIdEncodingService;
    @Mock
    private HttpClient httpClient;
    @Mock
    private HttpClientFactory httpClientFactory;
    @Mock
    private MarketClickerService marketClickerService;

    private CartServiceImpl service;

    private User user;

    @Before
    public void setUp() {
        when(offerIdEncodingService.decode(any())).thenAnswer(invocation -> {
            String s = (String) invocation.getArguments()[0];
            String[] split = s.split(":");
            return new OfferId(split[0], split.length > 1 ? split[1] : null);
        });

        user = mock(User.class);

        OfferV2 offer = new OfferV2();
        offer.setCpa(true);
        offer.setId(new OfferId(WARE_MD5, FEE_SHOW));
        offer.setPrice(new OfferPriceV2("111", null, null));

        when(offerService.getOffersV2(eq(Collections.singleton(WARE_MD5)), any(), anyBoolean(), any()))
            .thenReturn(Pipelines.startWithValue(ImmutableMap.of(WARE_MD5, offer)));

        AddItemResult result = new AddItemResult();
        result.setStatus("success");
        result.setResult(1L);

        GetCartResult cart = new GetCartResult();

        when(carterClient.addItem(eq(user), any(), eq(false))).thenReturn(Pipelines.startWithValue(result));
        when(carterClient.getCart(eq(user), anyInt(), any(), any())).thenReturn(Pipelines.startWithValue(cart));

        HttpClientBuilder builder = mock(HttpClientBuilder.class);
        when(builder.build()).thenReturn(httpClient);
        when(httpClientFactory.create("MarketClick")).thenReturn(builder);

        service = new CartServiceImpl(
            carterClient,
            offerService,
            offerIdEncodingService,
            marketClickerService
        );
    }

    @Test
    public void addItemWithPlainOfferId() throws Exception {
        exception.expect(new ExceptionMatcher<InvalidParameterValueException>() {

            @Override
            protected boolean match(InvalidParameterValueException e) {
                return e.getErrors().contains(new IncorrectParameterValueError(CartServiceImpl.AddItemRequestXpathError.OFFER_ID));
            }
        });

        service.addItem(user, "plain-ware-md5-offer-id", null, GenericParams.DEFAULT);
    }

    @Test
    public void addItemWithCpaUrl() {
        String cpaUrl = "https://market-click2.yandex.ru/redir/338FT8NBg56v_gbYff0piSt7Lyx_vLSnO-6kkBDFUcw03VVZ" +
            "KpNnKtitg2T2kSZ7u-rsIRZDa6w,,&b64e=1&sign=79180574400ef9ccca2f5feb398ad732&keyno=1";

        service.addItem(user, OFFER_ID, cpaUrl, GenericParams.DEFAULT);

        verify(marketClickerService).click(cpaUrl);
    }

    @Test
    public void noCpaUrlCallOnAddItemWithNoCpaUrl() {
        service.addItem(user, OFFER_ID, null, GenericParams.DEFAULT);
        Mockito.verifyZeroInteractions(httpClient);
    }


    @Test
    public void deleteNotExistingItem() {
        RemoveItemResult result = new RemoveItemResult();
        result.setMessage("Item not found");
        when(carterClient.removeItem(user, 111)).thenReturn(Pipelines.startWithValue(result));

        service.removeItem(user, 111);
    }

    @Test
    public void deleteItemFromNotExistingList() {
        RemoveItemResult result = new RemoveItemResult();
        result.setMessage("List not found");
        when(carterClient.removeItem(user, 111)).thenReturn(Pipelines.startWithValue(result));

        service.removeItem(user, 111);
    }
}

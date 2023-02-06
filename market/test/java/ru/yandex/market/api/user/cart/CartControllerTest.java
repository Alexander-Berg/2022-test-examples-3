package ru.yandex.market.api.user.cart;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.domain.OfferId;
import ru.yandex.market.api.domain.OfferIdEncodingService;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.blackbox.data.OauthUser;
import ru.yandex.market.api.internal.common.GenericParams;
import ru.yandex.market.api.server.sec.User;
import ru.yandex.market.api.util.httpclient.clients.CarterTestClient;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Set;

public class CartControllerTest extends BaseTest {

    private static final long USER_UID = 123;
    private static final String OFFER_ID = "yDpJekrrgZHdGKY0rj7oWdZ_QlrDNIisBOHWOYL5bevYKXqsbYZDk_8h3Ww8CvOF-0xWgi4K4BK3EnH5CrLZxNvpVtmI9haWWpfT4Ku3eNhzkGAy87ISO9r-XHJYtg5mFTEYPiz9nvcTmP8IA3XXd0CH868do4ihVV9h6FcH3fgCGsCVURHLHFcWKYzP2oacYIw_x50_5SY3G3zbxUzQf6aMa3QDPpD4nZnezlgaDzw";
    @Inject
    ReportTestClient reportTestClient;
    @Inject
    private CartController controller;
    @Inject
    private CarterTestClient carterTestClient;
    @Inject
    private OfferIdEncodingService offerIdEncodingService;

    @Test
    public void createTimeIsNotEqualToZero() {
        GenericParams genericParams = getGenericParams();
        User user = getUser();
        CartItemAddRequest request = getCartItemAddRequest(OFFER_ID);

        reportTestClient.getOffersV2(getOfferIds(OFFER_ID), "createTimeIsNotEqualToZero_offer.json");
        carterTestClient.addItem(USER_UID, 3031961l, 1493119391000l);
        carterTestClient.getCart(USER_UID, "createTimeIsNotEqualToZero_cart.json");

        CartItem cartItem = controller.addItem(request, user, genericParams);
        Assert.assertEquals(1493117669000l, cartItem.getCreationTime());
    }

    @NotNull
    private CartItemAddRequest getCartItemAddRequest(String offerId) {
        CartItemAddRequest cartItemAddRequest = new CartItemAddRequest();
        cartItemAddRequest.setOfferId(offerId);
        return cartItemAddRequest;
    }

    private GenericParams getGenericParams() {
        return GenericParams.DEFAULT;
    }

    private OfferId[] getOfferIds(String offerId) {
        OfferId decode = offerIdEncodingService.decode(offerId);
        Set<OfferId> coll = Collections.singleton(new OfferId(decode.getWareMd5(), decode.getFeeShow(), decode.getOriginalWareMd5()));
        return coll.toArray(new OfferId[coll.size()]);
    }

    private User getUser() {
        OauthUser oauthUser = new OauthUser(USER_UID);
        User user = new User(oauthUser, null, null, null);
        return user;
    }
}

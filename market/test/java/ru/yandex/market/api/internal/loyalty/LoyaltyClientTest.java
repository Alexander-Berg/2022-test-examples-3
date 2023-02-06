package ru.yandex.market.api.internal.loyalty;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.ContextHolderTestHelper;
import ru.yandex.market.api.domain.v2.PerkType;
import ru.yandex.market.api.domain.v2.PerkStatus;
import ru.yandex.market.api.domain.v2.PerkTag;
import ru.yandex.market.api.domain.v2.PromoThreshold;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.blackbox.data.OauthUser;
import ru.yandex.market.api.server.context.Context;
import ru.yandex.market.api.server.sec.User;
import ru.yandex.market.api.server.sec.Uuid;
import ru.yandex.market.api.util.httpclient.clients.LoyaltyTestClient;
import ru.yandex.market.loyalty.api.model.discount.FreeDeliveryStatus;

import static org.hamcrest.Matchers.hasSize;

public class LoyaltyClientTest extends BaseTest {
    private static final long USER_ID = 123;
    private static final String UUID = "12345678901234567890123456789012";
    private static final int REGION_ID = 213;

    @Inject
    LoyaltyTestClient loyaltyTestClient;

    @Inject
    LoyaltyClient loyaltyClient;

    @Test
    public void shouldReturnEmptyCollectionWhenLoyaltyUnavailable() throws ExecutionException, InterruptedException {
        loyaltyTestClient.checkStatusTimeout(REGION_ID, USER_ID, 1500);
        User user = new User(new OauthUser(USER_ID), null, new Uuid(UUID), null);
        List<PerkStatus> perks = loyaltyClient.checkStatus(REGION_ID, user).get().getPerkStatuses();
        Assert.assertEquals(Collections.emptyList(), perks);
    }

    @Test
    public void shouldReturnEmptyCollectionWhenLoyaltyReturnsError() throws ExecutionException, InterruptedException {
        loyaltyTestClient.checkStatusError(REGION_ID, USER_ID);
        User user = new User(new OauthUser(USER_ID), null, new Uuid(UUID), null);
        List<PerkStatus> perks = loyaltyClient.checkStatus(REGION_ID, user).get().getPerkStatuses();
        Assert.assertEquals(Collections.emptyList(), perks);
    }

    @Test
    public void shouldReturnEmptyThresholdsWhenLoyaltyReturnsError() throws ExecutionException, InterruptedException {
        loyaltyTestClient.checkStatusError(REGION_ID, USER_ID);
        User user = new User(new OauthUser(USER_ID), null, new Uuid(UUID), null);
        List<PromoThreshold> perks = loyaltyClient.checkStatus(REGION_ID, user).get().getDisabledPromoThresholds();
        Assert.assertEquals(Collections.emptyList(), perks);
    }

    @Test
    public void shouldReturnEmptyTagsWhenLoyaltyReturnsError() throws ExecutionException, InterruptedException {
        loyaltyTestClient.checkStatusError(REGION_ID, USER_ID);
        User user = new User(new OauthUser(USER_ID), null, new Uuid(UUID), null);
        List<PerkTag> tags = loyaltyClient.checkStatus(REGION_ID, user).get().getPerkTags();
        Assert.assertEquals(Collections.emptyList(), tags);
    }

    @Test
    public void shouldReturnLoyaltyResponseWhenLoyaltyAvailable() throws ExecutionException, InterruptedException {
        loyaltyTestClient.checkStatus(REGION_ID, USER_ID, "loyalty_1.json");
        User user = new User(new OauthUser(USER_ID), null, new Uuid(UUID), null);
        List<PerkStatus> perks = loyaltyClient.checkStatus(REGION_ID, user).get().getPerkStatuses();
        Assert.assertThat(perks, hasSize(3));
    }

    @Test
    public void shouldReturnPersonalPerksWhenLoyaltyReturns() throws ExecutionException, InterruptedException {
        loyaltyTestClient.checkStatus(REGION_ID, USER_ID, "loyalty_1.json");
        User user = new User(new OauthUser(USER_ID), null, new Uuid(UUID), null);
        List<PerkStatus> perks = loyaltyClient.checkStatus(REGION_ID, user).get().getPerkStatuses();
        Assert.assertEquals(perks.get(2).getType(), PerkType.PERSONAL_PROMO.getId());
    }

    @Test
    public void shouldReturnBigThresholdWhenLoyaltyUnavailable() throws ExecutionException, InterruptedException {
        Context context = new Context("req1");
        context.getRegionInfo().setRawRegionId(REGION_ID);
        ContextHolderTestHelper.initContext(context);

        String applicationType = "APPLICATION";
        loyaltyTestClient.getCartThresholdError(REGION_ID, applicationType);
        OrderItemsRequest orderItemsRequest = new OrderItemsRequest();
        orderItemsRequest.setItems(Collections.emptyList());

        ThresholdResponse thresholdResponse = loyaltyClient.getCartThreshold(orderItemsRequest, Collections.emptyList(),
                applicationType).get();

        Assert.assertEquals(BigDecimal.valueOf(Integer.MAX_VALUE), thresholdResponse.getPriceLeftForFreeDelivery());
        Assert.assertEquals(BigDecimal.valueOf(Integer.MAX_VALUE), thresholdResponse.getThreshold());
        Assert.assertEquals(FreeDeliveryStatus.NO_FREE_DELIVERY,thresholdResponse.getFreeDeliveryStatus());
    }
}

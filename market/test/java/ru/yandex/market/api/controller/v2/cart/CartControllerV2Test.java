package ru.yandex.market.api.controller.v2.cart;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Lists;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;

import ru.yandex.market.api.MockClientHelper;
import ru.yandex.market.api.common.client.KnownMobileClientVersionInfo;
import ru.yandex.market.api.common.client.SemanticVersion;
import ru.yandex.market.api.controller.v2.cart.request.CartItemV2AddRequest;
import ru.yandex.market.api.controller.v2.cart.request.CartItemV2EditRequest;
import ru.yandex.market.api.controller.v2.cart.request.GetCartCoinsRequest;
import ru.yandex.market.api.controller.v2.cart.response.CartItemV2ListResult;
import ru.yandex.market.api.controller.v2.cart.response.CartItemV2Result;
import ru.yandex.market.api.domain.OfferId;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.v2.DiscountType;
import ru.yandex.market.api.domain.v2.ResultContextV2;
import ru.yandex.market.api.domain.v2.loyalty.CoinsForCart;
import ru.yandex.market.api.error.ValidationErrors;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.blackbox.data.OauthUser;
import ru.yandex.market.api.internal.common.DeviceType;
import ru.yandex.market.api.internal.common.GenericParams;
import ru.yandex.market.api.internal.common.GenericParamsBuilder;
import ru.yandex.market.api.internal.common.Platform;
import ru.yandex.market.api.internal.resizer.ResizeImageParams;
import ru.yandex.market.api.internal.resizer.ScreenSize;
import ru.yandex.market.api.matchers.SummaryMatcher;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.User;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.ClientHelper;
import ru.yandex.market.api.util.httpclient.clients.CarterTestClient;
import ru.yandex.market.api.util.httpclient.clients.LoyaltyTestClient;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.api.matchers.PageInfoMatcher.count;
import static ru.yandex.market.api.matchers.PageInfoMatcher.page;
import static ru.yandex.market.api.matchers.PageInfoMatcher.pageInfo;
import static ru.yandex.market.api.matchers.PageInfoMatcher.totalPages;

@ActiveProfiles(CartControllerV2Test.PROFILE)
public class CartControllerV2Test extends BaseTest {
    static final String PROFILE = "CartControllerV2Test";

    @Configuration
    @Profile(PROFILE)
    public static class Config {
        @Bean
        @Primary
        public ClientHelper localHelper() {
            return Mockito.mock(ClientHelper.class);
        }
    }
    @Inject
    private CartControllerV2 cartControllerV2;

    @Inject
    private CarterTestClient carterTestClient;

    @Inject
    private ReportTestClient reportTestClient;

    @Inject
    private LoyaltyTestClient loyaltyTestClient;

    @Inject
    private ClientHelper clientHelper;

    private MockClientHelper mockClientHelper;

    private static final long UID = 1L;

    private static final User USER = new User(
        new OauthUser(UID),
        null,
        null,
        null
    );

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        mockClientHelper = new MockClientHelper(clientHelper);
    }

    @Test
    public void getItemWithMsku() {
        long originalId = 112233L;
        long updatedId = 4169576L;
        OfferId offerId = new OfferId("-08kO7GPpSOppv5rr3Oouw", "");

        carterTestClient.getCart(UID, "carter__get_cart__after_add_14112311.json");

        reportTestClient.getOfferInfo(offerId, "report__offerinfo__14112311.json");
        reportTestClient.getModelInfoById(14112311L, "report__modelinfo__14112311.json");

        CartItemV2Result result = cartControllerV2.getItem(USER,
            originalId,
            100131945205L,
            null,
            Collections.emptyList(),
            genericParams,
            new CartControllerV2.SpecificCarterParams(true, false)
        )
            .waitResult();

        assertEquals(updatedId, result.getItem().getId());
    }

    @Test
    public void getItemWithConsolidateDisabled() {
        long originalId = 112233L;
        OfferId offerId = new OfferId("-08kO7GPpSOppv5rr3Oouw", "");
        // setting up httpExpectations
        carterTestClient.getCartWithConsolidate(UID, 213, false, "carter__get_cart__after_add_14112311.json");

        reportTestClient.getOfferInfo(offerId, "report__offerinfo__14112311.json");
        reportTestClient.getModelInfoById(14112311L, "report__modelinfo__14112311.json");

        // will verify httpExpectations in BaseTest#tеarDown()
        cartControllerV2.getItem(USER,
            originalId,
            100131945205L,
            null,
            Collections.emptyList(),
            genericParams,
            new CartControllerV2.SpecificCarterParams(false, false)
        )
                .waitResult();
    }

    @Test
    public void getItemsWithSummary() {
        carterTestClient.getCart(UID, 213, "get_cart_items.json");
        reportTestClient.getOffersV2(
                new OfferId[]{
                        new OfferId("1", "fee-1"),
                        new OfferId("2", "fee-2")
                },
                "report_offers.json"
        );

        loyaltyTestClient.getCartThreshold(213, "APPLICATION",
                "loyalty_get_non_zero_threshold.json");

        CartItemV2ListResult result = cartControllerV2.getItems(
                USER,
                PageInfo.DEFAULT,
                Collections.emptyList(),
                genericParams,
                new CartControllerV2.SpecificCarterParams(true, false)
        ).waitResult();

        assertThat(
                result.getSummary().getDelivery(),
                SummaryMatcher.deliveryCost(BigDecimal.valueOf(0), BigDecimal.valueOf(1000))
        );

        PageInfo pageInfo = ((ResultContextV2) result.getContext()).getPage();
        assertThat(
                pageInfo,
                pageInfo(
                        page(1),
                        count(10),
                        totalPages(1)
                )
        );

    }

    @Test
    public void getItemsWithSummaryEnableMultiOffers() {
        carterTestClient.getRawCart(UID, 213, x -> x.param("enableMultiOffers", "true"), "get_cart_items.json");
        reportTestClient.getOffersV2(
                new OfferId[]{
                        new OfferId("1", "fee-1"),
                        new OfferId("2", "fee-2")
                },
                "report_offers.json"
        );

        loyaltyTestClient.getCartThreshold(213, "APPLICATION",
                "loyalty_get_non_zero_threshold.json");

        CartItemV2ListResult result = cartControllerV2.getItems(
                USER,
                PageInfo.DEFAULT,
                Collections.emptyList(),
                genericParams,
                new CartControllerV2.SpecificCarterParams(true, true)
        ).waitResult();

        assertThat(
                result.getSummary().getDelivery(),
                SummaryMatcher.deliveryCost(BigDecimal.valueOf(0), BigDecimal.valueOf(1000))
        );

        PageInfo pageInfo = ((ResultContextV2) result.getContext()).getPage();
        assertThat(
                pageInfo,
                pageInfo(
                        page(1),
                        count(10),
                        totalPages(1)
                )
        );

    }

    @Test
    public void getItemsWithExperiments() {
        carterTestClient.getCartWithExperiments(UID, 213, "yoyoyo","get_cart_items.json");

        GenericParams params = new GenericParamsBuilder()
                .setResizeImageParams(ResizeImageParams.DEFAULT)
                .setScreenSize(ScreenSize.DEFAULT)
                .setThumbnailSize(Collections.emptySet())
                .setMarketDeliveryOffersOnly(true)
                .setRemoveHtmlTags(false)
                .setRearrFactors("yoyoyo")
                .build();

        CartItemV2ListResult items = cartControllerV2.getItems(
                USER,
                PageInfo.DEFAULT,
                Collections.emptyList(),
                params,
                new CartControllerV2.SpecificCarterParams(true, false)
        ).waitResult();

        assertThat(items,  notNullValue());
        assertThat(items.getItems(),  not(empty()));
    }

    @Test
    public void getItemsWithPromoId() {
        carterTestClient.getCartWithPromoId(UID, 213, "some-promo-id","get_cart_items.json");

        GenericParams params = new GenericParamsBuilder()
                .setResizeImageParams(ResizeImageParams.DEFAULT)
                .setScreenSize(ScreenSize.DEFAULT)
                .setThumbnailSize(Collections.emptySet())
                .setMarketDeliveryOffersOnly(true)
                .setRemoveHtmlTags(false)
                .setPromoId("some-promo-id")
                .build();

        CartItemV2ListResult items = cartControllerV2.getItems(
                USER,
                PageInfo.DEFAULT,
                Collections.emptyList(),
                params,
                new CartControllerV2.SpecificCarterParams(true, false)
        ).waitResult();

        assertThat(items, notNullValue());
        assertThat(items.getItems(), not(empty()));
    }

    @Test
    public void getItemsWithConsolidateDisabled() {
        // setting up httpExpectations
        carterTestClient.getCartWithConsolidate(UID, 213, false, "get_cart_items.json");

        GenericParams params = new GenericParamsBuilder()
                .setResizeImageParams(ResizeImageParams.DEFAULT)
                .setScreenSize(ScreenSize.DEFAULT)
                .setThumbnailSize(Collections.emptySet())
                .setMarketDeliveryOffersOnly(true)
                .setRemoveHtmlTags(false)
                .build();

        // will verify httpExpectations in BaseTest#tеarDown()
        cartControllerV2.getItems(
                USER,
                PageInfo.DEFAULT,
                Collections.emptyList(),
                params,
                new CartControllerV2.SpecificCarterParams(false, false)
        )
                .waitResult();
    }

    @Test
    public void editItemWithSummary() {
        long itemId = 1L;

        carterTestClient.editItem(UID, String.valueOf(itemId), 1, "edit_cart_items.json");
        carterTestClient.getCart(UID, "get_cart_items_before_edit.json");
        carterTestClient.getCart(UID, "get_cart_items_before_edit.json");
        carterTestClient.getCart(UID, "get_cart_items_before_edit.json");

        loyaltyTestClient.getCartThreshold(213, "APPLICATION",
                "loyalty_get_non_zero_threshold.json");

        CartItemV2EditRequest editRequest = new CartItemV2EditRequest();
        editRequest.setCount(1);

        CartItemV2Result result = cartControllerV2.editItem(
                USER,
                itemId,
                editRequest,
                Collections.emptyList(),
                genericParams,
                new CartControllerV2.SpecificCarterParams(true, false)
        ).waitResult();

        assertThat(
                result.getSummary().getDelivery(),
                SummaryMatcher.deliveryCost(BigDecimal.valueOf(0), BigDecimal.valueOf(1000))
        );

    }

    @Test
    public void editItemWithChangedId() {
        long itemId = 1L;
        long newItemId = 4171174L;
        OfferId offerId = new OfferId("OFTxmbtAsGmO-NXu_n2kaA", "feeShow");

        carterTestClient.editItem(UID, String.valueOf(newItemId), 2, "edit_cart_items.json");
        carterTestClient.getCart(UID, "carter__get_cart__before_edit.json");
        carterTestClient.getCart(UID, "carter__get_cart__after_edit.json");
        carterTestClient.getCart(UID, "carter__get_cart__after_edit.json");

        reportTestClient.getOfferInfo(offerId, "report__offerinfo__72179675.json");
        reportTestClient.getModelInfoById(72179675L, "report__modelinfo__72179675.json");

        loyaltyTestClient.getCartThreshold(213, "APPLICATION",
                "loyalty_get_threshold_2.json");

        CartItemV2EditRequest editRequest = new CartItemV2EditRequest();
        editRequest.setMsku(100257128730L);
        editRequest.setCount(2);

        CartItemV2Result result = cartControllerV2.editItem(
                USER,
                itemId,
                editRequest,
                Collections.emptyList(),
                genericParams,
                new CartControllerV2.SpecificCarterParams(true, false)
        ).waitResult();

        assertEquals(newItemId, result.getItem().getId());
        assertEquals(2, result.getItem().getCount());
    }

    @Test
    public void editItemWithConsolidateDisabled() {
        long itemId = 1L;

        // setting up httpExpectations
        carterTestClient.editItem(UID, String.valueOf(itemId), 1, "edit_cart_items.json");
        carterTestClient.getCartWithConsolidate(UID, 213, false, "get_cart_items_before_edit.json");
        carterTestClient.getCartWithConsolidate(UID, 213, false, "get_cart_items_before_edit.json");
        carterTestClient.getCartWithConsolidate(UID, 213, false, "get_cart_items_before_edit.json");

        loyaltyTestClient.getCartThreshold(213, "APPLICATION",
                "loyalty_get_non_zero_threshold.json");

        CartItemV2EditRequest editRequest = new CartItemV2EditRequest();
        editRequest.setCount(1);

        // will verify httpExpectations in BaseTest#tеarDown()
        cartControllerV2.editItem(
                USER,
                itemId,
                editRequest,
                Collections.emptyList(),
                genericParams,
                new CartControllerV2.SpecificCarterParams(false, false)
        )
                .waitResult();
    }

    @Test
    public void deleteItemWithSummary() {
        long itemId = 1L;

        carterTestClient.removeItem(UID, String.valueOf(itemId), "delete_cart_items.json");
        carterTestClient.getCart(UID, "get_cart_items_before_delete.json");
        carterTestClient.getCart(UID, "get_cart_items_before_delete.json");

        reportTestClient.getOffersV2(
                new OfferId[]{new OfferId("5", "fee-5")},
                "report_offer_5.json"
        );

        loyaltyTestClient.getCartThreshold(213, "APPLICATION",
                "loyalty_get_non_zero_threshold.json");

        CartItemV2Result result = cartControllerV2.deleteItem(
                USER,
                itemId,
                null,
                null,
                Collections.emptyList(),
                genericParams,
                new CartControllerV2.SpecificCarterParams(true, false)
        )
                .waitResult();

        assertThat(
                result.getSummary().getDelivery(),
                SummaryMatcher.deliveryCost(BigDecimal.valueOf(0), BigDecimal.valueOf(1000))
        );
    }

    @Test
    public void deleteItemWithChangedId() {
        long itemId = 1L;
        long newItemId = 4175294L;
        long msku = 100197483240L;

        carterTestClient.removeItem(UID, String.valueOf(itemId), "delete_cart_items.json");
        carterTestClient.getCart(UID, "carter__get_cart__before_delete.json");
        carterTestClient.getCart(UID, "carter__get_cart__before_delete.json");

        reportTestClient.getOffersV2(
                new OfferId[]{new OfferId("m-HVyuqxQPBSHG4t8ai6lw", "feeShow")},
                "report__offerinfo__1723497244.json"
        );

        loyaltyTestClient.getCartThreshold(213, "APPLICATION",
                "loyalty_get_threshold_zero_remainder.json");

        CartItemV2Result result = cartControllerV2.deleteItem(
                USER,
                itemId,
                msku,
                null,
                Collections.emptyList(),
                genericParams,
                new CartControllerV2.SpecificCarterParams(true, false)
        ).waitResult();

        assertEquals(newItemId, result.getItem().getId());
        assertThat(result.getItem().getMsku(), is(msku));
    }

    @Test
    public void deleteItemWithConsolidateDisabled() {
        long itemId = 1L;
        long msku = 100197483240L;

        // setting up httpExpectations
        carterTestClient.removeItem(UID, String.valueOf(itemId), "delete_cart_items.json");
        carterTestClient.getCartWithConsolidate(UID, 213, false, "carter__get_cart__before_delete.json");
        carterTestClient.getCartWithConsolidate(UID, 213, false, "carter__get_cart__before_delete.json");

        reportTestClient.getOffersV2(
                new OfferId[]{new OfferId("m-HVyuqxQPBSHG4t8ai6lw", "feeShow")},
                "report__offerinfo__1723497244.json"
        );

        loyaltyTestClient.getCartThreshold(213, "APPLICATION",
                "loyalty_get_threshold_zero_remainder.json");

        // will verify httpExpectations in BaseTest#tеarDown()
        cartControllerV2.deleteItem(
                USER,
                itemId,
                msku,
                null,
                Collections.emptyList(),
                genericParams,
                new CartControllerV2.SpecificCarterParams(false, false)
        )
                .waitResult();
    }

    @Test
    public void addItemWithSummary() {
        OfferId offerId = new OfferId("5y7H1VVQTS9LZdJ2QsgAew",
                "8-qH2tqoDtLBsdAuAHNe5e1XhJGiYa3eJSPpGqyoTt3Awykl2-M2gsEgjiJ02fiuQnM7jDK8FkaZrIzR6RFvp67mtvxKGsa7vo93HucCADls7Yr8sTB2l2uUtqfKbdAFH3CN-I3mhX8h3a4Xz3bL5g,,");

        carterTestClient.addItem(UID, 1, 1522839114000L);
        carterTestClient.getCart(UID, "carter__get_cart__after_add.json");
        carterTestClient.getCart(UID, "carter__get_cart__after_add.json");

        loyaltyTestClient.getCartThreshold(213, "APPLICATION",
                "loyalty_get_threshold.json");

        reportTestClient.getOfferInfo(offerId, "report__offerinfo__14206636.json");
        reportTestClient.getModelInfoById(14206636L, "report__modelinfo__14206636.json");

        CartItemV2AddRequest request = new CartItemV2AddRequest();
        request.setOfferId(offerId);

        CartItemV2Result result = cartControllerV2.addItem(USER, request, Collections.emptyList(), genericParams,
                new CartControllerV2.SpecificCarterParams(true, false), new ValidationErrors()).waitResult();

        assertNotNull(result.getSummary());

        assertThat(
                result.getSummary().getDelivery(),
                SummaryMatcher.deliveryIsFree(Matchers.is(DiscountType.THRESHOLD))
        );
    }

    @Test
    public void addItemWithExternalWareMd5AndFeeShow() {
        String wareMd5 = "5y7H1VVQTS9LZdJ2QsgAew";
        String feeShow = "8-qH2tqoDtLBsdAuAHNe5e1XhJGiYa3eJSPpGqyoTt3Awykl2-M2gsEgjiJ02fiuQnM7jDK8FkaZrIzR6RFvp67mtvxKGsa7vo93HucCADls7Yr8sTB2l2uUtqfKbdAFH3CN-I3mhX8h3a4Xz3bL5g,,";
        OfferId offerId = new OfferId(wareMd5, feeShow);

        carterTestClient.addItem(UID, 1, 1522839114000L);
        carterTestClient.getCart(UID, "carter__get_cart__after_add.json");
        carterTestClient.getCart(UID, "carter__get_cart__after_add.json");

        reportTestClient.getOfferInfo(offerId, "report__offerinfo__14206636.json");
        reportTestClient.getModelInfoById(14206636L, "report__modelinfo__14206636.json");

        loyaltyTestClient.getCartThreshold(213, "APPLICATION",
                "loyalty_get_threshold.json");

        CartItemV2AddRequest request = new CartItemV2AddRequest();
        request.setWareMd5(wareMd5);
        request.setFeeShow(feeShow);

        CartItemV2Result result = cartControllerV2.addItem(USER, request, Collections.emptyList(), genericParams,
                new CartControllerV2.SpecificCarterParams(true, false), new ValidationErrors()).waitResult();

        assertNotNull(result.getItem());
    }

    @Test
    public void addItemWithIdReplacement() {
        long originalId = 112233L;
        long updatedId = 4167868L;

        OfferId originalOfferId = new OfferId("OFTxmbtAsGmO-NXu_n2kaA", "feeShow");
        String wareMd5 = "FIXhfm_kFoUvjGOC2Uejjg";
        OfferId offerId = new OfferId(wareMd5, "");

        CartItemV2AddRequest request = new CartItemV2AddRequest();
        request.setOfferId(originalOfferId);

        //setting up httpExpectations
        carterTestClient.addItem(UID, originalId, 1548151802291L);
        carterTestClient.getCart(UID, "carter__get_cart__after_add_12568033.json");
        carterTestClient.getCart(UID, "carter__get_cart__after_add_12568033.json");

        reportTestClient.getOfferInfo(originalOfferId, "report__offerinfo__12568033_before_update.json");
        reportTestClient.getOfferInfo(offerId, "report__offerinfo__12568033.json");
        reportTestClient.getModelInfoById(12568033L, "report__modelinfo__12568033.json");

        loyaltyTestClient.getCartThreshold(213, "APPLICATION",
                "loyalty_get_threshold_zero_remainder.json");

        CartItemV2Result result = cartControllerV2.addItem(USER, request, Collections.emptyList(), genericParams,
                new CartControllerV2.SpecificCarterParams(true, false), new ValidationErrors()).waitResult();

        assertEquals(updatedId, result.getItem().getId());
        assertEquals(wareMd5, result.getItem().getOffer().getWareMd5());
    }

    @Test
    public void addItemWithConsolidateDisabled() {
        OfferId offerId = new OfferId("5y7H1VVQTS9LZdJ2QsgAew",
                "8-qH2tqoDtLBsdAuAHNe5e1XhJGiYa3eJSPpGqyoTt3Awykl2-M2gsEgjiJ02fiuQnM7jDK8FkaZrIzR6RFvp67mtvxKGsa7vo93HucCADls7Yr8sTB2l2uUtqfKbdAFH3CN-I3mhX8h3a4Xz3bL5g,,");

        // setting up httpExpectations
        carterTestClient.addItem(UID, 1, 1522839114000L);
        carterTestClient.getCartWithConsolidate(UID, 213, false, "carter__get_cart__after_add.json");
        carterTestClient.getCartWithConsolidate(UID, 213, false, "carter__get_cart__after_add.json");

        loyaltyTestClient.getCartThreshold(213, "APPLICATION",
                "loyalty_get_threshold.json");

        reportTestClient.getOfferInfo(offerId, "report__offerinfo__14206636.json");
        reportTestClient.getModelInfoById(14206636L, "report__modelinfo__14206636.json");

        CartItemV2AddRequest request = new CartItemV2AddRequest();
        request.setOfferId(offerId);

        // will verify httpExpectations in BaseTest#tеarDown()
        cartControllerV2.addItem(USER, request, Collections.emptyList(), genericParams,
                new CartControllerV2.SpecificCarterParams(false, false), new ValidationErrors()).waitResult();
    }

    @Test
    public void filterCoinsForOldBlueApp() {
        Client client = new Client();
        client.setType(Client.Type.MOBILE);

        mockClientHelper.is(ClientHelper.Type.BLUE_APP, true);

        ContextHolder.update(ctx -> {
            ctx.setClient(client);
            ctx.setClientVersionInfo(
                new KnownMobileClientVersionInfo(
                    Platform.ANDROID,
                    DeviceType.SMARTPHONE,
                    new SemanticVersion(1, 3, 2)
                )
            );
        });

        loyaltyTestClient.getCartCoins(UID, x -> true,"loyalty__coins_cart__with_inactive.json");

        GetCartCoinsRequest request = new GetCartCoinsRequest();
        request.setItems(Collections.emptyList());
        CoinsForCart result = cartControllerV2.coinsForCart(request, GenericParams.DEFAULT, USER).waitResult();
        assertEquals(1, result.getApplicableCoins().size());
    }

    @Test
    public void getCartCoinsBodyTest() {
        Client client = new Client();
        client.setType(Client.Type.MOBILE);

        mockClientHelper.is(ClientHelper.Type.BLUE_APP, true);

        ContextHolder.update(ctx -> {
            ctx.setClient(client);
            ctx.setClientVersionInfo(
                    new KnownMobileClientVersionInfo(
                            Platform.ANDROID,
                            DeviceType.SMARTPHONE,
                            new SemanticVersion(1, 3, 2)
                    )
            );
        });

        Function<JsonNode, Boolean> f = node -> {
            List<JsonNode> items = Lists.newArrayList(node.get("items").elements());
            if (items.size() != 2) {
                return false;
            }

            JsonNode node1 = items.get(0);
            boolean node1IsCorrect =
                    node1.get("feedId").asInt() == 1069
                            && node1.get("count").asInt() == 2
                            && "35".equals(node1.get("offerId").asText());

            if (!node1IsCorrect) {
                return false;
            }

            JsonNode node2 = items.get(1);
            boolean node2IsCorrect =
                    node2.get("feedId").asInt() == 1069
                            && node2.get("count").asInt() == 4
                            && "31".equals(node2.get("offerId").asText());

            if (!node2IsCorrect) {
                return false;
            }

            return true;
        };

        loyaltyTestClient.getCartCoins(UID, f,"loyalty__coins_cart__body_test.json");
        OfferId[] offerIds = {
                new OfferId("abc", "1"),
                new OfferId("def", "2"),
        };
        reportTestClient.getOffersV2(offerIds,
                "loyalty__coins_cart__body_report_offerinfo.json");



        GetCartCoinsRequest request = new GetCartCoinsRequest();


        GetCartCoinsRequest.CoinsItem item1 = new GetCartCoinsRequest.CoinsItem();
        item1.setOfferId("yDpJekrrgZFVlKWRpZDq8riCg_0DJT22i3nroMJxT4Y"); //new OfferId("abc", "1")
        item1.setCount(2);

        GetCartCoinsRequest.CoinsItem item2 = new GetCartCoinsRequest.CoinsItem();
        item2.setOfferId("yDpJekrrgZEwY5Vqj8e4y726KZpeCo9iunJj9Pxf4_c"); //new OfferId("def", "2")
        item2.setCount(4);

        request.setItems(Arrays.asList(item1, item2));
        CoinsForCart result = cartControllerV2.coinsForCart(request, GenericParams.DEFAULT, USER).waitResult();
        assertEquals(2, result.getApplicableCoins().size());
    }
}

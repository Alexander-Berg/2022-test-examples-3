package ru.yandex.market.api.controller.v2;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.hamcrest.Matchers;
import org.junit.Assert;
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
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.v2.ModelListResult;
import ru.yandex.market.api.domain.v2.ResultContextV2;
import ru.yandex.market.api.domain.v2.SkusResult;
import ru.yandex.market.api.domain.v2.comparisons.ComparisonListsResult;
import ru.yandex.market.api.domain.v2.loyalty.CoinsForOrder;
import ru.yandex.market.api.domain.v2.loyalty.CoinsForPerson;
import ru.yandex.market.api.domain.v2.loyalty.UserCoinResponse;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.blackbox.data.OauthUser;
import ru.yandex.market.api.internal.common.DeviceType;
import ru.yandex.market.api.internal.common.Platform;
import ru.yandex.market.api.matchers.FutureCoinResponseMatcher;
import ru.yandex.market.api.matchers.SkuMatcher;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.User;
import ru.yandex.market.api.server.sec.Uuid;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.ClientHelper;
import ru.yandex.market.api.user.order.MarketUid;
import ru.yandex.market.api.util.concurrent.ApiDeferredResult;
import ru.yandex.market.api.util.httpclient.clients.CarterTestClient;
import ru.yandex.market.api.util.httpclient.clients.HistoryTestClient;
import ru.yandex.market.api.util.httpclient.clients.LoyaltyTestClient;
import ru.yandex.market.api.util.httpclient.clients.PersBasketTestClient;
import ru.yandex.market.api.util.httpclient.clients.PersComparisonTestClient;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Created by tesseract on 04.05.17.
 */
@ActiveProfiles(UserControllerV2Test.PROFILE)
public class UserControllerV2Test extends BaseTest {
    static final String PROFILE = "UserControllerV2Test";

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
    UserControllerV2 controller;
    @Inject
    HistoryTestClient historyTestClient;
    @Inject
    ReportTestClient reportTestClient;
    @Inject
    PersComparisonTestClient persComparisonTestClient;
    @Inject
    LoyaltyTestClient loyaltyTestClient;
    @Inject
    CarterTestClient carterTestClient;
    @Inject
    PersBasketTestClient persBasketTestClient;
    @Inject
    ClientHelper clientHelper;
    MockClientHelper mockClientHelper;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        mockClientHelper = new MockClientHelper(clientHelper);
    }

    /**
     * Проверяем получение истории пользователя. Запрашиваем страницу на которой есть элементы.
     */
    @Test
    public void checkExistedHistoryPage() {
        long uid = 4001796369l;
        PageInfo pageInfo = new PageInfo(1, 10);
        // Настройка системы
        historyTestClient.getHistory(uid, pageInfo, "history_4001796369_page0.json");
        reportTestClient.getModelInfoById(13747917, "modelinfo_13747917.json");
        // вызов системы
        User user = new User(new OauthUser(uid), null, null, null);

        ModelListResult result = controller.getHistory(
            user,
            Collections.emptyList(),
            pageInfo,
            genericParams
        ).waitResult();
        // проверка утверждений
        Assert.assertFalse("Должны получить непустой список т.к. на первой странице есть элементы",
            result.getModels().isEmpty());
        PageInfo page = ((ResultContextV2) result.getContext()).getPage();
        Assert.assertEquals(1, page.getPage());
        Assert.assertEquals(10, page.getCount());
    }

    /**
     * Проверяем получение истории пользователя. Запрашиваем несуществующую страницу (в этом случае персы отдают
     * информацию с последней страницы).
     */
    @Test
    public void checkNotExistedHistoryPage() {
        long uid = 4001796369l;
        PageInfo pageInfo = new PageInfo(2, 10);
        // Настройка системы
        historyTestClient.getHistory(uid, pageInfo, "history_4001796369_page1.json");
        // вызов системы
        User user = new User(new OauthUser(uid), null, null, null);

        ModelListResult result = controller.getHistory(
            user,
            Collections.emptyList(),
            pageInfo,
            genericParams
        ).waitResult();
        // проверка утверждений
        Assert.assertTrue("Должны получить пустой список т.к. на второй странице нет элементы",
            result.getModels().isEmpty());
        PageInfo page = ((ResultContextV2) result.getContext()).getPage();
        Assert.assertEquals(2, page.getPage());
        Assert.assertEquals(10, page.getCount());
    }

    @Test
    public void testNotFilteredComparisonLists() {
        long uid = 4001796369L;

        persComparisonTestClient.getComparsionLists(uid, "comparison_lists.json");

        reportTestClient.getModelInfoById(Arrays.asList(1759344314L, 1759344315L, 13582382L), "comparison_models_result.json");

        User user = new User(new OauthUser(uid), null, null, null);

        ComparisonListsResult result = ((ApiDeferredResult<ComparisonListsResult>) controller.getComparisonLists(
            user,
            null,
            null,
            genericParams))
            .waitResult();

        Assert.assertEquals(2, result.getLists().size());
        Assert.assertEquals(1, result.getLists().get(0).getCount());
        Assert.assertEquals(2, result.getLists().get(1).getCount());
    }

    @Test
    public void testFilterComparison() {
        long uid = 4001796369L;

        persComparisonTestClient.getComparsionLists(uid, "comparison_lists.json");

        reportTestClient.getModelInfoById(Arrays.asList(1759344314L, 1759344315L), "comparison_models_result.json");

        User user = new User(new OauthUser(uid), null, null, null);

        ComparisonListsResult result = ((ApiDeferredResult<ComparisonListsResult>) controller.getComparisonLists(
            user,
            LocalDateTime.parse("2018-06-06T10:00:00"),
            LocalDateTime.parse("2018-06-08T23:00:00"),
            genericParams))
            .waitResult();

        Assert.assertEquals(1, result.getLists().size());
        Assert.assertEquals(2, result.getLists().get(0).getCount());
    }

    @Test
    public void testBlueHistoryPage() {
        long uid = 4011255600L;
        PageInfo pageInfo = new PageInfo(1, 10);

        historyTestClient.getHistory(uid, pageInfo, "blue_history.json");

        List<String> skus = Arrays.asList("100126189165", "100200838239", "100177127295", "100126173307", "100126173349", "100126174043", "100131946186");

        reportTestClient.skus(skus, "blue_history_skus.json");

        User user = new User(new OauthUser(uid), null, null, null);

        SkusResult result = controller.getHistoryBlue(user, Collections.emptyList(), pageInfo, genericParams).waitResult();


        assertThat(result.getSkus(), Matchers.contains(
            SkuMatcher.sku(SkuMatcher.id("100126189165")),
            SkuMatcher.sku(SkuMatcher.id("100177127295")),
            SkuMatcher.sku(SkuMatcher.id("100126173307")),
            SkuMatcher.sku(SkuMatcher.id("100126173349")),
            SkuMatcher.sku(SkuMatcher.id("100126174043")),
            SkuMatcher.sku(SkuMatcher.id("100131946186"))
        ));
    }

    @Test
    public void testBlueHistoryPageForPSKU() {
        long uid = 4011255600L;
        PageInfo pageInfo = new PageInfo(1, 10);

        historyTestClient.getHistory(uid, pageInfo, "blue_history_pskus.json");

        List<String> skus = Arrays.asList("100402204448", "100390554239");

        reportTestClient.skus(skus, "sku/psku-with-filters.json");

        User user = new User(new OauthUser(uid), null, null, null);

        SkusResult result = controller.getHistoryBlue(user, Collections.emptyList(), pageInfo, genericParams).waitResult();

        //проверяем что НЕ взяли из истории старое
        assertThat(result.getSkus(), Matchers.empty());
    }

    @Test
    public void testBlueHistoryOrderingWithDeleted() {
        long uid = 4011255600L;
        PageInfo pageInfo = new PageInfo(1, 10);

        historyTestClient.getHistory(uid, pageInfo, "blue_history_pskus.json");

        List<String> skus = Arrays.asList("100402204448", "100390554239");

        reportTestClient.skus(skus, "sku/deleted_skus.json");

        User user = new User(new OauthUser(uid), null, null, null);

        SkusResult result = controller.getHistoryBlue(user, Collections.emptyList(), pageInfo, genericParams).waitResult();

        //проверяем что НЕ взяли из истории старое
        assertThat(result.getSkus(), Matchers.empty());
    }

    @Test
    public void testCoinsForOrder() {
        long uid = 4004661923L;

        long orderId = 2448767;

        User user = new User(new OauthUser(uid), null, null, null);

        loyaltyTestClient.getCoinsForOrder(uid, orderId, "coins_for_order.json");

        CoinsForOrder coinsForOrder = controller.coinsForOrder(user, orderId).waitResult();

        Assert.assertEquals(2, coinsForOrder.getNewCoins().size());
    }

    @Test
    public void testFilterInactiveCoinsForOrderForOlbBlueApp() {
        long uid = 4004661923L;

        long orderId = 2448767;

        User user = new User(new OauthUser(uid), null, null, null);

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

        loyaltyTestClient.getCoinsForOrder(uid, orderId, "coins_for_order_with_inactive_coins.json");

        CoinsForOrder coinsForOrder = controller.coinsForOrder(user, orderId).waitResult();

        Assert.assertEquals(1, coinsForOrder.getNewCoins().size());
    }

    @Test
    public void testFilterInactiveUserCoinsForOldBlueApp() {
        long uid = 100L;
        int limit = 10;
        User user = new User(
            new OauthUser(uid),
            null,
            null,
            null
        );

        loyaltyTestClient.getCoinsForUser(uid, limit,"coins_for_person_with_inactive_coins.json");

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

        CoinsForPerson coins = controller
            .coinsForPerson(user, limit)
            .waitResult();

        Assert.assertEquals(1, coins.getCoins().size());
    }

    @Test
    public void testFilterEmptyUserCoinsForOldBlueApp() {
        long uid = 100L;
        int limit = 10;
        User user = new User(
            new OauthUser(uid),
            null,
            null,
            null
        );

        loyaltyTestClient.getCoinsForUser(uid, limit,"coins_for_person_with_empty_coins.json");

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

        CoinsForPerson coins = controller
            .coinsForPerson(user, limit)
            .waitResult();

        Assert.assertNull(coins.getCoins());
    }

    @Test
    public void testMergeUser() {
        final long uid = 4011255600L;
        final String uuid = "12345678901234567890123456789012";
        final int limit = 200;
        final boolean enableMultiOffers = true;

        User user = new User(OauthUser.newWithDefaultScope(uid), null, new Uuid(uuid), null);

        carterTestClient.merge(uuid, uid, enableMultiOffers);
        persBasketTestClient.merge(uuid, uid);
        historyTestClient.merge(uuid, uid, limit);
        persComparisonTestClient.merge(uuid, uid);

        controller.mergeUser(limit, enableMultiOffers, user);
    }

    @Test
    public void testCoinsForOAuth() {
        long uid = 100L;
        int limit = 10;
        User user = new User(
            new OauthUser(uid),
            null,
            null,
            null
        );

        loyaltyTestClient.getCoinsForUser(uid, limit,"coins_for_person_oauth.json");

        CoinsForPerson coins = controller
            .coinsForPerson(user, limit)
            .waitResult();

        Assert.assertThat(coins.getCoins(), Matchers.empty());
        Assert.assertThat(
            coins.getFutureCoins(),
            Matchers.containsInAnyOrder(
                FutureCoinResponseMatcher.coinResponse(
                    FutureCoinResponseMatcher.title(is("Скидка на 100 рублей")),
                    FutureCoinResponseMatcher.promoId(is(10319L))
                ),
                FutureCoinResponseMatcher.coinResponse(
                    FutureCoinResponseMatcher.title(is("Скидка 400 р")),
                    FutureCoinResponseMatcher.promoId(is(10337L))
                )
            )

        );
    }

    @Test
    public void testCoinsForMuid() {
        long muid = 100L;
        int limit = 10;
        User user = new User(
            null,
            new MarketUid(muid),
            null,
            null
        );

        loyaltyTestClient.getCoinsForUnauthorisedUser(limit,"coins_for_person_muid.json");

        CoinsForPerson coins = controller
            .coinsForPerson(user, limit)
            .waitResult();

        Assert.assertThat(coins.getCoins(), Matchers.empty());
        Assert.assertThat(
            coins.getFutureCoins(),
            Matchers.containsInAnyOrder(
                FutureCoinResponseMatcher.coinResponse(
                    FutureCoinResponseMatcher.title(is("Скидка на 100 рублей")),
                    FutureCoinResponseMatcher.promoId(is(10319L))
                ),
                FutureCoinResponseMatcher.coinResponse(
                    FutureCoinResponseMatcher.title(is("Скидка 400 р")),
                    FutureCoinResponseMatcher.promoId(is(10337L))
                )
            )

        );
    }

    @Test
    public void testBindByMuid() {
        long uid = 4004661923L;

        String ip = "10.10.10.10";

        ContextHolder.update(ctx -> ctx.setUserIp(ip));

        MarketUid muid = new MarketUid(100, "Signature");

        Uuid uuid = new Uuid("12345678901234567890123456789012");

        User user = new User(new OauthUser(uid), muid, uuid, null);

        loyaltyTestClient.bindByMuid(uid, muid, uuid.getValue(), ip, "bind_by_muid.json");

        List<UserCoinResponse> coins = controller.bindByMuid(user).waitResult();

        Assert.assertEquals(2, coins.size());
        Assert.assertEquals(35646L, coins.get(0).getId().longValue());
        Assert.assertEquals(35647L, coins.get(1).getId().longValue());
    }
}

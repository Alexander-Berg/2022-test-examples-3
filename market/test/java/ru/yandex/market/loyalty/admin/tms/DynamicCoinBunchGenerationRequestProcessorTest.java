package ru.yandex.market.loyalty.admin.tms;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.loyalty.admin.service.bunch.BunchRequestService;
import ru.yandex.market.loyalty.admin.service.bunch.generator.DynamicCoinDto;
import ru.yandex.market.loyalty.admin.test.MarketLoyaltyAdminMockedDbTest;
import ru.yandex.market.loyalty.core.config.YtHahn;
import ru.yandex.market.loyalty.core.dao.coin.CoinDao;
import ru.yandex.market.loyalty.core.dao.coin.GeneratorType;
import ru.yandex.market.loyalty.core.model.coin.BunchGenerationRequest;
import ru.yandex.market.loyalty.core.model.coin.Coin;
import ru.yandex.market.loyalty.core.model.coin.CoinDescription;
import ru.yandex.market.loyalty.core.model.coin.CoinProps;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinType;
import ru.yandex.market.loyalty.core.model.coin.ExpirationPolicy;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.utils.PromoUtils;

import static java.time.temporal.ChronoUnit.MINUTES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.admin.tms.PreparedBunchGenerationRequestProcessorTest.createTestScheduledRequest;
import static ru.yandex.market.loyalty.api.model.CoinGeneratorType.AUTH_DYNAMIC;

public class DynamicCoinBunchGenerationRequestProcessorTest extends MarketLoyaltyAdminMockedDbTest {

    public static final long TEST_UID = 1L;
    public static final List<Integer> TEST_CATEGORIES = List.of(100);
    public static final String TEST_HID_NAME_PLACEHOLDER = "some hid";
    public static final BigDecimal TEST_NOMINAL = BigDecimal.valueOf(10);
    public static final BigDecimal TEST_MIN_ORDER_TOTAL = BigDecimal.valueOf(10);
    public static final int TEST_MAX_ORDER_TOTAL = 100;
    public static final int TEST_IMAGE_ID = 1;
    public static final String TEST_IMAGE_NAME = "some image name";
    public static final String TEST_COLOR = "some color";
    public static final String TEST_DESCRIPTION = "some description";
    public static final CoreCoinType TEST_COIN_TYPE = CoreCoinType.FIXED;
    public static final int COIN_COUNT = 10;
    public static final String TEST_KEY = "someTestKey";
    public static final String TEST_TITLE = "Монетка на чтото полезное";

    @Autowired
    @YtHahn
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private BunchRequestService bunchRequestService;
    @Autowired
    BunchRequestProcessor bunchRequestProcessor;
    @Autowired
    CoinService coinService;
    @Autowired
    CoinDao coinDao;

    @Test
    public void shouldGenerateDynamicCoinsWithoutExpirationDaysUsingAuthDynamicGenerator() {
        YtTestUtils.mockAnyYtRequestWithArgsResult(createRecommendationCoins(), jdbcTemplate);
        Promo dynamicPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultDynamic()
        );
        bunchRequestService.scheduleRequest(
                createTestScheduledRequest(dynamicPromo.getId(), TEST_KEY, GeneratorType.COIN, AUTH_DYNAMIC, 10)
        );


        bunchRequestProcessor.coinBunchRequestProcess(
                500,
                Duration.of(1, MINUTES)
        );


        checkGeneratedCoins(ExpirationPolicy.toEndOfPromo(), getGeneratedCoins());
    }

    @Test
    public void shouldGenerateDynamicCoinsWithExpirationDaysUsingAuthDynamicGenerator() {
        int expectedDaysToExpire = 10;
        YtTestUtils.mockAnyYtRequestWithArgsResult(createRecommendationCoins(expectedDaysToExpire), jdbcTemplate);
        Promo dynamicPromo = promoManager.createSmartShoppingPromo(
                PromoUtils.SmartShopping.defaultDynamic()
        );
        bunchRequestService.scheduleRequest(
                createTestScheduledRequest(dynamicPromo.getId(), TEST_KEY, GeneratorType.COIN, AUTH_DYNAMIC, 10)
        );


        bunchRequestProcessor.coinBunchRequestProcess(
                500,
                Duration.of(1, MINUTES)
        );


        checkGeneratedCoins(ExpirationPolicy.expireByDays(expectedDaysToExpire), getGeneratedCoins());
    }

    private void checkGeneratedCoins(ExpirationPolicy expectedExpirationPolicy, List<Coin> coins) {
        assertThat(
                coins,
                hasSize(COIN_COUNT)
        );
        assertThat(
                coins,
                everyItem(
                        allOf(
                                hasProperty("nominal", comparesEqualTo(TEST_NOMINAL)),
                                hasProperty("uid", comparesEqualTo(TEST_UID))
                        )
                )
        );
        assertTrue(
                coins.stream()
                        .allMatch(coin ->
                                coin.getExpirationPolicy().getType() == expectedExpirationPolicy.getType())
        );

        checkCoinProps(
                getCoinProps(coins.stream().map(Coin::getCoinPropsId).collect(Collectors.toList()))
        );
        checkDescription(
                getCoinDescriptions(coins.stream().map(Coin::getCoinDescriptionId).collect(Collectors.toList()))
        );
    }

    private List<Coin> getGeneratedCoins() {
        return coinService.search.getCoinsWhereSourceKeyStartsWithValue(
                BunchGenerationRequest.getBunchRequestKeyPrefix() + TEST_KEY,
                TEST_UID
        );
    }

    private void checkDescription(List<CoinDescription> coinDescriptions) {
        assertThat(
                coinDescriptions,
                everyItem(
                        allOf(
                                hasProperty("title", equalTo(TEST_TITLE)),
                                hasProperty("description", equalTo(TEST_DESCRIPTION)),
                                hasProperty("backgroundColor", equalTo(TEST_COLOR)),
                                hasProperty("restrictionDescription",
                                        equalTo(TEST_HID_NAME_PLACEHOLDER)
                                )
                        )
                )
        );
        coinDescriptions
                .stream()
                .map(CoinDescription::getAvatarImageId)
                .forEach(avatarImageId -> {
                    assertEquals(TEST_IMAGE_ID, avatarImageId.getGroupId());
                    assertEquals(TEST_IMAGE_NAME, avatarImageId.getImageName());
                });
    }

    private void checkCoinProps(List<CoinProps> coinProps) {
        assertThat(
                coinProps,
                everyItem(
                        allOf(
                                hasProperty("nominal", comparesEqualTo(TEST_NOMINAL)),
                                hasProperty("type", equalTo(TEST_COIN_TYPE))
                        )
                )
        );
    }

    private List<CoinDescription> getCoinDescriptions(List<Long> descriptionIds) {
        return coinDao.getCoinDescriptionsByIds(Set.copyOf(descriptionIds))
                .values()
                .stream()
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private List<CoinProps> getCoinProps(List<Long> coinPropsIds) {
        return coinDao.getCoinPropsByIds(Set.copyOf(coinPropsIds))
                .values()
                .stream()
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    private static List<DynamicCoinDto> createRecommendationCoins() {
        return createRecommendationCoins(null);
    }

    private static List<DynamicCoinDto> createRecommendationCoins(
            @Nullable Integer daysToExpire
    ) {
        return IntStream.range(0, COIN_COUNT)
                .mapToObj(i -> {
                    return new DynamicCoinDto(
                            TEST_UID,
                            TEST_CATEGORIES,
                            TEST_COIN_TYPE,
                            TEST_NOMINAL,
                            daysToExpire,
                            TEST_TITLE,
                            TEST_HID_NAME_PLACEHOLDER,
                            TEST_MIN_ORDER_TOTAL,
                            BigDecimal.valueOf(TEST_MAX_ORDER_TOTAL),
                            TEST_IMAGE_ID,
                            TEST_IMAGE_NAME,
                            TEST_COLOR,
                            TEST_DESCRIPTION
                    );
                }).collect(Collectors.toList());
    }

}

package ru.yandex.market.loyalty.back.controller;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.antifraud.orders.web.dto.LoyaltyBuyerRestrictionsDto;
import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.PromoStatus;
import ru.yandex.market.loyalty.api.model.coin.BaseUserCoinResponse;
import ru.yandex.market.loyalty.api.model.coin.UserCoinResponse;
import ru.yandex.market.loyalty.api.model.coin.creation.CoinCreationError;
import ru.yandex.market.loyalty.api.model.coin.creation.CoinsCreationRequest;
import ru.yandex.market.loyalty.api.model.coin.creation.CoinsCreationRequestV2;
import ru.yandex.market.loyalty.api.model.coin.creation.CoinsCreationResponse;
import ru.yandex.market.loyalty.api.model.coin.creation.CoinsCreationResponseV2;
import ru.yandex.market.loyalty.api.model.coin.creation.DeviceInfoRequest;
import ru.yandex.market.loyalty.api.model.coin.creation.SingleCoinCreationRequest;
import ru.yandex.market.loyalty.api.model.identity.Uid;
import ru.yandex.market.loyalty.api.model.identity.Uuid;
import ru.yandex.market.loyalty.api.model.identity.YandexUid;
import ru.yandex.market.loyalty.back.test.MarketLoyaltyBackMockedDbTestBase;
import ru.yandex.market.loyalty.core.dao.EmissionLogDao;
import ru.yandex.market.loyalty.core.dao.EmissionLogService;
import ru.yandex.market.loyalty.core.dao.UserBlackListDao;
import ru.yandex.market.loyalty.core.model.BlacklistRecord;
import ru.yandex.market.loyalty.core.model.EmissionLogRecord;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.promo.PromoParameterName;
import ru.yandex.market.loyalty.core.model.promo.RuleParameterName;
import ru.yandex.market.loyalty.core.rule.RuleType;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.PromoManager;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.UserBlacklistService;
import ru.yandex.market.loyalty.test.TestFor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static ru.yandex.market.loyalty.api.model.coin.CoinCreationReason.EMAIL_COMPANY;
import static ru.yandex.market.loyalty.api.model.coin.CoinCreationReason.OTHER;
import static ru.yandex.market.loyalty.api.model.coin.CoinStatus.ACTIVE;
import static ru.yandex.market.loyalty.api.model.coin.creation.CoinCreationError.SAME_COIN_ALREADY_CREATED;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.YANDEX_PLUS;
import static ru.yandex.market.loyalty.back.util.AntifraudUtils.setupDeclinedAntifraudResponseForUid;
import static ru.yandex.market.loyalty.core.model.promo.PromoParameterName.COIN_CREATION_REASON;
import static ru.yandex.market.loyalty.core.test.BlackboxUtils.mockBlackbox;
import static ru.yandex.market.loyalty.core.utils.MatcherUtils.coinStatus;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.DEFAULT_NAME;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFixed;
import static ru.yandex.market.loyalty.core.utils.PromoUtils.SmartShopping.defaultFreeDelivery;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.ANOTHER_EMAIL;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.ANOTHER_MUID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.ANOTHER_PHONE;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.ANOTHER_UID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.ANOTHER_UUID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.ANOTHER_YANDEX_UID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_EMAIL;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_EMAIL_ID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_MUID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_PHONE_ID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_USER_FULL_NAME_ID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_UUID;
import static ru.yandex.market.loyalty.core.utils.UserDataFactory.DEFAULT_YANDEX_UID;
import static ru.yandex.market.loyalty.test.Junit5.assertThrows;

@TestFor(CoinsController.class)
public class CoinsControllerCreationTest extends MarketLoyaltyBackMockedDbTestBase {
    @Autowired
    private PromoManager promoManager;
    @Autowired
    private PromoService promoService;
    @Autowired
    private UserBlacklistService userBlacklistService;
    @Autowired
    private UserBlackListDao userBlackListDao;
    @Autowired
    private EmissionLogDao emissionLogDao;
    @Autowired
    private EmissionLogService emissionLogService;
    @Qualifier("antifraudRestTemplate")
    @Autowired
    RestTemplate antifraudRestTemplate;
    @Autowired
    private ConfigurationService configurationService;

    @Test
    public void shouldCreateCoinsByUids() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFreeDelivery());

        CoinsCreationResponse createdCoins = marketLoyaltyClient.createCoins(new CoinsCreationRequest(
                promo.getPromoId().getId(),
                EMAIL_COMPANY,
                "someIdempotentKey",
                Arrays.asList(new Uid(DEFAULT_UID), new Uid(ANOTHER_UID))
        ));

        assertThat(createdCoins.getCoins(), containsInAnyOrder(
                coinStatus(ACTIVE, false),
                coinStatus(ACTIVE, false)
        ));
        assertThat(createdCoins.getSuccess(), containsInAnyOrder(
                hasProperty("identity", equalTo(new Uid(DEFAULT_UID))),
                hasProperty("identity", equalTo(new Uid(ANOTHER_UID)))
        ));
        assertThat(createdCoins.getWithoutCoins(), is(empty()));
    }

    @Test
    public void shouldCreateCoinsByYndexId() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFreeDelivery());

        CoinsCreationResponse createdCoins = marketLoyaltyClient.createCoins(new CoinsCreationRequest(
                promo.getPromoId().getId(),
                EMAIL_COMPANY,
                "someIdempotentKey",
                Arrays.asList(new YandexUid(DEFAULT_YANDEX_UID), new YandexUid(ANOTHER_YANDEX_UID))
        ));

        assertThat(createdCoins.getCoins(), containsInAnyOrder(
                coinStatus(ACTIVE, true),
                coinStatus(ACTIVE, true)
        ));
        assertThat(createdCoins.getSuccess(), containsInAnyOrder(
                hasProperty("identity", equalTo(new YandexUid(DEFAULT_YANDEX_UID))),
                hasProperty("identity", equalTo(new YandexUid(ANOTHER_YANDEX_UID)))
        ));
        assertThat(createdCoins.getWithoutCoins(), is(empty()));
    }

    @Test
    public void shouldCreateCoinsByUuids() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFreeDelivery());

        CoinsCreationResponse createdCoins = marketLoyaltyClient.createCoins(new CoinsCreationRequest(
                promo.getPromoId().getId(),
                EMAIL_COMPANY,
                "someIdempotentKey",
                Arrays.asList(new Uuid(DEFAULT_UUID), new Uuid(ANOTHER_UUID))
        ));

        assertThat(createdCoins.getCoins(), containsInAnyOrder(
                coinStatus(ACTIVE, true),
                coinStatus(ACTIVE, true)
        ));
        assertThat(createdCoins.getSuccess(), containsInAnyOrder(
                hasProperty("identity", equalTo(new Uuid(DEFAULT_UUID))),
                hasProperty("identity", equalTo(new Uuid(ANOTHER_UUID)))
        ));
        assertThat(createdCoins.getWithoutCoins(), is(empty()));
    }

    @Test
    public void shouldCreateCoinsByUidAndUuid() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFreeDelivery());

        CoinsCreationResponse createdCoins = marketLoyaltyClient.createCoins(new CoinsCreationRequest(
                promo.getPromoId().getId(),
                EMAIL_COMPANY,
                "someIdempotentKey",
                Arrays.asList(new Uid(DEFAULT_UID), new Uuid(DEFAULT_UUID))
        ));

        assertThat(createdCoins.getCoins(), containsInAnyOrder(
                coinStatus(ACTIVE, true),
                coinStatus(ACTIVE, false)
        ));
        assertThat(createdCoins.getSuccess(), containsInAnyOrder(
                hasProperty("identity", equalTo(new Uid(DEFAULT_UID))),
                hasProperty("identity", equalTo(new Uuid(DEFAULT_UUID)))
        ));
        assertThat(createdCoins.getWithoutCoins(), is(empty()));
    }

    @Test
    public void shouldCreateCoinsByDeviceIdOnceForWelcome() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFreeDelivery()
                .setName(DEFAULT_NAME)
                .setBindOnlyOnce(true)
        );


        final ImmutableMap<String, String> DEVICE_ID = ImmutableMap.of(
                "deviceId", "01234"
        );
        CoinsCreationResponseV2 createdCoins = createCoin(promo, DEVICE_ID, DEFAULT_UID, DEFAULT_UUID,
                DEFAULT_YANDEX_UID, DEFAULT_EMAIL, DEFAULT_MUID
        );

        assertThat(createdCoins.getCoins(), contains(
                coinStatus(ACTIVE, false)
        ));
        assertThat(createdCoins.getSuccess(), contains(
                hasProperty("key", equalTo("someIdempotentKey"))
        ));
        assertThat(createdCoins.getFailure(), is(empty()));

        assertFalse(emissionLogDao.anyEmissionLogRecordExists());

        createdCoins = marketLoyaltyClient.createCoins(new CoinsCreationRequestV2(
                promo.getPromoId().getId(),
                EMAIL_COMPANY,
                Collections.singletonList(new SingleCoinCreationRequest(
                        "someIdempotentKey2", ANOTHER_UID, ANOTHER_UUID, ANOTHER_YANDEX_UID, ANOTHER_EMAIL,
                        ANOTHER_MUID, true, null, ANOTHER_PHONE, DEVICE_ID
                ))
        ));

        assertThat(createdCoins.getCoins(), empty());
        assertThat(createdCoins.getSuccess(), empty());
        assertThat(createdCoins.getFailure(), contains(
                hasProperty("key", equalTo("someIdempotentKey2"))
        ));

        assertTrue(emissionLogDao.anyEmissionLogRecordExists());

        String emissionLogRecords = emissionLogService.findEmissionLogRecords(
                        promo.getPromoId().getId())
                .stream()
                .map(EmissionLogRecord::getDescription)
                .collect(Collectors.joining(" "));
        assertFalse(emissionLogRecords.isEmpty());
        assertEquals("Совпадение по паре: deviceId=01234", emissionLogRecords);
    }

    @Test
    public void shouldReturnPossibilityOfCreationWelcomeBonesByDeviceId() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFreeDelivery()
                .setName(DEFAULT_NAME)
                .setBindOnlyOnce(true)
        );

        final ImmutableMap<String, String> DEVICE_ID = ImmutableMap.of(
                "deviceId", "01234"
        );

        DeviceInfoRequest deviceInfoRequest = new DeviceInfoRequest(
                DEFAULT_UUID, DEVICE_ID, DEFAULT_MUID, DEFAULT_EMAIL, null,
                DEFAULT_EMAIL_ID, DEFAULT_USER_FULL_NAME_ID, DEFAULT_PHONE_ID
        );
        assertTrue(marketLoyaltyClient.willCreateCoin(promo.getPromoId().getId(), deviceInfoRequest));

        createCoin(promo, DEVICE_ID, DEFAULT_UID, DEFAULT_UUID, DEFAULT_YANDEX_UID, DEFAULT_EMAIL, DEFAULT_MUID);

        assertFalse(marketLoyaltyClient.willCreateCoin(promo.getPromoId().getId(), deviceInfoRequest));
    }

    @Test
    public void shouldReturnImpossibilityOfCreationWelcomeBonusesForInactivePromo() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFreeDelivery()
                .setName(DEFAULT_NAME)
                .setBindOnlyOnce(true)
                .setStatus(PromoStatus.INACTIVE)
        );

        final ImmutableMap<String, String> DEVICE_ID = ImmutableMap.of(
                "deviceId", "01234"
        );

        DeviceInfoRequest deviceInfoRequest = new DeviceInfoRequest(
                DEFAULT_UUID, DEVICE_ID, DEFAULT_MUID, DEFAULT_EMAIL, null,
                DEFAULT_EMAIL_ID, DEFAULT_USER_FULL_NAME_ID, DEFAULT_PHONE_ID
        );
        assertFalse(marketLoyaltyClient.willCreateCoin(promo.getPromoId().getId(), deviceInfoRequest));
    }

    @Test
    public void shouldCreateCoinsByDeviceIdOnce() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFreeDelivery()
                .setName(DEFAULT_NAME)
                .setBindOnlyOnce(true)
        );

        final ImmutableMap<String, String> DEVICE_ID = ImmutableMap.of(
                "deviceId", "01234"
        );
        CoinsCreationResponseV2 createdCoins = createCoin(promo, DEVICE_ID, DEFAULT_UID, DEFAULT_UUID,
                DEFAULT_YANDEX_UID, DEFAULT_EMAIL, DEFAULT_MUID
        );

        assertThat(createdCoins.getCoins(), contains(
                coinStatus(ACTIVE, false)
        ));
        assertThat(createdCoins.getSuccess(), contains(
                hasProperty("key", equalTo("someIdempotentKey"))
        ));
        assertThat(createdCoins.getFailure(), is(empty()));

        assertFalse(emissionLogDao.anyEmissionLogRecordExists());

        createdCoins = marketLoyaltyClient.createCoins(new CoinsCreationRequestV2(
                promo.getPromoId().getId(),
                EMAIL_COMPANY,
                Collections.singletonList(new SingleCoinCreationRequest(
                        "someIdempotentKey2", ANOTHER_UID, ANOTHER_UUID, ANOTHER_YANDEX_UID, ANOTHER_EMAIL,
                        ANOTHER_MUID, true, null, ANOTHER_PHONE, DEVICE_ID
                ))
        ));

        assertThat(createdCoins.getCoins(), empty());
        assertThat(createdCoins.getSuccess(), empty());
        assertThat(createdCoins.getFailure(), contains(
                hasProperty("key", equalTo("someIdempotentKey2"))
        ));

        assertTrue(emissionLogDao.anyEmissionLogRecordExists());

        String emissionLogRecords = emissionLogService.findEmissionLogRecords(
                        promo.getPromoId().getId()
                )
                .stream()
                .map(EmissionLogRecord::getDescription)
                .collect(Collectors.joining(" "));
        assertFalse(emissionLogRecords.isEmpty());
        assertEquals("Совпадение по паре: deviceId=01234", emissionLogRecords);
    }

    @Test
    public void shouldCreateCoinsIfDeviceIdAbsent() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFreeDelivery()
                .setName(DEFAULT_NAME)
                .setBindOnlyOnce(true)
        );

        final ImmutableMap<String, String> DEVICE_ID = ImmutableMap.of();
        CoinsCreationResponseV2 createdCoins = createCoin(promo, DEVICE_ID, DEFAULT_UID, DEFAULT_UUID,
                DEFAULT_YANDEX_UID, DEFAULT_EMAIL, DEFAULT_MUID
        );

        assertThat(createdCoins.getCoins(), contains(
                coinStatus(ACTIVE, false)
        ));
        assertThat(createdCoins.getSuccess(), contains(
                hasProperty("key", equalTo("someIdempotentKey"))
        ));
        assertThat(createdCoins.getFailure(), is(empty()));

        assertFalse(emissionLogDao.anyEmissionLogRecordExists());
    }

    @Test
    public void shouldDistinctCreatedCoinsByUidIfDeviceIdAbsent() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFreeDelivery()
                .setName(DEFAULT_NAME)
                .setBindOnlyOnce(true)
        );

        final ImmutableMap<String, String> DEVICE_ID = ImmutableMap.of();
        CoinsCreationResponseV2 createdCoins = createCoin(promo, DEVICE_ID, DEFAULT_UID, DEFAULT_UUID,
                DEFAULT_YANDEX_UID, DEFAULT_EMAIL, DEFAULT_MUID
        );

        CoinsCreationResponseV2 anotherCoins = createCoin(promo, DEVICE_ID, ANOTHER_UID, ANOTHER_UUID,
                ANOTHER_YANDEX_UID, ANOTHER_EMAIL, ANOTHER_MUID
        );

        assertThat(createdCoins.getCoins(), contains(
                coinStatus(ACTIVE, false)
        ));
        assertThat(anotherCoins.getCoins(), contains(
                coinStatus(ACTIVE, false)
        ));
    }

    @Test
    public void shouldReturnPossibilityOfCreationByDeviceId() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFreeDelivery()
                .setName(DEFAULT_NAME)
                .setBindOnlyOnce(true)
        );

        final ImmutableMap<String, String> DEVICE_ID = ImmutableMap.of(
                "deviceId", "01234"
        );

        DeviceInfoRequest deviceInfoRequest = new DeviceInfoRequest(
                DEFAULT_UUID, DEVICE_ID, DEFAULT_MUID, DEFAULT_EMAIL,
                null, DEFAULT_EMAIL_ID, DEFAULT_USER_FULL_NAME_ID, DEFAULT_PHONE_ID
        );
        assertTrue(marketLoyaltyClient.willCreateCoin(promo.getPromoId().getId(), deviceInfoRequest));

        createCoin(promo, DEVICE_ID, DEFAULT_UID, DEFAULT_UUID, DEFAULT_YANDEX_UID, DEFAULT_EMAIL, DEFAULT_MUID);

        assertFalse(marketLoyaltyClient.willCreateCoin(promo.getPromoId().getId(), deviceInfoRequest));
    }

    @Test
    public void shouldCreatePartOfCoinsIfBudgetExceeded() {
        int possibleCountOfCoinsToCreate = 100;
        int requestedCoinsCount = 200;

        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFixed().setEmissionBudget(BigDecimal.valueOf(possibleCountOfCoinsToCreate)));

        CoinsCreationResponse createdCoins = marketLoyaltyClient.createCoins(new CoinsCreationRequest(
                promo.getPromoId().getId(),
                EMAIL_COMPANY,
                "someIdempotentKey",
                LongStream.range(0, requestedCoinsCount).mapToObj(Uid::new).collect(Collectors.toList())
        ));

        assertThat(createdCoins.getCoins(), hasSize(possibleCountOfCoinsToCreate));
        assertThat(createdCoins.getSuccess(), hasSize(possibleCountOfCoinsToCreate));
        assertThat(createdCoins.getWithoutCoins(), hasSize(requestedCoinsCount - possibleCountOfCoinsToCreate));
        assertThat(createdCoins.getFailure(), hasSize(requestedCoinsCount - possibleCountOfCoinsToCreate));
    }

    @Test
    public void shouldFailNewCoinIfUserAlreadyHasCoin() {
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFixed().setBindOnlyOnce(true));

        CoinsCreationResponse createdCoins = marketLoyaltyClient.createCoins(new CoinsCreationRequest(
                        promo.getPromoId().getId(),
                        EMAIL_COMPANY,
                        "someIdempotentKey",
                        Collections.singletonList(new Uid(DEFAULT_UID))
                )
        );

        assertThat(createdCoins.getCoins(), hasSize(1));
        assertThat(createdCoins.getSuccess(), hasSize(1));
        assertThat(createdCoins.getWithoutCoins(), hasSize(0));
        assertThat(createdCoins.getFailure(), hasSize(0));


        createdCoins = marketLoyaltyClient.createCoins(new CoinsCreationRequest(
                        promo.getPromoId().getId(),
                        EMAIL_COMPANY,
                        "someIdempotentKey2",
                        Collections.singletonList(new Uid(DEFAULT_UID))
                )
        );

        assertThat(createdCoins.getCoins(), hasSize(0));
        assertThat(createdCoins.getSuccess(), hasSize(0));
        assertThat(createdCoins.getWithoutCoins(), hasSize(1));
        assertThat(createdCoins.getFailure(), contains(
                        allOf(
                                hasProperty("error", equalTo(SAME_COIN_ALREADY_CREATED)),
                                hasProperty("identity", equalTo(new Uid(DEFAULT_UID)))
                        )
                )
        );
    }

    @Test
    public void shouldCreateSingleCoin() {
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFixed().setBindOnlyOnce(true));

        UserCoinResponse createdCoin = marketLoyaltyClient.createCoin(
                promo.getPromoId().getId(),
                EMAIL_COMPANY,
                "someIdempotentKey",
                new Uid(DEFAULT_UID)
        );

        assertNotNull(createdCoin);
    }

    @Test
    public void shouldCreateOneSingleCoinV2() {
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFixed().setBindOnlyOnce(true));

        UserCoinResponse createdCoin = marketLoyaltyClient.createCoin(
                promo.getPromoId().getId(),
                EMAIL_COMPANY,
                SingleCoinCreationRequest.forAuth("123", 123L).build()
        );

        assertNotNull(createdCoin);

        MarketLoyaltyException marketLoyaltyException = assertThrows(MarketLoyaltyException.class,
                () -> marketLoyaltyClient.createCoin(
                        promo.getPromoId().getId(),
                        EMAIL_COMPANY,
                        SingleCoinCreationRequest.forAuth("123", 123L).build()
                ));

        assertThat(marketLoyaltyException.getMarketLoyaltyErrorCode(),
                is(MarketLoyaltyErrorCode.SAME_COIN_ALREADY_CREATED));
    }

    @Test
    public void shouldCreateOneSingleCoinV2AndCheckHiddenDataUntilBindFlag() {
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFixed()
                        .setBindOnlyOnce(true)
                        .addCoinRule(RuleType.HIDDEN_DATA_UNTIL_BIND_RESTRICTION_RULE,
                                RuleParameterName.HIDDEN_DATA_UNTIL_BIND, Set.of(true)));

        UserCoinResponse createdCoin = marketLoyaltyClient.createCoin(
                promo.getPromoId().getId(),
                EMAIL_COMPANY,
                SingleCoinCreationRequest.forAuth("123", 123L).build()
        );

        assertNotNull(createdCoin);
        assertTrue(createdCoin.getHiddenDataUntilBind());
    }

    @Test
    public void shouldCreateOneSingleCoinV2AndCheckOnlyForPlusFlag() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());
        promoService.setPromoParam(promo.getPromoId().getId(), PromoParameterName.ONLY_FOR_PLUS, true);
        mockBlackbox(123L, YANDEX_PLUS, true, blackboxRestTemplate);
        UserCoinResponse createdCoin = marketLoyaltyClient.createCoin(
                promo.getPromoId().getId(),
                EMAIL_COMPANY,
                SingleCoinCreationRequest.forAuth("123", 123L).build()
        );

        assertNotNull(createdCoin);
        assertTrue(createdCoin.getForPlus());
    }

    @Test
    public void shouldCreateOneSingleMockedCoinV2ForShooting() {
        configurationService.set(ConfigurationService.MOCK_CREATE_COIN_FOR_SHOOTING_ENABLED, true);
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());
        promoService.setPromoParam(promo.getPromoId().getId(), PromoParameterName.ONLY_FOR_PLUS, true);
        mockBlackbox(2_190_550_858_753_437_195L, YANDEX_PLUS, true, blackboxRestTemplate);
        UserCoinResponse createdCoin = marketLoyaltyClient.createCoin(
                promo.getPromoId().getId(),
                EMAIL_COMPANY,
                SingleCoinCreationRequest.forAuth("123", 2_190_550_858_753_437_195L).build()
        );

        assertNotNull(createdCoin);
        assertThat(createdCoin.getTitle(), is("mock"));
    }

    @Test
    public void shouldCreateCoinsV2() {
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFixed().setBindOnlyOnce(true));

        CoinsCreationResponseV2 createdCoins = marketLoyaltyClient.createCoins(
                new CoinsCreationRequestV2(promo.getPromoId().getId(), EMAIL_COMPANY, Collections.singletonList(
                        SingleCoinCreationRequest.forAuth("123", DEFAULT_UID).build()
                ))
        );

        assertNotNull(createdCoins);
    }

    @Test
    public void shouldCreateCoinsV2AppliedToFakeUsers() {
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFixed().setBindOnlyOnce(true).setAppliedToFakeUser(true));

        CoinsCreationResponseV2 createdCoins = marketLoyaltyClient.createCoins(
                new CoinsCreationRequestV2(promo.getPromoId().getId(), EMAIL_COMPANY, Collections.singletonList(
                        SingleCoinCreationRequest.forAuth("123", DEFAULT_UID).build()
                ))
        );

        assertNotNull(createdCoins);
        assertNotNull(createdCoins.getFailure());
        assertThat(createdCoins.getFailure(), hasSize(1));
        assertThat(createdCoins.getFailure().get(0).getError(),
                is(CoinCreationError.FAKE_COIN_ONLY_FOR_FAKE_USER));
    }

    @Test
    public void shouldCreateCoinsWithNullUid() {
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFixed().setBindOnlyOnce(true));
        SingleCoinCreationRequest.Builder builder = SingleCoinCreationRequest.forNoAuth("4096632A-EFF7-4D96-BFD0" +
                "-262D46DFB4D6");
        builder.setUid(null)
                .setUuid(null)
                .setYandexUid("2692177051632464413")
                .setMuid(1152921505361800803L)
                .setReasonParam("null")
                .setPhone("null")
                .putAllDeviceId(Map.of("ios_device_id", "26DEBA5D-C741-4079-B71C-164C4EBEF142"));
        CoinsCreationResponseV2 createdCoins = marketLoyaltyClient.createCoins(
                new CoinsCreationRequestV2(promo.getPromoId().getId(), OTHER, Collections.singletonList(
                        builder.build()
                ))
        );

        assertNotNull(createdCoins);
    }

    @Test
    public void shouldThrow422OnCreateSingleCoin() {
        MarketLoyaltyException ex = assertThrows(MarketLoyaltyException.class, () -> marketLoyaltyClient.createCoin(
                100L,
                EMAIL_COMPANY,
                "someIdempotentKey",
                new Uid(DEFAULT_UID)
        ));

        assertEquals(HttpClientErrorException.UnprocessableEntity.class, ex.getCause().getClass());
    }

    @Test
    public void shouldFailNewCoinIfUserInBlacklist() {

        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFixed().setBindOnlyOnce(true));

        long uid = 123123141412412132L;
        userBlackListDao.addRecord(new BlacklistRecord.Uid(uid));
        userBlacklistService.reloadBlacklist();

        CoinsCreationResponse createdCoins = marketLoyaltyClient.createCoins(new CoinsCreationRequest(
                        promo.getPromoId().getId(),
                        EMAIL_COMPANY,
                        "someIdempotentKey",
                        Collections.singletonList(new Uid(uid))
                )
        );

        assertThat(createdCoins.getCoins(), hasSize(0));
        assertThat(createdCoins.getSuccess(), hasSize(0));
        assertThat(createdCoins.getWithoutCoins(), hasSize(1));
        assertThat(createdCoins.getFailure(), contains(
                allOf(
                        hasProperty("error", equalTo(CoinCreationError.USER_IN_BLACKLIST)),
                        hasProperty("identity", equalTo(new Uid(uid)))
                ))
        );
    }


    @Test
    public void testMarketdiscount5578Fixed() {
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFixed().setBindOnlyOnce(true));

        long uid = 123123141412412132L;

        CoinsCreationResponse createdCoins = marketLoyaltyClient.createCoins(new CoinsCreationRequest(
                        promo.getPromoId().getId(),
                        EMAIL_COMPANY,
                        "someIdempotentKey",
                        Collections.singletonList(new Uid(uid))
                )
        );

        assertThat(createdCoins.getCoins(), hasSize(1));
        assertThat(createdCoins.getSuccess(), hasSize(1));
        assertThat(createdCoins.getWithoutCoins(), hasSize(0));

        userBlackListDao.addRecord(new BlacklistRecord.Uid(uid));
        userBlacklistService.reloadBlacklist();

        // теперь мы ожидаем что будет две ошибки SAME_COIN_ALREDY_CREATED и USER_IN_BLACKLIST
        CoinsCreationResponse createdCoins2 = marketLoyaltyClient.createCoins(new CoinsCreationRequest(
                        promo.getPromoId().getId(),
                        EMAIL_COMPANY,
                        "someIdempotentKey",
                        Collections.singletonList(new Uid(uid))
                )
        );

        assertThat(createdCoins2.getCoins(), hasSize(0));
        assertThat(createdCoins2.getSuccess(), hasSize(0));
        assertThat(createdCoins2.getWithoutCoins(), hasSize(1));
        assertThat(createdCoins2.getFailure(), contains(
                allOf(
                        hasProperty("error", equalTo(SAME_COIN_ALREADY_CREATED)),
                        hasProperty("identity", equalTo(new Uid(uid)))
                ))
        );

    }

    @Test
    public void shouldSuccessNewCoinIfUserAlreadyHasCoin() {
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFixed().setBindOnlyOnce(false));

        CoinsCreationResponse createdCoins = marketLoyaltyClient.createCoins(new CoinsCreationRequest(
                        promo.getPromoId().getId(),
                        EMAIL_COMPANY,
                        "someIdempotentKey",
                        Collections.singletonList(new Uid(DEFAULT_UID))
                )
        );

        assertThat(createdCoins.getCoins(), hasSize(1));
        assertThat(createdCoins.getSuccess(), hasSize(1));
        assertThat(createdCoins.getWithoutCoins(), hasSize(0));
        assertThat(createdCoins.getFailure(), hasSize(0));


        createdCoins = marketLoyaltyClient.createCoins(new CoinsCreationRequest(
                        promo.getPromoId().getId(),
                        EMAIL_COMPANY,
                        "someIdempotentKey2",
                        Collections.singletonList(new Uid(DEFAULT_UID))
                )
        );

        assertThat(createdCoins.getCoins(), hasSize(1));
        assertThat(createdCoins.getSuccess(), hasSize(1));
        assertThat(createdCoins.getWithoutCoins(), hasSize(0));
        assertThat(createdCoins.getFailure(), hasSize(0));
    }

    @Test
    public void creationOfCoinsShouldBeIdempotent() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());

        CoinsCreationRequest requestForFirstPart = new CoinsCreationRequest(
                promo.getPromoId().getId(),
                EMAIL_COMPANY,
                "someIdempotentKey",
                LongStream.range(0, 10).mapToObj(Uid::new).collect(Collectors.toList())
        );

        CoinsCreationResponse firstResponse = marketLoyaltyClient.createCoins(requestForFirstPart);
        CoinsCreationResponse secondResponse = marketLoyaltyClient.createCoins(requestForFirstPart);

        assertThat(
                extractIds(firstResponse),
                containsInAnyOrder(extractIds(secondResponse).toArray())
        );
    }

    @Test
    public void creationOfCoinsShouldBeIdempotentIfBudgetExceeded() {
        int possibleCountOfCoinsToCreate = 5;

        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFixed().setEmissionBudget(BigDecimal.valueOf(possibleCountOfCoinsToCreate)));

        CoinsCreationRequest request = new CoinsCreationRequest(
                promo.getPromoId().getId(),
                EMAIL_COMPANY,
                "someIdempotentKey",
                LongStream.range(0, possibleCountOfCoinsToCreate * 2).mapToObj(Uid::new).collect(Collectors.toList())
        );

        CoinsCreationResponse firstResponse = marketLoyaltyClient.createCoins(request);
        CoinsCreationResponse secondResponse = marketLoyaltyClient.createCoins(request);

        assertThat(
                extractIds(firstResponse),
                containsInAnyOrder(extractIds(secondResponse).toArray())
        );
    }

    @Test
    public void shouldHandleCreationOfCoinsWithEmptyIdentityList() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed());

        CoinsCreationRequest request = new CoinsCreationRequest(
                promo.getPromoId().getId(),
                EMAIL_COMPANY,
                "someIdempotentKey",
                Collections.emptyList()
        );

        CoinsCreationResponse response = marketLoyaltyClient.createCoins(request);

        assertThat(response.getCoins(), is(empty()));
    }

    @Test
    public void shouldReturnDeclinedByAntifraudResult() {

        setupDeclinedAntifraudResponseForUid(
                antifraudRestTemplate,
                DEFAULT_UID,
                LoyaltyBuyerRestrictionsDto.prohibited(DEFAULT_UID, null)
        );

        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFixed().setBindOnlyOnce(true));

        CoinsCreationResponse response = marketLoyaltyClient.createCoins(new CoinsCreationRequest(
                        promo.getPromoId().getId(),
                        EMAIL_COMPANY,
                        "someIdempotentKey",
                        Collections.singletonList(new Uid(DEFAULT_UID))
                )
        );

        List<CoinsCreationResponse.FailureUser> failures = response.getFailure();
        assertThat(response.getCoins(), is(empty()));
        assertThat(failures, hasSize(1));
        assertThat(failures, hasItem(
                hasProperty("error", equalTo(CoinCreationError.DECLINED_BY_ANTIFRAUD)))
        );
        assertTrue(true);
    }


    @Test
    public void shouldAddPromoKeyToSingleCoinCreationResponse() {
        Promo promo = promoManager.createSmartShoppingPromo(
                defaultFixed().setBindOnlyOnce(true));

        UserCoinResponse createSingleCoinResponse = marketLoyaltyClient.createCoin(
                promo.getPromoId().getId(),
                EMAIL_COMPANY,
                "someIdempotentKey",
                new Uid(DEFAULT_UID)
        );

        assertEquals(promo.getPromoKey(), createSingleCoinResponse.getPromoKey());
    }

    @Test
    public void shouldAddPromoKeyToCreateCoinsResponse() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFreeDelivery());

        CoinsCreationResponse createdCoins = marketLoyaltyClient.createCoins(new CoinsCreationRequest(
                promo.getPromoId().getId(),
                EMAIL_COMPANY,
                "someIdempotentKey",
                Arrays.asList(new Uid(DEFAULT_UID), new Uid(ANOTHER_UID))
        ));

        assertThat(
                createdCoins.getCoins(),
                everyItem(
                        hasProperty("promoKey", equalTo(promo.getPromoKey()))
                )
        );
    }

    @Test
    public void shouldCorrectCheckYaPlusUser() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFixed().setOnlyForPlus(true));
        assertThrows(MarketLoyaltyException.class, () -> marketLoyaltyClient.createCoin(
                promo.getPromoId().getId(),
                EMAIL_COMPANY,
                "someIdempotentKey",
                new Uid(DEFAULT_UID)
        ));
        mockBlackbox(DEFAULT_UID, YANDEX_PLUS, true, blackboxRestTemplate);
        UserCoinResponse response = marketLoyaltyClient.createCoin(
                promo.getPromoId().getId(),
                EMAIL_COMPANY,
                "someIdempotentKey",
                new Uid(DEFAULT_UID));
        assertTrue(response.getCoinRestrictions().isEmpty());
    }

    @Test
    public void shouldCreateCoinWithReasonFromPromo() {
        Promo promo = promoManager.createSmartShoppingPromo(defaultFreeDelivery());

        final ImmutableMap<String, String> DEVICE_ID = ImmutableMap.of(
                "deviceId", "01234"
        );
        CoinsCreationResponseV2 createdCoins = createCoin(promo, DEVICE_ID, DEFAULT_UID, DEFAULT_UUID,
                DEFAULT_YANDEX_UID, DEFAULT_EMAIL, DEFAULT_MUID);

        assertThat(
                createdCoins.getCoins(),
                everyItem(
                        hasProperty(
                                "reason",
                                equalTo(promo.getPromoParamRequired(COIN_CREATION_REASON).getApiType())
                        )
                )
        );
    }


    private static List<Long> extractIds(CoinsCreationResponse firstResponse) {
        return firstResponse.getCoins().stream().map(BaseUserCoinResponse::getId).collect(Collectors.toList());
    }

    private CoinsCreationResponseV2 createCoin(
            Promo promo, ImmutableMap<String, String> DEVICE_ID, long uid, String uuid, String yandexUid, String email,
            long muid
    ) {
        return marketLoyaltyClient.createCoins(new CoinsCreationRequestV2(
                promo.getPromoId().getId(),
                EMAIL_COMPANY,
                Collections.singletonList(new SingleCoinCreationRequest(
                        "someIdempotentKey", uid, uuid, yandexUid, email,
                        muid, true, null, null, DEVICE_ID
                ))
        ));
    }
}

package ru.yandex.market.loyalty.back.controller;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;

import io.swagger.annotations.Api;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.yandex.market.checkout.checkouter.client.CheckouterClient;
import ru.yandex.market.checkout.checkouter.client.ClientRole;
import ru.yandex.market.checkout.checkouter.pay.PaymentType;
import ru.yandex.market.health.annotations.NoHealth;
import ru.yandex.market.loyalty.api.model.IdObject;
import ru.yandex.market.loyalty.api.model.coin.CoinsForFront;
import ru.yandex.market.loyalty.api.model.coin.UserCoinResponse;
import ru.yandex.market.loyalty.back.controller.model.CompleteOrderStatusUpdatedRequest;
import ru.yandex.market.loyalty.core.config.DatabaseUsage;
import ru.yandex.market.loyalty.core.model.CoreMarketPlatform;
import ru.yandex.market.loyalty.core.model.coin.CoreCoinCreationReason;
import ru.yandex.market.loyalty.core.model.coin.UserInfo;
import ru.yandex.market.loyalty.core.model.promo.Promo;
import ru.yandex.market.loyalty.core.model.trigger.event.OrderStatusUpdatedEvent;
import ru.yandex.market.loyalty.core.model.trigger.event.data.OrderEventInfo;
import ru.yandex.market.loyalty.core.service.PromoService;
import ru.yandex.market.loyalty.core.service.coin.CoinService;
import ru.yandex.market.loyalty.core.service.discount.OrderItemsConverter;
import ru.yandex.market.loyalty.core.trigger.actions.CoinInsertRequest;
import ru.yandex.market.loyalty.core.utils.DiscountUtils;
import ru.yandex.market.loyalty.health.method.LogMethod;
import ru.yandex.market.loyalty.spring.retry.spring.PgaasRetryable;

import static ru.yandex.market.loyalty.back.security.Actions.TEST_FEATURE;
import static ru.yandex.market.loyalty.core.config.DatasourceType.READ_ONLY_ASYNC;
import static ru.yandex.market.loyalty.core.config.DatasourceType.READ_WRITE;

@Validated
@RestController
@RequestMapping("/dontUseInProduction")
@Profile({"testing", "load"})
@Api("Этот контроллер поднимается только в тестинге.")
public class TestingLoadController {
    @SuppressWarnings("NumericOverflow")
    private static final long LONG_SIGN_BIT_MASK = Long.MAX_VALUE + 1;
    private static final long TEST_COIN_PROMO_ID = 10432L;

    private final CheckouterClient checkouterClient;
    private final CoinsController coinsController;
    private final CoinService coinService;
    private final PromoService promoService;
    private final Promo testCoinPromo;
    private final DiscountUtils discountUtils;

    public TestingLoadController(
            CheckouterClient checkouterClient, CoinsController coinsController,
            CoinService coinService, PromoService promoService,
            DiscountUtils discountUtils
    ) {
        this.checkouterClient = checkouterClient;
        this.coinsController = coinsController;
        this.coinService = coinService;
        this.promoService = promoService;
        this.discountUtils = discountUtils;
        Assert.state(
                !System.getProperty("environment", System.getenv("ENVIRONMENT")).toLowerCase().equals("production"),
                "don't use in production"
        );
        testCoinPromo = READ_ONLY_ASYNC.within(() -> promoService.getPromo(TEST_COIN_PROMO_ID));
    }

    @RolesAllowed(TEST_FEATURE)
    @DatabaseUsage(READ_WRITE)
    @PostMapping(path = "/createCoinForLoad", produces = MediaType.APPLICATION_JSON_VALUE)
    @LogMethod
    @NoHealth
    public IdObject createCoin(long promoId, long uid) {
        return new IdObject(coinService.create.createCoin(
                promoService.getPromo(promoId),
                CoinInsertRequest.authMarketBonus(uid)
                        .setSourceKey(UUID.randomUUID().toString())
                        .setReason(CoreCoinCreationReason.OTHER)
                        .build()
        ).getId());
    }

    @RolesAllowed(TEST_FEATURE)
    @DatabaseUsage(READ_WRITE)
    @LogMethod
    @PutMapping(path = "/bindCoinsToUserWithCoinCreation", produces = MediaType.APPLICATION_JSON_VALUE)
    @PgaasRetryable
    @NoHealth
    public List<UserCoinResponse> bindCoinsToUserWithCoinCreation() {
        long uid = randomNegativeLong();
        String activationToken = UUID.randomUUID().toString();
        coinService.create.createCoin(
                testCoinPromo,
                CoinInsertRequest.noAuthMarketBonus(
                        UserInfo.builder()
                                .setEmail("test@yandex-team.ru")
                                .setMuid(randomNegativeLong())
                                .setUuid(UUID.randomUUID().toString())
                                .setPhone("12312312123")
                                .setYandexUid(UUID.randomUUID().toString())
                                .build(),
                        activationToken
                )
                        .setSourceKey(UUID.randomUUID().toString())
                        .setReason(CoreCoinCreationReason.OTHER)
                        .build()
        );
        return coinsController.bindCoinsToUser(uid, activationToken);
    }

    @RolesAllowed(TEST_FEATURE)
    @DatabaseUsage(READ_WRITE)
    @LogMethod
    @PostMapping(path = "/completeOrderStatusUpdated", produces = MediaType.APPLICATION_JSON_VALUE)
    @PgaasRetryable
    @NoHealth
    public CoinsForFront completeOrderStatusUpdated(@Valid @RequestBody CompleteOrderStatusUpdatedRequest request) {
        request.setOrderId(randomNegativeLong());
        request.setUid(randomNegativeLong());
        return coinsController.completeOrderStatusUpdated(requestToEvent(request),
                discountUtils.getRulesPayload()
        );
    }

    @RolesAllowed(TEST_FEATURE)
    @DatabaseUsage(READ_WRITE)
    @LogMethod
    @PostMapping(path = "/completeOrderStatusUpdatedWithCheckouter", produces = MediaType.APPLICATION_JSON_VALUE)
    @PgaasRetryable
    @NoHealth
    public CoinsForFront completeOrderStatusUpdatedWithCheckouter(
            @Valid @RequestBody CompleteOrderStatusUpdatedRequest request
    ) {
        checkouterClient.getOrder(2525942L, ClientRole.USER, 543647426L);
        request.setOrderId(randomNegativeLong());
        request.setUid(randomNegativeLong());
        return coinsController.completeOrderStatusUpdated(requestToEvent(request),
                discountUtils.getRulesPayload()
        );
    }

    private static OrderStatusUpdatedEvent requestToEvent(CompleteOrderStatusUpdatedRequest request) {
        return OrderStatusUpdatedEvent.builder()
                .setSingleOrderUniqueKey(request.getOrderId())
                .addPersistentData(OrderEventInfo.builder()
                        .setPlatform(CoreMarketPlatform.findByApiPlatform(request.getPlatform()))
                        .setDeliveryRegion(request.getDeliveryRegion())
                        .setEmail(request.getUserEmail())
                        .setPhone(request.getUserPhoneNumber())
                        .setPaymentType(PaymentType.valueOf(request.getPaymentType()))
                        .setItems(OrderItemsConverter.constructItemsFromRequest(request.getItems()))
                        .setUid(request.getUid())
                        .setMuid(request.getMuid())
                        .setNoAuth(request.getNoAuth())
                        .setOrderId(request.getOrderId())
                        .build())
                .build();
    }

    private static long randomNegativeLong() {
        return ThreadLocalRandom.current().nextLong() | LONG_SIGN_BIT_MASK;
    }
}

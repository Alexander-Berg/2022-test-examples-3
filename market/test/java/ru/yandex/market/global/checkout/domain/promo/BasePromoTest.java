package ru.yandex.market.global.checkout.domain.promo;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseApiTest;
import ru.yandex.market.global.checkout.api.CartApiService;
import ru.yandex.market.global.checkout.api.OrderApiService;
import ru.yandex.market.global.checkout.domain.promo.subject.PromoUserRepository;
import ru.yandex.market.global.checkout.domain.shop.ShopQueryService;
import ru.yandex.market.global.checkout.factory.TestCartFactory;
import ru.yandex.market.global.checkout.factory.TestElasticOfferFactory;
import ru.yandex.market.global.checkout.factory.TestPromoFactory;
import ru.yandex.market.global.checkout.factory.TestShopFactory;
import ru.yandex.market.global.checkout.order.OrderApiServiceTest;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.market.global.common.elastic.dictionary.DictionaryQueryService;
import ru.yandex.market.global.common.test.TestClock;
import ru.yandex.mj.generated.server.model.OfferDto;
import ru.yandex.mj.generated.server.model.ScheduleItemDto;
import ru.yandex.mj.generated.server.model.ShopDto;

public class BasePromoTest extends BaseApiTest {

    protected static final long UID = 1;

    protected static final String SOME_USER_TICKET = "some_user_ticket";
    protected static final String YA_TAXI_USERID = "some-ya-taxi-userid";
    protected static final String IDEMPOTENCY_KEY = UUID.randomUUID().toString();

    protected static final long SHOP_ID = 40;
    protected static final long BUSINESS_ID = 41;
    protected static final String OFFER_ID = "OFFER_ID";

    protected static final Instant VALID_FROM = Instant.parse("2021-12-01T00:00:00.00Z");
    protected static final Instant VALID_TILL = Instant.parse("2021-12-31T23:59:59.00Z");
    protected static final String FREE_DELIVERY_NAME = "FIRST_THREE_DELIVERIES_ISRAEL";

    protected static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(OrderApiServiceTest.class).build();

    protected static final ShopDto SHOP_DTO = RANDOM.nextObject(ShopDto.class)
            .businessId(BUSINESS_ID)
            .id(SHOP_ID)
            .schedule(List.of(new ScheduleItemDto()
                    .open(true)
                    .day("ANY")
                    .startAt("00:00:00")
                    .endAt("23:59:59")
            ))
            .enabled(true);

    @Autowired
    protected ShopQueryService shopQueryService;

    @Autowired
    protected DictionaryQueryService<OfferDto> offersDictionary;

    @Autowired
    protected TestClock clock;

    @Autowired
    protected PromoRepository promoRepository;

    @Autowired
    protected OrderApiService orderApiService;

    @Autowired
    protected CartApiService cartApiService;

    @Autowired
    protected PromoUserRepository promoUserRepository;

    @Autowired
    protected TestPromoFactory testPromoFactory;

    @Autowired
    protected TestCartFactory testCartFactory;

    @Autowired
    protected PromoCommandSerivce promoCommandSerivce;

    @Autowired
    protected TestShopFactory testShopFactory;

    @Autowired
    protected TestElasticOfferFactory testElasticOfferFactory;

    @BeforeEach
    private void prepare() {
        clock.setTime(VALID_FROM.plus(1, ChronoUnit.DAYS));

        Mockito.when(shopQueryService.get(Mockito.anyLong()))
                .thenReturn(testShopFactory.buildShopDto(TestShopFactory.CreateShopDtoBuilder.builder()
                        .setupShop(s -> s
                                .id(SHOP_ID)
                                .businessId(BUSINESS_ID)
                        )
                        .build()
                ));


        Mockito.when(offersDictionary.get(Mockito.anyList()))
                .thenReturn(testElasticOfferFactory.buildOne(
                        offer -> offer.price(123_00L)));

    }


}

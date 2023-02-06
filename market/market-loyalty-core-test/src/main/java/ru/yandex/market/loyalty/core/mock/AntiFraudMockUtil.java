package ru.yandex.market.loyalty.core.mock;

import java.net.URI;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import gumi.builders.UrlBuilder;
import org.hamcrest.Matcher;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.antifraud.orders.web.dto.LoyaltyBuyerRestrictionsDto;
import ru.yandex.market.antifraud.orders.web.dto.LoyaltyVerdictDto;
import ru.yandex.market.antifraud.orders.web.dto.LoyaltyVerdictRequestDto;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountDto;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountRequestDtoV2;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountRequestFiltersDto;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountResponseDtoV2;
import ru.yandex.market.antifraud.orders.web.dto.OrderCountResponseItemDto;
import ru.yandex.market.antifraud.orders.web.dto.OrderStatsDto;
import ru.yandex.market.antifraud.orders.web.dto.PromoVerdictDto;
import ru.yandex.market.antifraud.orders.web.dto.loyalty.LoyaltyBonusInfoResponseDto;
import ru.yandex.market.antifraud.orders.web.entity.LoyaltyRestrictionType;
import ru.yandex.market.antifraud.orders.web.entity.LoyaltyVerdictType;
import ru.yandex.market.antifraud.orders.web.entity.PromoVerdictType;
import ru.yandex.market.loyalty.core.config.Antifraud;
import ru.yandex.market.loyalty.core.model.coin.CoinKey;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

@Service
public class AntiFraudMockUtil {
    @Antifraud
    private final RestTemplate restTemplate;
    private final String antifraudUrl;

    public AntiFraudMockUtil(
            @Antifraud RestTemplate restTemplate,
            @Value("${checkout.antifraud.url}") String antifraudUrl
    ) {
        this.restTemplate = restTemplate;
        this.antifraudUrl = antifraudUrl;
    }

    public void userInBlacklist() {
        mockResponse(LoyaltyVerdictType.BLACKLIST, Collections.emptyMap());
    }

    public void coinWasUsed(CoinKey coinKey) {
        mockResponse(LoyaltyVerdictType.OTHER, Collections.singletonMap(coinKey, PromoVerdictType.USED));
    }

    public void previousOrders(int totalOrderCount, int totalMobileOrdersCount) {
        when(restTemplate.exchange(
                any(RequestEntity.class), any(Class.class)
        )).thenAnswer(invocation -> {
            OrderCountRequestDtoV2 requestDtoV2 = (OrderCountRequestDtoV2)
                    invocation.getArgument(0, RequestEntity.class).getBody();
            Map<String, OrderCountResponseItemDto> responseItems =
                    requestDtoV2.getRequestItems()
                            .entrySet()
                            .stream()
                            .map(
                                    e -> buildAnswer(e.getKey(), e.getValue(), totalOrderCount, totalMobileOrdersCount))
                            .collect(Collectors.toMap(Map.Entry::getKey,
                                    Map.Entry::getValue
                            ));
            OrderCountResponseDtoV2 orderCountResponseDtoV2 = OrderCountResponseDtoV2.builder()
                    .puid(100L)
                    .responseItems(responseItems)
                    .build();

            return ResponseEntity.ok(orderCountResponseDtoV2);
        });
    }

    private Map.Entry<String, OrderCountResponseItemDto> buildAnswer(
            String key, OrderCountRequestFiltersDto value, int totalOrderCount, int totalMobileOrdersCount
    ) {
        if (value.getPlatformType() != null) {
            return new AbstractMap.SimpleEntry<>(key,
                    OrderCountResponseItemDto.builder()
                            .from(value.getFrom())
                            .to(value.getTo())
                            .userOrderCount(new OrderCountDto(0, 0, 0, 0))
                            .glueOrderCount(
                                    new OrderCountDto(0, totalMobileOrdersCount, 0, totalMobileOrdersCount)
                            )
                            .build()
            );
        } else {
            return new AbstractMap.SimpleEntry<>(key,
                    OrderCountResponseItemDto.builder()
                            .from(Instant.MIN)
                            .to(Instant.MAX)
                            .userOrderCount(new OrderCountDto(0, 0, 0, 0))
                            .glueOrderCount(new OrderCountDto(0, totalOrderCount, 0, totalOrderCount))
                            .build()
            );
        }
    }

    public void mockUserRestrictions(long uid, LoyaltyRestrictionType restrictionType, OrderStatsDto orderStatsDto) {
        final URI uri = UrlBuilder.fromString(antifraudUrl)
                .withPath("/antifraud/loyalty/restrictions")
                .toUri();

        when(restTemplate.exchange(argThat(hasProperty("url", equalTo(uri))), eq(LoyaltyBuyerRestrictionsDto.class)))
                .thenReturn(ResponseEntity.ok(new LoyaltyBuyerRestrictionsDto(uid, restrictionType, orderStatsDto)));
    }

    public void detectWithSleep() {
        var uri = UrlBuilder.fromString(antifraudUrl)
                .withPath("/antifraud/loyalty/detect")
                .toUri();

        when(restTemplate.exchange(argThat(hasProperty("url", equalTo(uri))), eq(LoyaltyVerdictDto.class)))
                .thenAnswer((interaction) -> {
                    Thread.sleep(1000);
                    return ResponseEntity.ok(new LoyaltyVerdictDto(
                            LoyaltyVerdictType.BLACKLIST,
                            Collections.emptyList(),
                            Collections.emptyList(),
                            false
                    ));
                });
    }

    public void loyaltyDetect(LoyaltyVerdictDto verdictDto, Matcher<RequestEntity>... requestMatchers) {
        var uri = UrlBuilder.fromString(antifraudUrl)
                .withPath("/antifraud/loyalty/detect")
                .toUri();

        List<Matcher<RequestEntity>> matchers = new ArrayList<>();
        matchers.add(hasProperty("url", equalTo(uri)));
        matchers.addAll(Arrays.asList(requestMatchers));
        Matcher<RequestEntity> allOf = allOf(matchers.toArray(new Matcher[0]));

        when(restTemplate.exchange(argThat(allOf), eq(LoyaltyVerdictDto.class)))
                .thenAnswer((interaction) -> ResponseEntity.ok(verdictDto));
    }

    public void ordersCountWithSleep() {
        var uri = UrlBuilder.fromString(antifraudUrl)
                .withPath("/antifraud/loyalty/orders-count/v2")
                .toUri();

        when(restTemplate.exchange(argThat(hasProperty("url", equalTo(uri))), eq(OrderCountResponseDtoV2.class)))
                .thenAnswer((interaction) -> {
                    Thread.sleep(1000);
                    return ResponseEntity.ok(OrderCountResponseDtoV2.builder().build());
                });
    }

    public void restrictionsWithSleep() {
        var uri = UrlBuilder.fromString(antifraudUrl)
                .withPath("/antifraud/loyalty/restrictions")
                .toUri();

        when(restTemplate.exchange(argThat(hasProperty("url", equalTo(uri))), eq(LoyaltyBuyerRestrictionsDto.class)))
                .thenAnswer((interaction) -> {
                    Thread.sleep(1000);
                    return ResponseEntity.ok(LoyaltyBuyerRestrictionsDto.ok(null, null));
                });
    }

    public void restrictionsBonusWithSleep() {
        var uri = UrlBuilder.fromString(antifraudUrl)
                .withPath("/antifraud/loyalty/restrictions/bonus")
                .toUri();

        when(restTemplate.exchange(argThat(hasProperty("url", equalTo(uri))), eq(LoyaltyBonusInfoResponseDto.class)))
                .thenAnswer((interaction) -> {
                    Thread.sleep(1000);
                    return ResponseEntity.ok(LoyaltyBonusInfoResponseDto.builder().build());
                });
    }

    private void mockResponse(LoyaltyVerdictType userVerdict, Map<CoinKey, PromoVerdictType> responsesPerCoin) {
        when(restTemplate.exchange(
                any(RequestEntity.class), eq(LoyaltyVerdictDto.class)
        )).thenAnswer((Answer<ResponseEntity<LoyaltyVerdictDto>>) invocation -> {
            LoyaltyVerdictRequestDto request = (LoyaltyVerdictRequestDto)
                    invocation.getArgument(0, RequestEntity.class).getBody();
            return ResponseEntity.ok(new LoyaltyVerdictDto(
                    userVerdict, Collections.emptyList(),
                    request.getCoins().stream()
                            .map(c -> new PromoVerdictDto(
                                    c.getCoinId(), c.getPromoId(),
                                    responsesPerCoin.getOrDefault(new CoinKey(c.getCoinId()), PromoVerdictType.OK)
                            ))
                            .collect(Collectors.toList()),
                    false
            ));
        });
    }
}

package ru.yandex.market.global.checkout.domain.actualize.actualizer;

import java.util.List;

import io.github.benas.randombeans.api.EnhancedRandom;
import lombok.RequiredArgsConstructor;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.global.checkout.BaseFunctionalTest;
import ru.yandex.market.global.checkout.domain.actualize.OrderActualization;
import ru.yandex.market.global.checkout.factory.TestOrderFactory.CreateOrderActualizationBuilder;
import ru.yandex.market.global.checkout.util.PaymentUtil;
import ru.yandex.market.global.checkout.util.RandomDataGenerator;
import ru.yandex.market.global.common.elastic.dictionary.DictionaryQueryService;
import ru.yandex.market.global.common.trust.client.TrustClient;
import ru.yandex.market.global.common.trust.client.dto.BoundPaymentMethod;
import ru.yandex.market.global.common.trust.client.dto.GetPaymentMethodsResponse;
import ru.yandex.market.global.common.util.configuration.ConfigurationService;
import ru.yandex.market.global.db.jooq.tables.pojos.Order;
import ru.yandex.market.global.db.jooq.tables.pojos.OrderItem;
import ru.yandex.mj.generated.client.blackbox.api.BlackboxApiClient;
import ru.yandex.mj.generated.client.blackbox.model.BlackboxResponse;
import ru.yandex.mj.generated.client.blackbox.model.BlackboxUserinfoItem;
import ru.yandex.mj.generated.client.blackbox.model.BlackboxUserinfoItemAttributes;
import ru.yandex.mj.generated.server.model.OfferDto;

import static ru.yandex.market.global.checkout.configuration.ConfigurationProperties.PLUS_MODE;
import static ru.yandex.market.global.checkout.factory.TestOrderFactory.buildOrderActualization;
import static ru.yandex.market.global.db.jooq.enums.EPlusActionType.EARN;
import static ru.yandex.market.global.db.jooq.enums.EPlusActionType.SPEND;

@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PlusActualizerTest extends BaseFunctionalTest {
    private static final EnhancedRandom RANDOM = RandomDataGenerator.dataRandom(PlusActualizerTest.class).build();
    private static final RecursiveComparisonConfiguration RECURSIVE_COMPARISON_CONFIGURATION =
            RecursiveComparisonConfiguration.builder()
                    .withIgnoreCollectionOrder(true)
                    .withIgnoreAllExpectedNullFields(true)
                    .build();

    private final ConfigurationService configurationService;
    private final PlusCartActualizer plusCartActualizer;
    private final PlusOrderActualizer plusOrderActualizer;
    private final BlackboxApiClient blackboxApiClient;
    private final TrustClient trustClient;
    private final DictionaryQueryService<OfferDto> offersDictionary;

    @BeforeEach
    public void setup() {
        configurationService.mergeValue(PLUS_MODE, "ON");
    }

    @Test
    public void testCartEarn() {
        mockPlusEnabled("1");
        mockPlusBalance("1000.00");
        mockOffers(List.of(
                RANDOM.nextObject(OfferDto.class)
                        .offerId("ID200")
                        .plusMaxRewardAmount(2_00L),
                RANDOM.nextObject(OfferDto.class)
                        .offerId("ID300")
                        .plusMaxRewardAmount(3_00L)
        ));

        OrderActualization actualization = buildOrderActualization(CreateOrderActualizationBuilder.builder()
                .setupOrder(o -> o.setPlusAction(EARN))
                .setupItems(l -> List.of(
                        RANDOM.nextObject(OrderItem.class)
                                .setOfferId("ID200")
                                .setCount(2L)
                                .setPrice(200_00L)
                                .setTotalCost(400_00L)
                                .setTotalCostWithoutPromo(400_00L),
                        RANDOM.nextObject(OrderItem.class)
                                .setOfferId("ID300")
                                .setCount(2L)
                                .setPrice(300_00L)
                                .setTotalCost(600_00L)
                                .setTotalCostWithoutPromo(600_00L)
                ))
                .build()
        );

        OrderActualization actualized = plusCartActualizer.actualize(actualization);
        Assertions.assertThat(actualized.getOrder()).usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(new Order()
                        .setPlusAction(EARN)
                        .setPlusEarned(10_00L)
                        .setPlusSpent(996_00L)
                        .setPlusAvailableAmount(1000_00L)
                );
        Assertions.assertThat(actualized.getOrderItems())
                .usingRecursiveFieldByFieldElementComparator(RECURSIVE_COMPARISON_CONFIGURATION)
                .containsExactlyInAnyOrder(
                        new OrderItem()
                                .setTotalCost(400_00L)
                                .setPlusEarned(4_00L)
                                .setPlusSpent(398_00L),
                        new OrderItem()
                                .setTotalCost(600_00L)
                                .setPlusEarned(6_00L)
                                .setPlusSpent(598_00L)
                );

    }

    @Test
    public void testOrderEarn() {
        mockPlusEnabled("1");
        mockPlusBalance("1000.00");
        mockOffers(List.of(RANDOM.nextObject(OfferDto.class)
                .offerId("ID200")
                .plusMaxRewardAmount(2_00L)
        ));

        OrderActualization actualization = buildOrderActualization(CreateOrderActualizationBuilder.builder()
                .setupOrder(o -> o.setPlusAction(EARN))
                .setupItems(l -> List.of(
                        RANDOM.nextObject(OrderItem.class)
                                .setOfferId("ID200")
                                .setCount(2L)
                                .setPrice(200_00L)
                                .setTotalCost(400_00L)
                                .setTotalCostWithoutPromo(400_00L)
                ))
                .build()
        );

        OrderActualization actualized = plusOrderActualizer.actualize(actualization);
        Assertions.assertThat(actualized.getOrder())
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withComparedFields("plusAction", "plusSpent", "plusEarned", "plusAvailableAmount")
                        .build()
                )
                .isEqualTo(new Order()
                        .setPlusAction(EARN)
                        .setPlusEarned(4_00L)
                        .setPlusSpent(null)
                        .setPlusAvailableAmount(1000_00L)
                );

        Assertions.assertThat(actualized.getOrderItems())
                .usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
                        .withComparedFields("totalCostWithoutPromo", "totalCost", "plusSpent", "plusEarned")
                        .build()
                )
                .containsExactlyInAnyOrder(
                        new OrderItem()
                                .setTotalCostWithoutPromo(400_00L)
                                .setTotalCost(400_00L)
                                .setPlusEarned(4_00L)
                                .setPlusSpent(null)
                );
    }

    @Test
    public void testOrderSpent() {
        mockPlusEnabled("1");
        mockPlusBalance("1000.00");
        mockOffers(List.of(RANDOM.nextObject(OfferDto.class)
                .offerId("ID200")
                .plusMaxRewardAmount(2_00L)
        ));

        OrderActualization actualization = buildOrderActualization(CreateOrderActualizationBuilder.builder()
                .setupOrder(o -> o.setPlusAction(SPEND))
                .setupItems(l -> List.of(
                        RANDOM.nextObject(OrderItem.class)
                                .setOfferId("ID200")
                                .setCount(2L)
                                .setPrice(200_00L)
                                .setTotalCost(400_00L)
                                .setTotalCostWithoutPromo(400_00L)
                ))
                .build()
        );

        OrderActualization actualized = plusOrderActualizer.actualize(actualization);
        Assertions.assertThat(actualized.getOrder())
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withComparedFields("plusAction", "plusSpent", "plusEarned", "plusAvailableAmount")
                        .build()
                )
                .isEqualTo(new Order()
                        .setPlusAction(SPEND)
                        .setPlusEarned(null)
                        .setPlusSpent(398_00L)
                        .setPlusAvailableAmount(1000_00L)
                );

        Assertions.assertThat(actualized.getOrderItems())
                .usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
                        .withComparedFields("totalCostWithoutPromo", "totalCost", "plusSpent", "plusEarned")
                        .build()
                )
                .containsExactlyInAnyOrder(
                        new OrderItem()
                                .setTotalCostWithoutPromo(400_00L)
                                .setTotalCost(2_00L)
                                .setPlusEarned(null)
                                .setPlusSpent(398_00L)
                );
    }

    @Test
    public void testCartNoAction() {
        mockPlusEnabled("1");
        mockPlusBalance("1000.00");
        mockOffers(List.of(RANDOM.nextObject(OfferDto.class)
                .offerId("ID200")
                .plusMaxRewardAmount(2_00L)
        ));

        OrderActualization actualization = buildOrderActualization(CreateOrderActualizationBuilder.builder()
                .setupOrder(o -> o.setPlusAction(null))
                .setupItems(l -> List.of(
                        RANDOM.nextObject(OrderItem.class)
                                .setOfferId("ID200")
                                .setCount(2L)
                                .setPrice(200_00L)
                                .setTotalCost(400_00L)
                                .setTotalCostWithoutPromo(400_00L)
                ))
                .build()
        );

        OrderActualization actualized = plusCartActualizer.actualize(actualization);
        Assertions.assertThat(actualized.getOrder()).usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(new Order()
                        .setPlusAction(null)
                        .setPlusEarned(4_00L)
                        .setPlusSpent(398_00L)
                        .setPlusAvailableAmount(1000_00L)
                );
        Assertions.assertThat(actualized.getOrderItems())
                .usingRecursiveFieldByFieldElementComparator(RECURSIVE_COMPARISON_CONFIGURATION)
                .containsExactlyInAnyOrder(
                        new OrderItem()
                                .setTotalCost(400_00L)
                                .setPlusEarned(4_00L)
                                .setPlusSpent(398_00L)
                );
    }

    @Test
    public void testCartNoPlus() {
        mockPlusEnabled("0");
        mockPlusBalance("1000.00");
        mockOffers(List.of(RANDOM.nextObject(OfferDto.class)
                .offerId("ID200")
                .plusMaxRewardAmount(2_00L)
        ));

        OrderActualization actualization = buildOrderActualization(CreateOrderActualizationBuilder.builder()
                .setupOrder(o -> o
                        .setPlusAction(EARN)
                )
                .setupItems(l -> List.of(
                        RANDOM.nextObject(OrderItem.class)
                                .setOfferId("ID200")
                                .setCount(2L)
                                .setPrice(200_00L)
                                .setTotalCost(400_00L)
                                .setTotalCostWithoutPromo(400_00L)
                ))
                .build()
        );

        OrderActualization actualized = plusCartActualizer.actualize(actualization);
        Assertions.assertThat(actualized.getOrder())
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withComparedFields("plusAction", "plusSpent", "plusEarned", "plusAvailableAmount")
                        .build()
                )
                .isEqualTo(new Order()
                        .setPlusAction(null)
                        .setPlusSpent(null)
                        .setPlusEarned(null)
                        .setPlusAvailableAmount(null)
                );

        Assertions.assertThat(actualized.getOrderItems().get(0))
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withComparedFields("totalCost", "totalCostWithoutPromo", "plusSpent", "plusEarned")
                        .build()
                )
                .isEqualTo(
                        new OrderItem()
                                .setTotalCostWithoutPromo(400_00L)
                                .setTotalCost(400_00L)
                                .setPlusEarned(null)
                                .setPlusSpent(null)
                );
    }

    @Test
    public void testCartSpendRound() {
        mockPlusEnabled("1");
        mockPlusBalance("10.50");
        mockOffers(List.of(
                RANDOM.nextObject(OfferDto.class)
                        .offerId("ID200")
                        .plusMaxRewardAmount(2_10L),
                RANDOM.nextObject(OfferDto.class)
                        .offerId("ID300")
                        .plusMaxRewardAmount(3_10L)
        ));

        OrderActualization actualization = buildOrderActualization(CreateOrderActualizationBuilder.builder()
                .setupOrder(o -> o.setPlusAction(SPEND))
                .setupItems(l -> List.of(
                        RANDOM.nextObject(OrderItem.class)
                                .setOfferId("ID200")
                                .setCount(2L)
                                .setPrice(200_00L)
                                .setTotalCost(400_00L)
                                .setTotalCostWithoutPromo(400_00L),
                        RANDOM.nextObject(OrderItem.class)
                                .setOfferId("ID300")
                                .setCount(2L)
                                .setPrice(300_00L)
                                .setTotalCost(600_00L)
                                .setTotalCostWithoutPromo(600_00L)
                ))
                .build()
        );

        OrderActualization actualized = plusCartActualizer.actualize(actualization);

        Assertions.assertThat(actualized.getOrderItems())
                .usingRecursiveFieldByFieldElementComparator(RECURSIVE_COMPARISON_CONFIGURATION)
                .containsExactlyInAnyOrder(
                        new OrderItem()
                                .setOfferId("ID200")
                                .setTotalCost(395_90L)
                                .setPlusEarned(4_20L)
                                .setPlusSpent(4_10L),
                        new OrderItem()
                                .setOfferId("ID300")
                                .setTotalCost(593_60L)
                                .setPlusEarned(6_20L)
                                .setPlusSpent(6_40L)
                );

        Assertions.assertThat(actualized.getOrder()).usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(new Order()
                        .setPlusAction(SPEND)
                        .setPlusEarned(10_40L)
                        .setPlusSpent(10_50L)
                        .setPlusAvailableAmount(10_50L)
                );
    }

    @Test
    public void testCartSpend() {
        mockPlusEnabled("1");
        mockPlusBalance("1000.00");
        mockOffers(List.of(RANDOM.nextObject(OfferDto.class)
                .offerId("ID200")
                .plusMaxRewardAmount(2_00L)
        ));

        OrderActualization actualization = buildOrderActualization(CreateOrderActualizationBuilder.builder()
                .setupOrder(o -> o.setPlusAction(SPEND))
                .setupItems(l -> List.of(
                        RANDOM.nextObject(OrderItem.class)
                                .setOfferId("ID200")
                                .setCount(2L)
                                .setPrice(200_00L)
                                .setTotalCost(400_00L)
                                .setTotalCostWithoutPromo(400_00L)
                ))
                .build()
        );

        OrderActualization actualized = plusCartActualizer.actualize(actualization);

        Assertions.assertThat(actualized.getOrderItems())
                .usingRecursiveFieldByFieldElementComparator(RECURSIVE_COMPARISON_CONFIGURATION)
                .containsExactlyInAnyOrder(
                        new OrderItem()
                                .setTotalCost(2_00L)
                                .setPlusEarned(4_00L)
                                .setPlusSpent(398_00L)
                );
        Assertions.assertThat(actualized.getOrder()).usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(new Order()
                        .setPlusAction(SPEND)
                        .setPlusEarned(4_00L)
                        .setPlusSpent(398_00L)
                        .setPlusAvailableAmount(1000_00L)
                );
    }

    @Test
    public void testCartEarnWithPromo() {
        mockPlusEnabled("1");
        mockPlusBalance("1000.00");
        mockOffers(List.of(
                RANDOM.nextObject(OfferDto.class)
                        .offerId("ID200")
                        .plusMaxRewardAmount(20_00L),
                RANDOM.nextObject(OfferDto.class)
                        .offerId("ID300")
                        .plusMaxRewardAmount(30_00L)
        ));

        OrderActualization actualization = buildOrderActualization(CreateOrderActualizationBuilder.builder()
                .setupOrder(o -> o.setPlusAction(EARN))
                .setupItems(l -> List.of(
                        RANDOM.nextObject(OrderItem.class)
                                .setOfferId("ID200")
                                .setCount(2L)
                                .setPrice(200_00L)
                                .setTotalCost(200_00L)
                                .setTotalCostWithoutPromo(400_00L),
                        RANDOM.nextObject(OrderItem.class)
                                .setOfferId("ID300")
                                .setCount(2L)
                                .setPrice(300_00L)
                                .setTotalCost(300_00L)
                                .setTotalCostWithoutPromo(600_00L)
                ))
                .build()
        );

        OrderActualization actualized = plusCartActualizer.actualize(actualization);
        Assertions.assertThat(actualized.getOrder()).usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(new Order()
                        .setPlusAction(EARN)
                        .setPlusEarned(50_00L)
                        .setPlusSpent(496_00L)
                        .setPlusAvailableAmount(1000_00L)
                );
        Assertions.assertThat(actualized.getOrderItems())
                .usingRecursiveFieldByFieldElementComparator(RECURSIVE_COMPARISON_CONFIGURATION)
                .containsExactlyInAnyOrder(
                        new OrderItem()
                                .setTotalCost(200_00L)
                                .setPlusEarned(20_00L)
                                .setPlusSpent(198_00L),
                        new OrderItem()
                                .setTotalCost(300_00L)
                                .setPlusEarned(30_00L)
                                .setPlusSpent(298_00L)
                );

    }

    @Test
    public void testCartEarnWithAdult() {
        mockPlusEnabled("1");
        mockPlusBalance("1000.00");
        mockOffers(List.of(
                RANDOM.nextObject(OfferDto.class)
                        .offerId("ID200")
                        .plusMaxRewardAmount(0L),
                RANDOM.nextObject(OfferDto.class)
                        .offerId("ID300")
                        .plusMaxRewardAmount(30_00L)
        ));

        OrderActualization actualization = buildOrderActualization(CreateOrderActualizationBuilder.builder()
                .setupOrder(o -> o.setPlusAction(EARN))
                .setupItems(l -> List.of(
                        RANDOM.nextObject(OrderItem.class)
                                .setOfferId("ID200")
                                .setCount(2L)
                                .setPrice(200_00L)
                                .setTotalCost(400_00L)
                                .setTotalCostWithoutPromo(400_00L),
                        RANDOM.nextObject(OrderItem.class)
                                .setOfferId("ID300")
                                .setCount(2L)
                                .setPrice(300_00L)
                                .setTotalCost(300_00L)
                                .setTotalCostWithoutPromo(600_00L)
                ))
                .build()
        );

        OrderActualization actualized = plusCartActualizer.actualize(actualization);
        Assertions.assertThat(actualized.getOrder()).usingRecursiveComparison(RECURSIVE_COMPARISON_CONFIGURATION)
                .isEqualTo(new Order()
                        .setPlusAction(EARN)
                        .setPlusEarned(30_00L)
                        .setPlusSpent(696_00L)
                        .setPlusAvailableAmount(1000_00L)
                );
        Assertions.assertThat(actualized.getOrderItems())
                .usingRecursiveFieldByFieldElementComparator(RECURSIVE_COMPARISON_CONFIGURATION)
                .containsExactlyInAnyOrder(
                        new OrderItem()
                                .setTotalCost(400_00L)
                                .setPlusEarned(0L)
                                .setPlusSpent(398_00L),
                        new OrderItem()
                                .setTotalCost(300_00L)
                                .setPlusEarned(30_00L)
                                .setPlusSpent(298_00L)
                );

    }

    @Test
    public void testOrderEarnWithPromo() {
        mockPlusEnabled("1");
        mockPlusBalance("1000.00");
        mockOffers(List.of(RANDOM.nextObject(OfferDto.class)
                .offerId("ID200")
                .plusMaxRewardAmount(20_00L)
        ));

        OrderActualization actualization = buildOrderActualization(CreateOrderActualizationBuilder.builder()
                .setupOrder(o -> o.setPlusAction(EARN))
                .setupItems(l -> List.of(
                        RANDOM.nextObject(OrderItem.class)
                                .setOfferId("ID200")
                                .setCount(2L)
                                .setPrice(200_00L)
                                .setTotalCost(50_00L)
                                .setTotalCostWithoutPromo(400_00L)
                ))
                .build()
        );

        OrderActualization actualized = plusOrderActualizer.actualize(actualization);
        Assertions.assertThat(actualized.getOrder())
                .usingRecursiveComparison(RecursiveComparisonConfiguration.builder()
                        .withComparedFields("plusAction", "plusSpent", "plusEarned", "plusAvailableAmount")
                        .build()
                )
                .isEqualTo(new Order()
                        .setPlusAction(EARN)
                        .setPlusEarned(5_00L)
                        .setPlusSpent(null)
                        .setPlusAvailableAmount(1000_00L)
                );

        Assertions.assertThat(actualized.getOrderItems())
                .usingRecursiveFieldByFieldElementComparator(RecursiveComparisonConfiguration.builder()
                        .withComparedFields("totalCostWithoutPromo", "totalCost", "plusSpent", "plusEarned")
                        .build()
                )
                .containsExactlyInAnyOrder(
                        new OrderItem()
                                .setTotalCostWithoutPromo(400_00L)
                                .setTotalCost(50_00L)
                                .setPlusEarned(5_00L)
                                .setPlusSpent(null)
                );
    }

    private void mockOffers(List<OfferDto> offers) {
        //noinspection unchecked
        Mockito.when(offersDictionary.get(Mockito.any(List.class))).thenReturn(offers);
    }

    private void mockPlusBalance(String balance) {
        List<BoundPaymentMethod> boundPaymentMethods =
                balance != null
                        ? List.of(BoundPaymentMethod.builder()
                        .paymentMethod(PaymentUtil.PLUS_ACCOUNT_PREFIX)
                        .balance(balance)
                        .currency("ILS")
                        .build())
                        : List.of();
        Mockito.when(trustClient.getPaymentMethods(Mockito.any()))
                .thenReturn(GetPaymentMethodsResponse.builder()
                        .boundPaymentMethods(boundPaymentMethods)
                        .build()
                );
    }

    private void mockPlusEnabled(String enabled) {
        Mockito.when(blackboxApiClient.blackboxGet(
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any(),
                Mockito.any()
        ).schedule().join()).thenReturn(new BlackboxResponse().addUsersItem(
                new BlackboxUserinfoItem().attributes(
                        new BlackboxUserinfoItemAttributes()._1015(enabled)
                )
        ));
    }
}

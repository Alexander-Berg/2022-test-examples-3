package ru.yandex.market.loyalty.core.service.perks.impl;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.loyalty.api.model.perk.PerkType;
import ru.yandex.market.loyalty.core.dao.ydb.model.UserOrder;
import ru.yandex.market.loyalty.core.model.perk.PerkOwnership;
import ru.yandex.market.loyalty.core.service.ConfigurationService;
import ru.yandex.market.loyalty.core.service.YdbAllUserOrdersService;
import ru.yandex.market.loyalty.core.service.perks.Perk;
import ru.yandex.market.loyalty.core.service.perks.PerkProcessor;
import ru.yandex.market.loyalty.core.service.perks.StatusEnvironment;
import ru.yandex.market.loyalty.lightweight.OneTimeSupplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.loyalty.api.model.perk.PerkType.ORDER_YEAR_DELIVERED;

public class SearchForPrevYearOrdersPerkProcessorTest {

    SearchForPrevYearOrdersPerkProcessor processor;
    YdbAllUserOrdersService ydbAllUserOrdersServiceMock;

    @Before
    public void before() {
        ydbAllUserOrdersServiceMock = mock(YdbAllUserOrdersService.class);
        ConfigurationService configurationServiceMock = mock(ConfigurationService.class);
        processor = new SearchForPrevYearOrdersPerkProcessor(ydbAllUserOrdersServiceMock, configurationServiceMock);

        when(configurationServiceMock.perkOrderPrevYearDeliveredThreshold()).thenReturn(Optional.of(700_00L));
    }

    @Test
    public void hasProperOrdersNotEnoughAmount() {
        assertFalse(processor.hasProperOrders(
                Stream.concat(singleOrdersLower700.stream(), multiOrdersSumAbove700.stream())
                        .collect(Collectors.toSet())
        ));
    }

    @Test
    public void hasProperOrdersEnoughAmount() {
        assertTrue(processor.hasProperOrders(
                Stream.concat(singleOrdersAbove700.stream(), singleOrdersAbove700.stream())
                        .collect(Collectors.toSet())
        ));
    }

    @Test
    public void hasProperOrdersWithGroupEmpty() {
        assertFalse(
                processor.hasProperOrders(
                        processor.groupOrders(
                                List.of()
                        )));
    }

    @Test
    public void hasProperOrdersWithGroupEnoughAmount() {
        assertTrue(
                processor.hasProperOrders(
                        processor.groupOrders(
                                Stream.concat(
                                        singleOrdersLower700.stream()
                                                .flatMap(Collection::stream),
                                        multiOrdersSumAbove700.stream()
                                                .flatMap(Collection::stream)
                                )
                                        .collect(Collectors.toSet())
                        )));
    }

    @Test
    public void hasProperOrdersWithGroupNotEnoughAmount() {
        assertFalse(
                processor.hasProperOrders(
                        processor.groupOrders(
                                Stream.concat(
                                        singleOrdersLower700.stream()
                                                .flatMap(Collection::stream),
                                        multiOrdersLower700.stream()
                                                .flatMap(Collection::stream)
                                )
                                        .collect(Collectors.toSet())
                        )));
    }

    @Test
    public void fullAbove700() {
        PerkOwnership<?> perkOwnership = makeRequest(flatten(singleOrdersLower700, multiOrdersSumAbove700));

        assertEquals(PerkType.ORDER_YEAR_DELIVERED, perkOwnership.getType());
        assertTrue(perkOwnership.isPurchased());
    }

    @Test
    public void fullLower700() {
        PerkOwnership<?> perkOwnership = makeRequest(flatten(singleOrdersLower700, multiOrdersLower700));

        assertEquals(PerkType.ORDER_YEAR_DELIVERED, perkOwnership.getType());
        assertFalse(perkOwnership.isPurchased());
    }

    @Test
    public void fullEmpty() {
        PerkOwnership<?> perkOwnership = makeRequest(flatten(singleOrdersLower700, multiOrdersLower700));

        assertEquals(PerkType.ORDER_YEAR_DELIVERED, perkOwnership.getType());
        assertFalse(perkOwnership.isPurchased());
    }

    @NotNull
    private List<UserOrder> flatten(Collection<List<UserOrder>> left, Collection<List<UserOrder>> right) {
        return Stream.concat(
                left.stream()
                        .flatMap(Collection::stream),
                right.stream()
                        .flatMap(Collection::stream)
        )
                .collect(Collectors.toList());
    }


    @NotNull
    private PerkOwnership<?> makeRequest(List<UserOrder> mockResponse) {
        when(ydbAllUserOrdersServiceMock.getPreviousYearDeliveredOrders(100_00L)).thenReturn(mockResponse);
        StatusEnvironment perkEnvironment = new StatusEnvironment(
                100_00L, null, () -> 1, false, true, false, null, null, null, null,
                new OneTimeSupplier<>(List::of), Collections.emptyMap(), new OneTimeSupplier<>(Set::of),
                new OneTimeSupplier<>(() -> mockResponse)
        );
        return ((PerkProcessor.PerkProcessorResult) processor.processFeature(Perk.of(ORDER_YEAR_DELIVERED),
                perkEnvironment)).getPerkOwnership();
    }

    Collection<List<UserOrder>> multiOrdersSumAbove700 = List.of(
            List.of(
                    new UserOrder(null, null, Instant.now(), "9c81f50f-06cf-4982-84da-74687b76b50e,45405403", null,
                            300_00L)),
            List.of(new UserOrder(null, null, Instant.now(), "9c81f50f-06cf-4982-84da-74687b76b50e,454", null, 600_00L))
    );
    Collection<List<UserOrder>> multiOrdersLower700 = List.of(
            List.of(
                    new UserOrder(null, null, Instant.now(), "9c81f50f-06cf-4982-84da-74687b76b50e,45405403", null,
                            300_00L)),
            List.of(new UserOrder(null, null, Instant.now(), "9c81f50f-06cf-4982-84da-74687b76b50e,454", null, 200_00L))
    );

    Collection<List<UserOrder>> singleOrdersLower700 = List.of(
            List.of(new UserOrder(null, null, Instant.now(), "43895380", null, 100_00L)),
            List.of(new UserOrder(null, null, Instant.now(), "46327084", null, 200_00L)),
            List.of(new UserOrder(null, null, Instant.now(), "46079836", null, 400_00L)),
            List.of(new UserOrder(null, null, Instant.now(), "43997740", null, 500_00L))
    );


    Collection<List<UserOrder>> singleOrdersAbove700 = List.of(
            List.of(new UserOrder(null, null, Instant.now(), "43895380", null, 900_00L)),
            List.of(new UserOrder(null, null, Instant.now(), "46327084", null, 800_00L)),
            List.of(new UserOrder(null, null, Instant.now(), "46079836", null, 100_000L)),
            List.of(new UserOrder(null, null, Instant.now(), "43997740", null, 300_000L))
    );


}

package ru.yandex.market.logistics.nesu.service;

import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.dto.filter.ShopFilter;
import ru.yandex.market.logistics.nesu.model.entity.Shop;
import ru.yandex.market.logistics.nesu.service.shop.ShopService;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;

class ShopServiceTest extends AbstractContextualTest {

    @Autowired
    private ShopService shopService;

    @Test
    @DisplayName("Поиск магазинов DAAS магазинов с неотправленными заказами")
    @DatabaseSetup("/service/shop/prepare_database.xml")
    void findDaasShopsWithNonSentOrders() {
        List<Shop> shops = shopService.findDaasShopsWithNonSentOrders();
        softly.assertThat(shops).hasSize(1);
        softly.assertThat(shops.get(0).getId()).isEqualTo(1L);
    }

    @JpaQueriesCount(0)
    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("searchEmptySetArguments")
    @DisplayName("Передача пустой коллекции")
    @DatabaseSetup("/repository/sender/before/prepare_for_search.xml")
    void searchEmptySet(
        @SuppressWarnings("unused") String caseName,
        UnaryOperator<ShopFilter.ShopFilterBuilder> consumer
    ) {
        softly.assertThat(
            shopService.search(consumer.apply(ShopFilter.builder()).build(), Pageable.unpaged())
        ).hasSize(0);
    }

    @Nonnull
    private static Stream<Arguments> searchEmptySetArguments() {
        return Stream.<Pair<String, UnaryOperator<ShopFilter.ShopFilterBuilder>>>of(
            Pair.of(
                "По id магазина",
                builder -> builder.shopIds(Set.of())
            ),
            Pair.of(
                "По marketId магазина",
                builder -> builder.marketIds(Set.of())
            ),
            Pair.of(
                "По названию магазина",
                builder -> builder.shopNames(Set.of())
            ),
            Pair.of(
                "По businessId магазина",
                builder -> builder.businessIds(Set.of())
            ),
            Pair.of(
                "По статусу магазина",
                builder -> builder.statuses(Set.of())
            ),
            Pair.of(
                "По роли магазина",
                builder -> builder.roles(Set.of())
            )
        ).map(p -> Arguments.of(p.getLeft(), p.getRight()));
    }
}

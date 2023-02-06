package ru.yandex.market.logistics.tarifficator.repository;

import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.PickupPointDeliveryRuleStatus;
import ru.yandex.market.logistics.tarifficator.repository.shop.PickupPointDeliveryRuleRepository;

import static ru.yandex.market.logistics.tarifficator.util.TestUtils.PARAMETERIZED_TEST_DEFAULT_NAME;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
@DisplayName("Тесты на репозиторий работы с собственными тарифами доставки в пикпоинты")
public class PickupPointDeliveryRuleRepositoryTest extends AbstractContextualTest {

    @Autowired
    private PickupPointDeliveryRuleRepository tested;

    @DatabaseSetup("/repository/pickpoint-delivery-rule/findWithExistingPickpoints.before.xml")
    @ParameterizedTest(name = PARAMETERIZED_TEST_DEFAULT_NAME)
    @MethodSource("argumentsForHasPickupDelivery")
    @DisplayName("Есть ли пикап доставка у магазина")
    void hasPickupDelivery(String displayName, long shopId, boolean expectedResult) {
        softly.assertThat(tested.existsByStatusEqualsAndShopIdEquals(PickupPointDeliveryRuleStatus.ACTIVE, shopId))
            .isEqualTo(expectedResult);
    }

    @Test
    @DatabaseSetup("/repository/pickpoint-delivery-rule/findWithExistingPickpoints.before.xml")
    @DisplayName("Возврат магазинов, у которых есть пикап доставка")
    void getShopsHavingPickupDelivery() {
        softly.assertThat(tested.findShopsWithActiveRules(List.of(1L, 2L, 3L)))
            .contains(1L);
    }

    @Nonnull
    public static Stream<Arguments> argumentsForHasPickupDelivery() {
        return Stream.of(
            Arguments.of("Есть пикап доставка", 1, true),
            Arguments.of("Нет пикап доставки", 2, false)
        );
    }
}

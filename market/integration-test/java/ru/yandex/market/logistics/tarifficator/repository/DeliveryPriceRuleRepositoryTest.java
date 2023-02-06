package ru.yandex.market.logistics.tarifficator.repository;

import java.math.BigDecimal;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.model.shop.DeliveryPriceRule;
import ru.yandex.market.logistics.tarifficator.model.shop.DeliveryRuleId;
import ru.yandex.market.logistics.tarifficator.repository.shop.impl.DeliveryPriceRuleRepositoryImpl;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
public class DeliveryPriceRuleRepositoryTest extends AbstractContextualTest {

    @Autowired
    private DeliveryPriceRuleRepositoryImpl tested;

    @Test
    @DatabaseSetup("/repository/delivery-price-rule/deliveryPriceRuleRepository.before.xml")
    @ExpectedDatabase(
        value = "/repository/delivery-price-rule/groupWithPriceRules.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCreate() {
        tested.create(createRule((short) 1, BigDecimal.valueOf(0), BigDecimal.valueOf(500.50)));
        tested.create(createRule((short) 2, BigDecimal.valueOf(500.50), null));
    }

    @Test
    @DatabaseSetup("/repository/delivery-price-rule/groupWithPriceRules.xml")
    @ExpectedDatabase(
        value = "/repository/delivery-price-rule/deliveryPriceRuleRepository.before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testDeleteByRegionGroup() {
        tested.delete(101L);
    }

    @Test
    @DatabaseSetup("/repository/delivery-price-rule/groupWithPriceRules.xml")
    @ExpectedDatabase(
        value = "/repository/delivery-price-rule/deletePriceRuleById.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testDeleteById() {
        tested.delete(DeliveryRuleId.builder()
            .regionGroupId(101L)
            .orderNum((short) 1)
            .build()
        );
    }

    @Test
    @DatabaseSetup("/repository/delivery-price-rule/groupWithPriceRules.xml")
    @ExpectedDatabase(
        value = "/repository/delivery-price-rule/updatePriceRule.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdate() {
        tested.update(createRule((short) 1, BigDecimal.valueOf(1), BigDecimal.valueOf(500.40)));
    }

    @Test
    @DatabaseSetup("/repository/delivery-price-rule/groupWithPriceRules.xml")
    void testGet() {
        softly.assertThat(tested.find(101L))
            .isNotNull()
            .hasSize(2)
            .contains(createRule((short) 1, BigDecimal.valueOf(0), BigDecimal.valueOf(500.50)))
            .contains(createRule((short) 2, BigDecimal.valueOf(500.50), null));
    }

    @Nonnull
    private DeliveryPriceRule createRule(
        short id,
        BigDecimal priceFrom,
        BigDecimal priceTo
    ) {
        return DeliveryPriceRule.builder()
            .id(DeliveryRuleId.builder()
                .orderNum(id)
                .regionGroupId(101L)
                .build()
            )
            .priceFrom(priceFrom)
            .priceTo(priceTo)
            .build();
    }

}

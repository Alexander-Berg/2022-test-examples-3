package ru.yandex.market.logistics.tarifficator.repository;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.model.shop.DeliveryRuleId;
import ru.yandex.market.logistics.tarifficator.model.shop.DeliveryWeightRule;
import ru.yandex.market.logistics.tarifficator.repository.shop.impl.DeliveryWeightRuleRepositoryImpl;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
public class DeliveryWeightRuleRepositoryTest extends AbstractContextualTest {

    @Autowired
    private DeliveryWeightRuleRepositoryImpl tested;

    @Test
    @DatabaseSetup("/repository/delivery-weight-rule/deliveryWeightRuleRepository.before.xml")
    @ExpectedDatabase(
        value = "/repository/delivery-weight-rule/groupWithWeightRules.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCreate() {
        tested.create(createRule((short) 1, 0, 100));
        tested.create(createRule((short) 2, 100, null));
    }

    @Test
    @DatabaseSetup("/repository/delivery-weight-rule/groupWithWeightRules.xml")
    @ExpectedDatabase(
        value = "/repository/delivery-weight-rule/deliveryWeightRuleRepository.before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testDeleteByRegionGroup() {
        tested.delete(101L);
    }

    @Test
    @DatabaseSetup("/repository/delivery-weight-rule/groupWithWeightRules.xml")
    @ExpectedDatabase(
        value = "/repository/delivery-weight-rule/deleteWeightRuleById.after.xml",
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
    @DatabaseSetup("/repository/delivery-weight-rule/groupWithWeightRules.xml")
    @ExpectedDatabase(
        value = "/repository/delivery-weight-rule/updateWeightRule.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdate() {
        tested.update(createRule((short) 1, 1, 99));
    }

    @Test
    @DatabaseSetup("/repository/delivery-weight-rule/groupWithWeightRules.xml")
    void testGet() {
        softly.assertThat(tested.find(101L))
            .isNotNull()
            .hasSize(2)
            .contains(createRule((short) 1, 0, 100))
            .contains(createRule((short) 2, 100, null));
    }

    @Nonnull
    private DeliveryWeightRule createRule(
        short id,
        Integer weightFrom,
        Integer weightTo
    ) {
        return DeliveryWeightRule.builder()
            .id(DeliveryRuleId.builder()
                .orderNum(id)
                .regionGroupId(101L)
                .build()
            )
            .weightFrom(weightFrom)
            .weightTo(weightTo)
            .build();
    }
}

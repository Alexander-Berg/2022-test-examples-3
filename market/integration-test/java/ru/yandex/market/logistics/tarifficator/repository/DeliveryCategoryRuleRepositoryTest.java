package ru.yandex.market.logistics.tarifficator.repository;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.model.shop.CategoryId;
import ru.yandex.market.logistics.tarifficator.model.shop.DeliveryCategoryRule;
import ru.yandex.market.logistics.tarifficator.model.shop.DeliveryRuleId;
import ru.yandex.market.logistics.tarifficator.repository.shop.impl.DeliveryCategoryRuleRepositoryImpl;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
public class DeliveryCategoryRuleRepositoryTest extends AbstractContextualTest {

    @Autowired
    private DeliveryCategoryRuleRepositoryImpl tested;

    @Test
    @DatabaseSetup("/repository/delivery-category-rule/deliveryCategoryRuleRepository.before.xml")
    @ExpectedDatabase(
        value = "/repository/delivery-category-rule/groupWithCategoryRules.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCreateCategoryRule() {
        tested.create(createRule((short) 1, "A", "B"));
        tested.create(createRule((short) 2));
    }

    @Test
    @DatabaseSetup("/repository/delivery-category-rule/groupWithCategoryRules.xml")
    @ExpectedDatabase(
        value = "/repository/delivery-category-rule/deliveryCategoryRuleRepository.before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testDeleteByRegionGroup() {
        tested.delete(101L);
    }

    @Test
    @DatabaseSetup("/repository/delivery-category-rule/groupWithCategoryRules.xml")
    @ExpectedDatabase(
        value = "/repository/delivery-category-rule/deleteCategoryRuleById.after.xml",
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
    @DatabaseSetup("/repository/delivery-category-rule/groupWithCategoryRules.xml")
    @ExpectedDatabase(
        value = "/repository/delivery-category-rule/updateCategoryRule.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdateCategoryRule() {
        tested.update(createRule((short) 1, "C"));
    }

    @Test
    @DatabaseSetup("/repository/delivery-category-rule/groupWithCategoryRules.xml")
    @ExpectedDatabase(
        value = "/repository/delivery-category-rule/updateCategoryRuleNullifyCategories.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdateCategoryRuleNullifyCategories() {
        tested.update(createRule((short) 1));
    }

    @Test
    @DatabaseSetup("/repository/delivery-category-rule/groupWithCategoryRules.xml")
    void testGet() {
        softly.assertThat(tested.find(101L))
            .isNotNull()
            .hasSize(2)
            .contains(createRule((short) 1, "A", "B"))
            .contains(createRule((short) 2));
    }

    @Nonnull
    private DeliveryCategoryRule createRule(short id, String... categoryIds) {
        DeliveryCategoryRule.DeliveryCategoryRuleBuilder builder = DeliveryCategoryRule.builder()
            .id(DeliveryRuleId.builder()
                .orderNum(id)
                .regionGroupId(101L)
                .build()
            );

        if (categoryIds.length > 0) {
            Set<CategoryId> categoriesToBeAdd = new HashSet<>();

            for (String categoryId : categoryIds) {
                categoriesToBeAdd.add(CategoryId.builder()
                    .categoryId(categoryId)
                    .feedId(12L)
                    .build()
                );
            }

            builder.includes(categoriesToBeAdd);
        } else {
            builder.others(true);
        }

        return builder.build();
    }
}



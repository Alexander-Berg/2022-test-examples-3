package ru.yandex.market.logistics.tarifficator.repository;

import java.math.BigDecimal;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.DbUnitConfiguration;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import com.github.springtestdbunit.dataset.ReplacementDataSetLoader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistics.tarifficator.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.model.shop.DeliveryOption;
import ru.yandex.market.logistics.tarifficator.model.shop.DeliveryOptionGroup;
import ru.yandex.market.logistics.tarifficator.repository.shop.DeliveryOptionGroupRepository;

@DbUnitConfiguration(
    dataSetLoader = ReplacementDataSetLoader.class,
    databaseConnection = "dbUnitQualifiedDatabaseConnection"
)
@DatabaseSetup("/repository/option-group/deliveryOptionGroupRepository.before.xml")
public class DeliveryOptionGroupRepositoryTest extends AbstractContextualTest {

    @Autowired
    private DeliveryOptionGroupRepository tested;

    @Test
    @ExpectedDatabase(
        value = "/repository/option-group/createOptionGroup.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCreateOptionGroup() {
        tested.createOptionGroup(DeliveryOptionGroup.builder()
            .regionGroupId(101L)
            .categoryOrderNum((short) 1)
            .priceOrderNum((short) 2)
            .weightOrderNum((short) 3)
            .hasDelivery(true)
            .build()
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/option-group/updateOptionGroup.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdateOptionGroup() {
        tested.updateOptionGroup(DeliveryOptionGroup.builder()
            .id(1001L)
            .regionGroupId(102L)
            .weightOrderNum((short) 4)
            .hasDelivery(false)
            .build()
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/option-group/deleteOptionGroup.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testDeleteOptionGroup() {
        tested.deleteOptionGroups(102L);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/option-group/createOption.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testCreateOption() {
        tested.createOption(DeliveryOption.builder()
            .optionGroupId(1001L)
            .cost(BigDecimal.valueOf(700))
            .daysFrom((short) 5)
            .orderBeforeHour(23)
            .orderNum((short) 2)
            .build()
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/option-group/updateOption.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testUpdateOption() {
        tested.updateOption(DeliveryOption.builder()
            .optionGroupId(1001L)
            .cost(BigDecimal.valueOf(600))
            .daysTo((short) 10)
            .orderBeforeHour(22)
            .orderNum((short) 1)
            .build()
        );
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/option-group/deleteOption.after.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testDeleteOption() {
        tested.deleteOption(1001L, (short) 1);
    }

    @Test
    @ExpectedDatabase(
        value = "/repository/option-group/deliveryOptionGroupRepository.before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void testGetOptions() {
        softly.assertThat(tested.getOptions(102L))
            .isNotNull()
            .hasSize(1)
            .contains(DeliveryOptionGroup.builder()
                .regionGroupId(102L)
                .id(1001L)
                .categoryOrderNum((short) 1)
                .priceOrderNum((short) 2)
                .hasDelivery(true)
                .option(DeliveryOption.builder()
                    .optionGroupId(1001L)
                    .cost(BigDecimal.valueOf(525))
                    .daysFrom((short) 0)
                    .daysTo((short) 1)
                    .orderBeforeHour(13)
                    .orderNum((short) 1)
                    .build()
                )
                .build()
            );
    }

}

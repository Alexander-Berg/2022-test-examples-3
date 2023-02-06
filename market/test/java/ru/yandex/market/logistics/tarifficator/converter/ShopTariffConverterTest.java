package ru.yandex.market.logistics.tarifficator.converter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistics.tarifficator.AbstractUnitTest;
import ru.yandex.market.logistics.tarifficator.converter.shop.ShopTariffConverter;
import ru.yandex.market.logistics.tarifficator.model.enums.shop.CourierTariffType;
import ru.yandex.market.logistics.tarifficator.model.shop.CategoryId;
import ru.yandex.market.logistics.tarifficator.model.shop.DeliveryCategoryRule;
import ru.yandex.market.logistics.tarifficator.model.shop.DeliveryOption;
import ru.yandex.market.logistics.tarifficator.model.shop.DeliveryOptionGroup;
import ru.yandex.market.logistics.tarifficator.model.shop.DeliveryPriceRule;
import ru.yandex.market.logistics.tarifficator.model.shop.DeliveryRuleId;
import ru.yandex.market.logistics.tarifficator.model.shop.DeliveryTariff;
import ru.yandex.market.logistics.tarifficator.model.shop.DeliveryWeightRule;
import ru.yandex.market.logistics.tarificator.open.api.model.CategoryIdDto;
import ru.yandex.market.logistics.tarificator.open.api.model.CategoryRuleDto;
import ru.yandex.market.logistics.tarificator.open.api.model.DeliveryTariffDto;
import ru.yandex.market.logistics.tarificator.open.api.model.OptionDto;
import ru.yandex.market.logistics.tarificator.open.api.model.OptionGroupDto;
import ru.yandex.market.logistics.tarificator.open.api.model.PriceRuleDto;
import ru.yandex.market.logistics.tarificator.open.api.model.WeightRuleDto;

@DisplayName("Тест на конвертер собственных тарифов магазина")
class ShopTariffConverterTest extends AbstractUnitTest {

    private final ShopTariffConverter tested = new ShopTariffConverter();

    @Test
    @DisplayName("Тест конвертации из ДТО в модель")
    void testConvertDto() {
        softly.assertThat(tested.convertDto(1L, createTariffDto()))
            .usingRecursiveComparison()
            .isEqualTo(createTariffModel());
    }

    @Test
    @DisplayName("Тест конвертации из модели в ДТО")
    void testConvert() {
        softly.assertThat(tested.convertModel(createTariffModel()))
            .usingRecursiveComparison()
            .isEqualTo(createTariffDto());
    }

    @Nonnull
    private DeliveryTariffDto createTariffDto() {
        return new DeliveryTariffDto()
            .tariffType(CourierTariffType.WEIGHT_CATEGORY_PRICE)
            .notes("Notes")
            .useYml(false)
            .categoryRules(createCategoryRuleListDto())
            .priceRules(createPriceRuleListDto())
            .weightRules(createWeightRuleListDto())
            .optionsGroups(createOptionGroupListDto());
    }

    @Nonnull
    private List<OptionGroupDto> createOptionGroupListDto() {
        return List.of(
            new OptionGroupDto()
                .categoryOrderNum(1)
                .priceOrderNum(2)
                .weightOrderNum(4)
                .hasDelivery(true)
                .options(
                    List.of(
                        new OptionDto()
                            .cost(BigDecimal.valueOf(500.2))
                            .daysFrom(1)
                            .daysTo(2)
                            .orderBeforeHour(13)
                            .orderNum(1)
                    )
                ),
            new OptionGroupDto()
                .priceOrderNum(3)
                .weightOrderNum(5)
                .hasDelivery(false)
                .options(new ArrayList<>())
        );
    }

    @Nonnull
    private List<CategoryRuleDto> createCategoryRuleListDto() {
        CategoryRuleDto dto = new CategoryRuleDto()
            .others(false)
            .includes(
                List.of(
                    new CategoryIdDto()
                        .feedId(20L)
                        .categoryId("Cat1")
                )
            );
        dto.setOrderNum(1);

        return List.of(dto);
    }

    @Nonnull
    private List<PriceRuleDto> createPriceRuleListDto() {
        PriceRuleDto rule1 = new PriceRuleDto();
        rule1.setOrderNum(2);
        rule1.setPriceFrom(BigDecimal.valueOf(100.5));

        PriceRuleDto rule2 = new PriceRuleDto();
        rule2.setOrderNum(3);
        rule2.setPriceTo(BigDecimal.valueOf(1000.2));

        return List.of(rule1, rule2);
    }

    @Nonnull
    private List<WeightRuleDto> createWeightRuleListDto() {
        WeightRuleDto weightRuleDto1 = new WeightRuleDto();
        weightRuleDto1.setOrderNum(4);
        weightRuleDto1.setWeightFrom(10);

        WeightRuleDto weightRuleDto2 = new WeightRuleDto();
        weightRuleDto2.setOrderNum(5);
        weightRuleDto2.setWeightTo(1000);

        return List.of(weightRuleDto1, weightRuleDto2);
    }

    @Nonnull
    private DeliveryRuleId createRuleId(short orderNum) {
        return DeliveryRuleId.builder()
            .orderNum(orderNum)
            .regionGroupId(1L)
            .build();
    }

    @Nonnull
    private List<DeliveryCategoryRule> createCategoryRuleList() {
        return List.of(
            DeliveryCategoryRule.builder()
                .id(createRuleId((short) 1))
                .others(false)
                .includes(
                    Set.of(
                        CategoryId.builder()
                            .feedId(20L)
                            .categoryId("Cat1")
                            .build()
                    )
                )
                .build()
        );
    }

    @Nonnull
    private List<DeliveryPriceRule> createPriceRuleList() {
        return List.of(
            DeliveryPriceRule.builder()
                .id(createRuleId((short) 2))
                .priceFrom(BigDecimal.valueOf(100.5))
                .build(),
            DeliveryPriceRule.builder()
                .id(createRuleId((short) 3))
                .priceTo(BigDecimal.valueOf(1000.2))
                .build()
        );
    }

    @Nonnull
    private List<DeliveryWeightRule> createWeightRuleList() {
        return List.of(
            DeliveryWeightRule.builder()
                .id(createRuleId((short) 4))
                .weightFrom(10)
                .build(),
            DeliveryWeightRule.builder()
                .id(createRuleId((short) 5))
                .weightTo(1000)
                .build()
        );
    }

    @Nonnull
    private List<DeliveryOptionGroup> createdOptionGroupList() {
        return List.of(
            DeliveryOptionGroup.builder()
                .id(0L)
                .regionGroupId(1L)
                .categoryOrderNum((short) 1)
                .priceOrderNum((short) 2)
                .weightOrderNum((short) 4)
                .hasDelivery(true)
                .option(
                    DeliveryOption.builder()
                        .cost(BigDecimal.valueOf(500.2))
                        .daysFrom((short) 1)
                        .daysTo((short) 2)
                        .orderBeforeHour(13)
                        .orderNum((short) 1)
                        .build()
                )
                .build(),
            DeliveryOptionGroup.builder()
                .id(0L)
                .regionGroupId(1L)
                .priceOrderNum((short) 3)
                .weightOrderNum((short) 5)
                .hasDelivery(false)
                .build()
        );
    }

    @Nonnull
    private DeliveryTariff createTariffModel() {
        return DeliveryTariff.builder()
            .tariffType(CourierTariffType.WEIGHT_CATEGORY_PRICE)
            .optionsGroups(createdOptionGroupList())
            .categoryRules(createCategoryRuleList())
            .priceRules(createPriceRuleList())
            .weightRules(createWeightRuleList())
            .useYml(false)
            .notes("Notes")
            .build();
    }
}

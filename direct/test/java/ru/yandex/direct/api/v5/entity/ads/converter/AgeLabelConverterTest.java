package ru.yandex.direct.api.v5.entity.ads.converter;

import java.util.Map;
import java.util.function.Consumer;

import javax.xml.bind.JAXBElement;

import com.yandex.direct.api.v5.ads.AgeLabelEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.Age;
import ru.yandex.direct.core.entity.banner.model.BabyFood;
import ru.yandex.direct.core.entity.banner.model.BannerFlags;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;
import static ru.yandex.direct.api.v5.entity.ads.converter.AgeLabelConverter.convertAgeLabel;

@RunWith(Parameterized.class)
public class AgeLabelConverterTest {

    @Parameter
    public String desc;

    @Parameter(1)
    public BannerFlags internalValue;

    @Parameter(2)
    public Consumer<JAXBElement<AgeLabelEnum>> checkExpectations;

    @Parameters(name = "{0}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {"age 0", new BannerFlags().with(BannerFlags.AGE, Age.AGE_0), isEqual(AgeLabelEnum.AGE_0)},
                {"age 6", new BannerFlags().with(BannerFlags.AGE, Age.AGE_6), isEqual(AgeLabelEnum.AGE_6)},
                {"age 12", new BannerFlags().with(BannerFlags.AGE, Age.AGE_12), isEqual(AgeLabelEnum.AGE_12)},
                {"age 16", new BannerFlags().with(BannerFlags.AGE, Age.AGE_16), isEqual(AgeLabelEnum.AGE_16)},
                {"age 18", new BannerFlags().with(BannerFlags.AGE, Age.AGE_18), isEqual(AgeLabelEnum.AGE_18)},

                {"age age0", new BannerFlags().withFlags(Map.of("age", "age0")), isEqual(AgeLabelEnum.AGE_0)},
                {"age age6", new BannerFlags().withFlags(Map.of("age", "age6")), isEqual(AgeLabelEnum.AGE_6)},
                {"age age12", new BannerFlags().withFlags(Map.of("age", "age12")), isEqual(AgeLabelEnum.AGE_12)},
                {"age age16", new BannerFlags().withFlags(Map.of("age", "age16")), isEqual(AgeLabelEnum.AGE_16)},
                {"age age18", new BannerFlags().withFlags(Map.of("age", "age18")), isEqual(AgeLabelEnum.AGE_18)},

                {"baby food 0", new BannerFlags().with(BannerFlags.BABY_FOOD, BabyFood.BABY_FOOD_0),
                        isEqual(AgeLabelEnum.MONTHS_0)},
                {"baby food 1", new BannerFlags().with(BannerFlags.BABY_FOOD, BabyFood.BABY_FOOD_1),
                        isEqual(AgeLabelEnum.MONTHS_1)},
                {"baby food 2", new BannerFlags().with(BannerFlags.BABY_FOOD, BabyFood.BABY_FOOD_2),
                        isEqual(AgeLabelEnum.MONTHS_2)},
                {"baby food 3", new BannerFlags().with(BannerFlags.BABY_FOOD, BabyFood.BABY_FOOD_3),
                        isEqual(AgeLabelEnum.MONTHS_3)},
                {"baby food 4", new BannerFlags().with(BannerFlags.BABY_FOOD, BabyFood.BABY_FOOD_4),
                        isEqual(AgeLabelEnum.MONTHS_4)},
                {"baby food 5", new BannerFlags().with(BannerFlags.BABY_FOOD, BabyFood.BABY_FOOD_5),
                        isEqual(AgeLabelEnum.MONTHS_5)},
                {"baby food 6", new BannerFlags().with(BannerFlags.BABY_FOOD, BabyFood.BABY_FOOD_6),
                        isEqual(AgeLabelEnum.MONTHS_6)},
                {"baby food 7", new BannerFlags().with(BannerFlags.BABY_FOOD, BabyFood.BABY_FOOD_7),
                        isEqual(AgeLabelEnum.MONTHS_7)},
                {"baby food 8", new BannerFlags().with(BannerFlags.BABY_FOOD, BabyFood.BABY_FOOD_8),
                        isEqual(AgeLabelEnum.MONTHS_8)},
                {"baby food 9", new BannerFlags().with(BannerFlags.BABY_FOOD, BabyFood.BABY_FOOD_9),
                        isEqual(AgeLabelEnum.MONTHS_9)},
                {"baby food 10", new BannerFlags().with(BannerFlags.BABY_FOOD, BabyFood.BABY_FOOD_10),
                        isEqual(AgeLabelEnum.MONTHS_10)},
                {"baby food 11", new BannerFlags().with(BannerFlags.BABY_FOOD, BabyFood.BABY_FOOD_11),
                        isEqual(AgeLabelEnum.MONTHS_11)},
                {"baby food 12", new BannerFlags().with(BannerFlags.BABY_FOOD, BabyFood.BABY_FOOD_12),
                        isEqual(AgeLabelEnum.MONTHS_12)},

                {"baby food baby_food0", new BannerFlags().withFlags(Map.of("baby_food", "baby_food0")),
                        isEqual(AgeLabelEnum.MONTHS_0)},
                {"baby food baby_food1", new BannerFlags().withFlags(Map.of("baby_food", "baby_food1")),
                        isEqual(AgeLabelEnum.MONTHS_1)},
                {"baby food baby_food2", new BannerFlags().withFlags(Map.of("baby_food", "baby_food2")),
                        isEqual(AgeLabelEnum.MONTHS_2)},
                {"baby food baby_food3", new BannerFlags().withFlags(Map.of("baby_food", "baby_food3")),
                        isEqual(AgeLabelEnum.MONTHS_3)},
                {"baby food baby_food4", new BannerFlags().withFlags(Map.of("baby_food", "baby_food4")),
                        isEqual(AgeLabelEnum.MONTHS_4)},
                {"baby food baby_food5", new BannerFlags().withFlags(Map.of("baby_food", "baby_food5")),
                        isEqual(AgeLabelEnum.MONTHS_5)},
                {"baby food baby_food6", new BannerFlags().withFlags(Map.of("baby_food", "baby_food6")),
                        isEqual(AgeLabelEnum.MONTHS_6)},
                {"baby food baby_food7", new BannerFlags().withFlags(Map.of("baby_food", "baby_food7")),
                        isEqual(AgeLabelEnum.MONTHS_7)},
                {"baby food baby_food8", new BannerFlags().withFlags(Map.of("baby_food", "baby_food8")),
                        isEqual(AgeLabelEnum.MONTHS_8)},
                {"baby food baby_food9", new BannerFlags().withFlags(Map.of("baby_food", "baby_food9")),
                        isEqual(AgeLabelEnum.MONTHS_9)},
                {"baby food baby_food10", new BannerFlags().withFlags(Map.of("baby_food", "baby_food10")),
                        isEqual(AgeLabelEnum.MONTHS_10)},
                {"baby food baby_food11", new BannerFlags().withFlags(Map.of("baby_food", "baby_food11")),
                        isEqual(AgeLabelEnum.MONTHS_11)},
                {"baby food baby_food12", new BannerFlags().withFlags(Map.of("baby_food", "baby_food12")),
                        isEqual(AgeLabelEnum.MONTHS_12)},

                {"several supported flags",
                        new BannerFlags().with(BannerFlags.AGE, Age.AGE_0).with(BannerFlags.BABY_FOOD,
                                BabyFood.BABY_FOOD_12), isEqual(AgeLabelEnum.AGE_0)},
                {"not supported flag", new BannerFlags().with(BannerFlags.ALCOHOL, true), isNil()},
                {"no flags at all", new BannerFlags(), isNil()},
                {"null", null, isNil()},
        };
    }

    private static Consumer<JAXBElement<AgeLabelEnum>> isNil() {
        return elem -> assertThat(elem.isNil()).isTrue();
    }

    private static Consumer<JAXBElement<AgeLabelEnum>> isEqual(AgeLabelEnum expected) {
        return elem -> assertThat(elem.getValue()).isEqualByComparingTo(expected);
    }

    @Test
    public void test() {
        checkExpectations.accept(convertAgeLabel(internalValue));
    }
}


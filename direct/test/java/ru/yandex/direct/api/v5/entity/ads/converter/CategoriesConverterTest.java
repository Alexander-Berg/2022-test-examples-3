package ru.yandex.direct.api.v5.entity.ads.converter;

import java.util.function.Consumer;

import javax.xml.bind.JAXBElement;

import com.yandex.direct.api.v5.ads.AdCategoryEnum;
import com.yandex.direct.api.v5.ads.ArrayOfAdCategoryEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.BannerFlags;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;
import static ru.yandex.direct.api.v5.entity.ads.converter.CategoriesConverter.convertCategories;
import static ru.yandex.direct.core.entity.banner.model.Age.AGE_18;
import static ru.yandex.direct.core.entity.banner.model.BabyFood.BABY_FOOD_11;

@RunWith(Parameterized.class)
public class CategoriesConverterTest {

    @Parameter
    public String desc;

    @Parameter(1)
    public BannerFlags internalValue;

    @Parameter(2)
    public Consumer<JAXBElement<ArrayOfAdCategoryEnum>> checkExpectations;

    @Parameters(name = "{0}")
    public static Object[][] getParameters() {
        return new Object[][]{
                {"null", null, isNil()},
                {"no categories at all", new BannerFlags(), isNil()},
                {"not supported category: age 18", new BannerFlags().with(BannerFlags.AGE, AGE_18), isNil()},
                {"not supported category: asocial", new BannerFlags().with(BannerFlags.ASOCIAL, true), isNil()},
                {"not supported category: forex", new BannerFlags().with(BannerFlags.FOREX, true), isNil()},
                {"not supported category: plus 18", new BannerFlags().with(BannerFlags.PLUS18, true), isNil()},
                {"not supported category: tragic", new BannerFlags().with(BannerFlags.TRAGIC, true), isNil()},

                {"abortion", new BannerFlags().with(BannerFlags.ABORTION, true), isEqual(AdCategoryEnum.ABORTION)},
                {"alcohol", new BannerFlags().with(BannerFlags.ALCOHOL, true), isEqual(AdCategoryEnum.ALCOHOL)},
                {"baby food", new BannerFlags().with(BannerFlags.BABY_FOOD, BABY_FOOD_11),
                        isEqual(AdCategoryEnum.BABY_FOOD)},
                {"dietary supplements", new BannerFlags().with(BannerFlags.DIETARYSUPPL, true),
                        isEqual(AdCategoryEnum.DIETARY_SUPPLEMENTS)},
                {"med equipment", new BannerFlags().with(BannerFlags.MED_EQUIPMENT, true),
                        isEqual(AdCategoryEnum.MEDICINE)},
                {"med services", new BannerFlags().with(BannerFlags.MED_SERVICES, true),
                        isEqual(AdCategoryEnum.MEDICINE)},
                {"medicine", new BannerFlags().with(BannerFlags.MEDICINE, true), isEqual(AdCategoryEnum.MEDICINE)},
                {"pharmacy", new BannerFlags().with(BannerFlags.PHARMACY, true), isEqual(AdCategoryEnum.MEDICINE)},
                {"project declaration", new BannerFlags().with(BannerFlags.PROJECT_DECLARATION, true),
                        isEqual(AdCategoryEnum.PROJECT_DECLARATION)},
                {"pseudoweapon", new BannerFlags().with(BannerFlags.PSEUDOWEAPON, true),
                        isEqual(AdCategoryEnum.PSEUDO_WEAPON)},
                {"tobacco", new BannerFlags().with(BannerFlags.TOBACCO, true), isEqual(AdCategoryEnum.TOBACCO)},
                {"several medicine categories",
                        new BannerFlags().with(BannerFlags.MED_EQUIPMENT, true).with(BannerFlags.MED_SERVICES, true)
                                .with(BannerFlags.MEDICINE, true).with(BannerFlags.PHARMACY, true),
                        isEqual(AdCategoryEnum.MEDICINE)},
                {"all supported categories",
                        new BannerFlags().with(BannerFlags.ABORTION, true).with(BannerFlags.AGE, AGE_18)
                                .with(BannerFlags.ALCOHOL, true).with(BannerFlags.ASOCIAL, true)
                                .with(BannerFlags.BABY_FOOD, BABY_FOOD_11).with(BannerFlags.DIETARYSUPPL, true)
                                .with(BannerFlags.FOREX, true).with(BannerFlags.MED_EQUIPMENT, true)
                                .with(BannerFlags.MED_SERVICES, true).with(BannerFlags.MEDICINE, true)
                                .with(BannerFlags.PHARMACY, true).with(BannerFlags.PLUS18, true)
                                .with(BannerFlags.PROJECT_DECLARATION, true).with(BannerFlags.PSEUDOWEAPON, true)
                                .with(BannerFlags.TOBACCO, true).with(BannerFlags.TRAGIC, true),
                        isEqual(AdCategoryEnum.ABORTION, AdCategoryEnum.ALCOHOL, AdCategoryEnum.BABY_FOOD,
                                AdCategoryEnum.DIETARY_SUPPLEMENTS, AdCategoryEnum.MEDICINE,
                                AdCategoryEnum.PROJECT_DECLARATION, AdCategoryEnum.PSEUDO_WEAPON,
                                AdCategoryEnum.TOBACCO)},
        };
    }

    private static Consumer<JAXBElement<ArrayOfAdCategoryEnum>> isNil() {
        return elem -> assertThat(elem.isNil()).isTrue();
    }

    private static Consumer<JAXBElement<ArrayOfAdCategoryEnum>> isEqual(AdCategoryEnum... expected) {
        return elem -> assertThat(elem.getValue().getItems()).containsExactlyInAnyOrder(expected);
    }

    @Test
    public void test() {
        checkExpectations.accept(convertCategories(internalValue));
    }
}

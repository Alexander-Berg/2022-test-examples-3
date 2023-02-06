package ru.yandex.direct.core.testing.data;

import ru.yandex.direct.core.entity.banner.model.Age;
import ru.yandex.direct.core.entity.banner.model.BabyFood;
import ru.yandex.direct.core.entity.banner.model.BannerFlags;

public class TestBanerFlags {
    public static BannerFlags empty() {
        return new BannerFlags();
    }

    public static BannerFlags babyFood(Integer value) {
        return empty().with(BannerFlags.BABY_FOOD, BabyFood.fromTypedValue(value.toString()));
    }

    public static BannerFlags age(Integer value) {
        return empty().with(BannerFlags.AGE, Age.fromSource(value.toString()));
    }

    public static BannerFlags alcohol() {
        return empty().with(BannerFlags.ALCOHOL, true);
    }
}

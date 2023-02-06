package ru.yandex.direct.core.copyentity;

import java.util.List;

import org.junit.Test;

import ru.yandex.direct.core.entity.banner.model.BannerWithAdGroupId;
import ru.yandex.direct.core.entity.banner.model.TextBanner;
import ru.yandex.direct.model.Entity;

import static org.assertj.core.api.Assertions.assertThat;

public class TypeUtilsTest {

    private interface ExampleInterface {}

    private interface FirstInterface extends ExampleInterface {}

    private interface SecondInterface extends ExampleInterface {}

    private static class FinalClass implements FirstInterface, SecondInterface {}

    @Test
    public void findAncestorsImplementingExample() {
        List<Class<? extends ExampleInterface>> result = TypeUtils.findAncestorsImplementing(FinalClass.class, ExampleInterface.class);
        assertThat(result).containsExactlyInAnyOrder(FirstInterface.class, SecondInterface.class);
    }

    @Test
    public void findAncestorsImplementingBanner() {
        List<Class<? extends Entity>> result = TypeUtils.findAncestorsImplementing(TextBanner.class, Entity.class);
        assertThat(result).containsExactlyInAnyOrder(BannerWithAdGroupId.class);
    }
}

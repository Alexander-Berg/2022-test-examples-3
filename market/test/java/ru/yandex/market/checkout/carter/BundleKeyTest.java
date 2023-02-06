package ru.yandex.market.checkout.carter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.carter.model.BundleKey;
import ru.yandex.market.checkout.carter.model.MskuBundleItem;
import ru.yandex.market.checkout.carter.model.WareMd5BundleItem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItem;

public class BundleKeyTest {

    @Test
    public void shouldNotDependsFromOrderWithMskuKey() {
        Set<BundleKey> keys = new HashSet<>();

        keys.add(new BundleKey(Arrays.asList(
                MskuBundleItem.of(generateItem("some_offer")),
                MskuBundleItem.of(generateItem("some gift"))
        )));

        keys.add(new BundleKey(Arrays.asList(
                MskuBundleItem.of(generateItem("some gift")),
                MskuBundleItem.of(generateItem("some_offer"))
        )));

        assertThat(keys, hasSize(1));
    }

    @Test
    public void shouldNotDependsFromOrderWithWareMd5Key() {
        Set<BundleKey> keys = new HashSet<>();

        keys.add(new BundleKey(Arrays.asList(
                WareMd5BundleItem.of(generateItem("some_offer")),
                WareMd5BundleItem.of(generateItem("some gift"))
        )));

        keys.add(new BundleKey(Arrays.asList(
                WareMd5BundleItem.of(generateItem("some gift")),
                WareMd5BundleItem.of(generateItem("some_offer"))
        )));

        assertThat(keys, hasSize(1));
    }
}

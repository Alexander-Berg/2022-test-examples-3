package ru.yandex.market.checkout.carter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import ru.yandex.market.checkout.carter.model.CartItem;
import ru.yandex.market.checkout.carter.model.CartItemComparator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItem;
import static ru.yandex.market.checkout.carter.utils.CarterTestUtils.generateItemWithBundle;

public class CartItemsComparatorTest {

    @Test
    public void shouldOrderItemsInBundlesTogether() {
        List<CartItem> items = Arrays.asList(
                generateItem("some feed offer"),
                generateItemWithBundle("some bundle offer", "some bundle"),
                generateItem("some toy offer"),
                generateItemWithBundle("some gift offer", "some bundle"),
                generateItem("some another offer"),
                generateItemWithBundle("another bundle offer", "another bundle"),
                generateItem("and another offer"),
                generateItemWithBundle("another gift offer", "another bundle")
        );

        int count = 1000;

        while (count-- > 0) {
            Collections.shuffle(items, ThreadLocalRandom.current());

            assertThat(items.stream()
                    .sorted(new CartItemComparator(items))
                    .collect(Collectors.toList()), contains(
                    generateItem("some feed offer"),
                    generateItemWithBundle("some bundle offer", "some bundle"),
                    generateItemWithBundle("some gift offer", "some bundle"),
                    generateItem("some toy offer"),
                    generateItem("some another offer"),
                    generateItemWithBundle("another bundle offer", "another bundle"),
                    generateItemWithBundle("another gift offer", "another bundle"),
                    generateItem("and another offer")
            ));
        }
    }

    @Test
    public void shouldOrderItemsInBundlesByPrimaryKeyDesc() {
        List<CartItem> items = Arrays.asList(
                generateItem("some feed offer"),
                generateItemWithBundle("some gift offer", "some bundle"),
                generateItem("some toy offer"),
                generateItemWithBundle("some bundle offer", "some bundle", true),
                generateItem("some another offer"),
                generateItemWithBundle("another bundle offer", "another bundle", true),
                generateItem("and another offer"),
                generateItemWithBundle("another gift offer", "another bundle")
        );
        int count = 1000;

        while (count-- > 0) {
            Collections.shuffle(items, ThreadLocalRandom.current());

            assertThat(items.stream()
                    .sorted(new CartItemComparator(items))
                    .collect(Collectors.toList()), contains(
                    generateItem("some feed offer"),
                    generateItemWithBundle("some bundle offer", "some bundle"),
                    generateItemWithBundle("some gift offer", "some bundle"),
                    generateItem("some toy offer"),
                    generateItem("some another offer"),
                    generateItemWithBundle("another bundle offer", "another bundle"),
                    generateItemWithBundle("another gift offer", "another bundle"),
                    generateItem("and another offer")
            ));
        }
    }

    @Test
    public void shouldOrderItemsWithoutBundlesByCreationTime() {
        List<CartItem> items = Arrays.asList(
                generateItem("some feed offer"),
                generateItem("some toy offer"),
                generateItem("some another offer"),
                generateItem("and another offer")
        );

        int count = 1000;

        while (count-- > 0) {
            Collections.shuffle(items, ThreadLocalRandom.current());

            assertThat(items.stream()
                    .sorted(new CartItemComparator(items))
                    .collect(Collectors.toList()), contains(
                    generateItem("some feed offer"),
                    generateItem("some toy offer"),
                    generateItem("some another offer"),
                    generateItem("and another offer")
            ));
        }
    }
}

package ru.yandex.market.api.internal.cataloger;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.api.domain.catalog.NavigationCategoryV1;
import ru.yandex.market.api.matchers.ImageMatcher;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.Collections;
import java.util.List;

public class NavigationTreeV1ParserTest {
    @Test
    public void imageParse() {
        List<NavigationCategoryV1> categories = parse("tree-with-image.xml");

        Assert.assertThat(categories, Matchers.hasSize(1));

        NavigationCategoryV1 category = categories.get(0);

        Assert.assertEquals(59603, category.getId());
        Assert.assertThat(
            category.getImage(),
            cast(
                ImageMatcher.image(
                    "https://avatars.mdst.yandex.net/get-mpic/4868/cms",
                    582,
                    604
                )
            )
        );

    }

    private List<NavigationCategoryV1> parse(String filename) {
        return new NavigationTreeV1Parser(Collections.emptyList())
            .parse(ResourceHelpers.getResource(filename));
    }

    private static <T> Matcher<T> cast(Matcher<?> matcher) {
        return (Matcher<T>) matcher;
    }


}

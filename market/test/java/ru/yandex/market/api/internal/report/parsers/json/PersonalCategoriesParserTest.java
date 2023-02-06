package ru.yandex.market.api.internal.report.parsers.json;

import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.Arrays;

/**
 * Created by vivg on 28.07.16.
 */
public class PersonalCategoriesParserTest extends UnitTestBase {

    @Test
    public void usualUsage() {
        IntList result = new PersonalCategoriesParser().parse(
            ResourceHelpers.getResource("personal-categories.json")
        );
        Assert.assertEquals(Arrays.asList(56179, 56036, 54545, 56034, 54544, 56235, 54965, 56199, 54618, 54726), result);
    }
}

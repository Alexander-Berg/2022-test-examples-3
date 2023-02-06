package ru.yandex.market.crm.external.contentapi;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class NavCategoryIdsExtractorTest {

    static String content;

    private NavCategoryIdsExtractor parser = new NavCategoryIdsExtractor();


    @BeforeAll
    public static void init() throws Exception {
        content = IOUtils.toString(
                NavCategoryIdsExtractorTest.class.getResourceAsStream("nav-sub-categories.json"),
                "UTF-8"
        );
    }

    @Test
    public void test() {
        IntSet res = parser.parse(content.getBytes());
        Assertions.assertEquals(
                new IntOpenHashSet(new int[]{60969, 60988, 60990, 60992, 60994, 60996}),
                res
        );
    }
}

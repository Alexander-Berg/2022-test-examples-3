package ru.yandex.market.api.util.parser;

import org.junit.Test;

import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.server.version.Version;
import ru.yandex.market.api.util.parser.Enums.MultiEnum;

import java.util.Arrays;

import static org.junit.Assert.*;
import static ru.yandex.market.api.test.TestHelp.assertContains;

/**
 *
 * Created by apershukov on 14.11.16.
 */
public class MultiEnumTest extends UnitTestBase {

    private enum Enum1 {
        FIRST_ITEM(1),
        SECOND_ITEM(2),
        THIRD_ITEM(3);

        private final int code;

        Enum1(int code) {
            this.code = code;
        }
    }

    private enum Enum2 {
        RED, GREEN, BLUE
    }

    @Test
    public void testBuildSimpleMultiEnum() {
        MultiEnum<?> multiEnum = Enums.multiEnumBuilder()
                .add(Enum1.class)
                .build();

        assertEquals(Enum1.FIRST_ITEM, multiEnum.valueOf("FIRST_ITEM"));
        assertEquals(Enum1.SECOND_ITEM, multiEnum.valueOf("SECOND_ITEM"));
        assertEquals(Enum1.THIRD_ITEM, multiEnum.valueOf("THIRD_ITEM"));

        assertContains(Arrays.asList(Enum1.FIRST_ITEM, Enum1.SECOND_ITEM, Enum1.THIRD_ITEM),
                multiEnum.aliasOf(Version.V1_0_0, "ALL"));
    }

    @Test
    public void testBuildMultiEnumWithPrefix() {
        MultiEnum<?> multiEnum = Enums.multiEnumBuilder()
                .add(Enum1.class)
                .add("PREFIX", Enum2.class)
                .build();

        assertEquals(Enum1.SECOND_ITEM, multiEnum.valueOf("SECOND_ITEM"));
        assertEquals(Enum2.RED, multiEnum.valueOf("PREFIX_RED"));
        assertEquals(Enum2.BLUE, multiEnum.valueOf("PREFIX_BLUE"));

        assertContains(Arrays.asList(Enum1.FIRST_ITEM, Enum1.SECOND_ITEM, Enum1.THIRD_ITEM,
                Enum2.RED, Enum2.GREEN, Enum2.BLUE),
                multiEnum.aliasOf(Version.V1_0_0, "ALL"));

        assertContains(Arrays.asList(Enum2.RED, Enum2.GREEN, Enum2.BLUE),
                multiEnum.aliasOf(Version.V1_0_0, "PREFIX_ALL"));
    }

    @Test
    public void testBuildMultienumSetWithCustomView() {
        MultiEnum<?> multiEnum = Enums.<Enum1> multiEnumBuilder()
                .setRootView(item -> String.valueOf(item.code))
                .add(Enum1.class)
                .build();

        assertEquals(Enum1.FIRST_ITEM, multiEnum.valueOf("1"));
        assertEquals(Enum1.SECOND_ITEM, multiEnum.valueOf("2"));
        assertEquals(Enum1.THIRD_ITEM, multiEnum.valueOf("3"));

        assertContains(Arrays.asList(Enum1.FIRST_ITEM, Enum1.SECOND_ITEM, Enum1.THIRD_ITEM),
                multiEnum.aliasOf(Version.V1_0_0, "ALL"));
    }

    @Test(expected = IllegalStateException.class)
    public void testBuildMultienumWithDublicateViews() {
        Enums.<Enum1> multiEnumBuilder()
                .setRootView(item -> "1")
                .add(Enum1.class)
                .build();
    }
}
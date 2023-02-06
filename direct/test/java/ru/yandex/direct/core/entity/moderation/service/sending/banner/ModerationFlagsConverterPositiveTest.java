package ru.yandex.direct.core.entity.moderation.service.sending.banner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import ru.yandex.direct.core.entity.banner.model.BannerFlags;
import ru.yandex.modadvert.bigmod.protos.interfaces.MarkupFlags;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;
import static ru.yandex.modadvert.bigmod.protos.interfaces.MarkupFlags.EMarkupFlag.AGE0;
import static ru.yandex.modadvert.bigmod.protos.interfaces.MarkupFlags.EMarkupFlag.AGE12;
import static ru.yandex.modadvert.bigmod.protos.interfaces.MarkupFlags.EMarkupFlag.AGE18;
import static ru.yandex.modadvert.bigmod.protos.interfaces.MarkupFlags.EMarkupFlag.ANNOYING;
import static ru.yandex.modadvert.bigmod.protos.interfaces.MarkupFlags.EMarkupFlag.ASOCIAL;
import static ru.yandex.modadvert.bigmod.protos.interfaces.MarkupFlags.EMarkupFlag.BABY_FOOD0;
import static ru.yandex.modadvert.bigmod.protos.interfaces.MarkupFlags.EMarkupFlag.BABY_FOOD8;
import static ru.yandex.modadvert.bigmod.protos.interfaces.MarkupFlags.EMarkupFlag.MEDICINE;
import static ru.yandex.modadvert.bigmod.protos.interfaces.MarkupFlags.EMarkupFlag.YA_PAGES_ANYWHERE;

@RunWith(Parameterized.class)
public class ModerationFlagsConverterPositiveTest {

    private final ModerationFlagsAliases aliases = new ModerationFlagsAliases();
    private final ModerationFlagsConverter converter = new ModerationFlagsConverter(aliases);

    @Parameterized.Parameter
    public String sourceFlags;

    @Parameterized.Parameter(1)
    public List<String> whiteList;

    @Parameterized.Parameter(2)
    public List<MarkupFlags.EMarkupFlag> expectedParsedFlags;

    @Parameterized.Parameters(name = "flags = {0}, whiteList = {1}")
    public static List<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                {
                        "",
                        List.of(),
                        List.of()
                },

                // flags with value aliases
                {
                        "age:0",
                        List.of("age"),
                        List.of(AGE0)
                },
                {
                        "age:18",
                        List.of("age", "baby_food"),
                        List.of(AGE18)
                },
                {
                        "age:age0",
                        List.of("age", "baby_food", "unknown0"),
                        List.of(AGE0)
                },
                {
                        "age:age18",
                        List.of("age"),
                        List.of(AGE18)
                },

                {
                        "baby_food:0",
                        List.of("baby_food"),
                        List.of(BABY_FOOD0)
                },
                {
                        "baby_food:8",
                        List.of("baby_food"),
                        List.of(BABY_FOOD8)
                },
                {
                        "baby_food:baby_food0",
                        List.of("baby_food"),
                        List.of(BABY_FOOD0)
                },
                {
                        "baby_food:baby_food8",
                        List.of("baby_food"),
                        List.of(BABY_FOOD8)
                },

                // flags without value aliases
                {
                        "ya_pages:ya_anywhere",
                        List.of("ya_pages"),
                        List.of(YA_PAGES_ANYWHERE)
                },

                // flags with name aliases
                {
                        "plus18",
                        List.of("plus18"),
                        List.of(AGE18)
                },
                {
                        "plus18:1",
                        List.of("plus18"),
                        List.of(AGE18)
                },
                {
                        "plus18:1",
                        List.of("plus18", "baby_food"),
                        List.of(AGE18)
                },

                // flags without values
                {
                        "asocial",
                        List.of("asocial"),
                        List.of(ASOCIAL)
                },
                {
                        "medicine",
                        List.of("asocial", "medicine"),
                        List.of(MEDICINE)
                },

                // flags with default values
                {
                        "asocial:1",
                        List.of("asocial", "medicine"),
                        List.of(ASOCIAL)
                },
                {
                        "medicine:1",
                        List.of("asocial", "medicine"),
                        List.of(MEDICINE)
                },

                // filtering by white list
                {
                        "",
                        List.of(),
                        List.of()
                },
                {
                        "age:18",
                        List.of(),
                        List.of()
                },
                {
                        "age:0",
                        List.of("baby_food"),
                        List.of()
                },
                {
                        "asocial:1,baby_food:8,age:age18",
                        List.of("baby_food", "age"),
                        List.of(BABY_FOOD8, AGE18)
                },

                // filtering by white list: skip unknown keys
                {
                        "ololo",
                        List.of("baby_food", "age"),
                        List.of()
                },
                {
                        "ololo,baby_food:8,age:age18",
                        List.of("baby_food", "age"),
                        List.of(BABY_FOOD8, AGE18)
                },

                // filtering by white list: skip unknown values with aliases
                {
                        "baby_food:47",
                        List.of("age"),
                        List.of()
                },
                {
                        "baby_food:47,age:age18",
                        List.of("age"),
                        List.of(AGE18)
                },

                // filtering by white list: skip unknown values without aliases
                {
                        "asocial:123,age:age18",
                        List.of("age"),
                        List.of(AGE18)
                },

                // combo
                {
                        "asocial:1,baby_food:8,age:age18",
                        List.of("asocial", "baby_food", "age"),
                        List.of(ASOCIAL, BABY_FOOD8, AGE18)
                },
                {
                        "medicine:1,annoying,baby_food:8,ya_pages:ya_anywhere",
                        List.of("medicine", "annoying", "baby_food", "ya_pages", "age"),
                        List.of(MEDICINE, ANNOYING, BABY_FOOD8, YA_PAGES_ANYWHERE)
                },
                {
                        "medicine:1,annoying,plus18",
                        List.of("medicine", "annoying", "plus18"),
                        List.of(MEDICINE, ANNOYING, AGE18)
                },
                {
                        "medicine:1,annoying,plus18,age:12",
                        List.of("medicine", "annoying", "plus18", "age"),
                        List.of(MEDICINE, ANNOYING, AGE18, AGE12)
                },
                {
                        "medicine:1,plus18,annoying,age:12",
                        List.of("medicine", "annoying", "plus18", "age"),
                        List.of(MEDICINE, AGE18, ANNOYING, AGE12)
                },
                {
                        "medicine,plus18:1,annoying,ya_pages:ya_anywhere,age:12",
                        List.of("medicine", "annoying", "plus18", "age", "ya_pages"),
                        List.of(MEDICINE, AGE18, ANNOYING, YA_PAGES_ANYWHERE, AGE12)
                }
        });
    }

    @Test
    public void convertFlags() {
        Set<Integer> expectedFlagNumbers =
                new HashSet<>(mapList(expectedParsedFlags, MarkupFlags.EMarkupFlag::getNumber));
        List<Integer> actual = converter.convertFlags(BannerFlags.fromSource(sourceFlags), new HashSet<>(whiteList));
        assertThat(new HashSet<>(actual)).isEqualTo(expectedFlagNumbers);
    }
}

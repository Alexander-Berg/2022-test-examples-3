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

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@RunWith(Parameterized.class)
public class ModerationFlagsConverterNegativeTest {

    private static final Set<String> allTestFlags = Set.of("age", "ya_pages", "ololo", "baby_food");

    private final ModerationFlagsAliases aliases = new ModerationFlagsAliases();
    private final ModerationFlagsConverter converter = new ModerationFlagsConverter(aliases);

    @Parameterized.Parameter
    public String sourceFlags;

    @Parameterized.Parameter(1)
    public List<MarkupFlags.EMarkupFlag> expectedParsedFlags;

    @Parameterized.Parameters(name = "flags = {0}")
    public static List<Object[]> testData() {
        return Arrays.asList(new Object[][]{
                // flags with aliases, known names, unknown values
                {
                        "age",
                        emptyList()
                },
                {
                        "age:",
                        emptyList()
                },
                {
                        "age:1",
                        emptyList()
                },

                // flags with aliases, known names, different values
                {
                        "age:baby_food0",
                        emptyList()
                },

                // flags without aliases, known names, unknown values
                {
                        "ya_pages",
                        emptyList()
                },
                {
                        "ya_pages:",
                        emptyList()
                },
                {
                        "ya_pages:y",
                        emptyList()
                },

                // flags without aliases, known names, different values
                {
                        "ya_pages:baby_food0",
                        emptyList()
                },

                // unknown flag names
                {
                        "ololo:6",
                        emptyList()
                },
                {
                        "ololo:12",
                        emptyList()
                },

                // combo (invalid + invalid)
                {
                        "ololo:1,age:age90,ya_pages",
                        emptyList()
                },

                // combo (invalid + valid)
                {
                        "age:age6,ya_pages",
                        List.of(MarkupFlags.EMarkupFlag.AGE6)
                },
                {
                        "ya_pages,age:age6,baby_food:47",
                        List.of(MarkupFlags.EMarkupFlag.AGE6)
                },
                {
                        "age:age6,minus_region_ru,minus_region_kz",
                        List.of(MarkupFlags.EMarkupFlag.AGE6)
                },
        });
    }

    @Test
    public void returnEmptyResultWhenInvalidFlags() {
        Set<Integer> expectedFlagNumbers =
                new HashSet<>(mapList(expectedParsedFlags, MarkupFlags.EMarkupFlag::getNumber));
        List<Integer> actual = converter.convertFlags(BannerFlags.fromSource(sourceFlags), allTestFlags);
        assertThat(new HashSet<>(actual)).isEqualTo(expectedFlagNumbers);
    }
}

package ru.yandex.direct.grid.core.entity;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableSet;
import one.util.streamex.StreamEx;
import org.junit.Rule;
import org.junit.rules.ExpectedException;
import org.junit.runners.Parameterized;

import ru.yandex.direct.grid.core.entity.group.service.GridAdGroupConstants;
import ru.yandex.direct.utils.Counter;

import static java.util.Collections.emptySet;

@ParametersAreNonnullByDefault
public class AdGroupIdInFilterBaseTest {

    @Parameterized.Parameters(name = "{4}")
    public static Collection<Object[]> parameters() {
        Set<Long> campaignIdIn = ImmutableSet.of(1L, 2L);
        Set<Long> adGroupIdIn = ImmutableSet.of(11L, 22L);

        Counter counter = new Counter();
        Set<Long> adGroupIdOverLimit = StreamEx.generate(counter::next)
                .limit(GridAdGroupConstants.getMaxGroupRows() + 1)
                .map(Long::valueOf)
                .toSet();

        return Arrays.asList(new Object[][]{
                {null, campaignIdIn, null, false, "adGroupIdIn = null; expectedAdGroupIdIn = null"},
                {emptySet(), campaignIdIn, emptySet(), false, "adGroupIdIn is empty; expectedAdGroupIdIn is empty"},
                {adGroupIdIn, campaignIdIn, adGroupIdIn, false,
                        String.format("adGroupIdIn = %s; expectedAdGroupIdIn = %s", adGroupIdIn, adGroupIdIn)},
                {adGroupIdOverLimit, campaignIdIn, null, false,
                        "adGroupIdIn is over limit; expectedAdGroupIdIn is empty"},
                {adGroupIdOverLimit, null, null, true,
                        "adGroupIdIn is over limit and campaignIdIn is null; expected method throw exception"},
        });
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Parameterized.Parameter
    public Set<Long> adGroupIdIn;

    @Parameterized.Parameter(1)
    public Set<Long> campaignIdIn;

    @Parameterized.Parameter(2)
    public Set<Long> expectedAdGroupIdIn;

    @Parameterized.Parameter(3)
    public boolean expectedException;

    @Parameterized.Parameter(4)
    public String description;

}

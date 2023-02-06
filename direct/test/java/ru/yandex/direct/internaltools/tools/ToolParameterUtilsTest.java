package ru.yandex.direct.internaltools.tools;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ru.yandex.direct.internaltools.utils.ToolParameterUtils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class ToolParameterUtilsTest {
    private static final String ASSERT_MSG = "Ответ соответствует ожидаемому";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void checkGetCampaignIds() {
        Set<Long> campaignIds = ToolParameterUtils.getLongIdsFromString("    1    12,   123,,,,   1234   ,   ");
        assertThat(ASSERT_MSG, campaignIds, equalTo(ImmutableSet.of(1L, 12L, 123L, 1234L)));
    }

    @Test
    public void checkGetNumberFormatException_WhenGetInvalidCampaignIds() {
        thrown.expect(NumberFormatException.class);
        ToolParameterUtils.getLongIdsFromString("1, a123, 456");
    }

    @Test
    public void checkParseCommaSeparatedString() {
        Set<String> logins = ToolParameterUtils.parseCommaSeparatedString("    a    ab,   AbC,,,,   aB.Cd   ,   Ab-Cd");
        assertThat(ASSERT_MSG, logins,
                equalTo(ImmutableSet.of("a", "ab", "AbC", "aB.Cd", "Ab-Cd")));
    }

    @Test
    public void checkParseLogins() {
        Set<String> logins = ToolParameterUtils.parseLogins("    a    ab,   AbC,,,,   aB.Cd   ,   Ab-Cd");
        assertThat(ASSERT_MSG, logins, equalTo(ImmutableSet.of("a", "ab", "abc", "ab-cd")));
    }

    @Test
    public void checkParseLongLongMap() {
        var result = ToolParameterUtils.parseLongLongMap("45 7 \r\n32,77\n40, 40\n");
        assertThat(ASSERT_MSG, result, equalTo(Map.of(45L, 7L, 32L, 77L, 40L, 40L)));
    }

    @Test
    public void checkParseLongLongMapIllegalStateException_whenCollision() {
        thrown.expect(IllegalStateException.class);
        ToolParameterUtils.parseLongLongMap("45 7\r\n45 8");
    }

    @Test
    public void checkParseLongLongLinkedMap() {
        var result = ToolParameterUtils.parseLongLongLinkedMap("6051 1 \r\n5987,0\n555, 555,|22 22\n");
        var expectedMap = Map.of(
                5987L, 0L,
                6051L, 1L,
                555L, 555L,
                22L, 22L);
        assertThat(ASSERT_MSG, result, equalTo(expectedMap));
    }

    @Test
    public void checkParseLongLongLinkedMapIllegalStateException_whenCollision() {
        thrown.expect(IllegalStateException.class);
        ToolParameterUtils.parseLongLongLinkedMap("45 7\r\n45 8");
    }
}

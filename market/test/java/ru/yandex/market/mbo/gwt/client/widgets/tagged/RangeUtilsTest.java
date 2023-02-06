package ru.yandex.market.mbo.gwt.client.widgets.tagged;

import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.tagged.Range;
import ru.yandex.market.mbo.gwt.utils.RangeUtils;
import ru.yandex.market.mbo.gwt.utils.Replacement;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * @author Alexander Kramarev (https://staff.yandex-team.ru/pochemuto/)
 * @date 28.09.2015
 */
@SuppressWarnings({"NonJREEmulationClassesInClientCode", "checkstyle:lineLength", "checkstyle:magicNumber"})
public class RangeUtilsTest {
    private static final Pattern RANGE_PATTERN = Pattern.compile("[^-]+");

    @Test
    public void testIntersectOrBeside() throws Exception {
        assertTrue(RangeUtils.isIntersectOrBeside(range("---one---"), range("two------")));
        assertTrue(RangeUtils.isIntersectOrBeside(range("---one---"), range("------two")));
        assertTrue(RangeUtils.isIntersectOrBeside(range("---one---"), range("---two---")));
        assertTrue(RangeUtils.isIntersectOrBeside(range("---one---"), range("----two--")));
        assertTrue(RangeUtils.isIntersectOrBeside(range("---one---"), range("--two----")));

        assertFalse(RangeUtils.isIntersectOrBeside(range("---one---"), range("no-------")));
        assertFalse(RangeUtils.isIntersectOrBeside(range("---one---"), range("-------no--")));
    }

    @Test
    public void testIntersect() throws Exception {
        assertFalse(RangeUtils.isIntersect(range("---one---"), range("two------")));
        assertFalse(RangeUtils.isIntersect(range("---one---"), range("------two")));
        assertTrue(RangeUtils.isIntersect(range("---one---"), range("---two---")));
        assertTrue(RangeUtils.isIntersect(range("---one---"), range("----two--")));
        assertTrue(RangeUtils.isIntersect(range("---one---"), range("--two----")));

        assertFalse(RangeUtils.isIntersect(range("---one---"), range("no-------")));
        assertFalse(RangeUtils.isIntersect(range("---one---"), range("-------no--")));
    }

    @Test
    public void testBeside() throws Exception {
        assertFalse(RangeUtils.isBeside(range("123----"),
                range("----567")));

        assertFalse(RangeUtils.isBeside(range("----567"),
                range("123----")));

        assertFalse(RangeUtils.isBeside(range("123----"),
                range("1234---")));

        assertFalse(RangeUtils.isBeside(range("1234---"),
                range("123----")));

        assertTrue(RangeUtils.isBeside(range("---2344"),
                range("-23----")));

        assertTrue(RangeUtils.isBeside(range("-23----"),
                range("---4567")));
    }

    @Test
    public void testUnion() throws Exception {
        assertEquals(range("123456---"), RangeUtils.union(range("123------"), range("---456")));
        assertEquals(range("--123456"), RangeUtils.union(range("--123456"), range("--123---")));
        assertEquals(range("123456789"), RangeUtils.union(range("123------"), range("------789")));
    }

    @Test
    public void testContains() throws Exception {
        assertTrue(RangeUtils.contains(range("123456"), range("123456")));
        assertTrue(RangeUtils.contains(range("123456"), range("1234--")));
        assertTrue(RangeUtils.contains(range("123456"), range("1234--")));
        assertTrue(RangeUtils.contains(range("123456"), range("--34--")));
        assertTrue(RangeUtils.contains(range("--34--"), range("--34--")));
        assertTrue(RangeUtils.contains(range("--34--"), range("--3---")));

        assertFalse(RangeUtils.contains(range("--34--"), range("--345-")));
        assertFalse(RangeUtils.contains(range("123456"), range("1234567")));
    }

    @Test
    public void testRemove() throws Exception {
        assertThat(RangeUtils.remove(range("123------"), range("-----6789")), is(rangesOf("123------")));
        assertThat(RangeUtils.remove(range("123------"), range("---456789")), is(rangesOf("123------")));
        assertThat(RangeUtils.remove(range("1234-----"), range("---456789")), is(rangesOf("123------")));
        assertThat(RangeUtils.remove(range("--345----"), range("-23456---")), is(rangesOf("---------")));
        assertThat(RangeUtils.remove(range("123456789"), range("---456---")), is(rangesOf("123---789")));
        assertThat(RangeUtils.remove(range("123456789"), range("12345----")), is(rangesOf("-----6789")));
        assertThat(RangeUtils.remove(range("------78901"), range("---4567890")), is(rangesOf("----------1")));
    }

    @Test
    public void testToggleRangeNoRemoveNoJoin() throws Exception {
        // removing
        assertThat(RangeUtils.toggleRange("Lorem ipsum dolor sit amet", rangesOf("Lorem-------dolor"), range("------ipsum-----"), false, false), is(rangesOf("Lorem-ipsum-dolor")));
        assertThat(RangeUtils.toggleRange("Lorem ipsum dolor sit amet", rangesOf("Lorem-ipsum-dolor"), range("------ipsum-----"), false, false), is(rangesOf("Lorem-------dolor")));
        assertThat(RangeUtils.toggleRange("Lorem ipsum dolor sit amet", rangesOf("Lorem-ipsum-dolor"), range("--------sum-----"), false, false), is(rangesOf("Lorem-ip----dolor")));
        // adding
        assertThat(RangeUtils.toggleRange("Lorem ipsum dolor sit amet", rangesOf("Lor--------------"), range("---em ipsu------"), false, false), is(rangesOf("Lorem ipsu-------")));
        assertThat(RangeUtils.toggleRange("Lorem ipsum dolor sit amet", rangesOf("Lorem-ipsum-dolor"), range("---em ipsu------"), false, false), is(rangesOf("Lorem ipsum-dolor")));
        assertThat(RangeUtils.toggleRange("Lorem ipsum dolor sit amet", rangesOf("Lorem-ipsum-dolor"), range("---em ipsum do--"), false, false), is(rangesOf("Lorem ipsum dolor")));
        assertThat(RangeUtils.toggleRange("Lorem ipsum dolor sit amet", rangesOf("Lorem-ipsum-dolor"), range("-------ps-------"), false, false), is(rangesOf("Lorem-i--um-dolor")));
    }


    @Test
    public void testToggleRangeRemoveNoJoin() throws Exception {
        // removing
        assertThat(RangeUtils.toggleRange("Lorem ipsum dolor sit amet", rangesOf("Lorem-------dolor"), range("------ipsum-----"), true, false), is(rangesOf("Lorem-ipsum-dolor")));
        assertThat(RangeUtils.toggleRange("Lorem ipsum dolor sit amet", rangesOf("Lorem-ipsum-dolor"), range("------ipsum-----"), true, false), is(rangesOf("Lorem-------dolor")));
        assertThat(RangeUtils.toggleRange("Lorem ipsum dolor sit amet", rangesOf("Lorem-ipsum-dolor"), range("--------sum-----"), true, false), is(rangesOf("Lorem-ip----dolor")));
        assertThat(RangeUtils.toggleRange("Lorem ipsum dolor sit amet", rangesOf("Lore-------------"), range("---em ipsu------"), true, false), is(rangesOf("Lor--------------")));

        // adding
        assertThat(RangeUtils.toggleRange("Lorem ipsum dolor sit amet", rangesOf("Lor-------m------"), range("---em ipsu------"), true, false), is(rangesOf("Lorem ipsum------")));
    }

    @Test
    public void testToggleRangeRemoveNoJoinIntersect() throws Exception {
        assertThat(RangeUtils.toggleRange("Lorem ipsum dolor sit amet", rangesOf("Lorem-ipsum-dolor"), range("---em ipsu------"), true, false), is(rangesOf("Lor-------m-dolor")));
    }

    @Test
    public void testToggleRangeWithJoin() throws Exception {
        assertThat(RangeUtils.toggleRange("Lorem ipsum dolor sit amet", rangesOf("-----------------"), range("Lorem------------"), true, true), is(rangesOf("Lorem------------")));
        assertThat(RangeUtils.toggleRange("Lorem ipsum dolor sit amet", rangesOf("-----------------"), range("------ipsum------"), true, true), is(rangesOf("------ipsum------")));
        assertThat(RangeUtils.toggleRange("Lorem ipsum dolor sit amet", rangesOf("-----------------"), range("------------dolor"), true, true), is(rangesOf("------------dolor")));
        assertThat(RangeUtils.toggleRange("Lorem ipsum dolor sit amet", rangesOf("Lorem-------dolor"), range("------ipsum------"), true, true), is(rangesOf("Lorem ipsum dolor")));

        Set<Range> value = rangesOf("Lorem ipsum-----------amet");
        Set<Range> actual = RangeUtils.toggleRange("Lorem ipsum dolor sit amet", rangesOf("Lorem ipsum-------sit amet"), range("------------------sit----"), false, true);
        assertThat(actual, is(value));
    }

    @Test
    public void testToggleWithJoinAndSpaces() throws Exception {
        assertThat(RangeUtils.toggleRange("Lorem ipsum dolor", rangesOf("Lorem----um dolor"), range("------ipsum"), false, true), is(rangesOf("Lorem ipsum dolor")));
        assertThat(RangeUtils.toggleRange("Lorem ipsum dolor", rangesOf("Lorem-----m dolor"), range("------ipsum"), false, true), is(rangesOf("Lorem ipsum dolor")));
        assertThat(RangeUtils.toggleRange("Lorem ipsum dolor", rangesOf("Lorem------ dolor"), range("------ipsum"), false, true), is(rangesOf("Lorem ipsum dolor")));
        assertThat(RangeUtils.toggleRange("Lorem ipsum dolor", rangesOf("Lorem ips---dolor"), range("------ipsum"), false, true), is(rangesOf("Lorem ipsum dolor")));
        assertThat(RangeUtils.toggleRange("Lorem ipsum dolor", rangesOf("Lorem ip----dolor"), range("------ipsum"), false, true), is(rangesOf("Lorem ipsum dolor")));
        assertThat(RangeUtils.toggleRange("Lorem ipsum dolor", rangesOf("Lorem i-----dolor"), range("------ipsum"), false, true), is(rangesOf("Lorem ipsum dolor")));
        assertThat(RangeUtils.toggleRange("Lorem ipsum dolor", rangesOf("Lorem ------dolor"), range("------ipsum"), false, true), is(rangesOf("Lorem ipsum dolor")));
        assertThat(RangeUtils.toggleRange("Lorem ipsum dolor", rangesOf("Lorem ipsum-dolor"), range("------ipsum"), false, true), is(rangesOf("Lorem-------dolor")));
        assertThat(RangeUtils.toggleRange("Lorem ipsum dolor", rangesOf("Lorem-ipsum dolor"), range("------ipsum"), false, true), is(rangesOf("Lorem-------dolor")));
    }

    @Test
    public void testToggleRangeWithJoinRemove() throws Exception {
        assertThat("remove in middle with join", RangeUtils.toggleRange("Lorem ipsum dolor sit amet", rangesOf("Lorem ipsum dolor"), range("------ipsum------"), true, true), is(rangesOf("Lorem-------dolor")));
        assertThat("remove in head with join",   RangeUtils.toggleRange("Lorem ipsum dolor sit amet", rangesOf("Lorem ipsum dolor"), range("Lorem------------"), true, true), is(rangesOf("------ipsum dolor")));
        assertThat("remove in tail with join",   RangeUtils.toggleRange("Lorem ipsum dolor sit amet", rangesOf("Lorem ipsum dolor"), range("------------dolor"), true, true), is(rangesOf("Lorem ipsum------")));
    }

    @Test
    public void testExpand() throws Exception {
        assertThat(RangeUtils.expand("Lorem ipsum dolor", range("Lorem")), is(range("Lorem ")));
        assertThat(RangeUtils.expand("Lorem     ipsum dolor", range("Lorem")), is(range("Lorem     ")));
        assertThat(RangeUtils.expand("Lorem ipsum dolor", range("------------dolor")), is(range("----------- dolor")));
        assertThat(RangeUtils.expand("Lorem ipsum    dolor", range("---------------dolor")), is(range("-----------    dolor")));
        assertThat(RangeUtils.expand("Lorem ipsum dolor", range("------ipsum------")), is(range("----- ipsum -----")));
        assertThat(RangeUtils.expand("Lorem           ipsum   dolor", range("----------------ipsum--------")), is(range("-----           ipsum   -----")));
    }

    @Test
    public void testShift() throws Exception {
        assertThat(RangeUtils.shift(range("--##---"), 0), is(range("--##---")));
        assertThat(RangeUtils.shift(range("--##---"), 2), is(range("----##")));
        assertThat(RangeUtils.shift(range("----###"), -3), is(range("-###---")));
    }

    @Test
    public void testDiffAddText() throws Exception {
        Range range = range("---Text-----");
        Replacement replacement = replacement("---Text", "TextAdded");
        assertThat(RangeUtils.applyDiff(range, replacement), is(range("---TextAdded")));

        range = range("---Text-----");
        replacement = replacement("----ext", "extAdded");
        assertThat("include text replaced in range", RangeUtils.applyDiff(range, replacement), is(range("---TextAdded")));

        range = range("---Text-----");
        replacement = replacement("----ex-", "E##X");
        assertThat("include text replaced in range", RangeUtils.applyDiff(range, replacement), is(range("---TE##Xt")));

        range = range("---Text-----");
        replacement = new Replacement(7, 0, "After");
        assertThat("include text added after range", RangeUtils.applyDiff(range, replacement), is(range("---textAfter")));

        range = range("---Text-----");
        replacement = new Replacement(7, 1, "After");
        assertThat("do not include text in range if text was replaced after range", RangeUtils.applyDiff(range, replacement), is(range("---test")));

        range = range("---Text-----");
        replacement = new Replacement(3, 0, "Before");
        assertThat("include text added before range", RangeUtils.applyDiff(range, replacement), is(range("---BeforeText")));

        range = range("---Text-----");
        replacement = replacement("--#", "##");
        assertThat("do not include text added before range with replacement", RangeUtils.applyDiff(range, replacement), is(range("----Text")));

        range = range("---Text-----");
        replacement = replacement("##-", "######");
        assertThat("shift right", RangeUtils.applyDiff(range, replacement), is(range("-------Text")));
    }

    @Test
    public void testDiffRemoveText() throws Exception {
        Range range = range("---test-----");
        Replacement replacement = replacement("---test", "");
        assertThat("removing whole range from it start leaves empty range", RangeUtils.applyDiff(range, replacement).isEmpty(), is(true));

        range = range("---test-----");
        replacement = replacement("---with_extra_text--", "");
        assertThat("removing whole range with extra chars removes range", RangeUtils.applyDiff(range, replacement), is(nullValue()));

        range = range("---test-----");
        replacement = replacement("--with_extra_text--", "");
        assertThat("removing whole range with extra chars removes range", RangeUtils.applyDiff(range, replacement), is(nullValue()));

        range = range("---test-----");
        replacement = replacement("###", "##");
        assertThat("shift left", RangeUtils.applyDiff(range, replacement), is(range("--test")));

        range = range("---test-----");
        replacement = replacement("###", "");
        assertThat("removing all text before range", RangeUtils.applyDiff(range, replacement), is(range("test")));

    }

    @Test
    public void testRangesOf() throws Exception {

        Set<Range> ranges = rangesOf("-test-----");

        assertEquals(1, ranges.size());
        assertEquals(new Range(1, 4), ranges.iterator().next());

        ranges = rangesOf("--test--another--one");
        assertEquals(3, ranges.size());
        assertEquals(new HashSet<>(Arrays.asList(new Range(2, 4), new Range(8, 7), new Range(17, 3))), ranges);

        ranges = rangesOf("------------dolor");
        assertEquals(1, ranges.size());
        assertEquals(new Range(12, 5), ranges.iterator().next());

        assertThat(Range.withBounds(1, 10), is(Range.withBounds(1, 10)));
        assertThat(Range.withBounds(1, 10), is(not(Range.withBounds(1, 11))));
        assertThat(Range.withBounds(1, 10), is(not(Range.withBounds(2, 10))));
    }

    @Test
    public void testReplacement() throws Exception {
        Replacement replacement = replacement("---test-------", "");
        assertThat(replacement.getStart(), is(3));
        assertThat(replacement.getLength(), is(4));
        assertThat(replacement.getText(), is(""));

        replacement = replacement("-----test-------", "text");
        assertThat(replacement.getStart(), is(5));
        assertThat(replacement.getLength(), is(4));
        assertThat(replacement.getText(), is("text"));

        replacement = replacement("old_text-------", "another text");
        assertThat(replacement.getStart(), is(0));
        assertThat(replacement.getLength(), is(8));
        assertThat(replacement.getText(), is("another text"));
    }

    private static Set<Range> rangesOf(String pattern) {
        Set<Range> ranges = new HashSet<>();
        Matcher matcher = RANGE_PATTERN.matcher(pattern);
        while (matcher.find()) {
            ranges.add(Range.withBounds(matcher.start(), matcher.end()));
        }
        return ranges;
    }

    private static String dump(Range range) {
        return dump(Collections.singleton(range));
    }

    private static String dump(Set<Range> ranges) {
        String pattern = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Innumerabilia dici possunt in hanc sententiam";
        //noinspection ReplaceAllDot
        String result = pattern.replaceAll(".", "-");
        for (Range range : ranges) {
            result = result.substring(0, range.getStart()) + pattern.substring(range.getStart(), range.getEnd())
                    + result.substring(range.getEnd(), result.length());
        }
        return result;
    }

    private static Range range(String pattern) {
        Set<Range> ranges = rangesOf(pattern);
        if (ranges.size() != 1) {
            throw new IllegalArgumentException("pattern must have exactly one range. " + ranges.size() + " found.");
        }
        return ranges.iterator().next();
    }

    private static Replacement replacement(String pattern, String newText) {
        Range range = range(pattern);
        return new Replacement(range.getStart(), range.getLength(), newText);
    }
}

package ru.yandex.market.mbo.db.tagged;

import org.junit.Test;
import ru.yandex.market.mbo.gwt.models.tagged.Range;
import ru.yandex.market.mbo.gwt.models.tagged.TagType;
import ru.yandex.market.mbo.gwt.models.tagged.TaggedQuery;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Alexander Kramarev (https://staff.yandex-team.ru/pochemuto/)
 * @date 22.09.2015
 */
@SuppressWarnings("checkstyle:magicNumber")
public class TaggedQueryServiceTest {
    @Test
    public void testExtractTag() throws Exception {
        TaggedQuery query = new TaggedQuery();
        query.setQuery("test string that and that");

        TaggedQueryService.extractTag(query, "string", TagType.CATEGORY, 0, true);

        assertThat(query.getTags(), hasSize(1));
        assertThat(query.getTags().get(0).getTagType(), is(TagType.CATEGORY));
        assertThat(query.getTags().get(0).getRanges(), hasSize(1));
        assertThat(query.getTags().get(0).getRanges(), is(Collections.singleton(new Range(5, 6))));

        query = new TaggedQuery();
        query.setQuery("test string that and that");
        TaggedQueryService.extractTag(query, "that", TagType.PARAMETER, 0, true);
        TaggedQueryService.extractTag(query, "that", TagType.PARAMETER, 1, true);
        assertThat(query.getTags(), hasSize(2));
        assertThat(query.getTags().get(0).getRanges(), is(Collections.singleton(new Range(12, 4))));
        assertThat(query.getTags().get(1).getRanges(), is(Collections.singleton(new Range(21, 4))));
    }

    @Test
    public void testExtractTagMultipleRanges() throws Exception {
        TaggedQuery query = new TaggedQuery();
        query.setQuery("купить ботинки балдинини женские");

        TaggedQueryService.extractTag(query, "ботинки | женские", TagType.CATEGORY, 0, true);
        assertThat(query.getTags(), hasSize(1));
        assertThat(query.getTags().get(0).getRanges(), hasSize(2));
        assertThat(query.getTags().get(0).getRanges(), contains(new Range(7, 7), new Range(25, 7)));
    }

    @Test
    public void testMultiple() throws Exception {
        TaggedQuery query = new TaggedQuery();
        query.setQuery("коляску baby care");
        TaggedQueryService.extractTag(query, "коляску", TagType.CATEGORY, 0, true);
        TaggedQueryService.extractTag(query, "baby", TagType.PARAMETER, 0, true);
        TaggedQueryService.extractTag(query, "care", TagType.PARAMETER, 0, true);

        assertThat(query.getTags().get(0).getRanges(), contains(new Range(0, 7)));
        assertThat(query.getTags().get(1).getRanges(), contains(new Range(8, 4)));
        assertThat(query.getTags().get(2).getRanges(), contains(new Range(13, 4)));
    }

    @Test(expected = RuntimeException.class)
    public void testExtractTagError() throws Exception {
        TaggedQuery query = new TaggedQuery();
        query.setQuery("test string that and this");
        TaggedQueryService.extractTag(query, "that", TagType.PARAMETER, 1, true);
    }

    @Test(expected = RuntimeException.class)
    public void testExtractTagIntersectError() throws Exception {
        TaggedQuery query = new TaggedQuery();
        query.setQuery("джинсы женские лето");
        TaggedQueryService.extractTag(query, "джинсы женские|женские", TagType.PARAMETER, 1, true);
    }

    @Test
    public void testExtractIntersectAtEnd() throws Exception {
        TaggedQuery query = new TaggedQuery();
        query.setQuery("чехол для планшета samsung galaxy s");

        TaggedQueryService.extractTag(query, "чехол для планшета", TagType.CATEGORY, 0, true);
        assertThat(query.getTags().get(0).getRanges(), contains(new Range(0, 18)));

        TaggedQueryService.extractTag(query, "samsung", TagType.PARAMETER, 0, true);
        assertThat(query.getTags().get(1).getRanges(), contains(new Range(19, 7)));

        TaggedQueryService.extractTag(query, "galaxy", TagType.PARAMETER, 0, true);
        assertThat(query.getTags().get(2).getRanges(), contains(new Range(27, 6)));

        TaggedQueryService.extractTag(query, "s", TagType.PARAMETER, 0, true);
        assertThat(query.getTags().get(3).getRanges(), contains(new Range(34, 1)));
    }

    @Test
    public void testExtractIntersect() throws Exception {
        TaggedQuery query = new TaggedQuery();
        query.setQuery("парогенератор пар");

        TaggedQueryService.extractTag(query, "парогенератор", TagType.CATEGORY, 0, true);
        assertThat(query.getTags().get(0).getRanges(), contains(new Range(0, 13)));

        TaggedQueryService.extractTag(query, "пар", TagType.PARAMETER, 0, true);
        assertThat(query.getTags().get(1).getRanges(), contains(new Range(14, 3)));
    }

    @Test
    public void testExtractNumberIntesect() throws Exception {
        TaggedQuery query = new TaggedQuery();
        query.setQuery("ванна 180х80");

        TaggedQueryService.extractTag(query, "ванна", TagType.CATEGORY, 0, true);
        assertThat(query.getTags().get(0).getRanges(), contains(new Range(0, 5)));

        TaggedQueryService.extractTag(query, "180", TagType.PARAMETER, 0, true);
        assertThat(query.getTags().get(1).getRanges(), contains(new Range(6, 3)));

        TaggedQueryService.extractTag(query, "80", TagType.PARAMETER, 0, true);
        assertThat(query.getTags().get(2).getRanges(), contains(new Range(10, 2)));
    }

    @Test
    public void testSplitValues() throws Exception {
        assertThat(TaggedQueryService.splitTagValue("single"), is(Collections.singletonList("single")));
        assertThat(TaggedQueryService.splitTagValue("one|two"), is(Arrays.asList("one", "two")));
        assertThat(TaggedQueryService.splitTagValue("one\\|two"), is(Collections.singletonList("one|two")));
        assertThat(TaggedQueryService.splitTagValue("one\\\\|two"), is(Arrays.asList("one\\", "two")));
        assertThat(TaggedQueryService.splitTagValue("\\s\\i\\n\\g\\l\\e"),
            is(Collections.singletonList("\\s\\i\\n\\g\\l\\e")));
    }
}

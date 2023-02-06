package ru.yandex.market.mbo.db.tagged;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.junit.Before;
import org.junit.Test;
import ru.yandex.common.util.Builder;
import ru.yandex.common.util.collections.MultiSet;
import ru.yandex.market.mbo.db.tagged.TaggedQueryExpansionService.Variant;
import ru.yandex.market.mbo.gwt.models.tagged.QueryTag;
import ru.yandex.market.mbo.gwt.models.tagged.Range;
import ru.yandex.market.mbo.gwt.models.tagged.Tag;
import ru.yandex.market.mbo.gwt.models.tagged.TagType;
import ru.yandex.market.mbo.gwt.models.tagged.TaggedQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Alexander Kramarev (https://staff.yandex-team.ru/pochemuto/)
 * @date 30.11.2015
 */
@SuppressWarnings({"checkstyle:magicNumber", "checkstyle:lineLength"})
public class TaggedQueryExpansionServiceTest {

    // sort by query for having predictable order and have possibility to use Hamcrest.contains
    // instead containsInAnyOrder. Last have not nice output on failure and and it makes it hard to debug
    // See https://github.com/hamcrest/JavaHamcrest/issues/47
    private static final Comparator<TaggedQuery> BY_QUERY = Comparator.comparing(TaggedQuery::getQuery);

    private static final Pattern RANGE_PATTERN = Pattern.compile("[^-]+");

    private TaggedQueryExpansionService expansionService;

    @Before
    public void initTest() {
        expansionService = new TaggedQueryExpansionService();
    }


    @Test
    public void testExtractHid() throws Exception {
        Map<Long, Long> nidToHid = new HashMap<>();
        nidToHid.put(56372L, 91616L);
        nidToHid.put(56344L, 91657L);
        nidToHid.put(55299L, 2662954L);

        assertThat("hid as query param", expansionService.extractHid("https://market.yandex.ru/catalog/56372/list?hid=91616&how=dpop&gfilter=2142591552%3A100~100&gfilter=2142591553%3A100~100", nidToHid), is(91616L));
        assertThat("hid as url path", expansionService.extractHid("https://market.yandex.ru/catalog/90639/list?how=dpop&gfilter=2142557766%3A-1756489729%2C-804512675&gfilter=2142557761%3Aselect", nidToHid), is(90639L));
        assertThat("nid as query param", expansionService.extractHid("https://market.yandex.ru/search?&glfilter=11153059:11153241&nid=55299", nidToHid), is(2662954L));
        assertThat("nid as url path", expansionService.extractHid("https://market.yandex.ru/catalog/56344/list?how=dpop&gfilter=2142584489%3A-1483554843&gfilter=1801946%3A7843413", nidToHid), is(91657L));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateByVariant() throws Exception {
        TaggedQueryBuilder builder = new TaggedQueryBuilder("one more time")
                .addTag("one------time", "and again")
                .addTag("----more-----", "my", "CRaZY");

        List<TaggedQuery> result = expansionService.createTaggedQueries(builder.get(), builder.variants(), false);
        Collections.sort(result, BY_QUERY);
        assertThat("tags check", result, contains(
                allOf(hasProperty("query", is("and again CRaZY")), hasTagsWithRanges("and again", "----------CRaZY")),
                allOf(hasProperty("query", is("and again more")), hasTagsWithRanges("and again", "----------more")),
                allOf(hasProperty("query", is("and again my")), hasTagsWithRanges("and again", "----------my")),
                allOf(hasProperty("query", is("one CRaZY time")), hasTagsWithRanges("one-------time", "----CRAZY")),
                allOf(hasProperty("query", is("one more time")), hasTagsWithRanges("one------time", "----more")),
                allOf(hasProperty("query", is("one my time")), hasTagsWithRanges("one----time", "----my"))
        ));
    }

    @Test
    public void testTagsWithSharing() throws Exception {
        TaggedQueryBuilder builder = new TaggedQueryBuilder("смартфон nokia")
                .addTag("смартфон", "коммуникатор")
                .addTag("смартфон", "сотовый телефон")
                .addTag("---------nokia", "Nokla");

        List<TaggedQuery> result = expansionService.createTaggedQueries(builder.get(), builder.variants(), false);
        Collections.sort(result, BY_QUERY);

        // result collection has duplicates, that is ok
        assertThat(result, hasItems(
                allOf(hasProperty("query", is("смартфон nokia")), hasTagsWithRanges("смартфон", "смартфон", "---------nokia")),
                allOf(hasProperty("query", is("коммуникатор nokia")), hasTagsWithRanges("коммуникатор", "коммуникатор", "-------------nokia")),
                allOf(hasProperty("query", is("сотовый телефон nokia")), hasTagsWithRanges("сотовый телефон", "сотовый телефон", "----------------nokia")),
                allOf(hasProperty("query", is("смартфон Nokla")), hasTagsWithRanges("смартфон", "смартфон", "---------Nokla")),
                allOf(hasProperty("query", is("коммуникатор Nokla")), hasTagsWithRanges("коммуникатор", "коммуникатор", "-------------Nokla")),
                allOf(hasProperty("query", is("сотовый телефон Nokla")), hasTagsWithRanges("сотовый телефон", "сотовый телефон", "----------------Nokla"))
        ));
    }

    @Test
    public void testSpaceReducing() throws Exception {
        TaggedQueryBuilder builder = new TaggedQueryBuilder("ботинки балдинини женские на каблуке")
                .addTag("ботинки-----------женские", "боты")
                .addTag("--------------------------на каблуке");

        List<TaggedQuery> queries = expansionService.createTaggedQueries(builder.get(), builder.variants(), false);

        assertThat("trim right single space on collapse", queries, containsInAnyOrder(
                allOf(hasProperty("query", is("ботинки балдинини женские на каблуке")), hasTagsWithRanges("ботинки-----------женские", "--------------------------на каблуке")),
                allOf(hasProperty("query", is("боты балдинини на каблуке")), hasTagsWithRanges("боты", "---------------на каблуке"))
        ));
    }

    @Test
    public void testSpaceReducingMore() throws Exception {
        TaggedQueryBuilder builder = new TaggedQueryBuilder("ботинки балдинини женские      на каблуке")
                .addTag("ботинки-----------женские", "боты")
                .addTag("-------------------------------на каблуке");

        List<TaggedQuery> queries = expansionService.createTaggedQueries(builder.get(), builder.variants(), false);

        assertThat("trim right spaces on collapse", queries, containsInAnyOrder(
                allOf(hasProperty("query", is("ботинки балдинини женские      на каблуке")), hasTagsWithRanges("ботинки-----------женские", "-------------------------------на каблуке")),
                allOf(hasProperty("query", is("боты балдинини на каблуке")), hasTagsWithRanges("боты", "---------------на каблуке"))
        ));
    }

    @Test
    public void testSpaceReducingAtEnd() throws Exception {
        TaggedQueryBuilder builder = new TaggedQueryBuilder("ботинки балдинини    женские")
                .addTag("ботинки--------------женские", "боты");

        List<TaggedQuery> queries = expansionService.createTaggedQueries(builder.get(), builder.variants(), false);

        assertThat("trim left spaces at end of query", queries, containsInAnyOrder(
                allOf(hasProperty("query", is("ботинки балдинини    женские")), hasTagsWithRanges("ботинки--------------женские")),
                allOf(hasProperty("query", is("боты балдинини")), hasTagsWithRanges("боты"))
        ));
    }

    @Test
    public void testIntersected() throws Exception {
        Tag t1 = parameterTag("-------7--");
        Tag t2 = parameterTag("-12-45----");
        Tag t3 = parameterTag("--2-4-6---");
        Tag t4 = parameterTag("0---------");
        Tag t5 = parameterTag("--------89");
        Tag t6 = parameterTag("--------89");
        List<Tag> collection = new ArrayList<>(Arrays.asList(t1, t2, t3, t4, t5, t6));
        assertThat("without loyalty", expansionService.intersectedTags(collection, false), containsInAnyOrder(t2, t3, t5, t6));
        assertThat("with loyalty", expansionService.intersectedTags(collection, true), containsInAnyOrder(t2, t3));
        Tag t7 = parameterTag("0-------89");
        collection.add(t7);
        assertThat("with loyalty intersects with another", expansionService.intersectedTags(collection, true), containsInAnyOrder(t2, t3, t4, t5, t6, t7));
    }

    @Test
    public void testIntersectedTwoGroups() throws Exception {
        Tag t1 = parameterTag("01-----78-");
        Tag t2 = parameterTag("--234-----");
        Tag t3 = parameterTag("01-----78-");
        Tag t4 = parameterTag("--234-----");
        Tag t5 = parameterTag("--234-----");
        List<Tag> collection = new ArrayList<>(Arrays.asList(t1, t2, t3, t4, t5));
        assertThat("two groups", expansionService.intersectedTags(collection, true), is(empty()));
        assertThat("two groups", expansionService.intersectedTags(collection, false), containsInAnyOrder(t1, t2, t3, t4, t5));
        Tag t6 = parameterTag("--234--78-"); // glue that connect two groups
        collection.add(t6);
        assertThat("two groups but with glue", expansionService.intersectedTags(collection, true), containsInAnyOrder(t1, t2, t3, t4, t5, t6));
    }

    @Test
    public void testIntersectedGroupWithIntersect() throws Exception {
        Tag t1 = parameterTag("-12----78-");
        Tag t2 = parameterTag("--234-----");
        Tag t3 = parameterTag("-12----78-");
        Tag t4 = parameterTag("-12----78-");
        List<Tag> collection = new ArrayList<>(Arrays.asList(t1, t2, t3, t4));
        assertThat("two groups", expansionService.intersectedTags(collection, true), containsInAnyOrder(t1, t2, t3, t4));
    }

    private static class TaggedQueryBuilder {
        private final TaggedQuery target = new TaggedQuery();
        private final MultiSet<QueryTag, Variant> variants = new MultiSet<>(new Builder<Map<QueryTag, Set<Variant>>>() {
            @Override
            public Map<QueryTag, Set<Variant>> build() {
                return new LinkedHashMap<>();
            }
        });

        TaggedQueryBuilder(String q) {
            target.setQuery(q);
        }

        public TaggedQuery get() {
            return target;
        }

        TaggedQueryBuilder addTag(String rangePattern, String... replacements) {
            QueryTag tag = new QueryTag();
            tag.getRanges().addAll(rangesOf(rangePattern));
            target.getTags().add(tag);

            variants.add(tag, Variant.original(tag));
            for (String replacement : replacements) {
                variants.add(tag, new Variant(tag, replacement));
            }
            return this;
        }

        List<Set<Variant>> variants() {
            return new ArrayList<>(variants.values());
        }
    }

    private static Matcher<TaggedQuery> hasTagsWithRanges(final String... tagPatterns) {
        final List<Set<Range>> ranges = new ArrayList<>(tagPatterns.length);
        final List<Matcher<Set<Range>>> rangeMatchers = new ArrayList<>(tagPatterns.length);
        for (String pattern : tagPatterns) {
            ranges.add(rangesOf(pattern));
            rangeMatchers.add(Matchers.is(rangesOf(pattern)));
        }
        return new TypeSafeDiagnosingMatcher<TaggedQuery>() {
            @Override
            protected boolean matchesSafely(TaggedQuery item, Description mismatchDescription) {
                if (item.getTags().size() != tagPatterns.length) {
                    mismatchDescription.appendValue(" was size ").appendValue(item.getTags().size());
                    return false;
                }
                boolean result = true;
                for (int i = 0; i < tagPatterns.length; i++) {
                    TreeSet<Range> actual = item.getTags().get(i).getRanges();
                    boolean localResult = rangeMatchers.get(i).matches(actual);
                    if (!localResult) {
                        if (!result) {
                            mismatchDescription.appendText("; ");
                        }
                        mismatchDescription.appendText("tag at ").appendValue(i).appendText(" ");
                        rangeMatchers.get(i).describeMismatch(actual, mismatchDescription);
                        result = false;
                    }
                }
                return result;
            }

            @Override
            public void describeTo(Description description) {
                description.appendValueList("must have tags: ", ", ", "", ranges);
            }
        };
    }

    private static QueryTag queryTag(String pattern, TagType type) {
        QueryTag tag = new QueryTag();
        tag.setTagType(type);
        tag.getRanges().addAll(rangesOf(pattern));
        return tag;
    }

    private static QueryTag parameterTag(String pattern) {
        return queryTag(pattern, TagType.PARAMETER);
    }

    private static Set<Range> rangesOf(String pattern) {
        Set<Range> ranges = new HashSet<>();
        java.util.regex.Matcher matcher = RANGE_PATTERN.matcher(pattern);
        while (matcher.find()) {
            ranges.add(Range.withBounds(matcher.start(), matcher.end()));
        }
        return ranges;
    }
}

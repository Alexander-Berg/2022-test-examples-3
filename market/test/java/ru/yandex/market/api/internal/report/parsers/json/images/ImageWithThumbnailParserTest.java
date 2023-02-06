package ru.yandex.market.api.internal.report.parsers.json.images;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;
import org.junit.Test;
import ru.yandex.market.api.domain.v2.ImageWithThumbnails;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.common.UrlSchema;
import ru.yandex.market.api.internal.filters.Filters;
import ru.yandex.market.api.internal.report.parsers.json.ImageWithThumbnailsParser;
import ru.yandex.market.api.internal.report.parsers.json.ImageWithThumbnailsParser.Result;
import ru.yandex.market.api.matchers.CriterionMatcher;
import ru.yandex.market.api.matchers.ImageMatcher;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.version.Version;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.ResourceHelpers;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static ru.yandex.market.api.ApiMatchers.collectionToStr;
import static ru.yandex.market.api.ApiMatchers.map;

@WithContext
public class ImageWithThumbnailParserTest extends BaseTest {
    private static final String ONE_IMAGE_WITHOUT_URL = "one-image-without-url.json";
    private static final String ONE_IMAGE_WITH_EMPTY_URL = "one-image-with-empty-url.json";

    private ImageWithThumbnailsParser parser;

    @Override
    public void setUp() throws Exception {
        parser = new ImageWithThumbnailsParser();
    }


    @Test
    public void shouldNullCriteria_ifFiltersMatchingEmpty() {
        checkWithoutCriteriaTests();
    }


    @Test
    public void shouldHaveCriteria_ifFiltersMatchingNotEmpty() {
        checkWithCriteriaTests();
    }

    @Test
    public void shouldCorrectReset() {
        checkWithoutCriteriaTests();
        checkWithCriteriaTests();
    }

    @Test
    public void shouldHaveHttpsInUrl_ifUrlEmptyV2() {
        ContextHolder.update(ctx -> ctx.setVersion(Version.V2_0_0));
        ContextHolder.update(ctx -> ctx.setUrlSchema(UrlSchema.HTTPS));

        doTest(
            parse(ONE_IMAGE_WITH_EMPTY_URL).getImage(),
            (Matcher<ImageWithThumbnails>) ImageMatcher.image(
                "https://",
                400,
                400
            )
        );
    }
    @Test
    public void shouldHaveHttpsInUrl_ifUrlEmptyV2_1_5() {
        ContextHolder.update(ctx -> ctx.setVersion(Version.V2_1_5));
        ContextHolder.update(ctx -> ctx.setUrlSchema(UrlSchema.HTTPS));

        doTest(
            parse(ONE_IMAGE_WITH_EMPTY_URL).getImage(),
            (Matcher<ImageWithThumbnails>) ImageMatcher.image(
                "",
                0,
                0
            )
        );
    }


    @Test
    public void shouldHaveHttpsInUrl_ifNoUrlV2() {
        ContextHolder.update(ctx -> ctx.setVersion(Version.V2_0_0));
        ContextHolder.update(ctx -> ctx.setUrlSchema(UrlSchema.HTTPS));

        doTest(
            parse(ONE_IMAGE_WITHOUT_URL).getImage(),
            (Matcher<ImageWithThumbnails>) ImageMatcher.image(
                "https://",
                400,
                400
            )
        );
    }

    @Test
    public void shouldHaveHttpsInUrl_ifNoUrlV2_1_5() {
        ContextHolder.update(ctx -> ctx.setVersion(Version.V2_1_5));
        ContextHolder.update(ctx -> ctx.setUrlSchema(UrlSchema.HTTPS));

        doTest(
            parse(ONE_IMAGE_WITHOUT_URL).getImage(),
            (Matcher<ImageWithThumbnails>) ImageMatcher.image(
                "",
                0,
                0
            )
        );
    }

    @Test
    public void checkAltParsing() {
        doTest(
                parse("one-image-with-alt.json").getImage(),
                (Matcher<ImageWithThumbnails>) ImageMatcher.image(
                        "http://",
                        400,
                        400
                ),
                map(
                        ImageWithThumbnails::getAlt,
                        "'alt'",
                        is("alt text")
                )
        );
    }

    @Test
    public void checkEmptyAltParsing() {
        doTest(
                parse("one-image-without-alt.json").getImage(),
                (Matcher<ImageWithThumbnails>) ImageMatcher.image(
                        "http://",
                        400,
                        400
                ),
                map(
                        ImageWithThumbnails::getAlt,
                        "'alt'",
                        CoreMatchers.nullValue(String.class)
                )
        );
    }

    private void checkWithoutCriteriaTests() {
        doTest(
            parse("one-image-without-filters.json").getImage(),
            (Matcher<ImageWithThumbnails>) ImageMatcher.image(
                "http://avatars.mds.yandex.net/get-mpic/397397/img_id8407956531717956576.jpeg/orig",
                358,
                701
            ),
            map(
                ImageWithThumbnails::getCriteria,
                "'criteria'",
                CoreMatchers.nullValue(List.class)
            )
        );
    }

    private void checkWithCriteriaTests() {
        doTest(
            parse("one-image-with-filters.json").getImage(),
            (Matcher<ImageWithThumbnails>) ImageMatcher.image(
                "http://avatars.mds.yandex.net/get-mpic/364668/img_id1001298848489664964.jpeg/orig",
                357,
                701
            ),
            map(
                ImageWithThumbnails::getCriteria,
                "'criteria'",
                cast(
                    containsInAnyOrder(
                        CriterionMatcher.criterion("13887626", "13898623"),
                        CriterionMatcher.criterion("14871214", "14897638"),
                        CriterionMatcher.criterion(Filters.VENDOR_FILTER_CODE, "152863")
                    )
                ),
                ImageWithThumbnails::toString,
                c -> collectionToStr(c, CriterionMatcher::toStr)
            )
        );
    }


    private Result parse(String filename) {
        return parser.parse(ResourceHelpers.getResource(filename));
    }

    private void doTest(ImageWithThumbnails result, Matcher<ImageWithThumbnails>... matchers) {
        Arrays.stream(matchers).forEach(m -> assertThat(result, m));
    }

    private static  <X, T extends Iterable<? extends X>> Matcher<Collection<X>> cast(Matcher<T> arg) {
        return (Matcher<Collection<X>>) arg;
    }
}

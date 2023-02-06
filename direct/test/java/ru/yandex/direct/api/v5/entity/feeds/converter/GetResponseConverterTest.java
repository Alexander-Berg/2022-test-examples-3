package ru.yandex.direct.api.v5.entity.feeds.converter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.yandex.direct.api.v5.feeds.BusinessTypeEnum;
import com.yandex.direct.api.v5.feeds.FeedFieldEnum;
import com.yandex.direct.api.v5.feeds.FeedGetItem;
import com.yandex.direct.api.v5.feeds.FeedStatusEnum;
import com.yandex.direct.api.v5.feeds.FileFeedFieldEnum;
import com.yandex.direct.api.v5.feeds.FileFeedGet;
import com.yandex.direct.api.v5.feeds.ObjectFactory;
import com.yandex.direct.api.v5.feeds.SourceTypeEnum;
import com.yandex.direct.api.v5.feeds.UrlFeedFieldEnum;
import com.yandex.direct.api.v5.feeds.UrlFeedGet;
import com.yandex.direct.api.v5.general.ArrayOfLong;
import one.util.streamex.StreamEx;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.entity.feeds.delegate.FeedAnyFieldEnum;
import ru.yandex.direct.api.v5.testing.configuration.Api5Test;

import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.allFields;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@Api5Test
@RunWith(SpringRunner.class)
public class GetResponseConverterTest {
    private final static ObjectFactory OBJECT_FACTORY = new ObjectFactory();
    private final static LocalDateTime LOCAL_DATE_TIME = LocalDateTime.of(2020, 4, 1, 10, 0, 0);
    private final static String S_LOCAL_DATE_TIME = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LOCAL_DATE_TIME);

    private static final Long FEED_ID = 1L;
    private static final String FEED_NAME = "feed name";
    private static final String FEED_FILENAME = "filename.csv";
    private static final String FEED_URL = "http://ya.ru/feeds/filename.csv";
    private static final long NUMBER_OF_ITEMS = 2020L;
    private static final List<Long> CAMPAIGN_IDS = List.of(1L, 2L);

    @Autowired
    private GetResponseConverterService getResponseConverterService;

    @Test
    public void filterProperties_file_idAndName() {
        FeedGetItem item = buildFileFeed();
        var expected = new FeedGetItem()
                .withId(FEED_ID)
                .withName(FEED_NAME);
        getResponseConverterService.filterProperties(List.of(item), Set.of(FeedAnyFieldEnum.ID, FeedAnyFieldEnum.NAME));
        assertThat(item).is(matchedBy(beanDiffer(expected).useCompareStrategy(allFields())));
    }

    @Test
    public void filterProperties_url_businessTypeAndSourceType() {
        FeedGetItem item = buildUrlFeed();
        var expected = new FeedGetItem()
                .withBusinessType(BusinessTypeEnum.AUTOMOBILES)
                .withSourceType(SourceTypeEnum.URL);
        getResponseConverterService.filterProperties(
                List.of(item),
                Set.of(FeedAnyFieldEnum.BUSINESS_TYPE, FeedAnyFieldEnum.SOURCE_TYPE)
        );
        assertThat(item).is(matchedBy(beanDiffer(expected).useCompareStrategy(allFields())));
    }

    @Test
    public void filterProperties_file_baseAndFileFields() {
        Map<? extends Class<?>, Set<FeedAnyFieldEnum>> feedFieldEnumsByClass =
                StreamEx.of(EnumSet.allOf(FeedAnyFieldEnum.class))
                        .groupingBy(FeedAnyFieldEnum::getEnumClass, toSet());

        Set<FeedAnyFieldEnum> fields = new HashSet<>();
        fields.addAll(feedFieldEnumsByClass.get(FeedFieldEnum.class));
        fields.addAll(feedFieldEnumsByClass.get(FileFeedFieldEnum.class));

        FeedGetItem item = buildFileFeed();
        FeedGetItem expected = buildFileFeed();
        getResponseConverterService.filterProperties(List.of(item), fields);
        assertThat(item).is(matchedBy(beanDiffer(expected).useCompareStrategy(allFields())));
    }

    @Test
    public void filterProperties_url_baseAndUrlFields() {
        Map<? extends Class<?>, Set<FeedAnyFieldEnum>> feedFieldEnumsByClass =
                StreamEx.of(EnumSet.allOf(FeedAnyFieldEnum.class))
                        .groupingBy(FeedAnyFieldEnum::getEnumClass, toSet());

        Set<FeedAnyFieldEnum> fields = new HashSet<>();
        fields.addAll(feedFieldEnumsByClass.get(FeedFieldEnum.class));
        fields.addAll(feedFieldEnumsByClass.get(UrlFeedFieldEnum.class));

        FeedGetItem item = buildUrlFeed();
        FeedGetItem expected = buildUrlFeed();
        getResponseConverterService.filterProperties(List.of(item), fields);
        assertThat(item).is(matchedBy(beanDiffer(expected).useCompareStrategy(allFields())));
    }

    @Test
    public void filterProperties_file_fileFields() {
        Map<? extends Class<?>, Set<FeedAnyFieldEnum>> feedFieldEnumsByClass =
                StreamEx.of(EnumSet.allOf(FeedAnyFieldEnum.class))
                        .groupingBy(FeedAnyFieldEnum::getEnumClass, toSet());

        Set<FeedAnyFieldEnum> fields = new HashSet<>(feedFieldEnumsByClass.get(FileFeedFieldEnum.class));

        FeedGetItem item = buildFileFeed();
        var expected = new FeedGetItem()
                .withFileFeed(OBJECT_FACTORY.createFeedGetItemFileFeed(new FileFeedGet().withFilename(FEED_FILENAME)));
        getResponseConverterService.filterProperties(List.of(item), fields);
        assertThat(item).is(matchedBy(beanDiffer(expected).useCompareStrategy(allFields())));
    }


    @Test
    public void filterProperties_url_urlFields() {
        Map<? extends Class<?>, Set<FeedAnyFieldEnum>> feedFieldEnumsByClass =
                StreamEx.of(EnumSet.allOf(FeedAnyFieldEnum.class))
                        .groupingBy(FeedAnyFieldEnum::getEnumClass, toSet());

        Set<FeedAnyFieldEnum> fields = new HashSet<>(feedFieldEnumsByClass.get(UrlFeedFieldEnum.class));

        FeedGetItem item = buildUrlFeed();
        var expected = new FeedGetItem()
                .withUrlFeed(OBJECT_FACTORY.createFeedGetItemUrlFeed(new UrlFeedGet().withUrl(FEED_URL)));
        getResponseConverterService.filterProperties(List.of(item), fields);
        assertThat(item).is(matchedBy(beanDiffer(expected).useCompareStrategy(allFields())));
    }

    private FeedGetItem buildFileFeed() {
        return new FeedGetItem()
                .withId(FEED_ID)
                .withName(FEED_NAME)
                .withBusinessType(BusinessTypeEnum.AUTOMOBILES)
                .withSourceType(SourceTypeEnum.FILE)
                .withUpdatedAt(OBJECT_FACTORY.createFeedGetItemUpdatedAt(S_LOCAL_DATE_TIME))
                .withCampaignIds(OBJECT_FACTORY.createFeedGetItemCampaignIds(new ArrayOfLong().withItems(CAMPAIGN_IDS)))
                .withNumberOfItems(OBJECT_FACTORY.createFeedGetItemNumberOfItems(NUMBER_OF_ITEMS))
                .withStatus(FeedStatusEnum.NEW)
                .withFileFeed(OBJECT_FACTORY.createFeedGetItemFileFeed(new FileFeedGet().withFilename(FEED_FILENAME)));
    }

    private FeedGetItem buildUrlFeed() {
        return new FeedGetItem()
                .withId(FEED_ID)
                .withName(FEED_NAME)
                .withBusinessType(BusinessTypeEnum.AUTOMOBILES)
                .withSourceType(SourceTypeEnum.URL)
                .withUpdatedAt(OBJECT_FACTORY.createFeedGetItemUpdatedAt(S_LOCAL_DATE_TIME))
                .withCampaignIds(null)
                .withNumberOfItems(OBJECT_FACTORY.createFeedGetItemNumberOfItems(NUMBER_OF_ITEMS))
                .withStatus(FeedStatusEnum.NEW)
                .withUrlFeed(OBJECT_FACTORY.createFeedGetItemUrlFeed(new UrlFeedGet().withUrl(FEED_URL)));
    }
}

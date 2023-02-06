package ru.yandex.direct.api.v5.entity.feeds.converter;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import com.yandex.direct.api.v5.feeds.BusinessTypeEnum;
import com.yandex.direct.api.v5.feeds.FeedAddItem;
import com.yandex.direct.api.v5.feeds.FileFeedAdd;
import com.yandex.direct.api.v5.feeds.SourceTypeEnum;
import com.yandex.direct.api.v5.feeds.UrlFeedAdd;
import com.yandex.direct.api.v5.general.YesNoEnum;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.api.v5.testing.configuration.Api5Test;
import ru.yandex.direct.core.entity.feed.model.BusinessType;
import ru.yandex.direct.core.entity.feed.model.Feed;
import ru.yandex.direct.core.entity.feed.model.Source;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;

@Api5Test
@RunWith(SpringRunner.class)
@ParametersAreNonnullByDefault
public class AddRequestConverterTest {

    private static final String NAME = "name";
    private static final String FILENAME = "filename.csv";
    private static final String URL = "http://mysite.com/feed.csv";

    @Test
    public void convert_file() {
        var fileFeed = new FileFeedAdd()
                .withFilename(FILENAME)
                .withData("<xml></xml>".getBytes());
        var addItem = new FeedAddItem()
                .withName(NAME)
                .withBusinessType(BusinessTypeEnum.AUTOMOBILES)
                .withSourceType(SourceTypeEnum.FILE)
                .withFileFeed(fileFeed);

        Feed feed = AddRequestConverter.convertItemsToModels(List.of(addItem)).get(0);
        Feed expectedFeed = new Feed()
                .withName(NAME)
                .withBusinessType(BusinessType.AUTO)
                .withSource(Source.FILE)
                .withFilename(FILENAME);
        assertThat(feed).is(matchedBy(beanDiffer(expectedFeed).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void convert_url_withoutLoginAndPassword() {
        var urlFeed = new UrlFeedAdd()
                .withUrl(URL)
                .withRemoveUtmTags(YesNoEnum.YES);
        var addItem = new FeedAddItem()
                .withName(NAME)
                .withBusinessType(BusinessTypeEnum.FLIGHTS)
                .withSourceType(SourceTypeEnum.URL)
                .withUrlFeed(urlFeed);
        Feed feed = AddRequestConverter.convertItemsToModels(List.of(addItem)).get(0);
        Feed expectedFeed = new Feed()
                .withName(NAME)
                .withBusinessType(BusinessType.FLIGHTS)
                .withSource(Source.URL)
                .withIsRemoveUtm(Boolean.TRUE);
        assertThat(feed).is(matchedBy(beanDiffer(expectedFeed).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void convert_url_withLoginAndPassword() {
        String login = "login";
        String password = "password";
        var urlFeed = new UrlFeedAdd()
                .withUrl(URL)
                .withRemoveUtmTags(YesNoEnum.NO)
                .withLogin(login)
                .withPassword(password);
        var addItem = new FeedAddItem()
                .withName(NAME)
                .withBusinessType(BusinessTypeEnum.HOTELS)
                .withSourceType(SourceTypeEnum.URL)
                .withUrlFeed(urlFeed);
        Feed feed = AddRequestConverter.convertItemsToModels(List.of(addItem)).get(0);
        Feed expectedFeed = new Feed()
                .withName(NAME)
                .withBusinessType(BusinessType.HOTELS)
                .withSource(Source.URL)
                .withIsRemoveUtm(Boolean.FALSE)
                .withLogin(login)
                .withPlainPassword(password);
        assertThat(feed).is(matchedBy(beanDiffer(expectedFeed).useCompareStrategy(onlyExpectedFields())));
    }
}

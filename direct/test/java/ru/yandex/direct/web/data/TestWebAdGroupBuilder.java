package ru.yandex.direct.web.data;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import ru.yandex.direct.web.entity.adgroup.model.WebAdGroupRelevanceMatch;
import ru.yandex.direct.web.entity.adgroup.model.WebTextAdGroup;
import ru.yandex.direct.web.entity.banner.model.WebBanner;
import ru.yandex.direct.web.entity.keyword.model.WebKeyword;

import static ru.yandex.direct.web.testing.data.TestAdGroups.randomNameWebAdGroup;
import static ru.yandex.direct.web.testing.data.TestBanners.randomTitleWebTextBanner;
import static ru.yandex.direct.web.testing.data.TestKeywords.randomPhraseKeyword;

public class TestWebAdGroupBuilder {
    private final long campaignId;

    private List<WebBanner> banners;
    private List<WebKeyword> keywords;
    private List<WebAdGroupRelevanceMatch> relevanceMatches;

    private TestWebAdGroupBuilder(long campaignId) {
        this.campaignId = campaignId;
        banners = new ArrayList<>();
        keywords = new ArrayList<>();
        relevanceMatches = new ArrayList<>();
    }

    public static TestWebAdGroupBuilder someWebAdGroup(long campaignId) {
        return new TestWebAdGroupBuilder(campaignId);
    }

    public TestWebAdGroupBuilder withSomeBanner() {
        banners.add(randomTitleWebTextBanner(null));
        return this;
    }

    public TestWebAdGroupBuilder withSomeKeyword() {
        keywords.add(randomPhraseKeyword(null));
        return this;
    }

    public TestWebAdGroupBuilder withSomeKeyword(Function<WebKeyword, WebKeyword> modification) {
        WebKeyword kw = modification.apply(randomPhraseKeyword(null));
        keywords.add(kw);
        return this;
    }

    public TestWebAdGroupBuilder withSomeRelMatch() {
        WebAdGroupRelevanceMatch rm = new WebAdGroupRelevanceMatch().withId(0L);
        relevanceMatches.add(rm);
        return this;
    }

    public WebTextAdGroup build() {
        return randomNameWebAdGroup(null, campaignId)
                .withBanners(banners)
                .withKeywords(keywords)
                .withRelevanceMatches(relevanceMatches);
    }
}

package ru.yandex.direct.grid.processing.service.group.converter;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.grid.processing.model.group.mutation.GdUpdateAdGroupKeywordItem;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.assertj.core.api.Assertions.assertThat;

@ParametersAreNonnullByDefault
public class AdGroupsMutationDataConverterToCoreKeywordsTest {

    private static final String PHRASE_1 = "фраза_1";

    @Test
    public void convertWithoutExistsKeywords() {
        GdUpdateAdGroupKeywordItem updateAdGroupKeywordItem = new GdUpdateAdGroupKeywordItem()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withPhrase(PHRASE_1);

        List<GdUpdateAdGroupKeywordItem> phrase = List.of(updateAdGroupKeywordItem);
        List<Keyword> keywords = AdGroupsMutationDataConverter.toCoreKeywords(phrase, null);

        assertThat(keywords).hasSize(1);
        assertThat(keywords.get(0).getPhrase()).isEqualTo(PHRASE_1);
    }

    @Test
    public void convertWithExistsKeywords() {
        GdUpdateAdGroupKeywordItem updateAdGroupKeywordItem = new GdUpdateAdGroupKeywordItem()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withPhrase(PHRASE_1);

        List<GdUpdateAdGroupKeywordItem> phrase = List.of(updateAdGroupKeywordItem);
        Keyword keyword = new Keyword()
                .withPhrase(PHRASE_1)
                .withId(RandomNumberUtils.nextPositiveLong())
                .withCampaignId(RandomNumberUtils.nextPositiveLong())

                .withAdGroupId(RandomNumberUtils.nextPositiveLong());
        List<Keyword> keywords = AdGroupsMutationDataConverter.toCoreKeywords(phrase, List.of(keyword));

        assertThat(keywords).hasSize(1);
        assertThat(keywords.get(0).getPhrase()).isEqualTo(PHRASE_1);
    }

    @Test
    public void convertWithDoubledExistsKeywords() {
        GdUpdateAdGroupKeywordItem updateAdGroupKeywordItem = new GdUpdateAdGroupKeywordItem()
                .withId(RandomNumberUtils.nextPositiveLong())
                .withPhrase(PHRASE_1);

        List<GdUpdateAdGroupKeywordItem> phrase = List.of(updateAdGroupKeywordItem);
        Keyword keyword = new Keyword()
                .withPhrase(PHRASE_1)
                .withId(RandomNumberUtils.nextPositiveLong())
                .withCampaignId(RandomNumberUtils.nextPositiveLong())
                .withAdGroupId(RandomNumberUtils.nextPositiveLong());

        List<Keyword> keywords = AdGroupsMutationDataConverter.toCoreKeywords(phrase, List.of(keyword, keyword));

        assertThat(keywords).hasSize(1);
        assertThat(keywords.get(0).getPhrase()).isEqualTo(PHRASE_1);
    }

}

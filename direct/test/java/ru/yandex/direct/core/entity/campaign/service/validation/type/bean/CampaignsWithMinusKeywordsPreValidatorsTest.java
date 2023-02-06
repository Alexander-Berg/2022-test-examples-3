package ru.yandex.direct.core.entity.campaign.service.validation.type.bean;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Test;

import ru.yandex.direct.core.entity.campaign.model.CampaignWithMinusKeywords;
import ru.yandex.direct.core.entity.campaign.model.TextCampaign;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.defect.ids.StringDefectIds;
import ru.yandex.direct.validation.result.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.validation.type.bean.CampaignWithMinusKeywordsPreValidators.CAMPAIGN_VALIDATOR;
import static ru.yandex.direct.core.entity.campaign.service.validation.type.bean.CampaignWithMinusKeywordsPreValidators.MODEL_CHANGES_VALIDATOR;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseConstraints.CAMPAIGN_MINUS_KEYWORDS_MAX_LENGTH_BEFORE_NORMALIZATION;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.maxLengthMinusKeywords;
import static ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseDefects.nestedOrEmptySquareBrackets;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@ParametersAreNonnullByDefault
public class CampaignsWithMinusKeywordsPreValidatorsTest {
    @Test
    public void testNoMinusPhrases() {
        var vr = CAMPAIGN_VALIDATOR.apply(createCampaign(null));
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void testValidMinusPhrase() {
        var minusKeywords = List.of("валидная фраза");
        var vr = CAMPAIGN_VALIDATOR.apply(createCampaign(minusKeywords));
        assertThat(vr, hasNoDefectsDefinitions());
    }

    @Test
    public void testTooLongMinusPhrases() {
        List<String> tooLongList = new ArrayList<>();
        for (int i = 0; i < 2000; i++) { // Суммарная длина — 90 000 при ограничении 80 000
            tooLongList.add("валиднаяФразаБезПробеловИСпецСимволовДлиной45");
        }
        var vr = CAMPAIGN_VALIDATOR.apply(createCampaign(tooLongList));
        assertThat(vr, hasDefectDefinitionWith(validationError(minusKeywordsPath(),
                maxLengthMinusKeywords(CAMPAIGN_MINUS_KEYWORDS_MAX_LENGTH_BEFORE_NORMALIZATION))));
    }

    @Test
    public void testNullPhrase() {
        var minusKeywords = Arrays.asList("фраза", null);
        var vr = CAMPAIGN_VALIDATOR.apply(createCampaign(minusKeywords));
        assertThat(vr, hasDefectDefinitionWith(validationError(secondMinusKeywordPath(), CommonDefects.notNull())));
    }

    @Test
    public void testEmptyPhrase() {
        var minusKeywords = List.of("фраза", "");
        var vr = CAMPAIGN_VALIDATOR.apply(createCampaign(minusKeywords));
        assertThat(vr, hasDefectDefinitionWith(validationError(secondMinusKeywordPath(), StringDefectIds.CANNOT_BE_EMPTY)));
    }

    @Test
    public void testCorrectPathOnModelChangesValidator() {
        var minusKeywords = List.of("[[]] невалидная фраза");
        var campaign = createCampaign(minusKeywords);
        var mc = ModelChanges.build(campaign,
                CampaignWithMinusKeywords.MINUS_KEYWORDS, minusKeywords);
        var vr = MODEL_CHANGES_VALIDATOR.apply(mc);

        var expectedPath = path(field(CampaignWithMinusKeywords.MINUS_KEYWORDS), index(0));
        assertThat(vr, hasDefectDefinitionWith(validationError(expectedPath, nestedOrEmptySquareBrackets(minusKeywords))));
    }

    private CampaignWithMinusKeywords createCampaign(@Nullable List<String> minusKeywords) {
        return new TextCampaign().withMinusKeywords(minusKeywords);
    }

    private static Path minusKeywordsPath() {
        return path(field(CampaignWithMinusKeywords.MINUS_KEYWORDS));
    }

    private static Path secondMinusKeywordPath() {
        return path(field(CampaignWithMinusKeywords.MINUS_KEYWORDS), index(1));
    }
}

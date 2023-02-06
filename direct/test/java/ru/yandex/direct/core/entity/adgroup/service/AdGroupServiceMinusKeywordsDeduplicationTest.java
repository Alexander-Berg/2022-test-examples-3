package ru.yandex.direct.core.entity.adgroup.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.adgroup.model.AdGroup;
import ru.yandex.direct.core.entity.keyword.service.validation.phrase.minusphrase.MinusPhraseValidator;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.CampaignInfo;
import ru.yandex.direct.core.testing.steps.AdGroupSteps;
import ru.yandex.direct.core.testing.steps.CampaignSteps;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.regions.GeoTree;
import ru.yandex.direct.regions.GeoTreeFactory;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.testing.data.TestGroups.defaultTextAdGroup;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;

/**
 * Проверяем что при сохранении минус-фраз дубликаты удаляются
 */
@CoreTest
@RunWith(SpringRunner.class)
public class AdGroupServiceMinusKeywordsDeduplicationTest {
    private static final List<String> DUBLICATED_MINUS_KEYWORDS = Arrays.asList("слово1", "слово1", "слово2");
    private static final String[] EXPECTED_MINUS_KEYWORDS = Sets.newHashSet(DUBLICATED_MINUS_KEYWORDS)
            .toArray(new String[0]);

    @Autowired
    public CampaignSteps campaignSteps;
    @Autowired
    public AdGroupSteps adGroupSteps;
    @Autowired
    public AdGroupService adGroupService;
    @Autowired
    public GeoTreeFactory geoTreeFactory;

    private GeoTree geoTree;
    private CampaignInfo campaign;

    @Before
    public void setUp() {
        geoTree = geoTreeFactory.getGlobalGeoTree();
        campaign = campaignSteps.createActiveTextCampaign();
    }

    @Test
    public void testAdd() {

        MassResult<Long> result = adGroupService.addAdGroupsPartial(
                Collections.singletonList(
                        defaultTextAdGroup(campaign.getCampaignId())
                                .withMinusKeywords(DUBLICATED_MINUS_KEYWORDS)),
                geoTree,
                MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE,
                campaign.getUid(),
                campaign.getClientId());
        assumeThat(result.getValidationResult(), hasNoDefectsDefinitions());

        assertMinusKeywordsSavedWithoutDublication(result);
    }

    @Test
    public void testUpdate() {
        AdGroupInfo adGroup = adGroupSteps.createAdGroup(defaultTextAdGroup(campaign.getCampaignId()));

        MassResult<Long> result = adGroupService.updateAdGroupsPartialWithFullValidation(
                Collections.singletonList(
                        new ModelChanges<>(adGroup.getAdGroupId(), AdGroup.class)
                                .process(DUBLICATED_MINUS_KEYWORDS, AdGroup.MINUS_KEYWORDS)),
                geoTree,
                MinusPhraseValidator.ValidationMode.ONE_ERROR_PER_TYPE,
                adGroup.getUid(),
                adGroup.getClientId());
        assumeThat(result.getValidationResult(), hasNoDefectsDefinitions());

        assertMinusKeywordsSavedWithoutDublication(result);
    }

    private void assertMinusKeywordsSavedWithoutDublication(MassResult<Long> result) {
        AdGroup adGroup = adGroupService.getAdGroups(
                campaign.getClientId(), singleton(result.get(0).getResult()))
                .get(0);

        assertThat(adGroup.getMinusKeywords()).isNotNull()
                .containsExactlyInAnyOrder(EXPECTED_MINUS_KEYWORDS);
    }
}

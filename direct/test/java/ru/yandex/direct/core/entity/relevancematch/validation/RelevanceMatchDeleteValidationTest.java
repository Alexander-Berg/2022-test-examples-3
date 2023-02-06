package ru.yandex.direct.core.entity.relevancematch.validation;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.relevancematch.repository.RelevanceMatchRepository;
import ru.yandex.direct.core.entity.relevancematch.service.RelevanceMatchModificationBaseTest;
import ru.yandex.direct.core.entity.relevancematch.valdiation.RelevanceMatchValidationService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.repository.TestCampaignRepository;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.campaign.service.validation.CampaignDefects.archivedCampaignModification;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class RelevanceMatchDeleteValidationTest extends RelevanceMatchModificationBaseTest {

    @Autowired
    protected RelevanceMatchRepository relevanceMatchRepository;

    @Autowired
    private RelevanceMatchValidationService relevanceMatchValidationService;

    @Autowired
    private TestCampaignRepository campaignRepository;

    @Test
    public void validate_Success() {
        ValidationResult<List<Long>, Defect> actual =
                relevanceMatchValidationService
                        .validateDeleteRelevanceMatches(singletonList(getSavedRelevanceMatch().getId()),
                                relevanceMatchByIds, getOperatorUid(), getClientId());
        assertThat(actual).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_NotExistingIds() {
        ValidationResult<List<Long>, Defect> actual =
                relevanceMatchValidationService.validateDeleteRelevanceMatches(
                        singletonList(getSavedRelevanceMatch().getId() + 1000), relevanceMatchByIds,
                        getOperatorUid(), getClientId());

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), CommonDefects.objectNotFound()))));
    }

    @Test
    public void validate_ArchivedCampaign_ArchivedCampaignModification() {
        campaignRepository.archiveCampaign(defaultAdGroup.getShard(), defaultAdGroup.getCampaignId());

        ValidationResult<List<Long>, Defect> actual =
                relevanceMatchValidationService.validateDeleteRelevanceMatches(
                        singletonList(getSavedRelevanceMatch().getId()), relevanceMatchByIds,
                        getOperatorUid(), getClientId());

        assertThat(actual).is(matchedBy(hasDefectWithDefinition(
                validationError(path(index(0)), archivedCampaignModification()))));
    }
}

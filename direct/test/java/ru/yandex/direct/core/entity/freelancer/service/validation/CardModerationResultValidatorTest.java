package ru.yandex.direct.core.entity.freelancer.service.validation;

import java.util.Set;

import one.util.streamex.StreamEx;
import org.junit.Test;

import ru.yandex.direct.core.entity.freelancer.model.FreelancerCard;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerCardModeration;
import ru.yandex.direct.core.entity.freelancer.model.FreelancersCardDeclineReason;
import ru.yandex.direct.core.entity.freelancer.model.FreelancersCardStatusModerate;
import ru.yandex.direct.testing.matchers.validation.Matchers;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class CardModerationResultValidatorTest {

    private CardModerationResultValidator testedValidator = new CardModerationResultValidator();

    @Test
    public void success_with_acceptedModerate() {
        FreelancerCardModeration moderationResult =
                getModerationResult(1L, 1L, FreelancersCardStatusModerate.ACCEPTED, emptySet());
        ValidationResult<FreelancerCardModeration, Defect> actual = testedValidator.apply(moderationResult);
        assertThat(actual).is(matchedBy(Matchers.hasNoErrorsAndWarnings()));
    }

    @Test
    public void success_with_declineModerate() {
        Set<FreelancersCardDeclineReason> freelancersCardDeclineReasons =
                StreamEx.of(FreelancersCardDeclineReason.values()).toSet();
        FreelancerCardModeration moderationResult =
                getModerationResult(1L,
                        1L,
                        FreelancersCardStatusModerate.DECLINED,
                        freelancersCardDeclineReasons);
        ValidationResult<FreelancerCardModeration, Defect> actual = testedValidator.apply(moderationResult);
        assertThat(actual).is(matchedBy(Matchers.hasNoErrorsAndWarnings()));
    }

    @Test
    public void fail_whenCardIdIsNull() {
        FreelancerCardModeration moderationResult =
                getModerationResult(null, 1L, FreelancersCardStatusModerate.ACCEPTED, emptySet());
        ValidationResult<FreelancerCardModeration, Defect> actual = testedValidator.apply(moderationResult);
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerCard.ID.name())),
                        CommonDefects.notNull()))));
    }

    @Test
    public void fail_whenFreelancerIdIsNull() {
        FreelancerCardModeration moderationResult =
                getModerationResult(1L, null, FreelancersCardStatusModerate.ACCEPTED, emptySet());
        ValidationResult<FreelancerCardModeration, Defect> actual = testedValidator.apply(moderationResult);
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerCard.FREELANCER_ID.name())),
                        CommonDefects.notNull()))));
    }

    @Test
    public void fail_whenStatusModerateIsNull() {
        FreelancerCardModeration moderationResult =
                getModerationResult(1L, 1L, null, emptySet());
        ValidationResult<FreelancerCardModeration, Defect> actual = testedValidator.apply(moderationResult);
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerCard.STATUS_MODERATE.name())),
                        CommonDefects.notNull()))));
    }

    @Test
    public void fail_whenStatusModerateNotValid() {
        FreelancerCardModeration moderationResult =
                getModerationResult(1L, 1L, FreelancersCardStatusModerate.IN_PROGRESS, emptySet());
        ValidationResult<FreelancerCardModeration, Defect> actual = testedValidator.apply(moderationResult);
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerCard.STATUS_MODERATE.name())),
                        CollectionDefects.inCollection()))));
    }

    @Test
    public void fail_whenAcceptedAndNotEmptyReasons() {
        Set<FreelancersCardDeclineReason> freelancersCardDeclineReasons =
                StreamEx.of(FreelancersCardDeclineReason.values()).toSet();
        FreelancerCardModeration moderationResult =
                getModerationResult(1L, 1L, FreelancersCardStatusModerate.ACCEPTED, freelancersCardDeclineReasons);
        ValidationResult<FreelancerCardModeration, Defect> actual = testedValidator.apply(moderationResult);
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerCard.DECLINE_REASON.name())),
                        CollectionDefects.isEmptyCollection()))));
    }

    @Test
    public void fail_whenAcceptAndNullReasons() {
        FreelancerCardModeration moderationResult =
                getModerationResult(1L, 1L, FreelancersCardStatusModerate.ACCEPTED, null);
        ValidationResult<FreelancerCardModeration, Defect> actual = testedValidator.apply(moderationResult);
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerCard.DECLINE_REASON)),
                        CommonDefects.notNull()))));
    }

    @Test
    public void fail_whenDeclineAndEmptyReasons() {
        FreelancerCardModeration moderationResult =
                getModerationResult(1L, 1L, FreelancersCardStatusModerate.DECLINED, emptySet());
        ValidationResult<FreelancerCardModeration, Defect> actual = testedValidator.apply(moderationResult);
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerCard.DECLINE_REASON.name())),
                        CollectionDefects.notEmptyCollection()))));
    }

    @Test
    public void fail_whenDeclineAndNullReasons() {
        FreelancerCardModeration moderationResult =
                getModerationResult(1L, 1L, FreelancersCardStatusModerate.DECLINED, null);
        ValidationResult<FreelancerCardModeration, Defect> actual = testedValidator.apply(moderationResult);
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerCard.DECLINE_REASON)),
                        CommonDefects.notNull()))));
    }


    private FreelancerCardModeration getModerationResult(Long cardId,
                                                         Long freelancerId,
                                                         FreelancersCardStatusModerate statusModerate,
                                                         Set<FreelancersCardDeclineReason> declineReasons) {
        return new FreelancerCard()
                .withId(cardId)
                .withFreelancerId(freelancerId)
                .withStatusModerate(statusModerate)
                .withDeclineReason(declineReasons);
    }
}

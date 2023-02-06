package ru.yandex.direct.core.entity.freelancer.service.validation;

import java.time.ZonedDateTime;
import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.freelancer.model.FreelancerFeedback;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerSkill;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerUgcModerationStatus;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.defect.StringDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class FreelancerFeedbackValidatorTest {

    private final FreelancerFeedbackValidator validator = new FreelancerFeedbackValidator();
    private FreelancerFeedback correctFeedback;

    @Before
    public void setUp() {

        correctFeedback = new FreelancerFeedback()
                .withFeedbackId("feedbackId")
                .withAuthorUid(1L)
                .withFreelancerId(2L)
                .withCreatedTime(ZonedDateTime.now())
                .withOverallMark(1L)
                .withWillRecommend(true)
                .withModerationStatus(FreelancerUgcModerationStatus.IN_PROGRESS)
                .withSkills(Collections.singletonList(FreelancerSkill.CAMPAIGN_CONDUCTING))
                .withFeedbackText("feedback text");
    }

    @Test
    public void freelancerFeedbackValidator_success() {
        ValidationResult<FreelancerFeedback, Defect> result = validator.apply(correctFeedback);

        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    public void freelancerFeedbackValidator_nullFeedbackId() {
        correctFeedback.withFeedbackId(null);

        ValidationResult<FreelancerFeedback, Defect> result = validator.apply(correctFeedback);

        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerFeedback.FEEDBACK_ID)),
                        CommonDefects.notNull()))));
    }

    @Test
    public void freelancerFeedbackValidator_nullAuthorUid() {
        correctFeedback.withAuthorUid(null);

        ValidationResult<FreelancerFeedback, Defect> result = validator.apply(correctFeedback);

        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerFeedback.AUTHOR_UID)),
                        CommonDefects.notNull()))));
    }

    @Test
    public void freelancerFeedbackValidator_nullFreelancerId() {
        correctFeedback.withFreelancerId(null);

        ValidationResult<FreelancerFeedback, Defect> result = validator.apply(correctFeedback);

        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerFeedback.FREELANCER_ID)),
                        CommonDefects.notNull()))));
    }

    @Test
    public void freelancerFeedbackValidator_nullCreatedTime() {
        correctFeedback.withCreatedTime(null);

        ValidationResult<FreelancerFeedback, Defect> result = validator.apply(correctFeedback);

        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerFeedback.CREATED_TIME)),
                        CommonDefects.notNull()))));
    }

    @Test
    public void freelancerFeedbackValidator_nullOverallMark() {
        correctFeedback.withOverallMark(null);

        ValidationResult<FreelancerFeedback, Defect> result = validator.apply(correctFeedback);

        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerFeedback.OVERALL_MARK)),
                        CommonDefects.notNull()))));
    }

    @Test
    public void freelancerFeedbackValidator_nullWillRecommend() {
        correctFeedback.withWillRecommend(null);

        ValidationResult<FreelancerFeedback, Defect> result = validator.apply(correctFeedback);

        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerFeedback.WILL_RECOMMEND)),
                        CommonDefects.notNull()))));
    }

    @Test
    public void freelancerFeedbackValidator_nullSkills() {
        correctFeedback.withSkills(null);

        ValidationResult<FreelancerFeedback, Defect> result = validator.apply(correctFeedback);

        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerFeedback.SKILLS)),
                        CommonDefects.notNull()))));
    }

    @Test
    public void freelancerFeedbackValidator_nullFeedbackText() {
        correctFeedback.withFeedbackText(null);

        ValidationResult<FreelancerFeedback, Defect> result = validator.apply(correctFeedback);

        assertThat(result).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerFeedback.FEEDBACK_TEXT)),
                        CommonDefects.notNull()))));
    }

    @Test
    public void freelancerFeedbackValidator_blankFeedbackText() {
        FreelancerFeedback feedback = correctFeedback.withFeedbackText("");

        ValidationResult<FreelancerFeedback, Defect> result = validator.apply(feedback);

        assertThat(result).is(matchedBy(hasDefectWithDefinition(
                validationError(path(field(FreelancerFeedback.FEEDBACK_TEXT)),
                        StringDefects.notEmptyString()))));
    }

}

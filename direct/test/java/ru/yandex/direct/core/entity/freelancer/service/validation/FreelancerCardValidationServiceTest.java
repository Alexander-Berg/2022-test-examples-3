package ru.yandex.direct.core.entity.freelancer.service.validation;

import java.util.List;

import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.freelancer.model.FreelancerCard;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerContacts;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestFreelancers;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.validation.defect.ids.StringDefectIds;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.DefectIds;
import ru.yandex.direct.validation.result.DefectInfo;
import ru.yandex.direct.validation.result.Path;
import ru.yandex.direct.validation.result.ValidationResult;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.index;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringRunner.class)
public class FreelancerCardValidationServiceTest {

    @Autowired
    FreelancerCardValidationService testedService;

    @Autowired
    Steps steps;

    @Test
    public void validate_success() {
        FreelancerCard card = TestFreelancers.defaultFreelancerCard(1L);
        ValidationResult<List<FreelancerCard>, Defect> result = testedService.validate(singletonList(card));
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_success_onEmptyList() {
        ValidationResult<List<FreelancerCard>, Defect> result = testedService.validate(emptyList());
        assertThat(result).is(matchedBy(hasNoDefectsDefinitions()));
    }

    @Test
    public void validate_error_onNonSetPhone() {
        FreelancerCard wrongCard = TestFreelancers.defaultFreelancerCard(1L);
        wrongCard.getContacts().withPhone("");
        Path errorPath = path(index(0), field(FreelancerCard.CONTACTS.name()), field(FreelancerContacts.PHONE.name()));
        Matcher<DefectInfo<Defect>> expected = validationError(errorPath, StringDefectIds.CANNOT_BE_EMPTY);
        ValidationResult<List<FreelancerCard>, Defect> result = testedService.validate(singletonList(wrongCard));
        assertThat(result).is(matchedBy(hasDefectWithDefinition(expected)));
    }

    @Test
    public void validate_error_onWrongEmail() {
        FreelancerCard wrongCard = TestFreelancers.defaultFreelancerCard(1L);
        wrongCard.getContacts().withEmail("mail@");
        ValidationResult<List<FreelancerCard>, Defect> result = testedService.validate(singletonList(wrongCard));
        Path errorPath = path(index(0), field(FreelancerCard.CONTACTS.name()), field(FreelancerContacts.EMAIL.name()));
        assertThat(result).is(matchedBy(hasDefectWithDefinition(validationError(errorPath, DefectIds.INVALID_VALUE))));
    }

    @Test
    public void preValidate_error_onNonSetFreelancerId() {
        FreelancerCard wrongCard = TestFreelancers.defaultFreelancerCard(1L)
                .withFreelancerId(null);
        ValidationResult<List<FreelancerCard>, Defect> result = testedService.validate(singletonList(wrongCard));
        Path errorPath = path(index(0), field(FreelancerCard.FREELANCER_ID.name()));
        assertThat(result).is(matchedBy(hasDefectWithDefinition(validationError(errorPath, DefectIds.CANNOT_BE_NULL))));
    }
}

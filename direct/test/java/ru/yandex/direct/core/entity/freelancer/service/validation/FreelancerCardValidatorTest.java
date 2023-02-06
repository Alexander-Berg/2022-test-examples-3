package ru.yandex.direct.core.entity.freelancer.service.validation;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;

import ru.yandex.direct.core.entity.freelancer.model.FreelancerCard;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerContacts;
import ru.yandex.direct.core.testing.data.TestFreelancers;
import ru.yandex.direct.testing.matchers.validation.Matchers;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.defect.StringDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.freelancer.service.validation.FreelancerCardValidator.MAX_BRIEF_INFO_LENGTH;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class FreelancerCardValidatorTest {

    private FreelancerCardValidator validator = new FreelancerCardValidator();

    @Test
    public void success_whenCardIsValid() {
        FreelancerCard card = TestFreelancers.defaultFreelancerCard(1L);
        ValidationResult<FreelancerCard, Defect> actual = validator.apply(card);
        assertThat(actual).is(matchedBy(Matchers.hasNoErrorsAndWarnings()));
    }

    @Test
    public void fail_whenContactsNull() {
        FreelancerCard card = TestFreelancers.defaultFreelancerCard(1L)
                .withContacts(null);
        ValidationResult<FreelancerCard, Defect> actual = validator.apply(card);
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerCard.CONTACTS.name())),
                        CommonDefects.notNull()))));
    }

    @Test
    public void fail_whenBriefInfoTooLong() {
        FreelancerCard card = TestFreelancers.defaultFreelancerCard(1L);
        card.withBriefInfo(RandomStringUtils.random(MAX_BRIEF_INFO_LENGTH + 1));
        ValidationResult<FreelancerCard, Defect> actual = validator.apply(card);
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerCard.BRIEF_INFO.name())),
                        CollectionDefects.maxStringLength(MAX_BRIEF_INFO_LENGTH)))));
    }

    @Test
    public void fail_whenPhoneNull() {
        FreelancerCard card = TestFreelancers.defaultFreelancerCard(1L);
        card.getContacts().withPhone(null);
        ValidationResult<FreelancerCard, Defect> actual = validator.apply(card);
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerCard.CONTACTS.name()), field(FreelancerContacts.PHONE.name())),
                        CommonDefects.notNull()))));
    }

    @Test
    public void fail_whenPhoneBlank() {
        FreelancerCard card = TestFreelancers.defaultFreelancerCard(1L);
        card.getContacts().withPhone("");
        ValidationResult<FreelancerCard, Defect> actual = validator.apply(card);
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerCard.CONTACTS.name()), field(FreelancerContacts.PHONE.name())),
                        StringDefects.notEmptyString()))));
    }

    @Test
    public void fail_whenPhoneInvalid() {
        FreelancerCard card = TestFreelancers.defaultFreelancerCard(1L);
        card.getContacts().withPhone("плюс семь 495 три топора -12-34");
        ValidationResult<FreelancerCard, Defect> actual = validator.apply(card);
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerCard.CONTACTS.name()), field(FreelancerContacts.PHONE.name())),
                        CommonDefects.invalidValue()))));
    }

    @Test
    public void fail_whenEmailNull() {
        FreelancerCard card = TestFreelancers.defaultFreelancerCard(1L);
        card.getContacts().withEmail(null);
        ValidationResult<FreelancerCard, Defect> actual = validator.apply(card);
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerCard.CONTACTS.name()), field(FreelancerContacts.EMAIL.name())),
                        CommonDefects.notNull()))));
    }

    @Test
    public void fail_whenEmailBlank() {
        FreelancerCard card = TestFreelancers.defaultFreelancerCard(1L);
        card.getContacts().withEmail("");
        ValidationResult<FreelancerCard, Defect> actual = validator.apply(card);
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerCard.CONTACTS.name()), field(FreelancerContacts.EMAIL.name())),
                        StringDefects.notEmptyString()))));
    }

    @Test
    public void fail_whenEmailInvalid() {
        FreelancerCard card = TestFreelancers.defaultFreelancerCard(1L);
        card.getContacts().withEmail("abc");
        ValidationResult<FreelancerCard, Defect> actual = validator.apply(card);
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerCard.CONTACTS.name()), field(FreelancerContacts.EMAIL.name())),
                        CommonDefects.invalidValue()))));
    }

    @Test
    public void fail_whenIcqInvalid() {
        FreelancerCard card = TestFreelancers.defaultFreelancerCard(1L);
        card.getContacts().withIcq("1234");
        ValidationResult<FreelancerCard, Defect> actual = validator.apply(card);
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerCard.CONTACTS.name()), field(FreelancerContacts.ICQ.name())),
                        CommonDefects.invalidValue()))));
    }

    @Test
    public void fail_whenTelegramInvalid() {
        FreelancerCard card = TestFreelancers.defaultFreelancerCard(1L);
        card.getContacts().withTelegram("@name");
        ValidationResult<FreelancerCard, Defect> actual = validator.apply(card);
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerCard.CONTACTS.name()), field(FreelancerContacts.TELEGRAM.name())),
                        CommonDefects.invalidValue()))));
    }

    @Test
    public void fail_whenWhatsAppInvalid() {
        FreelancerCard card = TestFreelancers.defaultFreelancerCard(1L);
        card.getContacts().withWhatsApp("Arkady");
        ValidationResult<FreelancerCard, Defect> actual = validator.apply(card);
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerCard.CONTACTS.name()), field(FreelancerContacts.WHATS_APP.name())),
                        CommonDefects.invalidValue()))));
    }

    @Test
    public void success_whenSkypeIsValid() {
        FreelancerCard card = TestFreelancers.defaultFreelancerCard(1L);
        card.getContacts().withSkype("live:skype_123");
        ValidationResult<FreelancerCard, Defect> actual = validator.apply(card);
        assertThat(actual).is(matchedBy(Matchers.hasNoErrorsAndWarnings()));
    }

    @Test
    public void fail_whenSkypeIsInvalid() {
        FreelancerCard card = TestFreelancers.defaultFreelancerCard(1L);
        card.getContacts().withSkype("123");
        ValidationResult<FreelancerCard, Defect> actual = validator.apply(card);
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerCard.CONTACTS.name()), field(FreelancerContacts.SKYPE.name())),
                        CommonDefects.invalidValue()))));
    }

    @Test
    public void fail_whenSkypeIsBlank() {
        FreelancerCard card = TestFreelancers.defaultFreelancerCard(1L);
        card.getContacts().withSkype("");
        ValidationResult<FreelancerCard, Defect> actual = validator.apply(card);
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerCard.CONTACTS.name()), field(FreelancerContacts.SKYPE.name())),
                        StringDefects.notEmptyString()))));
    }

    @Test
    public void success_whenViberIsValid() {
        FreelancerCard card = TestFreelancers.defaultFreelancerCard(1L);
        card.getContacts().withViber("+798512345678");
        ValidationResult<FreelancerCard, Defect> actual = validator.apply(card);
        assertThat(actual).is(matchedBy(Matchers.hasNoErrorsAndWarnings()));
    }

    @Test
    public void fail_whenViberIsInvalid() {
        FreelancerCard card = TestFreelancers.defaultFreelancerCard(1L);
        card.getContacts().withViber("viber");
        ValidationResult<FreelancerCard, Defect> actual = validator.apply(card);
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerCard.CONTACTS.name()), field(FreelancerContacts.VIBER.name())),
                        CommonDefects.invalidValue()))));
    }

    @Test
    public void fail_whenViberIsBlank() {
        FreelancerCard card = TestFreelancers.defaultFreelancerCard(1L);
        card.getContacts().withViber("");
        ValidationResult<FreelancerCard, Defect> actual = validator.apply(card);
        assertThat(actual).is(matchedBy(hasDefectDefinitionWith(
                validationError(
                        path(field(FreelancerCard.CONTACTS.name()), field(FreelancerContacts.VIBER.name())),
                        StringDefects.notEmptyString()))));
    }
}

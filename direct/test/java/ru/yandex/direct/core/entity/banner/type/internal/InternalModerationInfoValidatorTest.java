package ru.yandex.direct.core.entity.banner.type.internal;

import org.junit.Before;
import org.junit.Test;

import ru.yandex.direct.core.entity.banner.model.InternalModerationInfo;
import ru.yandex.direct.validation.defect.CommonDefects;

import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.banner.model.InternalModerationInfo.IS_SECRET_AD;
import static ru.yandex.direct.core.entity.banner.model.InternalModerationInfo.SEND_TO_MODERATION;
import static ru.yandex.direct.core.entity.banner.model.InternalModerationInfo.STATUS_SHOW_AFTER_MODERATION;
import static ru.yandex.direct.core.entity.banner.model.InternalModerationInfo.TICKET_URL;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrorsAndWarnings;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

public class InternalModerationInfoValidatorTest {

    private static final String CORRECT_TICKET_URL = "https://st.yandex-team.ru/DIRECT-135207";
    private static final String INCORRECT_TICKET_URL = "incorrect url";

    InternalModerationInfoValidator validator;

    @Before
    public void setUp() {
        validator = createValidator();
    }

    @Test
    public void correct() {
        var moderationInfo = new InternalModerationInfo()
                .withTicketUrl(CORRECT_TICKET_URL)
                .withIsSecretAd(true)
                .withCustomComment("comment")
                .withSendToModeration(false)
                .withStatusShowAfterModeration(false);

        var validationResult = validator.apply(moderationInfo);

        assertThat(validationResult, hasNoErrorsAndWarnings());
    }

    @Test
    public void correctWithoutComment() {
        var moderationInfo = new InternalModerationInfo()
                .withTicketUrl(CORRECT_TICKET_URL)
                .withIsSecretAd(false)
                .withSendToModeration(true)
                .withStatusShowAfterModeration(true);

        var validationResult = validator.apply(moderationInfo);

        assertThat(validationResult, hasNoErrorsAndWarnings());
    }

    @Test
    public void validationErrorIfTicketUrlIsNull() {
        var moderationInfo = new InternalModerationInfo()
                .withIsSecretAd(true)
                .withStatusShowAfterModeration(true);

        var validationResult = validator.apply(moderationInfo);

        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(field(TICKET_URL)), CommonDefects.notNull())));
    }

    @Test
    public void validationErrorIfTicketUrlIsIncorrect() {
        var moderationInfo = new InternalModerationInfo()
                .withTicketUrl(INCORRECT_TICKET_URL)
                .withIsSecretAd(true)
                .withStatusShowAfterModeration(true);

        var validationResult = validator.apply(moderationInfo);

        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(field(TICKET_URL)), CommonDefects.invalidValue())));
    }

    @Test
    public void validationErrorIfIsSecretAdIsNull() {
        var moderationInfo = new InternalModerationInfo()
                .withTicketUrl(CORRECT_TICKET_URL)
                .withStatusShowAfterModeration(true);

        var validationResult = validator.apply(moderationInfo);

        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(field(IS_SECRET_AD)), CommonDefects.notNull())));
    }

    @Test
    public void validationError_IfStatusShowAfterModerationIsNull() {
        var moderationInfo = new InternalModerationInfo()
                .withTicketUrl(CORRECT_TICKET_URL)
                .withIsSecretAd(false)
                .withStatusShowAfterModeration(null);

        var validationResult = validator.apply(moderationInfo);

        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(field(STATUS_SHOW_AFTER_MODERATION)), CommonDefects.notNull())));
    }

    @Test
    public void validationError_IfSendToModerationIsNull() {
        var moderationInfo = new InternalModerationInfo()
                .withTicketUrl(CORRECT_TICKET_URL)
                .withIsSecretAd(false)
                .withStatusShowAfterModeration(true)
                .withSendToModeration(null);

        var validationResult = validator.apply(moderationInfo);

        assertThat(validationResult, hasDefectDefinitionWith(
                validationError(path(field(SEND_TO_MODERATION)), CommonDefects.notNull())));
    }

    private InternalModerationInfoValidator createValidator() {
        return new InternalModerationInfoValidator();
    }
}

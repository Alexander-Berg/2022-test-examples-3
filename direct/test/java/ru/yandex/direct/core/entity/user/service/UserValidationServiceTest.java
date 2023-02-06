package ru.yandex.direct.core.entity.user.service;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.service.validation.UserValidationService;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.validation.defect.CollectionDefects;
import ru.yandex.direct.validation.defect.CommonDefects;
import ru.yandex.direct.validation.defect.StringDefects;
import ru.yandex.direct.validation.result.Defect;
import ru.yandex.direct.validation.result.ValidationResult;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoDefectsDefinitions;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class UserValidationServiceTest {

    @Autowired
    private UserValidationService userValidationService;

    @Test
    public void validateUser_valid() throws Exception {
        User user = createValidUser();
        ValidationResult<User, Defect> result = userValidationService.validateUser(user);
        assertThat(result, hasNoDefectsDefinitions());
    }

    @Test
    public void validateUser_nullEmail() throws Exception {
        User user = createValidUser().withEmail(null);
        ValidationResult<User, Defect> result = userValidationService.validateUser(user);
        assertThat(result.flattenErrors(), contains(
                validationError(path(field("email")), CommonDefects.notNull())));
    }

    @Test
    public void validateUser_emptyEmail() throws Exception {
        User user = createValidUser().withEmail("");
        ValidationResult<User, Defect> result = userValidationService.validateUser(user);
        assertThat(result.flattenErrors(), contains(
                validationError(path(field("email")), StringDefects.notEmptyString())));
    }

    @Test
    public void validateUser_tooLongEmail() throws Exception {
        User user = createValidUser().withEmail(StringUtils.repeat("x", 256));
        ValidationResult<User, Defect> result = userValidationService.validateUser(user);
        assertThat(result.flattenErrors(), contains(
                validationError(path(field("email")), CollectionDefects.maxStringLength(255))));
    }

    @Test
    public void validateUser_invalidEmail() throws Exception {
        User user = createValidUser().withEmail("2");
        ValidationResult<User, Defect> result = userValidationService.validateUser(user);
        assertThat(result.flattenErrors(), contains(
                validationError(path(field("email")), CommonDefects.invalidValue())));
    }

    @Test
    public void validateUser_nullFio() throws Exception {
        User user = createValidUser().withFio(null);
        ValidationResult<User, Defect> result = userValidationService.validateUser(user);
        assertThat(result.flattenErrors(), contains(
                validationError(path(field("fio")), CommonDefects.notNull())));
    }

    @Test
    public void validateUser_emptyFio() throws Exception {
        User user = createValidUser().withFio("");
        ValidationResult<User, Defect> result = userValidationService.validateUser(user);
        assertThat(result.flattenErrors(), contains(
                validationError(path(field("fio")), StringDefects.notEmptyString())));
    }

    @Test
    public void validateUser_tooLongFio() throws Exception {
        User user = createValidUser().withFio(StringUtils.repeat("x", 256));
        ValidationResult<User, Defect> result = userValidationService.validateUser(user);
        assertThat(result.flattenErrors(), contains(
                validationError(path(field("fio")), CollectionDefects.maxStringLength(255))));
    }

    @Test
    public void validateUser_invalidCharsFio() throws Exception {
        User user = createValidUser().withFio("a\nb");
        ValidationResult<User, Defect> result = userValidationService.validateUser(user);
        assertThat(result.flattenErrors(), contains(
                validationError(path(field("fio")), StringDefects.admissibleChars())));
    }

    @Test
    public void validateUser_emptyPhone() throws Exception {
        User user = createValidUser().withPhone("");
        ValidationResult<User, Defect> result = userValidationService.validateUser(user);
        assertThat(result.flattenErrors(), contains(
                validationError(path(field("phone")), StringDefects.notEmptyString())));
    }

    @Test
    public void validateUser_tooLongPhone() throws Exception {
        User user = createValidUser().withPhone(StringUtils.repeat("1", 256));
        ValidationResult<User, Defect> result = userValidationService.validateUser(user);
        assertThat(result.flattenErrors(), contains(
                validationError(path(field("phone")), CollectionDefects.maxStringLength(255))));
    }

    private User createValidUser() {
        return new User()
                .withEmail("aaa@ooo.ru")
                .withFio("aaa")
                .withPhone("+72323769936");
    }
}

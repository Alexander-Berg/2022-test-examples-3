package ru.yandex.market.replenishment.autoorder.validation;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.validation.annotation.UserLoginExists;
import ru.yandex.market.replenishment.autoorder.validation.validator.UserLoginExistsValidator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class UserLoginExistsTest extends FunctionalTest {
    private static final List<String> LOGINS_EXISTS = List.of("User1", "User2");
    private static final List<String> LOGINS_NOT_EXISTS = List.of("NotExists1", "NotExists2");

    @Autowired
    private UserLoginExistsValidator userLoginExistsValidator;

    @UserLoginExists
    private String login;

    @Test
    @DbUnitDataSet(before = "UserLoginExistsTest.before.csv")
    public void testLoginExists_isOk() throws NoSuchFieldException {
        ConstraintValidatorContextImpl constraintValidatorContext = mock(ConstraintValidatorContextImpl.class);
        String userLogin = LOGINS_EXISTS.get(0);
        UserLoginExists annotation = UserLoginExistsTest.class.getDeclaredField("login")
            .getAnnotation(UserLoginExists.class);
        userLoginExistsValidator.initialize(annotation);
        assertTrue(userLoginExistsValidator.isValid(userLogin, constraintValidatorContext));
        verify(constraintValidatorContext, never()).addMessageParameter(anyString(), any());
    }

    @Test
    @DbUnitDataSet(before = "UserLoginExistsTest.before.csv")
    public void testLoginNotExists_throwException() throws NoSuchFieldException {
        ConstraintValidatorContextImpl constraintValidatorContext = mock(ConstraintValidatorContextImpl.class);
        String userLogin = LOGINS_NOT_EXISTS.get(0);
        UserLoginExists annotation = UserLoginExistsTest.class.getDeclaredField("login")
            .getAnnotation(UserLoginExists.class);
        userLoginExistsValidator.initialize(annotation);
        assertFalse(userLoginExistsValidator.isValid(userLogin, constraintValidatorContext));
        verify(constraintValidatorContext).addMessageParameter(UserLoginExists.MESSAGE_KEY,
            "Пользователь с указанным логином не существует " + userLogin);
    }

    @Test
    @DbUnitDataSet(before = "UserLoginExistsTest.before.csv")
    public void testLoginsExists_isOk() throws NoSuchFieldException {
        ConstraintValidatorContextImpl constraintValidatorContext = mock(ConstraintValidatorContextImpl.class);
        UserLoginExists annotation = UserLoginExistsTest.class.getDeclaredField("login")
            .getAnnotation(UserLoginExists.class);
        userLoginExistsValidator.initialize(annotation);
        assertTrue(userLoginExistsValidator.isValid(LOGINS_EXISTS, constraintValidatorContext));
        verify(constraintValidatorContext, never()).addMessageParameter(anyString(), any());
    }

    @Test
    @DbUnitDataSet(before = "UserLoginExistsTest.before.csv")
    public void testLoginsNotExists_throwException() throws NoSuchFieldException {
        ConstraintValidatorContextImpl constraintValidatorContext = mock(ConstraintValidatorContextImpl.class);
        UserLoginExists annotation = UserLoginExistsTest.class.getDeclaredField("login")
            .getAnnotation(UserLoginExists.class);
        userLoginExistsValidator.initialize(annotation);
        assertFalse(userLoginExistsValidator.isValid(LOGINS_NOT_EXISTS, constraintValidatorContext));
        verify(constraintValidatorContext).addMessageParameter(UserLoginExists.MESSAGE_KEY,
            "Пользователь с указанным логином не существует "
                + LOGINS_NOT_EXISTS.stream().collect(Collectors.joining(", ")));
    }
}

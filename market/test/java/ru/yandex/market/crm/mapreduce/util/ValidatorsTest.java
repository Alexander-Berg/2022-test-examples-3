package ru.yandex.market.crm.mapreduce.util;

import org.junit.Test;

import ru.yandex.market.crm.mapreduce.domain.user.UidType;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ValidatorsTest {

    @Test
    public void isValidEmail() {
        assertTrue(Validators.isValidUid("kk@ya.ru", UidType.EMAIL));
        assertFalse(Validators.isValidUid("kk@ya.r", UidType.EMAIL));
        assertFalse(Validators.isValidUid("somestring", UidType.EMAIL));
    }
    @Test
    public void checkValidityNullOrEmptyUids() {
        assertTrue(
                UidType.ALL.stream().allMatch(uidType ->
                !Validators.isValidUid("", uidType) && !Validators.isValidUid(null, uidType))
        );
    }
}

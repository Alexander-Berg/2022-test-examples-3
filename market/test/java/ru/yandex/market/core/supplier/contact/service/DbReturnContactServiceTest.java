package ru.yandex.market.core.supplier.contact.service;

import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.api.cpa.yam.exception.InvalidPrepayRequestOperationException;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.supplier.contact.model.ReturnContact;
import ru.yandex.market.core.supplier.contact.model.ReturnContactType;

/**
 * Функциональные тесты на {@link ru.yandex.market.core.supplier.contact.service.ReturnContactService}.
 *
 * @author stani on 16.02.18.
 */
@DbUnitDataSet(before = "DbReturnContactServiceTest.before.csv")
class DbReturnContactServiceTest extends FunctionalTest {

    @Autowired
    private ReturnContactService returnContactService;

    @Test
    @DbUnitDataSet(after = "testCreate.after.csv")
    void testCreate() {
        returnContactService.saveReturnContactsSnapshot(
                Collections.singletonList(
                        ReturnContact.builder()
                                .setSupplierId(102L)
                                .setFirstName("Алексей")
                                .setLastName("Метелкин")
                                .setPhoneNumber("+79161005060")
                                .setEmail("alexey@yandex.ru")
                                .setEnabled(false)
                                .build()),
                true,
                100500L
        );
    }

    @Test
    @DbUnitDataSet(after = "testUpdate.after.csv")
    void testUpdate() {
        returnContactService.saveReturnContactsSnapshot(Collections.singletonList(
                ReturnContact.builder()
                        .setSupplierId(100L)
                        .setFirstName("Иван")
                        .setLastName("Стелькин")
                        .setPhoneNumber("+791820055555")
                        .setEmail("ivan@yandex.ru")
                        .setEnabled(true)
                        .build())
                ,
                false,
                100500L
        );
    }

    @Test
    @DbUnitDataSet(after = "testUpdateSnapshot.after.csv")
    void testUpdateSnapshotWithDeletion() {
        returnContactService.saveReturnContactsSnapshot(Arrays.asList(
                ReturnContact.builder()
                        .setSupplierId(100L)
                        .setType(ReturnContactType.POST)
                        .setCompanyName("Маленькая такая компания, с огромным таким секретом")
                        .setAddress("Москва, Красная площадь, д.1")
                        .setEnabled(true)
                        .build(),
                ReturnContact.builder()
                        .setSupplierId(101L)
                        .setType(ReturnContactType.SELF)
                        .setEnabled(false)
                        .build(),
                ReturnContact.builder()
                        .setSupplierId(102L)
                        .setType(ReturnContactType.SELF)
                        .setPhoneNumber("+71111111111")
                        .setAddress("Москва, Красная площадь, д.2")
                        .setComments("Без комментариев")
                        .setEnabled(true)
                        .build()),
                true,
                100500L
        );
    }

    @Test
    void testIntermediateUpdate() {
        returnContactService.saveReturnContactsSnapshot(Collections.singletonList(
                ReturnContact.builder()
                        .setSupplierId(100L)
                        .setFirstName(null)
                        .setLastName(null)
                        .setPhoneNumber("+791820055555")
                        .setEmail("ivan@yandex.ru")
                        .build())
                ,
                true,
                100500L
        );
    }

    @Test
    void testRequiredField() {
        Assertions.assertThrows(
                InvalidPrepayRequestOperationException.class,
                () -> returnContactService.saveReturnContactsSnapshot(Collections.singletonList(
                        ReturnContact.builder()
                                .setSupplierId(102L)
                                .setFirstName("hello")
                                .setLastName("world")
                                .setPhoneNumber("112")
                                .setEmail(null)
                                .build()),
                        false,
                        100500L
                )
        );
    }

    @Test
    void testRequiredMaxSizeField() {
        Assertions.assertThrows(
                InvalidPrepayRequestOperationException.class,
                () -> returnContactService.saveReturnContactsSnapshot(Collections.singletonList(
                        ReturnContact.builder()
                                .setSupplierId(102L)
                                .setFirstName("hello")
                                .setLastName("world")
                                .setPhoneNumber(RandomStringUtils.randomAlphanumeric(101))
                                .setEmail("hello@yandex.ru")
                                .build()),
                        false,
                        100500L
                )
        );
    }
}

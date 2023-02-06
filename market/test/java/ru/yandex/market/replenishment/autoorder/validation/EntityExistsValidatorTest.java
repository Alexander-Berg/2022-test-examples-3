package ru.yandex.market.replenishment.autoorder.validation;


import java.util.Set;

import org.hibernate.validator.internal.engine.constraintvalidation.ConstraintValidatorContextImpl;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.repository.postgres.SupplierRequestRepository;
import ru.yandex.market.replenishment.autoorder.validation.annotation.MyBatisEntityExists;
import ru.yandex.market.replenishment.autoorder.validation.validator.EntityExistsValidator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
public class EntityExistsValidatorTest extends FunctionalTest {

    @Autowired
    private EntityExistsValidator entityExistsValidator;

    @MyBatisEntityExists(myBatisRepository = SupplierRequestRepository.class)
    private Set<Long> ids;

    @Test
    @DbUnitDataSet(before = "EntityExistsValidatorTest.before.csv")
    public void existsIdsIsAccepted() throws NoSuchFieldException {
        ConstraintValidatorContextImpl constraintValidatorContext = mock(ConstraintValidatorContextImpl.class);
        ids = Set.of(1L, 101L);
        MyBatisEntityExists annotation = EntityExistsValidatorTest.class.getDeclaredField("ids")
                .getAnnotation(MyBatisEntityExists.class);
        entityExistsValidator.initialize(annotation);
        assertTrue(entityExistsValidator.isValid(ids, constraintValidatorContext));
        verify(constraintValidatorContext, never()).addMessageParameter(anyString(), any());

    }

    @Test
    @DbUnitDataSet(before = "EntityExistsValidatorTest.before.csv")
    public void absentIdsIsNotAccepted() throws NoSuchFieldException {
        ConstraintValidatorContextImpl constraintValidatorContext = mock(ConstraintValidatorContextImpl.class);
        ids = Set.of(1L, 101L, 201L, 301L);
        MyBatisEntityExists annotation = EntityExistsValidatorTest.class.getDeclaredField("ids")
                .getAnnotation(MyBatisEntityExists.class);
        entityExistsValidator.initialize(annotation);
        assertFalse(entityExistsValidator.isValid(ids, constraintValidatorContext));
        verify(constraintValidatorContext).addMessageParameter(MyBatisEntityExists.MESSAGE_KEY,
                "Entity of class SupplierRequestRepository with id 201, 301 is not exists");
    }
}

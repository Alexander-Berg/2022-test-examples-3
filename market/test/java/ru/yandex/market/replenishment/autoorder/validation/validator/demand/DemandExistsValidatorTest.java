package ru.yandex.market.replenishment.autoorder.validation.validator.demand;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.validation.ConstraintTarget;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import lombok.Data;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.replenishment.autoorder.config.FunctionalTest;
import ru.yandex.market.replenishment.autoorder.model.DemandType;
import ru.yandex.market.replenishment.autoorder.model.WithDemandIds;
import ru.yandex.market.replenishment.autoorder.model.WithDemandType;
import ru.yandex.market.replenishment.autoorder.validation.annotation.DemandExists;
public class DemandExistsValidatorTest extends FunctionalTest {

    @Autowired
    private Validator validator;

    @Test
    @DbUnitDataSet(before = "DemandExistsValidatorTest.before.csv")
    public void demandExistsOnMethodNegative1Test() throws NoSuchMethodException {
        final Set<ConstraintViolation<TestDTO>> violations = validator.forExecutables()
                .validateParameters(
                        new TestDTO(),
                        TestDTO.class.getMethod("method1", DemandType.class, long.class),
                        new Object[]{DemandType.TYPE_1P, 2L});
        Assert.assertFalse(violations.isEmpty());
    }

    @Test
    @DbUnitDataSet(before = "DemandExistsValidatorTest.before.csv")
    public void demandExistsOnMethodNegative2Test() throws NoSuchMethodException {
        final Set<ConstraintViolation<TestDTO>> violations = validator.forExecutables()
                .validateParameters(
                        new TestDTO(),
                        TestDTO.class.getMethod("method1", DemandType.class, long.class),
                        new Object[]{DemandType.TYPE_3P, 1L});
        Assert.assertFalse(violations.isEmpty());
    }

    @Test
    @DbUnitDataSet(before = "DemandExistsValidatorTest.before.csv")
    public void demandExistsOnMethodNegative3Test() throws NoSuchMethodException {
        final Set<ConstraintViolation<TestDTO>> violations = validator.forExecutables()
                .validateParameters(
                        new TestDTO(),
                        TestDTO.class.getMethod("method1", DemandType.class, long.class),
                        new Object[]{null, 1L});
        Assert.assertTrue(violations.isEmpty());
    }

    @Test
    @DbUnitDataSet(before = "DemandExistsValidatorTest.before.csv")
    public void demandExistsOnMethodPositive1Test() throws NoSuchMethodException {
        final Set<ConstraintViolation<TestDTO>> violations = validator.forExecutables()
                .validateParameters(
                        new TestDTO(),
                        TestDTO.class.getMethod("method1", DemandType.class, long.class),
                        new Object[]{DemandType.TYPE_1P, 1L});
        Assert.assertTrue(violations.isEmpty());
    }

    @Test
    @DbUnitDataSet(before = "DemandExistsValidatorTest.before.csv")
    public void demandExistsOnMethodPositive2Test() throws NoSuchMethodException {
        final Set<ConstraintViolation<TestDTO>> violations = validator.forExecutables()
                .validateParameters(
                        new TestDTO(),
                        TestDTO.class.getMethod("method2", DemandType.class, TestDTO.TestWithDemandIds.class),
                        new Object[]{DemandType.TYPE_1P, new TestDTO.TestWithDemandIds(Collections.singleton(1L))});
        Assert.assertTrue(violations.isEmpty());
    }

    @Test
    @DbUnitDataSet(before = "DemandExistsValidatorTest.before.csv")
    public void demandExistsOnMethodNegative4Test() throws NoSuchMethodException {
        final Set<ConstraintViolation<TestDTO>> violations = validator.forExecutables()
                .validateParameters(
                        new TestDTO(),
                        TestDTO.class.getMethod("method2", DemandType.class, TestDTO.TestWithDemandIds.class),
                        new Object[]{DemandType.TYPE_1P, new TestDTO.TestWithDemandIds(Collections.singleton(2L))});
        Assert.assertFalse(violations.isEmpty());
    }

    @Test
    @DbUnitDataSet(before = "DemandExistsValidatorTest.before.csv")
    public void demandExistsOnMethodPositive3Test() throws NoSuchMethodException {
        final Set<ConstraintViolation<TestDTO>> violations = validator.forExecutables()
                .validateParameters(
                        new TestDTO(),
                        TestDTO.class.getMethod("method2", DemandType.class, TestDTO.TestWithDemandIds.class),
                        new Object[]{DemandType.TYPE_1P, null});
        Assert.assertTrue(violations.isEmpty());
    }

    @Test
    @DbUnitDataSet(before = "DemandExistsValidatorTest.before.csv")
    public void demandExistsOnMethodPositive4Test() throws NoSuchMethodException {
        final Set<ConstraintViolation<TestDTO>> violations = validator.forExecutables()
                .validateParameters(
                        new TestDTO(),
                        TestDTO.class.getMethod("method3", TestDTO.TestWithDemandIdsAndDemandType.class),
                        new Object[]{new TestDTO.TestWithDemandIdsAndDemandType(Collections.singleton(1L),
                                DemandType.TYPE_1P)});
        Assert.assertTrue(violations.isEmpty());
    }

    @Test
    @DbUnitDataSet(before = "DemandExistsValidatorTest.before.csv")
    public void demandExistsOnMethodNegative5Test() throws NoSuchMethodException {
        final Set<ConstraintViolation<TestDTO>> violations = validator.forExecutables()
                .validateParameters(
                        new TestDTO(),
                        TestDTO.class.getMethod("method3", TestDTO.TestWithDemandIdsAndDemandType.class),
                        new Object[]{new TestDTO.TestWithDemandIdsAndDemandType(Collections.singleton(2L),
                                DemandType.TYPE_1P)});
        Assert.assertFalse(violations.isEmpty());
    }

    @Test
    @DbUnitDataSet(before = "DemandExistsValidatorTest.before.csv")
    public void demandExistsOnMethodPositive5Test() throws NoSuchMethodException {
        final Set<ConstraintViolation<TestDTO>> violations = validator.forExecutables()
                .validateParameters(
                        new TestDTO(),
                        TestDTO.class.getMethod("method3", TestDTO.TestWithDemandIdsAndDemandType.class),
                        new Object[]{new TestDTO.TestWithDemandIdsAndDemandType(Collections.singleton(1L), null)});
        Assert.assertTrue(violations.isEmpty());
    }

    @Test
    @DbUnitDataSet(before = "DemandExistsValidatorTest.before.csv")
    public void demandExistsOnMethodPositive6Test() throws NoSuchMethodException {
        final Set<ConstraintViolation<TestDTO>> violations = validator.forExecutables()
                .validateParameters(
                        new TestDTO(),
                        TestDTO.class.getMethod("method4", DemandType.class, Long.class, Long.class),
                        new Object[]{DemandType.TYPE_1P, 1L, 234L});
        Assert.assertTrue(violations.isEmpty());
    }

    @Test
    @DbUnitDataSet(before = "DemandExistsValidatorTest.before.csv")
    public void demandExistsOnMethodNegative6Test() throws NoSuchMethodException {
        final Set<ConstraintViolation<TestDTO>> violations = validator.forExecutables()
                .validateParameters(
                        new TestDTO(),
                        TestDTO.class.getMethod("method4", DemandType.class, Long.class, Long.class),
                        new Object[]{DemandType.TYPE_3P, 1L, 234L});
        Assert.assertFalse(violations.isEmpty());
    }

    @Test
    @DbUnitDataSet(before = "DemandExistsValidatorTest.before.csv")
    public void demandExistsOnClassPositive1Test() {
        final Set<ConstraintViolation<TestDTO.TestWithDemandIdsAndDemandType>> violations =
                validator.validate(new TestDTO.TestWithDemandIdsAndDemandType(Collections.singleton(1L),
                        DemandType.TYPE_1P));
        Assert.assertTrue(violations.isEmpty());
    }

    @Test
    @DbUnitDataSet(before = "DemandExistsValidatorTest.before.csv")
    public void demandExistsOnClassNegative1Test() {
        final Set<ConstraintViolation<TestDTO.TestWithDemandIdsAndDemandType>> violations =
                validator.validate(new TestDTO.TestWithDemandIdsAndDemandType(Collections.singleton(2L),
                        DemandType.TYPE_1P));
        Assert.assertFalse(violations.isEmpty());
    }

    public static class TestDTO {

        @DemandExists(validationAppliesTo = ConstraintTarget.PARAMETERS)
        public void method1(DemandType demandType, long demandId) {
            // NOTHING
        }

        @DemandExists(validationAppliesTo = ConstraintTarget.PARAMETERS)
        public void method2(DemandType demandType, TestWithDemandIds withDemandIds) {
            // NOTHING
        }

        @DemandExists(validationAppliesTo = ConstraintTarget.PARAMETERS)
        public void method3(TestWithDemandIdsAndDemandType withDemandIdsAndDemandType) {
            // NOTHING
        }

        @DemandExists(demandIdParam = "demandId", demandTypeParam = "demandType",
                validationAppliesTo = ConstraintTarget.PARAMETERS)
        public void method4(DemandType demandType, Long demandId, Long someId) {
            // NOTHING
        }

        @Data
        public static class TestWithDemandIds implements WithDemandIds {
            private final Collection<Long> demandIds;
        }

        @DemandExists
        @Data
        public static class TestWithDemandIdsAndDemandType implements WithDemandIds, WithDemandType {
            private final Collection<Long> demandIds;
            private final DemandType demandType;
        }
    }
}

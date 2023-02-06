package ru.yandex.market.mcrm.db.test;

import java.util.function.Predicate;

import org.springframework.test.context.TestContext;
import org.springframework.test.context.support.AbstractTestExecutionListener;

public class CleanDbTestExecutionListener extends AbstractTestExecutionListener {

    private static final Predicate<CleanDb.Mode> IS_BEFORE =
            mode -> CleanDb.Mode.BEFORE == mode || CleanDb.Mode.AROUND == mode;

    private static final Predicate<CleanDb.Mode> IS_AFTER =
            mode -> CleanDb.Mode.AFTER == mode || CleanDb.Mode.AROUND == mode;


    @Override
    public void beforeTestClass(TestContext testContext) throws Exception {
        CleanDb cleanDb = testContext.getTestClass().getAnnotation(CleanDb.class);
        if (cleanDb != null && IS_BEFORE.test(cleanDb.classMode())) {
            TestDbCleaner.reinitializeDb();
        }
    }

    @Override
    public void afterTestClass(TestContext testContext) throws Exception {
        CleanDb cleanDb = testContext.getTestClass().getAnnotation(CleanDb.class);
        if (cleanDb != null && IS_AFTER.test(cleanDb.classMode())) {
            TestDbCleaner.reinitializeDb();
        }
    }

    @Override
    public void beforeTestMethod(TestContext testContext) throws Exception {
        CleanDb cleanDb = testContext.getTestMethod().getAnnotation(CleanDb.class);
        if (cleanDb == null) {
            cleanDb = testContext.getTestClass().getAnnotation(CleanDb.class);
        }
        if (cleanDb != null && IS_BEFORE.test(cleanDb.methodMode())) {
            TestDbCleaner.reinitializeDb();
        }
    }

    @Override
    public void afterTestMethod(TestContext testContext) throws Exception {
        CleanDb cleanDb = testContext.getTestMethod().getAnnotation(CleanDb.class);
        if (cleanDb == null) {
            cleanDb = testContext.getTestClass().getAnnotation(CleanDb.class);
        }
        if (cleanDb != null && IS_AFTER.test(cleanDb.methodMode())) {
            TestDbCleaner.reinitializeDb();
        }
    }
}

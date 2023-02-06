package org.junit.runners.model;


/**
 * Represents one or more actions to be taken at runtime in the course
 * of running a JUnit test suite.
 * <p>
 * Скопирован из junit-4.12
 * <a href = "https://a.yandex-team.ru/arc/trunk/arcadia/direct/libs/test-utils-junit5/src/main/java/org/junit/runners/model/Statement.java?rev=r9411820">оригинал</a>
 */
public abstract class Statement {
    /**
     * Run the action, throwing a {@code Throwable} if anything goes wrong.
     */
    public abstract void evaluate() throws Throwable;
}

package org.junit.rules;

/**
 * Обманываем MockWebServer, который хочет быть TestRule от junit4.
 * <p>
 * Можем так делать, потому что не используем в тестах напрямую MockWebServer,
 * а всю работу по инициализации и завершении мы делаем сами в MockedHttpWebServer.
 * <a href = "https://a.yandex-team.ru/arc/trunk/arcadia/direct/libs/test-utils-junit5/src/main/java/org/junit/rules/TestRule.java?rev=r9411820">оригинал</a>
 */
public interface TestRule {
}

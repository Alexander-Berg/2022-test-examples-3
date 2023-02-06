package ru.yandex.direct.core.testing.stub;

import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.jooq.impl.DSL;

import ru.yandex.direct.dbutil.wrapper.DatabaseWrapperProvider;

import static ru.yandex.direct.dbschema.stubs.Tables.PASSPORT_USERS;

/**
 * Stub для Паспорта в тестах. Позволяет генерировать уникальные uid
 */
@ParametersAreNonnullByDefault
public class PassportClientStub {
    private final DatabaseWrapperProvider databaseWrapperProvider;

    private PassportClientStub(DatabaseWrapperProvider databaseWrapperProvider) {
        this.databaseWrapperProvider = databaseWrapperProvider;
    }

    public static PassportClientStub newInstance(DatabaseWrapperProvider databaseWrapperProvider) {
        PassportClientStub result = new PassportClientStub(databaseWrapperProvider);
        result.afterConstruction();
        return result;
    }

    private synchronized void afterConstruction() {
        // Гарантируем, что новые uid-и будут генерироваться после предопределенных в RBAC-е
        databaseWrapperProvider.get("stubs").getDslContext()
                .insertInto(PASSPORT_USERS)
                .columns(PASSPORT_USERS.UID, PASSPORT_USERS.LOGIN)
                .values(54L, "");
    }

    public synchronized Long generateNewUserUid() {
        return generateNewUserUid(null);
    }

    /**
     * Сгенерировать уникальный uid для нового пользователя
     * <p>
     * Делаем syncronized для того, чтобы минимизировать риск коллизий login-ов
     * при параллельном запуске
     */
    @Nonnull
    public synchronized Long generateNewUserUid(@Nullable String login) {
        // Есть риск, что будет нарушение уникальности
        //Логин должен быть в lowerCase, иначе это может приводить к спецэффектам в тестах
        //т.к. в некоторых местах логин приводится к нижнему регистру
        String internalLogin = login == null ? "fakeuser-"
                + System.nanoTime() + "-"
                + ThreadLocalRandom.current().nextInt(10)
                : login;
        return databaseWrapperProvider.get("stubs")
                .getDslContext()
                .insertInto(PASSPORT_USERS)
                .set(PASSPORT_USERS.UID, DSL.defaultValue(Long.class))
                .set(PASSPORT_USERS.LOGIN, internalLogin)
                .returning(PASSPORT_USERS.UID)
                .fetchOne()
                .getUid();
    }

    public String getLoginByUid(Long uid) {
        return databaseWrapperProvider.get("stubs")
                .getDslContext()
                .select(PASSPORT_USERS.LOGIN)
                .from(PASSPORT_USERS)
                .where(PASSPORT_USERS.UID.eq(uid))
                .fetchOne(PASSPORT_USERS.LOGIN);
    }

    public Long getUidByLogin(String login) {
        return databaseWrapperProvider.get("stubs")
                .getDslContext()
                .select(PASSPORT_USERS.UID)
                .from(PASSPORT_USERS)
                .where(PASSPORT_USERS.LOGIN.eq(login))
                .fetchOne(PASSPORT_USERS.UID);
    }
}

package ru.yandex.direct.core.testing.stub;

import org.apache.commons.lang3.RandomStringUtils;

import ru.yandex.direct.dbschema.stubs.tables.records.PassportUsersRecord;
import ru.yandex.direct.dbutil.wrapper.DatabaseWrapperProvider;

import static ru.yandex.direct.dbschema.stubs.Tables.PASSPORT_USERS;

public class BlackboxUserStub {

    private static final int DEFAULT_LENGTH_RANDOM_LOGIN = 10;

    private DatabaseWrapperProvider databaseWrapperProvider;

    public BlackboxUserStub(DatabaseWrapperProvider databaseWrapperProvider) {
        this.databaseWrapperProvider = databaseWrapperProvider;
    }

    public long createUser() {
        PassportUsersRecord usersRecord = databaseWrapperProvider.get("stubs").getDslContext()
                .insertInto(PASSPORT_USERS)
                .set(PASSPORT_USERS.LOGIN, RandomStringUtils.randomAlphabetic(DEFAULT_LENGTH_RANDOM_LOGIN))
                .returning(PASSPORT_USERS.UID)
                .fetchOne();

        return usersRecord.getUid();
    }
}

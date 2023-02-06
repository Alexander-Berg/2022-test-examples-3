package ru.yandex.direct.core.testing.repository;

import java.time.Instant;
import java.util.Objects;

import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.common.util.RepositoryUtils;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.dbschema.ppc.enums.InternalUsersIsDeveloper;
import ru.yandex.direct.dbschema.ppc.enums.UsersOptionsStatuseasy;
import ru.yandex.direct.dbschema.ppc.enums.UsersSendaccnews;
import ru.yandex.direct.dbschema.ppc.enums.UsersSendnews;
import ru.yandex.direct.dbschema.ppc.enums.UsersSendwarn;
import ru.yandex.direct.dbschema.ppc.enums.UsersStatusarch;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapper.write.JooqWriter;
import ru.yandex.direct.jooqmapper.write.JooqWriterBuilder;
import ru.yandex.direct.rbac.RbacRepType;
import ru.yandex.direct.rbac.RbacRole;

import static java.util.Collections.singleton;
import static org.jooq.impl.DSL.defaultValue;
import static ru.yandex.direct.dbschema.ppc.tables.InternalUsers.INTERNAL_USERS;
import static ru.yandex.direct.dbschema.ppc.tables.Users.USERS;
import static ru.yandex.direct.dbschema.ppc.tables.UsersOptions.USERS_OPTIONS;
import static ru.yandex.direct.dbschema.ppcdict.tables.ShardLogin.SHARD_LOGIN;
import static ru.yandex.direct.dbschema.ppcdict.tables.ShardUid.SHARD_UID;
import static ru.yandex.direct.jooqmapper.write.WriterBuilders.fromProperty;
import static ru.yandex.direct.jooqmapper.write.WriterBuilders.fromSupplier;
import static ru.yandex.direct.jooqmapperhelper.InsertHelper.saveModelObjectsToDbTable;
import static ru.yandex.direct.utils.CommonUtils.nvl;

/**
 * Работа с пользователями в тестах
 */
public class TestUserRepository {

    @Autowired
    private DslContextProvider dslContextProvider;

    @Autowired
    private ShardHelper shardHelper;

    private static final JooqWriter<User> JOOQ_WRITER_FOR_USERS_OPTIONS = jooqWriterForUsersOptions();

    private static JooqWriter<User> jooqWriterForUsersOptions() {
        return JooqWriterBuilder.<User>builder()
                .writeField(USERS_OPTIONS.UID, fromProperty(User.UID))
                .writeField(USERS_OPTIONS.YA_COUNTERS, fromSupplier(() -> 0L))
                .writeField(USERS_OPTIONS.GEO_ID, fromProperty(User.GEO_ID).by(val -> nvl(val, 0L)))
                .writeField(USERS_OPTIONS.OPTS, fromProperty(User.OPTS).by(val -> nvl(val, "")))
                .writeField(USERS_OPTIONS.STATUS_EASY,
                        fromProperty(User.STATUS_EASY)
                                .by(val -> Objects.equals(val, Boolean.TRUE) ?
                                        UsersOptionsStatuseasy.Yes : UsersOptionsStatuseasy.No))
                .writeField(USERS_OPTIONS.PASSPORT_KARMA, fromProperty(User.PASSPORT_KARMA))
                .writeField(USERS_OPTIONS.RECOMMENDATIONS_EMAIL, fromProperty(User.RECOMMENDATIONS_EMAIL))
                .build();
    }

    /**
     * Добавляет пользователя. Объединяет {@link TestUserRepository#addUserIdToPpcdict(User)},
     * {@link TestUserRepository#addUserToUsersTable(User)} и {@link TestUserRepository#addUserLoginToPpcdict(User)}
     *
     * @param user Информация о пользователе
     */
    public void addUser(User user) {
        addUserIdToPpcdict(user);
        addUserToUsersTable(user);
        addUserToUsersOptionsTable(user);
        addUserToInternalUsersTable(user);
        addUserLoginToPpcdict(user);
    }

    /**
     * Удаляет пользователя из таблицы users. Аналог {@link TestUserRepository#deleteUserFromUsersTable(int, long)}
     *
     * @param shard Шард
     * @param uid   ID пользователя
     */
    public void deleteUser(int shard, long uid) {
        deleteUserFromUsersTable(shard, uid);
    }

    /**
     * Создает связку ID пользователя с шардом
     *
     * @param user Информация о пользователе
     */
    private void addUserIdToPpcdict(User user) {
        dslContextProvider.ppcdict()
                .insertInto(SHARD_UID, SHARD_UID.CLIENT_ID, SHARD_UID.UID)
                .values(user.getClientId().asLong(), user.getUid())
                .onDuplicateKeyIgnore()
                .execute();

        dslContextProvider.ppcdict()
                .insertInto(SHARD_LOGIN, SHARD_LOGIN.UID, SHARD_LOGIN.LOGIN)
                .values(user.getUid(), user.getLogin())
                .onDuplicateKeyIgnore()
                .execute();
    }

    /**
     * Создает связку логина пользователя с шардом
     *
     * @param user Информация о пользователе
     */
    private void addUserLoginToPpcdict(User user) {
        dslContextProvider.ppcdict()
                .insertInto(SHARD_LOGIN, SHARD_LOGIN.LOGIN, SHARD_LOGIN.UID)
                .values(user.getLogin(), user.getUid())
                .onDuplicateKeyIgnore()
                .execute();
    }

    /**
     * Добавляет пользователя в таблицу users
     *
     * @param user Информация о пользователе
     */
    private void addUserToUsersTable(User user) {
        dslContextProvider.ppc(shardHelper.getShardByClientId(user.getClientId()))
                .insertInto(USERS, USERS.UID, USERS.CLIENT_ID, USERS.LOGIN,
                        USERS.REP_TYPE,
                        USERS.STATUS_ARCH,
                        USERS.FIO, USERS.EMAIL,
                        USERS.CREATETIME, USERS.PHONE,
                        USERS.SEND_NEWS, USERS.SEND_ACC_NEWS, USERS.SEND_WARN)
                .values(user.getUid(), user.getClientId().asLong(), user.getLogin(),
                        RbacRepType.toSource(user.getRepType()),
                        Boolean.TRUE.equals(user.getStatusArch()) ? UsersStatusarch.Yes : UsersStatusarch.No,
                        user.getFio(), user.getEmail(), Instant.now().toEpochMilli(), user.getPhone(),
                        user.getSendNews() == null ?
                                UsersSendnews.valueOf(
                                        defaultValue(USERS.SEND_NEWS).getDataType().defaultValue().getName()) :
                                user.getSendNews() ? UsersSendnews.Yes : UsersSendnews.No,
                        user.getSendAccNews() == null ?
                                UsersSendaccnews.valueOf(
                                        defaultValue(USERS.SEND_ACC_NEWS).getDataType().defaultValue().getName()) :
                                user.getSendAccNews() ? UsersSendaccnews.Yes : UsersSendaccnews.No,
                        user.getSendWarn() == null ?
                                UsersSendwarn.valueOf(
                                        defaultValue(USERS.SEND_WARN).getDataType().defaultValue().getName()) :
                                user.getSendWarn() ? UsersSendwarn.Yes : UsersSendwarn.No)
                .onDuplicateKeyIgnore()
                .execute();
    }

    /**
     * Добавляет пользователя в таблицу users_options
     */
    private void addUserToUsersOptionsTable(User user) {
        int shard = shardHelper.getShardByClientId(user.getClientId());
        DSLContext dslContext = dslContextProvider.ppc(shard);
        saveModelObjectsToDbTable(dslContext, USERS_OPTIONS, JOOQ_WRITER_FOR_USERS_OPTIONS, singleton(user));
    }

    /**
     * Добавляет пользователя в таблицу internal_users, но только если это внутренний пользователь
     *
     * @param user Информация о пользователе
     */
    private void addUserToInternalUsersTable(User user) {
        if (user.getRole() != null && (user.getRole().isInternal() || user.getRole().anyOf(RbacRole.LIMITED_SUPPORT))) {
            InternalUsersIsDeveloper isDeveloper = user.getDeveloper() == Boolean.TRUE ? InternalUsersIsDeveloper.Yes
                    : InternalUsersIsDeveloper.No;
            Long isSuperManager = user.getSuperManager() == Boolean.TRUE ? RepositoryUtils.TRUE : RepositoryUtils.FALSE;
            dslContextProvider.ppc(shardHelper.getShardByClientId(user.getClientId()))
                    .insertInto(INTERNAL_USERS, INTERNAL_USERS.UID, INTERNAL_USERS.DOMAIN_LOGIN,
                            INTERNAL_USERS.IS_DEVELOPER, INTERNAL_USERS.IS_SUPER_MANAGER)
                    .values(user.getUid(), user.getDomainLogin(), isDeveloper, isSuperManager)
                    .onDuplicateKeyIgnore()
                    .execute();
        }
    }

    /**
     * Удаляет пользователя из таблицы users
     *
     * @param shard Шард
     * @param uid   ID пользователя
     */
    private void deleteUserFromUsersTable(int shard, long uid) {
        dslContextProvider.ppc(shard)
                .deleteFrom(USERS)
                .where(USERS.UID.eq(uid))
                .execute();
    }

    /**
     * Проверяет наличие пользователя по ID
     *
     * @param uid ID пользователя
     * @return true если пользователь существует
     */
    public boolean userExists(long uid) {
        boolean isInPpcdict = !dslContextProvider.ppcdict()
                .select(SHARD_UID.UID)
                .from(SHARD_UID)
                .where(SHARD_UID.UID.eq(uid))
                .fetch(SHARD_UID.UID).isEmpty();
        return isInPpcdict &&
                !dslContextProvider.ppc(shardHelper.getShardByClientUid(uid))
                        .select(USERS.UID)
                        .from(USERS)
                        .where(USERS.UID.eq(uid))
                        .fetch(USERS.UID)
                        .isEmpty();
    }

    /**
     * Устанавливает пользователю невалидный email.
     *
     * @param shard Шард
     * @param userId ID пользователя
     */
    public void setUnvalidatedUserEmail(int shard, long userId) {
        dslContextProvider.ppc(shard)
                .update(USERS)
                .set(USERS.EMAIL, "")
                .where(USERS.UID.eq(userId))
                .execute();
    }
}

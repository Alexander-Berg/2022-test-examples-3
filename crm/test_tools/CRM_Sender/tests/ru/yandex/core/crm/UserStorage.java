package ru.yandex.core.crm;

import ru.yandex.core.DbProvider;

import java.sql.SQLException;

/**
 * Created by nasyrov on 18.04.2016.
 */
public class UserStorage {
    private static final String SqlSelectUserByLogin =
            "select id, login, full_name name from crm.ya_user where login=:plogin";

    private static final String SqlSelectUserById =
            "select id, login, full_name name from crm.ya_user where id=:pid";

    public static UserRow getUser(String login) throws Exception {
        return DbProvider.db().open()
                .createQuery(SqlSelectUserByLogin)
                .addParameter("plogin", login)
                .executeAndFetch(UserRow.class)
                .get(0);
    }

    public static UserRow getUser(Long id) throws Exception {
        return DbProvider.db().open()
                .createQuery(SqlSelectUserById)
                .addParameter("pid", id)
                .executeAndFetch(UserRow.class)
                .get(0);
    }
}

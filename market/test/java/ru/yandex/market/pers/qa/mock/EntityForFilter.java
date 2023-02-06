package ru.yandex.market.pers.qa.mock;

import ru.yandex.market.pers.qa.client.model.CommentProject;
import ru.yandex.market.pers.qa.model.ModState;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author korolyov
 * 21.06.18
 */
public class EntityForFilter {
    public long id;
    public long userId;
    public String text;
    public ModState modState;
    public CommentProject project;

    public static EntityForFilter valueOf(ResultSet rs) throws SQLException {
        EntityForFilter entityForFilter = new EntityForFilter();
        entityForFilter.id = rs.getLong("id");
        entityForFilter.userId = Long.parseLong(rs.getString("user_id"));
        entityForFilter.text = rs.getString("text");
        entityForFilter.modState = ModState.valueOf(rs.getInt("mod_state"));
        return entityForFilter;
    }
}

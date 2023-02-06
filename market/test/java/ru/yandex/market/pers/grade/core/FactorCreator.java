package ru.yandex.market.pers.grade.core;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.pers.grade.client.model.Delivery;
import ru.yandex.market.pers.grade.core.ugc.model.RadioFactorValue;

/**
 * @author grigor-vlad
 * 18.11.2021
 */
public class FactorCreator {

    @Autowired
    @Qualifier("pgJdbcTemplate")
    protected JdbcTemplate pgJdbcTemplate;

    public Long addFactorAndReturnId(String name, long categoryId, int order) {
        Long factorId = pgJdbcTemplate.queryForObject("select nextval('grade.s_grade_factor_id')", Long.class);
        pgJdbcTemplate.update("insert into grade_factor (id, name, category_id, order_num) values (?, ?, ?, ?)",
            factorId, name, categoryId, order);
        return factorId;
    }

    public Long addRadioFactorAndReturnId(String name, long categoryId, int order,
                                  boolean priority, List<RadioFactorValue> radioValues) {
        Long radioTypeId =
            pgJdbcTemplate.queryForObject("select nextval('grade.s_grade_factor_radio_type_id')", Long.class);
        pgJdbcTemplate.update("insert into grade_factor_radio_type (id, name) values(?, ?)",
            radioTypeId, name);

        pgJdbcTemplate.batchUpdate(
            "insert into grade_factor_radio_type_value (radio_type_id, value, name, order_num) " +
                "values (?, ?, ?, ?)",
            radioValues,
            radioValues.size(),
            (ps, item) -> {
                ps.setLong(1, radioTypeId);
                ps.setInt(2, item.getValue());
                ps.setString(3, item.getName());
                ps.setInt(4, item.getOrder());
            });

        Long factorId = pgJdbcTemplate.queryForObject("select nextval('grade.s_grade_factor_id')", Long.class);
        pgJdbcTemplate.update(
            "insert into grade_factor (id, name, category_id, order_num, type, priority, radio_type_id) " +
                "values (?, ?, ?, ?, ?, ?, ?)",
            factorId, name, categoryId, order, 1, priority ? 1 : 0, radioTypeId);
        return factorId;
    }

    public Long addShopFactorAndReturnId(String name, String description, int order,
                                 Delivery delivery, boolean feedbackFactor) {
        Long factorId = pgJdbcTemplate.queryForObject("select nextval('grade.s_grade_factor_id')", Long.class);
        pgJdbcTemplate.update("insert into grade_factor (id, name, description, order_num) values (?, ?, ?, ?)",
            factorId, name, description, order);
        pgJdbcTemplate.update("insert into grade_factor_shop values (?, ?, ?)",
            factorId, delivery.value(), feedbackFactor ? 1 : 0);

        return factorId;
    }

}

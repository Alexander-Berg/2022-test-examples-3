package ru.yandex.market.crm.campaign.test.utils;

import java.util.List;

import javax.inject.Named;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import ru.yandex.market.crm.campaign.domain.activity.Activity;
import ru.yandex.market.crm.campaign.services.activity.ActivityRowMapper;
import ru.yandex.market.crm.json.serialization.JsonDeserializer;
import ru.yandex.market.mcrm.db.Constants;

/**
 * @author apershukov
 */
@Component
public class ActivitiesTestHelper {

    private final JdbcTemplate jdbcTemplate;
    private final ActivityRowMapper rowMapper;

    public ActivitiesTestHelper(@Named(Constants.DEFAULT_JDBC_TEMPLATE) JdbcTemplate jdbcTemplate,
                                JsonDeserializer jsonDeserializer,
                                ActivityRowMapper rowMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapper = rowMapper;
    }

    public List<Activity> getAllActivities() {
        return jdbcTemplate.query(
                "SELECT * FROM activities",
                rowMapper
        );
    }
}

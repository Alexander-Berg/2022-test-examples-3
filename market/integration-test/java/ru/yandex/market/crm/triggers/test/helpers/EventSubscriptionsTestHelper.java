package ru.yandex.market.crm.triggers.test.helpers;

import java.time.LocalDateTime;
import java.util.List;

import javax.inject.Named;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import ru.yandex.market.crm.triggers.services.active.ActiveSubscription;
import ru.yandex.market.crm.triggers.services.active.ActiveSubscriptionRowMapper;
import ru.yandex.market.crm.triggers.test.EventSubscription;
import ru.yandex.market.mcrm.db.Constants;

/**
 * @author apershukov
 */
@Component
public class EventSubscriptionsTestHelper {

    private static final RowMapper<EventSubscription> ROW_MAPPER = (rs, rowNum) -> new EventSubscription(
            rs.getString("event_type_"),
            rs.getString("event_name_"),
            rs.getString("proc_inst_id_"),
            rs.getObject("visit_time_", LocalDateTime.class)
    );

    private static final String SELECT_QUERY = "SELECT * FROM act_ru_event_subscr";
    private static final String SELECT_ACTIVE_QUERY = "SELECT * FROM active_subscriptions";

    private final ActiveSubscriptionRowMapper activeSubscriptionRowMapper;
    private final JdbcTemplate jdbcTemplate;

    public EventSubscriptionsTestHelper(ActiveSubscriptionRowMapper activeSubscriptionRowMapper,
                                        @Named(Constants.DEFAULT_JDBC_TEMPLATE) JdbcTemplate jdbcTemplate) {
        this.activeSubscriptionRowMapper = activeSubscriptionRowMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public EventSubscription getSubscription(String processId, String messageName) {
        return jdbcTemplate.queryForObject(
                SELECT_QUERY + " WHERE proc_inst_id_ = ? AND event_name_ = ?",
                ROW_MAPPER,
                processId,
                messageName
        );
    }

    public List<ActiveSubscription> getActiveSubscriptions() {
        return jdbcTemplate.query(SELECT_ACTIVE_QUERY, activeSubscriptionRowMapper);
    }

    public ActiveSubscription getActiveSubscription(long id) {
        return jdbcTemplate.queryForObject(
                SELECT_ACTIVE_QUERY + " WHERE id = ?",
                activeSubscriptionRowMapper,
                id
        );
    }

    public ActiveSubscription getSubscriptionForProcess(String processInstanceId) {
        List<ActiveSubscription> subscriptions = jdbcTemplate.query(
                SELECT_ACTIVE_QUERY + " WHERE proc_inst_id = ?",
                activeSubscriptionRowMapper,
                processInstanceId
        );

        if (subscriptions.isEmpty()) {
            throw new IllegalArgumentException("No subscriptions for process " + processInstanceId);
        }

        return subscriptions.get(0);
    }
}

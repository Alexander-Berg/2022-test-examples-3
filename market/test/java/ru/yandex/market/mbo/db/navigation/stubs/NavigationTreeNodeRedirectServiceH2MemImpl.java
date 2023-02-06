package ru.yandex.market.mbo.db.navigation.stubs;

import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import ru.yandex.market.mbo.db.JdbcFactory;
import ru.yandex.market.mbo.db.navigation.NavigationTreeNodeRedirectService;

import java.util.Collection;

/**
 * @author moskovkin@yandex-team.ru
 * @since 26.10.18
 */
public class NavigationTreeNodeRedirectServiceH2MemImpl extends NavigationTreeNodeRedirectService {

    public NavigationTreeNodeRedirectServiceH2MemImpl() {

        BasicDataSource dataSource;

        dataSource = JdbcFactory.createH2DataSource(JdbcFactory.Mode.POSTGRES,
                "classpath:ru/yandex/market/mbo/navigation/dao/navigation_redirects_pg.sql");

        NamedParameterJdbcTemplate jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        setNamedParameterJdbcTemplate(jdbcTemplate);
    }

    @Override
    public void deleteRedirectsBySources(String code, Collection<Long> sourceIds) {
    }
}

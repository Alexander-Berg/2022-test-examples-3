package ru.yandex.market.crm.campaign.services.sql;

import javax.inject.Named;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import ru.yandex.market.crm.campaign.domain.promo.entities.TestPuid;
import ru.yandex.market.crm.campaign.domain.promo.entities.TestPuidsGroup;
import ru.yandex.market.mcrm.db.Constants;

@Repository
public class TestPuidsDAO extends AbstractAdressesDAO<TestPuidsGroup, TestPuid> {

    public TestPuidsDAO(PuidItemsDAO itemsDao,
                        @Named(Constants.DEFAULT_JDBC_TEMPLATE) JdbcTemplate jdbcTemplate) {
        super(TestPuidsGroup::new, itemsDao, jdbcTemplate, "test_puid_groups");
    }
}

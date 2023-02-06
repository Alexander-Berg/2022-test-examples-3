package ru.yandex.market.crm.campaign.services.sql;

import javax.inject.Named;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import ru.yandex.market.crm.campaign.domain.sending.TestEmail;
import ru.yandex.market.crm.campaign.domain.sending.TestEmailsGroup;
import ru.yandex.market.mcrm.db.Constants;

@Repository
public class TestEmailsDAO extends AbstractAdressesDAO<TestEmailsGroup, TestEmail> {

    TestEmailsDAO(EmailItemsDAO itemsDao,
                  @Named(Constants.DEFAULT_JDBC_TEMPLATE) JdbcTemplate jdbcTemplate) {
        super(TestEmailsGroup::new, itemsDao, jdbcTemplate, "test_email_groups");
    }
}

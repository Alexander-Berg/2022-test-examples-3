package ru.yandex.market.crm.campaign.services.sql;

import javax.inject.Named;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import ru.yandex.market.crm.campaign.domain.sending.TestDevicesGroup;
import ru.yandex.market.crm.campaign.domain.sending.TestPushDevice;
import ru.yandex.market.mcrm.db.Constants;

@Repository
public class TestDevicesDAO extends AbstractAdressesDAO<TestDevicesGroup, TestPushDevice> {

    TestDevicesDAO(DeviceItemsDAO itemsDao,
                   @Named(Constants.DEFAULT_JDBC_TEMPLATE) JdbcTemplate jdbcTemplate) {
        super(TestDevicesGroup::new, itemsDao, jdbcTemplate, "test_device_groups");
    }
}

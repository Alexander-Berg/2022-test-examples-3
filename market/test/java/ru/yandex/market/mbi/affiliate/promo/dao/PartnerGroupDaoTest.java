package ru.yandex.market.mbi.affiliate.promo.dao;

import java.util.List;
import java.util.Map;

import org.dbunit.database.DatabaseConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.mbi.affiliate.promo.config.FunctionalTestConfig;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FunctionalTestConfig.class)
@TestExecutionListeners(listeners = {
        DbUnitTestExecutionListener.class,
}, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@DbUnitDataBaseConfig(
        @DbUnitDataBaseConfig.Entry(
                name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS,
                value = "true"))
public class PartnerGroupDaoTest {
    @Autowired
    private PartnerGroupDao dao;

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource",
            before = "partner_group_dao.before.csv",
            after = "partner_group_dao.set.after.csv")
    public void testSetUserGroup() {
        dao.setUserGroup(List.of("user2", "user3"), "vip2");
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource",
            before = "partner_group_dao.before.csv",
            after = "partner_group_dao.unset.after.csv")
    public void testUnsetUserGroup() {
        dao.unsetUserGroup(List.of("user1", "user2"), "vip2");
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "partner_group_dao.before.csv")
    public void testGetUserGroups() {
        assertThat(dao.getUserGroups(List.of("user1")).get("user1"), containsInAnyOrder("vip1", "vip2"));
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "partner_group_dao.before.csv")
    public void testGetUserGroupsEmpty() {
        assertThat(dao.getUserGroups(List.of("user4")), equalTo(Map.of()));
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "partner_group_dao.before.csv")
    public void testGetUsersByGroup() {
        assertThat(dao.getUsersByGroups(List.of("vip2")), containsInAnyOrder("user1", "user2"));
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "partner_group_dao.before.csv")
    public void testGetUsersByGroupEmpty() {
        assertThat(dao.getUsersByGroups(List.of("vip4")), emptyIterable());
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", before = "partner_group_dao.before.csv")
    public void testGetAllGroups() {
        var result = dao.getAllGroups();
        assertThat(result.keySet(), containsInAnyOrder("vip1", "vip2"));
        assertThat(result.get("vip1"), containsInAnyOrder("user1"));
        assertThat(result.get("vip2"), containsInAnyOrder("user1", "user2"));
    }
}
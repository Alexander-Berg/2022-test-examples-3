package ru.yandex.market.cocon.environment;

import java.util.Collection;

import org.dbunit.database.DatabaseConfig;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.cocon.FunctionalTest;
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DbUnitDataBaseConfig({
        @DbUnitDataBaseConfig.Entry(name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, value = "true"),
})
@DbUnitDataSet(before = "EnvironmentDaoTest.before.csv")
public class EnvironmentDaoTest extends FunctionalTest {

    @Autowired
    private EnvironmentDao environmentDao;

    @Test
    public void shouldReturnValueForHost() {
        System.setProperty("host.name", "testing_market_cocon_vla");
        var result = environmentDao.getValue("test_a");
        System.clearProperty("host.name");
        assertEquals("2", result);
    }

    @Test
    public void shouldReturnDefautValueWhenHostNotFound() {
        var result = environmentDao.getValue("test_a");
        assertEquals("1", result);
    }

    @Test
    public void testListAll() {
        Collection<EnvironmentRow> actual = environmentDao.listAll();
        assertEquals(actual.size(), 3);
        MatcherAssert.assertThat(actual, Matchers.containsInAnyOrder(
                EnvironmentRow.newBuilder().withKey("test_a").withHost("").withValue("1").build(),
                EnvironmentRow.newBuilder().withKey("test_a").withHost("vla").withValue("2").build(),
                EnvironmentRow.newBuilder().withKey("test_b").withHost("").withValue("1").build()));
    }

    @Test
    public void testListValues() {
        Collection<EnvironmentRow> actual = environmentDao.list("test_a", null);
        assertEquals(actual.size(), 2);
        MatcherAssert.assertThat(actual, Matchers.containsInAnyOrder(
                EnvironmentRow.newBuilder().withKey("test_a").withHost("").withValue("1").build(),
                EnvironmentRow.newBuilder().withKey("test_a").withHost("vla").withValue("2").build()));
    }

    @Test
    public void testListValuesWithHost() {
        Collection<EnvironmentRow> actual = environmentDao.list("test_a", "vla");
        assertEquals(actual.size(), 1);
        MatcherAssert.assertThat(actual, Matchers.containsInAnyOrder(
                EnvironmentRow.newBuilder().withKey("test_a").withHost("vla").withValue("2").build()));
    }

    @Test
    public void testListValuesWithNullHost() {
        Collection<EnvironmentRow> actual = environmentDao.list("test_b", null);
        assertEquals(actual.size(), 1);
        assertEquals(actual.iterator().next(),
                EnvironmentRow.newBuilder().withKey("test_b").withHost("").withValue("1").build());
    }

    @Test
    @DbUnitDataSet(after = "EnvironmentDaoDeleteByKeyTest.after.csv")
    public void testDelete() {
        environmentDao.deleteValue("test_a", null);
    }

    @Test
    @DbUnitDataSet(after = "EnvironmentDaoDeleteByKeyAndHostTest.after.csv")
    public void testDeleteByKeyAndHost() {
        environmentDao.deleteValue("test_a", "vla");
    }
}


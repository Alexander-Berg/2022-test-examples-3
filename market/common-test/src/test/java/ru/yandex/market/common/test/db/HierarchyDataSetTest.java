package ru.yandex.market.common.test.db;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.common.test.spring.H2Config;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

/**
 * Кейс, когда DbUnitDataSet задан для иерархии классов.
 * Нужно обрабатываеть датасеты, начиная с самого базового.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = HierarchyDataSetTest.Config.class)
@DbUnitDataSet(before = "HierarchyDataSetTest.before.csv")
public class HierarchyDataSetTest extends Super1 {

    /**
     * Сперва должны вставляться данные из Super2, затем из Super1, а потом из HierarchyDataSetTest.
     * Тест упадет из-за fk, если вставка будет в обратном порядке.
     * Сам тест лишь проверяет вставленные данные.
     */
    @Test
    @DbUnitDataSet(before = "HierarchyDataSetTest.testSuccess.before.csv", after = "HierarchyDataSetTest.testSuccess.after.csv")
    public void testSuccess() {
        // do nothing
    }

    @Configuration
    public static class Config extends H2Config {

        @Nonnull
        @Override
        protected List<Resource> databaseResources() throws Exception {
            return Arrays.asList(
                    new ByteArrayResource("set mode oracle".getBytes()),
                    new ByteArrayResource("create schema my".getBytes()),
                    new ByteArrayResource("create table my.table01 (id int primary key)".getBytes()),
                    new ByteArrayResource("create table my.table02 (id int primary key, table01_id int, constraint fk0201 foreign key (table01_id) references my.table01 (id))".getBytes()),
                    new ByteArrayResource("create table my.table03 (id int primary key, table02_id int, constraint fk0302 foreign key (table02_id) references my.table02 (id))".getBytes()),
                    new ByteArrayResource("create table my.table04 (id int primary key, table03_id int, constraint fk0403 foreign key (table03_id) references my.table03 (id))".getBytes())
            );
        }
    }
}

@DbUnitDataSet(before = "Super1.before.csv")
class Super1 extends Super2 {
}

@DbUnitDataSet(before = "Super2.before.csv")
class Super2 extends DbUnitTest {
}

package ru.yandex.market.common.test.db;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import ru.yandex.market.common.test.spring.H2Config;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@Configuration
public class SampleTestConfig extends H2Config {

    @Nonnull
    @Override
    protected List<Resource> databaseResources() {
        return Arrays.asList(
                new ByteArrayResource("set mode oracle".getBytes()),
                new ByteArrayResource("create schema my".getBytes()),
                new ByteArrayResource("create table my.test (id integer primary key, txt varchar(4000 char))".getBytes()),
                new ByteArrayResource("create table my.dict (id integer primary key, txt varchar(4000 char))".getBytes()),
                new ByteArrayResource("insert into my.dict values (1, 'asdf')".getBytes()),
                new ByteArrayResource("create table my.dt_table (id integer primary key, dt datetime)".getBytes()),
                new ByteArrayResource("create view my.dt_view (id) as select id from my.dt_table".getBytes())
        );
    }
}

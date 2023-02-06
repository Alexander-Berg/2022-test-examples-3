package ru.yandex.market.volva.dao;

import java.util.List;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.volva.entity.IdType;
import ru.yandex.market.volva.entity.Node;

/**
 * @author dzvyagin
 */
public class PhonesYtDaoTest {


    @Test
    @Ignore
    public void smokeTest() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("ru.yandex.yql.YqlDriver");
        dataSource.setJdbcUrl("jdbc:yql://yql.yandex.net:443/?syntaxVersion=1");
        dataSource.setUsername("user");
        dataSource.setPassword("secret");
        dataSource.setMaximumPoolSize(2);
        dataSource.setConnectionTimeout(1_200_000);

        var cryptaYtDao = new PhonesYtDao(new NamedParameterJdbcTemplate(dataSource));
        System.out.println(cryptaYtDao.getPhoneEdgesForNodes(List.of(new Node("311870044", IdType.PUID))));
    }
}

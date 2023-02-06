package ru.yandex.market.antifraud.orders.storage.dao;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.antifraud.orders.storage.entity.Category;
import ru.yandex.market.antifraud.orders.test.annotations.DaoLayerTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author dzvyagin
 */
@DaoLayerTest
@RunWith(SpringJUnit4ClassRunner.class)
public class CategoryDaoTest {

    @Autowired
    private NamedParameterJdbcOperations jdbcTemplate;

    private CategoryDao categoryDao;

    @Before
    public void init() {
        categoryDao = new CategoryDao(jdbcTemplate);
    }

    @Test
    public void findByHid() {
        Category cat1 = Category.builder().hid(123).build();
        Category cat2 = Category.builder().hid(124).build();

        cat1 = categoryDao.save(cat1);
        cat2 = categoryDao.save(cat2);

        assertThat(categoryDao.findByHid(123).get()).isEqualTo(cat1);
        assertThat(categoryDao.findByHid(124).get()).isEqualTo(cat2);
    }

    @Test
    public void findByCatTeam() {
        Category cat1 = Category.builder().hid(125).catTeam("FMCG").build();
        Category cat2 = Category.builder().hid(126).catTeam("Автозапчасти").build();

        cat1 = categoryDao.save(cat1);
        cat2 = categoryDao.save(cat2);

        assertThat(categoryDao.findByCatTeam("FMCG")).contains(cat1);
        assertThat(categoryDao.findByCatTeam("Автозапчасти")).contains(cat2);
    }
}

package ru.yandex.market.mbo.db.size.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.mbo.db.size.model.ChartType;
import ru.yandex.market.mbo.db.size.model.SizeChartType;

import static org.mockito.Mockito.spy;

@SuppressWarnings("checkstyle:magicNumber")
public class SizeChartTypeDaoTest {

    private SizeChartTypeDao sizeChartTypeDao;
    private NamedParameterJdbcTemplate jdbcTemplate;

    @Before
    public void setUp() {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        String dbName = getClass().getSimpleName() + UUID.randomUUID();

        dataSource.setUrl(
            "jdbc:h2:mem:" + dbName +
                ";INIT=RUNSCRIPT FROM 'classpath:ru/yandex/market/mbo/db/size/init_size_chart.sql'" +
                ";MODE=Oracle"
        );

        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(dataSource);
        jdbcTemplate = spy(namedTemplate);

        sizeChartTypeDao = new SizeChartTypeDao(jdbcTemplate);
    }

    @Test
    public void testSaveChartTypes() {
        List<SizeChartType> sizeChartTypes = new ArrayList<>();

        SizeChartType type1 = new SizeChartType();
        type1.setParameterId(14474342L);
        type1.setCategoryId(7959202L);
        type1.setOptionId(102381L);
        type1.setType(ChartType.DEFAULT);
        sizeChartTypes.add(type1);

        SizeChartType type2 = new SizeChartType();
        type2.setParameterId(14474342L);
        type2.setCategoryId(7959202L);
        type2.setOptionId(93661L);
        type2.setType(ChartType.FILTER);
        sizeChartTypes.add(type2);

        int num = sizeChartTypeDao.saveChartTypes(sizeChartTypes);
        Assert.assertEquals(2, num);

        List<SizeChartType> savedTypes = sizeChartTypeDao.getChartTypesBySizeParameterId(7959202L, 14474342L);
        Assert.assertTrue(sizeChartTypes.containsAll(savedTypes) && savedTypes.containsAll(sizeChartTypes));
    }
}

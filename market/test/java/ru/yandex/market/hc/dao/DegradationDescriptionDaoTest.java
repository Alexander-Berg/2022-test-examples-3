package ru.yandex.market.hc.dao;

import java.util.Collections;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.market.hc.config.DaoTestConfig;
import ru.yandex.market.hc.entity.DegradationConfig;
import ru.yandex.market.hc.entity.DegradationDescription;

import static org.junit.Assert.assertEquals;

/**
 * Created by aproskriakov on 11/2/21
 */
@RunWith(SpringJUnit4ClassRunner.class)
@Import(DaoTestConfig.class)
public class DegradationDescriptionDaoTest {

    private DegradationDescriptionDao dao;

    @Autowired
    private NamedParameterJdbcOperations namedJdbcTemplate;

    @Before
    public void setUp() {
        ObjectMapper jsonMapper = new ObjectMapper();
        dao = new DegradationDescriptionDao(jsonMapper, namedJdbcTemplate);
    }

    @Test
    public void testGetDegradationDescriptionByName() throws JsonProcessingException {
        String nameOfTestDD = "junitTestName";
        DegradationConfig dc = DegradationConfig.builder()
                .updatePeriod(60)
                .degradationModes(Collections.singleton(90))
                .build();
        DegradationDescription testDD = DegradationDescription.builder()
                .name(nameOfTestDD)
                .config(dc)
                .build();
        dao.saveOrUpdateDegradationDescription(testDD);

        assertEquals(testDD, dao.getDegradationDescriptionByName(nameOfTestDD).get());
    }
}

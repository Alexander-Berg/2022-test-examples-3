package ru.yandex.market.robot.db;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import ru.yandex.market.robot.shared.models.RobotTaskInfo;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/testContext.xml"})
public class RobotTmsDaoTest {
    private EmbeddedDatabase db;

    @Before
    public void setUp() throws Exception {
        db = new EmbeddedDatabaseBuilder()
            .addScript("/test-schema.sql")
            .addScript("/test-data.sql")
            .build();
    }

    @After
    public void tearDown() throws Exception {
        db.shutdown();
    }

    @Test
    public void testGetLastFailTask() throws Exception {
        final RobotTmsDao dao = new RobotTmsDao();
        JdbcTemplate template = new JdbcTemplate(db);
        dao.setJdbcTemplate(template);

        RobotTaskInfo current = template.queryForObject("select * from task where id = 5", dao.getRobotTaskInfoMapper());
        RobotTaskInfo lastFailTask = dao.getLastFailTask(current);
        assertEquals("Первый фейл назад", 3, lastFailTask.getId());

        current = template.queryForObject("select * from task where id = 14", dao.getRobotTaskInfoMapper());
        lastFailTask = dao.getLastFailTask(current);
        assertEquals("Были одни фейлы", 11, lastFailTask.getId());

        current = template.queryForObject("select * from task where id = 21", dao.getRobotTaskInfoMapper());
        lastFailTask = dao.getLastFailTask(current);
        assertEquals("Не было ни одного фейла", 21, lastFailTask.getId());
    }
}
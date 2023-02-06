package ru.yandex.market.load.admin.dao;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.load.admin.AbstractFunctionalTest;
import ru.yandex.market.load.admin.entity.JugglerCheck;
import ru.yandex.market.load.admin.entity.Project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Created by aproskriakov on 3/18/22
 */
public class JugglerCheckDaoTest extends AbstractFunctionalTest {

    @Autowired
    protected JugglerCheckDao dao;
    @Autowired
    private ProjectDao projectDao;

    @Test
    void getAllChecksTest() {
        Project project = Project.builder().build();
        project.setDescription("test-desc");
        project.setTitle("test-title");
        project.setAbcRoles(Collections.singletonList("role"));
        project.setAbcServices(Collections.singletonList("service"));
        project = projectDao.save(project);

        JugglerCheck jugglerCheck = JugglerCheck.builder()
                .projectId(project.getId())
                .service("test")
                .host("test")
                .status("OK")
                .addedAt(Timestamp.from(Instant.now()))
                .updatedAt(Timestamp.from(Instant.now()))
                .build();
        dao.save(jugglerCheck);
        final JugglerCheck next = dao.findAll().iterator().next();
        assertEquals(next.getHost(), "test");
        assertEquals(next.getService(), "test");
        assertEquals(next.getStatus(), "OK");
        assertNotNull(next.getUpdatedAt());
    }
}

package ru.yandex.market.mstat.planner.dao;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.mstat.planner.model.Contour;
import ru.yandex.market.mstat.planner.model.Project;
import ru.yandex.market.mstat.planner.utils.AbstractDbIntegrationTest;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ContourDaoTest extends AbstractDbIntegrationTest {

    @Autowired
    private ContourDao contourDao;

    @Autowired
    private ProjectDao projectDao;

    @Test
    public void createReadUpdateContourTest() {

        // Create

        Contour contour = new Contour();
        contour.setName("insertContourTest1");
        contour.setTarget("target1");
        contour.setFteLimit(1L);
        contour.setDescription("desc1");
        contour.setStQueues("queue1");
        contour.setStProjectTickets("projectTicket1");
        contour.setWikiLinks("wiki1");
        contour.setBoards("board1");
        contour.setTechLead(data.login);
        contour.setProductOwner(data.login);
        contour.setEmployer(data.login);
        contour.setGroupId(data.groupId);

        long newContourId = contourDao.createNewContour(contour);

        // Read

        Contour contourFromDb = contourDao.getContour(newContourId);

        assertEquals(Long.valueOf(newContourId), contourFromDb.getId());
        assertEquals(contour.getName(), contourFromDb.getName());
        assertEquals(contour.getGroupId(), contourFromDb.getGroupId());
        assertEquals(contour.getEmployer(), contourFromDb.getEmployer());
        assertEquals(contour.getProductOwner(), contourFromDb.getProductOwner());
        assertEquals(contour.getTechLead(), contourFromDb.getTechLead());
        assertEquals(contour.getStQueues(), contourFromDb.getStQueues());
        assertEquals(contour.getStProjectTickets(), contourFromDb.getStProjectTickets());

        // Update

        String newEmployee = "updatedEmpl";
        data.createEmployee(data.departmentId, newEmployee);

        contour.setName("insertContourTest2");
        contour.setTarget("target2");
        contour.setGroupId(4L);
        contour.setFteLimit(23L);
        contour.setDescription("desc2");
        contour.setStQueues("queue2");
        contour.setStProjectTickets("projectTicket2");
        contour.setWikiLinks("wiki2");
        contour.setBoards("board2");
        contour.setEmployer(newEmployee);
        contour.setProductOwner(newEmployee);
        contour.setTechLead(newEmployee);

        long updatedId = contourDao.updateContour(newContourId, contour);

        Contour updatedContour = contourDao.getContour(updatedId);

        assertEquals(Long.valueOf(updatedId), updatedContour.getId());
        assertEquals(contour.getName(), updatedContour.getName());
        assertEquals(contour.getTarget(), updatedContour.getTarget());
        assertEquals(contour.getGroupId(), updatedContour.getGroupId());
        assertEquals(contour.getFteLimit(), updatedContour.getFteLimit());
        assertEquals(contour.getDescription(), updatedContour.getDescription());
        assertEquals(contour.getStQueues(), updatedContour.getStQueues());
        assertEquals(contour.getStProjectTickets(), updatedContour.getStProjectTickets());
        assertEquals(contour.getWikiLinks(), updatedContour.getWikiLinks());
        assertEquals(contour.getBoards(), updatedContour.getBoards());
        assertEquals(contour.getEmployer(), updatedContour.getEmployer());
        assertEquals(contour.getProductOwner(), updatedContour.getProductOwner());
        assertEquals(contour.getTechLead(), updatedContour.getTechLead());
    }

    @Test
    // подробнее в тикете MARKETQPLANNER-603
    // тест создавался для проверки удаления контура из проекта. однако оказалось, что это не требуется.
    // Сейчас этот тест просто для примера других тестов с embedded postgres
    public void testDeleteContour() {

        long contourIdToDelete = data.createTestContour(data.groupId, "Контур для удаления 2");

        Project testProject = new Project();
        testProject.setCreated_by("pavellysenko");
        testProject.setProject_desc("тестовый проект");
        testProject.setContours(ImmutableMap.<String, BigDecimal> builder()
                .put(String.valueOf(data.contourId), BigDecimal.valueOf(1))
                .put(String.valueOf(contourIdToDelete), BigDecimal.valueOf(1))
                .build()
        );
        long id = projectDao.createProject(testProject);


        Project project = projectDao.get(id);
        assertTrue(project.getContours().containsKey(String.valueOf(data.contourId)));
        assertTrue(project.getContours().containsKey(String.valueOf(contourIdToDelete)));

        contourDao.deleteContour(contourIdToDelete);

        Project projectAfterDelete = projectDao.get(id);
        assertTrue(projectAfterDelete.getContours().containsKey(String.valueOf(data.contourId)));
        assertTrue(projectAfterDelete.getContours().containsKey(String.valueOf(contourIdToDelete)));

    }

}

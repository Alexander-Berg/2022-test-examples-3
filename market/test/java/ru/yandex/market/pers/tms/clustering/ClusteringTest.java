package ru.yandex.market.pers.tms.clustering;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.pers.grade.client.model.GradeType;
import ru.yandex.market.pers.tms.MockedPersTmsTest;

/**
 * @author eugenets
 *         Date: 23.03.2009
 *         Time: 20:24:48
 */
public class ClusteringTest extends MockedPersTmsTest {
    private static final GradeType GRADE_TYPE = GradeType.MODEL_GRADE;

    @Autowired
    private ClusterFileGradeLoader clusterFileGradeLoader;
    @Autowired
    private ClusterGradeService clusterGradeService;
    @Autowired
    private SimilarFinder similarFinder;

    @Test
    public void testSaveGradesFromDb() throws IOException, SQLException {
        clusterFileGradeLoader.saveGradesToFile(GRADE_TYPE);
    }

    @Test
    public void testCluster() throws Exception {
        Integer[] lastGrade = new Integer[]{0};
        Map<Integer, Grade> id2Grade = loadGrades(lastGrade);

        SFProcessor processor = new SFProcessor(clusterGradeService, new ClusterConfig());
        processor.setLastGrade(lastGrade[0]);
        processor.test(GRADE_TYPE, id2Grade);
    }

    private Map<Integer, Grade> loadGrades(final Integer[] lastGrade) {
        final Map<Integer, Grade> id2Grade = new HashMap<>();

        for (final Grade g : clusterFileGradeLoader.loadGrades()) {
            id2Grade.put(g.getId(), g);
            if (lastGrade[0] < g.getId()) {
                lastGrade[0] = g.getId();
            }
        }
        return id2Grade;
    }

    @Test
    public void testFindSimilar() throws IOException, SQLException {
        similarFinder.findSimilar();
    }

}

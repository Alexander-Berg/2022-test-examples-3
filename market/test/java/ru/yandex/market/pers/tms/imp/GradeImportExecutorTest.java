package ru.yandex.market.pers.tms.imp;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.market.pers.grade.client.model.GradeType;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.client.model.UsageTime;
import ru.yandex.market.pers.grade.core.db.DbGradeService;
import ru.yandex.market.pers.grade.core.db.GradeImportService;
import ru.yandex.market.pers.grade.core.model.core.AbstractGrade;
import ru.yandex.market.pers.grade.core.model.core.GradeToImport;
import ru.yandex.market.pers.grade.core.model.core.ModelGrade;
import ru.yandex.market.pers.grade.core.model.core.SecurityData;
import ru.yandex.market.pers.tms.MockedPersTmsTest;
import ru.yandex.market.pers.tms.timer.imp.GradeImportExecutor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 29.11.2018
 */
public class GradeImportExecutorTest extends MockedPersTmsTest {

    @Autowired
    private GradeImportService gradeImportService;

    @Autowired
    private GradeImportExecutor executor;

    @Autowired
    private DbGradeService gradeService;

    @Test
    public void testPhilipsCsvLoading() throws Exception {
        String csv = IOUtils.toString(
            getClass().getResourceAsStream("/testdata/philips_grades_import.csv"),
            StandardCharsets.UTF_8
        );

        String source = "import;vendor;philips";
        SecurityData sec = SecurityData.generated();

        List<Long> queueIds = gradeImportService.importGradesFromCsv(csv, GradeType.MODEL_GRADE, source, sec, true);

        Map<Long, GradeToImport> gradesToImport = gradeImportService
            .getGradesToImport(GradeType.MODEL_GRADE, 100).stream()
            .collect(Collectors.toMap(
                GradeToImport::getQueueId,
                x -> x,
                (x, y) -> y
            ));

        assertEquals(49, gradesToImport.size());

        // check some grade
        ModelGrade testGrade1 = (ModelGrade) gradesToImport.get(queueIds.get(10)).getGrade();
        SecurityData testGrade1Sec = gradesToImport.get(queueIds.get(10)).getSecurityData();

        assertEquals(13481605, testGrade1.getModelId().longValue());
        assertEquals("2018-09-20 00:00:00", GradeImportService.DATE_FORMAT_REF.get().format(testGrade1.getCreated()));
        assertEquals(-2, testGrade1.getGr0FromAverage().intValue());
        assertEquals(1, testGrade1.getAverageGrade().intValue());
        assertEquals("Фотоэпилтрр получила в подарок в декабре прошло года, очень обрадовалась, так как мечтала " +
                "делать фотоэпиляцию в домашних условиях. После нескольких процедур прибор стал периодически " +
                "отключаться сам по себе, потом это стало происходить по несколько раз за процедуру. В " +
                "какой-то момент фотоэпилироваться просто стало невозможно, потому что каждую минуту прибор " +
                "выключался!сдали назад, от производителя пришёл ответ, что прибор неисправен и заменили на " +
                "новый. Начала все сначала, но через несколько процедур опять начались «чудеса»: то лампочки " +
                "все оранжевым горят, хотя кожа у меня светлая, то вспышка не работает, то прибор режим не " +
                "подбирает! Придётся опять нести назад! Безобразие! За такие деньги продавать товар такого " +
                "плохого качества!!!!!",
            testGrade1.getText());
        assertEquals("дизайн", testGrade1.getPro());
        assertEquals("не работает!!!!", testGrade1.getContra());
        assertNull(testGrade1.getUsageTime());
        assertFalse(testGrade1.getRecommend());
        assertEquals(source, testGrade1.getSource());

        assertEquals("127.0.0.1", testGrade1Sec.getIp());
        assertTrue(testGrade1Sec.getHeadersStr().contains("generated: true"));

        // some other grade
        ModelGrade testGrade2 = (ModelGrade) gradesToImport.get(queueIds.get(24)).getGrade();

        assertEquals(12909293, testGrade2.getModelId().longValue());
        assertEquals("2018-09-17 00:00:00", GradeImportService.DATE_FORMAT_REF.get().format(testGrade2.getCreated()));
        assertEquals(2, testGrade2.getGr0FromAverage().intValue());
        assertEquals(5, testGrade2.getAverageGrade().intValue());
        assertEquals(
            "Ни чуть не жалею, что перешел с обычной щетки на данную модель. Разница сразу ощутима. Раньше, с " +
                "помощью обычной щетки, у меня не получалось очистить труднодоступные места, соответственно, " +
                "на зубах оставался некий налет, что вызывало не приятные ощущения. Почистив зубы этой " +
                "щеткой, у убрал весь налет с зубов. Через две недели я заметил разницу в белезне своих зубов" +
                ". Если же ранее они были с желтым оттенком, то сейчас намного белее. В целом всем доволен!",
            testGrade2.getText());
        assertEquals("тщательная чистка отлично вычищает налет, красивый стильный дизайн щетки", testGrade2.getPro());
        assertNull(testGrade2.getContra());
        assertEquals(UsageTime.SEVERAL_DAYS, testGrade2.getUsageTime());
        assertTrue(testGrade2.getRecommend());
        assertEquals(source, testGrade2.getSource());

        // and one more
        ModelGrade testGrade3 = (ModelGrade) gradesToImport.get(queueIds.get(46)).getGrade();

        assertEquals(12916985, testGrade3.getModelId().longValue());
        assertEquals("2018-09-11 00:00:00", GradeImportService.DATE_FORMAT_REF.get().format(testGrade3.getCreated()));
        assertEquals(-1, testGrade3.getGr0FromAverage().intValue());
        assertEquals(2, testGrade3.getAverageGrade().intValue());
        assertEquals(
            "Через 1.5 года работы перестала нормально работать и заряжаться. По гарантии её поменяли. Не " +
                "устроило, что пройдёт гарантия скоро и она может опять сломаться и мне придётся её выкинуть." +
                " Думаю, что внутрь попала вода, теперь после чистки зубов вытираю её.",
            testGrade3.getText());
        assertEquals("качественные материалы.", testGrade3.getPro());
        assertEquals("негерметичная конструкция.", testGrade3.getContra());
        assertEquals(UsageTime.MORE_THAN_A_YEAR, testGrade3.getUsageTime());
        assertFalse(testGrade3.getRecommend());
        assertEquals(source, testGrade3.getSource());
    }

    @Test
    public void testSonyCsvLoading() throws Exception {
        String csv = IOUtils.toString(
            getClass().getResourceAsStream("/testdata/sony_grades_import.csv"),
            StandardCharsets.UTF_8
        );

        String source = "import;vendor;sony";
        SecurityData sec = SecurityData.generated();

        List<Long> queueIds = gradeImportService.importGradesFromCsv(csv, GradeType.MODEL_GRADE, source, sec, true);

        Map<Long, GradeToImport> gradesToImport = gradeImportService
            .getGradesToImport(GradeType.MODEL_GRADE, 100).stream()
            .collect(Collectors.toMap(
                GradeToImport::getQueueId,
                x -> x,
                (x, y) -> y
            ));

        assertEquals(42, gradesToImport.size());

        // check some grade
        ModelGrade testGrade1 = (ModelGrade) gradesToImport.get(queueIds.get(37)).getGrade();
        SecurityData testGrade1Sec = gradesToImport.get(queueIds.get(37)).getSecurityData();

        assertEquals(8270355, testGrade1.getModelId().longValue());
        assertEquals("2018-09-12 00:00:00", GradeImportService.DATE_FORMAT_REF.get().format(testGrade1.getCreated()));
        assertEquals(-1, testGrade1.getGr0FromAverage().intValue());
        assertEquals(2, testGrade1.getAverageGrade().intValue());
        assertEquals(
            "Если честно от фотоаппарата за такие деньги ожидали немного большего, ну а по факту - улучшенная " +
                "мыльница. Сама модель тоже довольно старая. На мой взгляд своих денег точно не стоит, да и " +
                "если бы знал - взял что нибудь получше.",
            testGrade1.getText());
        assertNull(testGrade1.getPro());
        assertNull(testGrade1.getContra());
        assertNull(testGrade1.getUsageTime());
        assertFalse(testGrade1.getRecommend());
        assertEquals(source, testGrade1.getSource());

        assertEquals("127.0.0.1", testGrade1Sec.getIp());
        assertTrue(testGrade1Sec.getHeadersStr().contains("generated: true"));

        // some other grade
        ModelGrade testGrade2 = (ModelGrade) gradesToImport.get(queueIds.get(35)).getGrade();

        assertEquals(1731730369, testGrade2.getModelId().longValue());
        assertEquals("2018-09-13 00:00:00", GradeImportService.DATE_FORMAT_REF.get().format(testGrade2.getCreated()));
        assertEquals(1, testGrade2.getGr0FromAverage().intValue());
        assertEquals(4, testGrade2.getAverageGrade().intValue());
        assertEquals(
            "Хороший телефон, но экран почему то отдаёт лёгким розо-красным оттенком.",
            testGrade2.getText());
        assertNull(testGrade2.getPro());
        assertNull(testGrade2.getContra());
        assertNull(testGrade2.getUsageTime());
        assertNull(testGrade2.getRecommend());
        assertEquals(source, testGrade2.getSource());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testBadCsvLoading() throws Exception {
        String csv = IOUtils.toString(
            getClass().getResourceAsStream("/testdata/sony_grades_import_bad.csv"),
            StandardCharsets.UTF_8
        );

        String source = "import;vendor;sony";
        SecurityData sec = SecurityData.generated();

        gradeImportService.importGradesFromCsv(csv, GradeType.MODEL_GRADE, source, sec, true);
    }

    @Test
    public void testImporting() throws Exception {
        String csv = IOUtils.toString(
            getClass().getResourceAsStream("/testdata/sony_grades_import.csv"),
            StandardCharsets.UTF_8
        );

        String source = "import;vendor;sony";
        SecurityData sec = SecurityData.generated();

        List<Long> queueIds = gradeImportService.importGradesFromCsv(csv, GradeType.MODEL_GRADE, source, sec, true);

        Map<Long, Long> queueIdToGradeId = executor.importNextGradesBatch(100).stream()
            .collect(Collectors.toMap(
                Pair::getFirst,
                Pair::getSecond,
                (x, y) -> y
            ));

        assertEquals(42, queueIdToGradeId.size());

        AbstractGrade testGrade2 = gradeService.getGrade(queueIdToGradeId.get(queueIds.get(35)));

        assertEquals(1731730369, testGrade2.getResourceId().longValue());
        assertEquals("2018-09-13 00:00:00", GradeImportService.DATE_FORMAT_REF.get().format(testGrade2.getCreated()));
        assertEquals(1, testGrade2.getGr0FromAverage().intValue());
        assertEquals(4, testGrade2.getAverageGrade().intValue());
        assertEquals("Хороший телефон, но экран почему то отдаёт лёгким розо-красным оттенком.",
            testGrade2.getText());
        assertNull(testGrade2.getPro());
        assertNull(testGrade2.getContra());
        assertNull(testGrade2.getRecommend());
        assertEquals(source, testGrade2.getSource());
    }

    @Test
    public void testImportAsApproved() throws Exception {
        String csv = IOUtils.toString(
            getClass().getResourceAsStream("/testdata/sony_grades_import.csv"),
            StandardCharsets.UTF_8
        );

        String source = "import;vendor;sony";
        SecurityData sec = SecurityData.generated();

        List<Long> queueIds = gradeImportService.importGradesFromCsv(csv, GradeType.MODEL_GRADE, source, sec, true);

        Map<Long, Long> queueIdToGradeId = executor.importNextGradesBatch(100).stream()
            .collect(Collectors.toMap(
                Pair::getFirst,
                Pair::getSecond,
                (x, y) -> y
            ));

        assertEquals(42, queueIdToGradeId.size());

        AbstractGrade testGrade = gradeService.getGrade(queueIdToGradeId.get(queueIds.get(35)));
        assertEquals(ModState.APPROVED, testGrade.getModState());
    }

    @Test
    public void testImportAsNotApproved() throws Exception {
        String csv = IOUtils.toString(
            getClass().getResourceAsStream("/testdata/sony_grades_import.csv"),
            StandardCharsets.UTF_8
        );

        String source = "import;vendor;sony";
        SecurityData sec = SecurityData.generated();

        List<Long> queueIds = gradeImportService.importGradesFromCsv(csv, GradeType.MODEL_GRADE, source, sec, false);

        Map<Long, Long> queueIdToGradeId = executor.importNextGradesBatch(100).stream()
            .collect(Collectors.toMap(
                Pair::getFirst,
                Pair::getSecond,
                (x, y) -> y
            ));

        assertEquals(42, queueIdToGradeId.size());

        AbstractGrade testGrade = gradeService.getGrade(queueIdToGradeId.get(queueIds.get(35)));
        assertEquals(ModState.UNMODERATED, testGrade.getModState());
    }
}

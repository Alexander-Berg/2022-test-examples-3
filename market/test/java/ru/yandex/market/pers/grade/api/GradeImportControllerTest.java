package ru.yandex.market.pers.grade.api;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import ru.yandex.market.pers.grade.MockedPersGradeTest;
import ru.yandex.market.pers.grade.client.model.GradeType;
import ru.yandex.market.pers.grade.client.model.ModState;
import ru.yandex.market.pers.grade.client.model.UsageTime;
import ru.yandex.market.pers.grade.core.db.GradeImportService;
import ru.yandex.market.pers.grade.core.model.core.GradeToImport;
import ru.yandex.market.pers.grade.core.model.core.ModelGrade;
import ru.yandex.market.pers.grade.core.model.core.SecurityData;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @author Ilya Kislitsyn / ilyakis@ / 30.11.2018
 */
public class GradeImportControllerTest extends MockedPersGradeTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GradeImportService gradeImportService;

    @Test
    public void testPhilipsCsvLoading() throws Exception {
        String csv = IOUtils.toString(
            getClass().getResourceAsStream("/data/philips_grades_import.csv"),
            StandardCharsets.UTF_8
        );

        String source = "import;vendor;philips";

        doImport(csv, "vendor", "philips");

        Map<Long, GradeToImport> gradesToImport = gradeImportService
            .getGradesToImport(GradeType.MODEL_GRADE, 100).stream()
            .collect(Collectors.toMap(
                x -> x.getGrade().getResourceId(),
                x -> x,
                (x, y) -> y
            ));

        assertEquals(38, gradesToImport.size());

        // check some grade
        GradeToImport gradeToImport1 = gradesToImport.get(13481605L);
        ModelGrade testGrade1 = (ModelGrade) gradeToImport1.getGrade();
        SecurityData testGrade1Sec = gradeToImport1.getSecurityData();

        assertTrue(gradeToImport1.isImportAsApproved());
        assertEquals(ModState.UNMODERATED, testGrade1.getModState());
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
        assertTrue(testGrade1Sec.getHeadersStr().contains("X-Real-Ip: 127.0.0.1"));

        // some other grade
        GradeToImport gradeToImport2 = gradesToImport.get(12909293L);
        ModelGrade testGrade2 = (ModelGrade) gradeToImport2.getGrade();

        assertTrue(gradeToImport2.isImportAsApproved());
        assertEquals(ModState.UNMODERATED, testGrade2.getModState());
        assertEquals(12909293, testGrade2.getModelId().longValue());
        assertEquals("2018-09-17 00:00:00", GradeImportService.DATE_FORMAT_REF.get().format(testGrade2.getCreated()));
        assertEquals(2, testGrade2.getGr0FromAverage().intValue());
        assertEquals(5, testGrade2.getAverageGrade().intValue());
        assertEquals("Ни чуть не жалею, что перешел с обычной щетки на данную модель. Разница сразу ощутима. Раньше, " +
                "с помощью обычной щетки, у меня не получалось очистить труднодоступные места, " +
                "соответственно, на зубах оставался некий налет, что вызывало не приятные ощущения. Почистив " +
                "зубы этой щеткой, у убрал весь налет с зубов. Через две недели я заметил разницу в белезне " +
                "своих зубов. Если же ранее они были с желтым оттенком, то сейчас намного белее. В целом всем" +
                " доволен!",
            testGrade2.getText());
        assertEquals("тщательная чистка отлично вычищает налет, красивый стильный дизайн щетки", testGrade2.getPro());
        assertNull(testGrade2.getContra());
        assertEquals(UsageTime.SEVERAL_DAYS, testGrade2.getUsageTime());
        assertTrue(testGrade2.getRecommend());
        assertEquals(source, testGrade2.getSource());

        // and one more
        GradeToImport gradeToImport3 = gradesToImport.get(12916985L);
        ModelGrade testGrade3 = (ModelGrade) gradeToImport3.getGrade();

        assertTrue(gradeToImport3.isImportAsApproved());
        assertEquals(ModState.UNMODERATED, testGrade3.getModState());
        assertEquals(12916985, testGrade3.getModelId().longValue());
        assertEquals("2018-09-11 00:00:00", GradeImportService.DATE_FORMAT_REF.get().format(testGrade3.getCreated()));
        assertEquals(-1, testGrade3.getGr0FromAverage().intValue());
        assertEquals(2, testGrade3.getAverageGrade().intValue());
        assertEquals("Через 1.5 года работы перестала нормально работать и заряжаться. По гарантии её поменяли. Не " +
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
    @Transactional //to view db changes in transaction
    public void testZeroModelId() throws Exception {
        String csv = IOUtils.toString(
            getClass().getResourceAsStream("/data/grades_import_zero_model_id.csv"),
            StandardCharsets.UTF_8
        );

        mockMvc.perform(
            post("/api/import/csv")
                .param("grade_type", String.valueOf(GradeType.MODEL_GRADE.value()))
                .param("source_type", "vendor")
                .param("source_code", "philips")
                .header("X-Real-Ip", "test-ip")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .content(csv)
        )
            .andDo(print())
            .andExpect(status().is4xxClientError())
            .andReturn();

        Map<Long, GradeToImport> gradesToImport = gradeImportService
            .getGradesToImport(GradeType.MODEL_GRADE, 100).stream()
            .collect(Collectors.toMap(
                x -> x.getGrade().getResourceId(),
                x -> x,
                (x, y) -> y
            ));

        assertEquals(38, gradesToImport.size());
    }

    @Test
    public void testNeedModeration() throws Exception {
        String csv = IOUtils.toString(
            getClass().getResourceAsStream("/data/philips_grades_import.csv"),
            StandardCharsets.UTF_8
        );

        mockMvc.perform(
            post("/api/import/csv")
                .param("grade_type", String.valueOf(GradeType.MODEL_GRADE.value()))
                .param("source_type", "vendor")
                .param("source_code", "philips")
                .param("import_as_approved", "false")
                .header("X-Real-Ip", "test-ip")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .content(csv)
        )
            .andDo(print())
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        Map<Long, GradeToImport> gradesToImport = gradeImportService
            .getGradesToImport(GradeType.MODEL_GRADE, 100).stream()
            .collect(Collectors.toMap(
                x -> x.getGrade().getResourceId(),
                x -> x,
                (x, y) -> y
            ));

        assertEquals(38, gradesToImport.size());

        // check mod state
        GradeToImport gradeToImport1 = gradesToImport.get(13481605L);
        ModelGrade testGrade1 = (ModelGrade) gradeToImport1.getGrade();
        assertFalse(gradeToImport1.isImportAsApproved());
        assertEquals(ModState.UNMODERATED, testGrade1.getModState());

        // some other grade
        GradeToImport gradeToImport2 = gradesToImport.get(12909293L);
        ModelGrade testGrade2 = (ModelGrade) gradeToImport2.getGrade();
        assertEquals(ModState.UNMODERATED, testGrade2.getModState());
        assertFalse(gradeToImport2.isImportAsApproved());

        // and one more
        GradeToImport gradeToImport3 = gradesToImport.get(12916985L);
        ModelGrade testGrade3 = (ModelGrade) gradeToImport3.getGrade();
        assertEquals(ModState.UNMODERATED, testGrade3.getModState());
        assertFalse(gradeToImport3.isImportAsApproved());
    }

    @Test
    public void testImportAsApproved() throws Exception {
        String csv = IOUtils.toString(
            getClass().getResourceAsStream("/data/philips_grades_import.csv"),
            StandardCharsets.UTF_8
        );

        mockMvc.perform(
            post("/api/import/csv")
                .param("grade_type", String.valueOf(GradeType.MODEL_GRADE.value()))
                .param("source_type", "vendor")
                .param("source_code", "philips")
                .param("import_as_approved", "true")
                .header("X-Real-Ip", "test-ip")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .content(csv)
        )
            .andDo(print())
            .andExpect(status().is2xxSuccessful())
            .andReturn();

        Map<Long, GradeToImport> gradesToImport = gradeImportService
            .getGradesToImport(GradeType.MODEL_GRADE, 100).stream()
            .collect(Collectors.toMap(
                x -> x.getGrade().getResourceId(),
                x -> x,
                (x, y) -> y
            ));

        assertEquals(38, gradesToImport.size());

        // check mod state
        GradeToImport gradeToImport1 = gradesToImport.get(13481605L);
        ModelGrade testGrade1 = (ModelGrade) gradeToImport1.getGrade();
        assertEquals(ModState.UNMODERATED, testGrade1.getModState());
        assertTrue(gradeToImport1.isImportAsApproved());

        // some other grade
        GradeToImport gradeToImport2 = gradesToImport.get(12909293L);
        ModelGrade testGrade2 = (ModelGrade) gradeToImport2.getGrade();
        assertEquals(ModState.UNMODERATED, testGrade2.getModState());
        assertTrue(gradeToImport2.isImportAsApproved());

        // and one more
        GradeToImport gradeToImport3 = gradesToImport.get(12916985L);
        ModelGrade testGrade3 = (ModelGrade) gradeToImport3.getGrade();
        assertEquals(ModState.UNMODERATED, testGrade3.getModState());
        assertTrue(gradeToImport3.isImportAsApproved());
    }


    private void doImport(String csv, String sourceType, String sourceCode) throws Exception {
        mockMvc.perform(
            post("/api/import/csv")
                .param("grade_type", String.valueOf(GradeType.MODEL_GRADE.value()))
                .param("source_type", sourceType)
                .param("source_code", sourceCode)
                .header("X-Real-Ip", "test-ip")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .content(csv)
        )
            .andDo(print())
            .andExpect(status().is2xxSuccessful())
            .andReturn();
    }

}

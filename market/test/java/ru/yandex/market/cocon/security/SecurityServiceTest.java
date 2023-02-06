package ru.yandex.market.cocon.security;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.dbunit.database.DatabaseConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.cocon.CabinetService;
import ru.yandex.market.cocon.FunctionalTest;
import ru.yandex.market.cocon.model.Cabinet;
import ru.yandex.market.cocon.model.CabinetType;
import ru.yandex.market.cocon.model.Feature;
import ru.yandex.market.cocon.model.Page;
import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.JsonTestUtil;
import ru.yandex.market.common.test.util.StringTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
@SuppressWarnings("OptionalGetWithoutIsPresent")
@DbUnitDataBaseConfig({
        @DbUnitDataBaseConfig.Entry(name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS, value = "true"),
        @DbUnitDataBaseConfig.Entry(name = DatabaseConfig.FEATURE_QUALIFIED_TABLE_NAMES, value = "true")
})
@DbUnitDataSet(before = "SecurityServiceTest.before.csv", schema = "cocon")
class SecurityServiceTest extends FunctionalTest {

    @Autowired
    private SecurityService securityService;
    @Autowired
    private CabinetService cabinetService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testPages() throws IOException {
        Cabinet cabinet = read("testcab.json");
        doReturn(cabinet).when(cabinetService).getCabinet(eq(CabinetType.SUPPLIER));
        int pagesNum = cabinet.getPages().size();
        Cabinet result = securityService.getPages(CabinetType.SUPPLIER, null, new Object());
        // Остаются все страницы
        assertEquals(pagesNum, result.getPages().size());
        // Проверяем результат для кабинета
        assertEquals(true, result.getStates().get().getResult());
        assertEquals(true, result.getRoles().get().getResult());
        // Фичи в ответе есть
        assertNotNull(result.getPages().get(0).getFeatures());
        // Проверяем результат для первой страницы
        assertEquals(true, result.getPages().get(0).getStates().get().getResult());
        assertEquals(true, result.getPages().get(0).getRoles().get().getResult());
        // Проверяем результат для первой страницы
        assertEquals(false, result.getPages().get(1).getStates().get().getResult());
        assertEquals(false, result.getPages().get(1).getRoles().get().getResult());
    }

    @Test
    void testSlimPages() throws  IOException {
        Cabinet cabinet = read("testcab-features.json");
        doReturn(cabinet).when(cabinetService).getCabinet(eq(CabinetType.SUPPLIER));
        Cabinet result = securityService.getSlimPages(CabinetType.SUPPLIER, null, new Object());
        // Фич в ответе нет
        assertNull(result.getPages().get(0).getFeatures());
        assertNull(result.getFeatures());
    }

    @Test
    void testFeatures() throws IOException {
        Cabinet cabinet = read("testcab.json");
        doReturn(cabinet).when(cabinetService).getCabinet(eq(CabinetType.SUPPLIER));
        Cabinet result = securityService.getFeatures(CabinetType.SUPPLIER, null, "testPage1", new Object());
        // Остаётся одна страница
        assertEquals(1, result.getPages().size());
        assertEquals("testPage1", result.getPages().get(0).getName());

        // С результатами для фичи
        Feature feature = result.getPages().get(0).getFeatures().get(0);
        assertEquals(true, feature.getStates().get().getResult());
        assertEquals(false, feature.getRoles().get().getResult());
    }

    @Test
    void testDefaults() throws IOException {
        Cabinet cabinet = read("testcab.json");
        doReturn(cabinet).when(cabinetService).getCabinet(eq(CabinetType.SUPPLIER));
        Cabinet result = securityService.getFeatures(CabinetType.SUPPLIER, null, "withDefaults", new Object());

        // defaults на странице не применяются к самой странице
        Page page = result.getPages().get(0);
        assertTrue(page.getStates().get().getResult());
        assertTrue(page.getRoles().get().getResult());

        // зато применяются к фиче без правил
        Feature justDefault = page.getFeatures().get(0);
        assertFalse(justDefault.getStates().get().getResult());
        assertTrue(justDefault.getRoles().get().getResult());

        // а вот фича с правилами перевычисляет целиком, даже если есть только одна группа
        Feature withSpecific = page.getFeatures().get(1);
        assertTrue(withSpecific.getStates().get().getResult());
        assertFalse(withSpecific.getRoles().get().getResult());
    }

    @Test
    void testOverridesOnPage() throws IOException {
        Cabinet cabinet = read("testcab-override.json");
        doReturn(cabinet).when(cabinetService).getCabinet(eq(CabinetType.SUPPLIER));
        Cabinet overridden = securityService.getFeatures(CabinetType.SUPPLIER, null, "overriden", new Object());
        assertFalse(overridden.getStates().get().getResult());

        Page page = overridden.getPages().get(0);
        assertTrue(page.getStates().get().getResult());
        Feature feature = page.getFeatures().get(0);
        assertTrue(feature.getStates().get().getResult());
    }

    @Test
    void testOverridesOnFeature() throws IOException {
        Cabinet cabinet = read("testcab-override.json");
        doReturn(cabinet).when(cabinetService).getCabinet(eq(CabinetType.SUPPLIER));
        Cabinet overridden = securityService.getFeatures(CabinetType.SUPPLIER, null, "overridenFeature", new Object());
        assertFalse(overridden.getStates().get().getResult());

        Page page = overridden.getPages().get(0);
        Feature feature = page.getFeatures().get(0);
        assertFalse(page.getStates().get().getResult());
        assertTrue(feature.getStates().get().getResult());
    }

    @Test
    void testCabinetFeatures() throws IOException {
        Cabinet cabinet = read("testcab-features.json");
        doReturn(cabinet).when(cabinetService).getCabinet(eq(CabinetType.SUPPLIER));
        Cabinet result = securityService.getPages(CabinetType.SUPPLIER, null, new Object());

        JsonTestUtil.assertEquals(
                StringTestUtil.getString(this.getClass(), "testcab-features.result.json"),
                objectMapper.writerFor(Cabinet.class).writeValueAsString(result));
    }

    private Cabinet read(String file) throws IOException {
        return objectMapper.readerFor(Cabinet.class)
                .readValue(StringTestUtil.getString(SecurityServiceTest.class, file));
    }

}

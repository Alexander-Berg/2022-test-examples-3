package ru.yandex.market.mbo.mdm.common.masterdata.services.param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mbo.excel.ExcelFile;
import ru.yandex.market.mbo.excel.ExcelFileConverter;
import ru.yandex.market.mbo.mdm.common.infrastructure.FileStatus;
import ru.yandex.market.mbo.mdm.common.masterdata.model.MappingCacheDao;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParam;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamIoType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValueType;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.MappingsCacheRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepository;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.offers.model.ShopSkuKey;
import ru.yandex.market.mboc.common.services.storage.StorageKeyValueServiceMock;
import ru.yandex.market.mboc.common.utils.MdmProperties;
import ru.yandex.market.mboc.common.utils.TaskQueueRegistratorMock;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("checkstyle:MagicNumber")
public class CategoryMdmParamExcelImportServiceTest extends MdmBaseDbTestClass {
    @Autowired
    private CategoryParamValueRepository categoryParamValueRepository;
    @Autowired
    private MappingsCacheRepository mappingsCacheRepository;

    private CategoryMdmParamExcelImportService service;
    private MdmParamProviderMock mdmParamProvider;
    private StorageKeyValueService keyValueService;
    private MdmParamCacheMock mdmParamCacheMock;

    @Before
    public void setup() {
        categoryParamValueRepository.deleteAll();
        mdmParamCacheMock = TestMdmParamUtils.createParamCacheMock();
        mdmParamProvider = new MdmParamProviderMock(mdmParamCacheMock);
        keyValueService = new StorageKeyValueServiceMock();
        service = new CategoryMdmParamExcelImportService(categoryParamValueRepository,
            new TaskQueueRegistratorMock(), mdmParamProvider, mdmParamCacheMock, mappingsCacheRepository,
            keyValueService);
    }

    @Test
    public void whenNoHidColumnShouldReturnError() {
        var file = new ExcelFile.Builder();
        List<String> errors = service.importExcel("", bytes(file)).getErrors();
        assertThat(errors).containsExactlyInAnyOrder("Не найдена колонка \"ID категории\"");
    }

    @Test
    public void whenNoParamColumnsShouldReturnError() {
        var file = new ExcelFile.Builder();
        file.addHeader(MdmParamExcelAttributes.HID_HEADER);
        file.addHeader(MdmParamExcelAttributes.NAME_HEADER);
        List<String> errors = service.importExcel("", bytes(file)).getErrors();
        assertThat(errors).containsExactlyInAnyOrder("Не найдено колонок для значений параметров");
    }

    @Test
    public void whenNonExistingParamColumnsFoundShouldReturnError() {
        var file = new ExcelFile.Builder();
        file.addHeader(MdmParamExcelAttributes.HID_HEADER);
        file.addHeader(MdmParamExcelAttributes.NAME_HEADER);
        file.addHeader("Микроорганизмы и вирусы");
        file.addHeader("Применимы применимости сроков годности");
        List<String> errors = service.importExcel("", bytes(file)).getErrors();
        assertThat(errors).containsExactlyInAnyOrder("Не найдены следующие параметры: " +
            "Применимы применимости сроков годности, Микроорганизмы и вирусы");
    }

    @Test
    public void whenInvalidBoolShouldReturnError() {
        String paramTitle = "Микроорганизмы и вирусы";
        long mdmParamId = 98053L;
        mdmParamProvider.markParamAllowed(MdmParamIoType.CATEGORY_EXCEL_IMPORT, mdmParamId);

        MdmParam param = new MdmParam()
            .setId(mdmParamId)
            .setTitle(paramTitle)
            .setXslName(paramTitle)
            .setValueType(MdmParamValueType.BOOL);
        mdmParamCacheMock.add(param);

        var file = new ExcelFile.Builder();
        file.addHeader(MdmParamExcelAttributes.HID_HEADER);
        file.addHeader(MdmParamExcelAttributes.NAME_HEADER);
        file.addHeader(paramTitle);
        file.addLine("959687", "Одежда и обувь/Полосатые носки", "неть");
        file.addLine("959688", "Одежда и обувь/Волосатые носки", "нит");
        file.addLine("959689", "Одежда и обувь/Инфузории туфельки", "дыа");
        file.addLine("959690", "Водоросли/Фитопланктон/Диатомовые водоросли", "дя");

        List<String> errors = service.importExcel("", bytes(file)).getErrors();
        assertThat(errors).containsExactlyInAnyOrder(
            "Ключ 959687, параметр \"Микроорганизмы и вирусы\": значение \"неть\" должно быть \"да\" или \"нет\"",
            "Ключ 959688, параметр \"Микроорганизмы и вирусы\": значение \"нит\" должно быть \"да\" или \"нет\"",
            "Ключ 959689, параметр \"Микроорганизмы и вирусы\": значение \"дыа\" должно быть \"да\" или \"нет\"",
            "Ключ 959690, параметр \"Микроорганизмы и вирусы\": значение \"дя\" должно быть \"да\" или \"нет\"");
    }

    @Test
    public void whenValidBoolShouldReturnSuccessAndSaveParams() {
        String paramTitle = "Микроорганизмы и вирусы";
        long mdmParamId = 98053L;
        mdmParamProvider.markParamAllowed(MdmParamIoType.CATEGORY_EXCEL_IMPORT, mdmParamId);

        MdmParam param = new MdmParam()
            .setId(mdmParamId)
            .setTitle(paramTitle)
            .setXslName(paramTitle)
            .setValueType(MdmParamValueType.BOOL);
        mdmParamCacheMock.add(param);

        var file = new ExcelFile.Builder();
        file.addHeader(MdmParamExcelAttributes.HID_HEADER);
        file.addHeader(MdmParamExcelAttributes.NAME_HEADER);
        file.addHeader(paramTitle);
        file.addLine("959687", "Одежда и обувь/Полосатые носки", "нет");
        file.addLine("959688", "Одежда и обувь/Волосатые носки", "Нет");
        file.addLine("959689", "Одежда и обувь/Инфузории туфельки", "дА");
        file.addLine("959690", "Водоросли/Фитопланктон/Диатомовые водоросли", " да\t");

        List<String> errors = service.importExcel("", bytes(file)).getErrors();
        assertThat(errors).isEmpty();

        CategoryParamValue stripedSocksValue = value(959687, param, false);
        CategoryParamValue hairedSocksValue = value(959688, param, false);
        CategoryParamValue parameciumCaudatumValue = value(959689, param, true);
        CategoryParamValue diatomeaValue = value(959690, param, true);

        assertThat(categoryParamValueRepository.findAll()).containsExactlyInAnyOrder(
            stripedSocksValue,
            hairedSocksValue,
            parameciumCaudatumValue,
            diatomeaValue
        );
    }

    @Test
    public void whenValidUnchangedBoolShouldReturnSuccessAndDoNothing() {
        String paramTitle = "Микроорганизмы и вирусы";
        long mdmParamId = 98053L;
        mdmParamProvider.markParamAllowed(MdmParamIoType.CATEGORY_EXCEL_IMPORT, mdmParamId);

        MdmParam param = new MdmParam()
            .setId(mdmParamId)
            .setTitle(paramTitle)
            .setXslName(paramTitle)
            .setValueType(MdmParamValueType.BOOL);
        mdmParamCacheMock.add(param);

        var file = new ExcelFile.Builder();
        file.addHeader(MdmParamExcelAttributes.HID_HEADER);
        file.addHeader(MdmParamExcelAttributes.NAME_HEADER);
        file.addHeader(paramTitle);
        file.addLine("959687", "Одежда и обувь/Полосатые носки", "нет");
        file.addLine("959688", "Одежда и обувь/Волосатые носки", "Нет");
        file.addLine("959689", "Одежда и обувь/Инфузории туфельки", "дА");
        file.addLine("959690", "Водоросли/Фитопланктон/Диатомовые водоросли", " да\t");

        CategoryParamValue stripedSocksValue = value(959687, param, false);
        CategoryParamValue hairedSocksValue = value(959688, param, false);
        CategoryParamValue parameciumCaudatumValue = value(959689, param, true);
        CategoryParamValue diatomeaValue = value(959690, param, true);
        categoryParamValueRepository.insertBatch(stripedSocksValue,
            hairedSocksValue,
            parameciumCaudatumValue,
            diatomeaValue
        );
        Instant importTime = Instant.now();

        List<String> errors = service.importExcel("", bytes(file)).getErrors();
        assertThat(errors).isEmpty();

        assertThat(categoryParamValueRepository.findAll()).containsExactlyInAnyOrder(
            stripedSocksValue,
            hairedSocksValue,
            parameciumCaudatumValue,
            diatomeaValue
        );
        categoryParamValueRepository.findAll().forEach(value ->
            assertThat(value.getUpdatedTs()).isBeforeOrEqualTo(importTime));
    }

    @Test
    public void whenValidChangedBoolShouldReturnSuccessAndUpdateValues() {
        String paramTitle = "Микроорганизмы и вирусы";
        long mdmParamId = 98053L;
        mdmParamProvider.markParamAllowed(MdmParamIoType.CATEGORY_EXCEL_IMPORT, mdmParamId);

        MdmParam param = new MdmParam()
            .setId(mdmParamId)
            .setTitle(paramTitle)
            .setXslName(paramTitle)
            .setValueType(MdmParamValueType.BOOL);
        mdmParamCacheMock.add(param);

        var file = new ExcelFile.Builder();
        file.addHeader(MdmParamExcelAttributes.HID_HEADER);
        file.addHeader(MdmParamExcelAttributes.NAME_HEADER);
        file.addHeader(paramTitle);
        file.addLine("959687", "Одежда и обувь/Полосатые носки", "нет");
        file.addLine("959688", "Одежда и обувь/Волосатые носки", "Нет");
        file.addLine("959689", "Одежда и обувь/Инфузории туфельки", "дА");
        file.addLine("959690", "Водоросли/Фитопланктон/Диатомовые водоросли", " да\t");

        CategoryParamValue stripedSocksValue = value(959687, param, true);
        CategoryParamValue hairedSocksValue = value(959688, param, true);
        CategoryParamValue parameciumCaudatumValue = value(959689, param, false);
        CategoryParamValue diatomeaValue = value(959690, param, false);
        categoryParamValueRepository.insertBatch(stripedSocksValue,
            hairedSocksValue,
            parameciumCaudatumValue,
            diatomeaValue
        );
        Instant importTime = Instant.now();

        // Изменим значения на ожидаемые из эксельки
        stripedSocksValue.setBool(false);
        hairedSocksValue.setBool(false);
        parameciumCaudatumValue.setBool(true);
        diatomeaValue.setBool(true);

        List<String> errors = service.importExcel("", bytes(file)).getErrors();
        assertThat(errors).isEmpty();

        assertThat(categoryParamValueRepository.findAll()).containsExactlyInAnyOrder(
            stripedSocksValue,
            hairedSocksValue,
            parameciumCaudatumValue,
            diatomeaValue
        );
        categoryParamValueRepository.findAll().forEach(value ->
            assertThat(value.getUpdatedTs()).isAfter(importTime));
    }

    @Test
    public void whenInvalidNumericShouldReturnError() {
        String paramTitle = "Индекс полосатости";
        long mdmParamId = 98053L;
        mdmParamProvider.markParamAllowed(MdmParamIoType.CATEGORY_EXCEL_IMPORT, mdmParamId);

        MdmParam param = new MdmParam()
            .setId(mdmParamId)
            .setTitle(paramTitle)
            .setXslName(paramTitle)
            .setValueType(MdmParamValueType.NUMERIC);
        mdmParamCacheMock.add(param);

        var file = new ExcelFile.Builder();
        file.addHeader(MdmParamExcelAttributes.HID_HEADER);
        file.addHeader(MdmParamExcelAttributes.NAME_HEADER);
        file.addHeader(paramTitle);
        file.addLine("959687", "Одежда и обувь/Полосатые носки", "99,,0");
        file.addLine("959688", "Одежда и обувь/Волосатые носки", "4 0");
        file.addLine("959689", "Одежда и обувь/Инфузории туфельки", "16O");
        file.addLine("959690", "Водоросли/Фитопланктон/Диатомовые водоросли", "g4.1");

        List<String> errors = service.importExcel("", bytes(file)).getErrors();
        assertThat(errors).containsExactlyInAnyOrder(
            "Ключ 959687, параметр \"Индекс полосатости\": значение \"99,,0\" не является корректным числом",
            "Ключ 959688, параметр \"Индекс полосатости\": значение \"4 0\" не является корректным числом",
            "Ключ 959689, параметр \"Индекс полосатости\": значение \"16O\" не является корректным числом",
            "Ключ 959690, параметр \"Индекс полосатости\": значение \"g4.1\" не является корректным числом");
    }

    @Test
    public void whenValidNumericShouldReturnSuccessAndSaveParams() {
        String paramTitle = "Микроорганизмы и вирусы";
        long mdmParamId = 98053L;
        mdmParamProvider.markParamAllowed(MdmParamIoType.CATEGORY_EXCEL_IMPORT, mdmParamId);

        MdmParam param = new MdmParam()
            .setId(mdmParamId)
            .setTitle(paramTitle)
            .setXslName(paramTitle)
            .setValueType(MdmParamValueType.NUMERIC);
        mdmParamCacheMock.add(param);

        var file = new ExcelFile.Builder();
        file.addHeader(MdmParamExcelAttributes.HID_HEADER);
        file.addHeader(MdmParamExcelAttributes.NAME_HEADER);
        file.addHeader(paramTitle);
        file.addLine("959687", "Одежда и обувь/Полосатые носки", "99,203"); // допускаем запятую
        file.addLine("959688", "Одежда и обувь/Волосатые носки", "4.0");
        file.addLine("959689", "Одежда и обувь/Инфузории туфельки", "-160");
        file.addLine("959690", "Водоросли/Фитопланктон/Диатомовые водоросли", "4e-1");

        List<String> errors = service.importExcel("", bytes(file)).getErrors();
        assertThat(errors).isEmpty();

        CategoryParamValue stripedSocksValue = value(959687, param, new BigDecimal("99.203"));
        CategoryParamValue hairedSocksValue = value(959688, param, new BigDecimal("4.0"));
        CategoryParamValue parameciumCaudatumValue = value(959689, param, new BigDecimal("-160"));
        CategoryParamValue diatomeaValue = value(959690, param, new BigDecimal("4e-1"));

        assertThat(categoryParamValueRepository.findAll()).containsExactlyInAnyOrder(
            stripedSocksValue,
            hairedSocksValue,
            parameciumCaudatumValue,
            diatomeaValue
        );
    }

    @Test
    public void whenValidUnchangedNumericShouldReturnSuccessAndDoNothing() {
        String paramTitle = "Микроорганизмы и вирусы";
        long mdmParamId = 98053L;
        mdmParamProvider.markParamAllowed(MdmParamIoType.CATEGORY_EXCEL_IMPORT, mdmParamId);

        MdmParam param = new MdmParam()
            .setId(mdmParamId)
            .setTitle(paramTitle)
            .setXslName(paramTitle)
            .setValueType(MdmParamValueType.NUMERIC);
        mdmParamCacheMock.add(param);

        var file = new ExcelFile.Builder();
        file.addHeader(MdmParamExcelAttributes.HID_HEADER);
        file.addHeader(MdmParamExcelAttributes.NAME_HEADER);
        file.addHeader(paramTitle);
        file.addLine("959687", "Одежда и обувь/Полосатые носки", "99.203");
        file.addLine("959688", "Одежда и обувь/Волосатые носки", "4");
        file.addLine("959689", "Одежда и обувь/Инфузории туфельки", "-160");
        file.addLine("959690", "Водоросли/Фитопланктон/Диатомовые водоросли", "4e-1");

        CategoryParamValue stripedSocksValue = value(959687, param, new BigDecimal("99.203"));
        CategoryParamValue hairedSocksValue = value(959688, param, new BigDecimal("4"));
        CategoryParamValue parameciumCaudatumValue = value(959689, param, new BigDecimal("-160"));
        CategoryParamValue diatomeaValue = value(959690, param, new BigDecimal("4e-1"));
        categoryParamValueRepository.insertBatch(stripedSocksValue,
            hairedSocksValue,
            parameciumCaudatumValue,
            diatomeaValue
        );
        Instant importTime = Instant.now();

        List<String> errors = service.importExcel("", bytes(file)).getErrors();
        assertThat(errors).isEmpty();

        assertThat(categoryParamValueRepository.findAll()).containsExactlyInAnyOrder(
            stripedSocksValue,
            hairedSocksValue,
            parameciumCaudatumValue,
            diatomeaValue
        );
        categoryParamValueRepository.findAll().forEach(value ->
            assertThat(value.getUpdatedTs()).isBeforeOrEqualTo(importTime));
    }

    @Test
    public void whenValidChangedNumericShouldReturnSuccessAndUpdateValues() {
        String paramTitle = "Микроорганизмы и вирусы";
        long mdmParamId = 98053L;
        mdmParamProvider.markParamAllowed(MdmParamIoType.CATEGORY_EXCEL_IMPORT, mdmParamId);

        MdmParam param = new MdmParam()
            .setId(mdmParamId)
            .setTitle(paramTitle)
            .setXslName(paramTitle)
            .setValueType(MdmParamValueType.NUMERIC);
        mdmParamCacheMock.add(param);

        var file = new ExcelFile.Builder();
        file.addHeader(MdmParamExcelAttributes.HID_HEADER);
        file.addHeader(MdmParamExcelAttributes.NAME_HEADER);
        file.addHeader(paramTitle);
        file.addLine("959687", "Одежда и обувь/Полосатые носки", "99.203");
        file.addLine("959688", "Одежда и обувь/Волосатые носки", "4");
        file.addLine("959689", "Одежда и обувь/Инфузории туфельки", "-160");
        file.addLine("959690", "Водоросли/Фитопланктон/Диатомовые водоросли", "4e-1");

        CategoryParamValue stripedSocksValue = value(959687, param, new BigDecimal("0"));
        CategoryParamValue hairedSocksValue = value(959688, param, new BigDecimal("0"));
        CategoryParamValue parameciumCaudatumValue = value(959689, param, new BigDecimal("0"));
        CategoryParamValue diatomeaValue = value(959690, param, new BigDecimal("0"));
        categoryParamValueRepository.insertBatch(stripedSocksValue,
            hairedSocksValue,
            parameciumCaudatumValue,
            diatomeaValue
        );
        Instant importTime = Instant.now();

        // Изменим значения на ожидаемые из эксельки
        stripedSocksValue.setNumeric(new BigDecimal("99.203"));
        hairedSocksValue.setNumeric(new BigDecimal("4"));
        parameciumCaudatumValue.setNumeric(new BigDecimal("-160"));
        diatomeaValue.setNumeric(new BigDecimal("4e-1"));

        List<String> errors = service.importExcel("", bytes(file)).getErrors();
        assertThat(errors).isEmpty();

        assertThat(categoryParamValueRepository.findAll()).containsExactlyInAnyOrder(
            stripedSocksValue,
            hairedSocksValue,
            parameciumCaudatumValue,
            diatomeaValue
        );
        categoryParamValueRepository.findAll().forEach(value ->
            assertThat(value.getUpdatedTs()).isAfter(importTime));
    }

    @Test
    public void whenInvalidOptionShouldReturnError() {
        String paramTitle = "Домен (надцарство)";
        long mdmParamId = 98053L;
        mdmParamProvider.markParamAllowed(MdmParamIoType.CATEGORY_EXCEL_IMPORT, mdmParamId);

        MdmParam param = new MdmParam()
            .setId(mdmParamId)
            .setTitle(paramTitle)
            .setXslName(paramTitle)
            .setValueType(MdmParamValueType.MBO_ENUM);
        param.getExternals()
            .setOptionRenders(Map.of(
                1L, "Археи",
                2L, "Бактерии",
                3L, "Эукариоты"
            ));
        mdmParamCacheMock.add(param);

        var file = new ExcelFile.Builder();
        file.addHeader(MdmParamExcelAttributes.HID_HEADER);
        file.addHeader(MdmParamExcelAttributes.NAME_HEADER);
        file.addHeader(paramTitle);
        file.addLine("959687", "Одежда и обувь/Полосатые носки", "Прокариоты");
        file.addLine("959688", "Одежда и обувь/Волосатые носки", "Эвкариоты");
        file.addLine("959689", "Одежда и обувь/Инфузории туфельки", "Доклеточные");
        file.addLine("959690", "Водоросли/Фитопланктон/Диатомовые водоросли", "Клеточные");

        List<String> errors = service.importExcel("", bytes(file)).getErrors();
        assertThat(errors).containsExactlyInAnyOrder(
            "Ключ 959687, параметр \"Домен (надцарство)\": значение или одно из значений \"Прокариоты\" " +
                "не является корректной опцией, доступные значения: Археи, Бактерии, Эукариоты",
            "Ключ 959688, параметр \"Домен (надцарство)\": значение или одно из значений \"Эвкариоты\" " +
                "не является корректной опцией, доступные значения: Археи, Бактерии, Эукариоты",
            "Ключ 959689, параметр \"Домен (надцарство)\": значение или одно из значений \"Доклеточные\" " +
                "не является корректной опцией, доступные значения: Археи, Бактерии, Эукариоты",
            "Ключ 959690, параметр \"Домен (надцарство)\": значение или одно из значений \"Клеточные\" " +
                "не является корректной опцией, доступные значения: Археи, Бактерии, Эукариоты"
        );
    }

    @Test
    public void whenValidOptionShouldReturnSuccessAndSaveParams() {
        String paramTitle = "Микроорганизмы и вирусы";
        long mdmParamId = 98053L;
        mdmParamProvider.markParamAllowed(MdmParamIoType.CATEGORY_EXCEL_IMPORT, mdmParamId);

        MdmParam param = new MdmParam()
            .setId(mdmParamId)
            .setTitle(paramTitle)
            .setXslName(paramTitle)
            .setValueType(MdmParamValueType.MBO_ENUM);
        param.getExternals()
            .setOptionRenders(Map.of(
                1L, "Археи",
                2L, "Бактерии",
                3L, "Эукариоты",
                4L, "Неживое" // вообще говоря, такого домена нет
            ));
        mdmParamCacheMock.add(param);

        var file = new ExcelFile.Builder();
        file.addHeader(MdmParamExcelAttributes.HID_HEADER);
        file.addHeader(MdmParamExcelAttributes.NAME_HEADER);
        file.addHeader(paramTitle);
        file.addLine("959687", "Одежда и обувь/Полосатые носки", "Неживое");
        file.addLine("959688", "Одежда и обувь/Волосатые носки", "Неживое");
        file.addLine("959689", "Одежда и обувь/Инфузории туфельки", "Эукариоты");
        file.addLine("959690", "Водоросли/Фитопланктон/Диатомовые водоросли", "Эукариоты");

        List<String> errors = service.importExcel("", bytes(file)).getErrors();
        assertThat(errors).isEmpty();

        CategoryParamValue stripedSocksValue = value(959687, param, "Неживое");
        CategoryParamValue hairedSocksValue = value(959688, param, "Неживое");
        CategoryParamValue parameciumCaudatumValue = value(959689, param, "Эукариоты");
        CategoryParamValue diatomeaValue = value(959690, param, "Эукариоты");

        assertThat(categoryParamValueRepository.findAll()).containsExactlyInAnyOrder(
            stripedSocksValue,
            hairedSocksValue,
            parameciumCaudatumValue,
            diatomeaValue
        );
    }

    @Test
    public void whenValidUnchangedOptionShouldReturnSuccessAndDoNothing() {
        String paramTitle = "Микроорганизмы и вирусы";
        long mdmParamId = 98053L;
        mdmParamProvider.markParamAllowed(MdmParamIoType.CATEGORY_EXCEL_IMPORT, mdmParamId);

        MdmParam param = new MdmParam()
            .setId(mdmParamId)
            .setTitle(paramTitle)
            .setXslName(paramTitle)
            .setValueType(MdmParamValueType.MBO_ENUM);
        param.getExternals()
            .setOptionRenders(Map.of(
                1L, "Археи",
                2L, "Бактерии",
                3L, "Эукариоты",
                4L, "Неживое" // вообще говоря, такого домена нет
            ));
        mdmParamCacheMock.add(param);

        var file = new ExcelFile.Builder();
        file.addHeader(MdmParamExcelAttributes.HID_HEADER);
        file.addHeader(MdmParamExcelAttributes.NAME_HEADER);
        file.addHeader(paramTitle);
        file.addLine("959687", "Одежда и обувь/Полосатые носки", "Неживое");
        file.addLine("959688", "Одежда и обувь/Волосатые носки", "Неживое");
        file.addLine("959689", "Одежда и обувь/Инфузории туфельки", "Эукариоты");
        file.addLine("959690", "Водоросли/Фитопланктон/Диатомовые водоросли", "Эукариоты");
        CategoryParamValue stripedSocksValue = value(959687, param, "Неживое");
        CategoryParamValue hairedSocksValue = value(959688, param, "Неживое");
        CategoryParamValue parameciumCaudatumValue = value(959689, param, "Эукариоты");
        CategoryParamValue diatomeaValue = value(959690, param, "Эукариоты");
        categoryParamValueRepository.insertBatch(stripedSocksValue,
            hairedSocksValue,
            parameciumCaudatumValue,
            diatomeaValue
        );
        Instant importTime = Instant.now();

        List<String> errors = service.importExcel("", bytes(file)).getErrors();
        assertThat(errors).isEmpty();

        assertThat(categoryParamValueRepository.findAll()).containsExactlyInAnyOrder(
            stripedSocksValue,
            hairedSocksValue,
            parameciumCaudatumValue,
            diatomeaValue
        );
        categoryParamValueRepository.findAll().forEach(value ->
            assertThat(value.getUpdatedTs()).isBeforeOrEqualTo(importTime));
    }

    @Test
    public void whenValidChangedOptionShouldReturnSuccessAndUpdateValues() {
        String paramTitle = "Микроорганизмы и вирусы";
        long mdmParamId = 98053L;
        mdmParamProvider.markParamAllowed(MdmParamIoType.CATEGORY_EXCEL_IMPORT, mdmParamId);

        MdmParam param = new MdmParam()
            .setId(mdmParamId)
            .setTitle(paramTitle)
            .setXslName(paramTitle)
            .setValueType(MdmParamValueType.MBO_ENUM);
        param.getExternals()
            .setOptionRenders(Map.of(
                1L, "Археи",
                2L, "Бактерии",
                3L, "Эукариоты",
                4L, "Неживое" // вообще говоря, такого домена нет
            ));
        mdmParamCacheMock.add(param);

        var file = new ExcelFile.Builder();
        file.addHeader(MdmParamExcelAttributes.HID_HEADER);
        file.addHeader(MdmParamExcelAttributes.NAME_HEADER);
        file.addHeader(paramTitle);
        file.addLine("959687", "Одежда и обувь/Полосатые носки", "Неживое");
        file.addLine("959688", "Одежда и обувь/Волосатые носки", "Неживое");
        file.addLine("959689", "Одежда и обувь/Инфузории туфельки", "Эукариоты");
        file.addLine("959690", "Водоросли/Фитопланктон/Диатомовые водоросли", "Эукариоты");
        CategoryParamValue stripedSocksValue = value(959687, param, "Бактерии");
        CategoryParamValue hairedSocksValue = value(959688, param, "Бактерии");
        CategoryParamValue parameciumCaudatumValue = value(959689, param, "Бактерии");
        CategoryParamValue diatomeaValue = value(959690, param, "Бактерии");
        categoryParamValueRepository.insertBatch(stripedSocksValue,
            hairedSocksValue,
            parameciumCaudatumValue,
            diatomeaValue
        );
        Instant importTime = Instant.now();

        // Изменим значения на ожидаемые из эксельки
        stripedSocksValue.setOption(param.getExternals().getOptionByRender("Неживое"));
        hairedSocksValue.setOption(param.getExternals().getOptionByRender("Неживое"));
        parameciumCaudatumValue.setOption(param.getExternals().getOptionByRender("Эукариоты"));
        diatomeaValue.setOption(param.getExternals().getOptionByRender("Эукариоты"));

        List<String> errors = service.importExcel("", bytes(file)).getErrors();
        assertThat(errors).isEmpty();

        assertThat(categoryParamValueRepository.findAll()).containsExactlyInAnyOrder(
            stripedSocksValue,
            hairedSocksValue,
            parameciumCaudatumValue,
            diatomeaValue
        );
        categoryParamValueRepository.findAll().forEach(value ->
            assertThat(value.getUpdatedTs()).isAfter(importTime));
    }

    @Test
    public void whenSomeParamsExcludedShouldIgnoreThemAndSaveOthers() {
        String paramTitle = "Микроорганизмы и вирусы";
        String excludedTitle = "Размер клетки, микрометры";
        long mdmParamId = 98053L;
        long excludedParamId = 807562L;
        mdmParamProvider.markParamAllowed(MdmParamIoType.CATEGORY_EXCEL_IMPORT, mdmParamId);

        MdmParam param = new MdmParam()
            .setId(mdmParamId)
            .setTitle(paramTitle)
            .setXslName(paramTitle)
            .setValueType(MdmParamValueType.NUMERIC);
        mdmParamCacheMock.add(param);
        MdmParam excludedParam = new MdmParam()
            .setId(excludedParamId)
            .setTitle(excludedTitle)
            .setXslName(excludedTitle)
            .setValueType(MdmParamValueType.NUMERIC);
        mdmParamCacheMock.add(excludedParam);

        var file = new ExcelFile.Builder();
        file.addHeader(MdmParamExcelAttributes.HID_HEADER);
        file.addHeader(MdmParamExcelAttributes.NAME_HEADER);
        file.addHeader(paramTitle);
        file.addHeader(excludedTitle);
        file.addLine("959687", "Одежда и обувь/Полосатые носки", "99,203", "0"); // допускаем запятую
        file.addLine("959688", "Одежда и обувь/Волосатые носки", "4.0", "0");
        file.addLine("959689", "Одежда и обувь/Инфузории туфельки", "-160", "155");
        file.addLine("959690", "Водоросли/Фитопланктон/Диатомовые водоросли", "4e-1", "200");

        List<String> errors = service.importExcel("", bytes(file)).getErrors();
        assertThat(errors).isEmpty();

        CategoryParamValue stripedSocksValue = value(959687, param, new BigDecimal("99.203"));
        CategoryParamValue hairedSocksValue = value(959688, param, new BigDecimal("4.0"));
        CategoryParamValue parameciumCaudatumValue = value(959689, param, new BigDecimal("-160"));
        CategoryParamValue diatomeaValue = value(959690, param, new BigDecimal("4e-1"));

        assertThat(categoryParamValueRepository.findAll()).containsExactlyInAnyOrder(
            stripedSocksValue,
            hairedSocksValue,
            parameciumCaudatumValue,
            diatomeaValue
        );
    }

    @Test
    public void whenNumberOfMskusAndSskusInCategoriesExceedsLimitShouldReturnError() {
        keyValueService.putValue(MdmProperties.ACCEPTABLE_AMOUNT_OF_ENTITIES_FOR_GOLDEN_RECALC, 2);

        // подготовим маппинги
        int categoryId = 9041;
        ShopSkuKey shopSkuKey1 = new ShopSkuKey(1, "Коварный вирус");
        ShopSkuKey shopSkuKey2 = new ShopSkuKey(2, "Неизвестный вирус");
        ShopSkuKey shopSkuKey3 = new ShopSkuKey(3, "Безобидный вирус");
        MappingCacheDao mapping1 =
            new MappingCacheDao().setCategoryId(categoryId).setMskuId(1L).setShopSkuKey(shopSkuKey1).setUpdateStamp(1L);
        MappingCacheDao mapping2 =
            new MappingCacheDao().setCategoryId(categoryId).setMskuId(2L).setShopSkuKey(shopSkuKey2).setUpdateStamp(1L);
        MappingCacheDao mapping3 =
            new MappingCacheDao().setCategoryId(categoryId).setMskuId(3L).setShopSkuKey(shopSkuKey3).setUpdateStamp(1L);

        mappingsCacheRepository.insertBatch(mapping1, mapping2, mapping3);

        // подготовим файл для импорта
        String paramTitle = "Микроорганизмы и вирусы";
        long mdmParamId = 98053L;
        mdmParamProvider.markParamAllowed(MdmParamIoType.CATEGORY_EXCEL_IMPORT, mdmParamId);

        MdmParam param = new MdmParam()
            .setId(mdmParamId)
            .setTitle(paramTitle)
            .setXslName(paramTitle)
            .setValueType(MdmParamValueType.BOOL);
        mdmParamCacheMock.add(param);

        var file = new ExcelFile.Builder();
        file.addHeader(MdmParamExcelAttributes.HID_HEADER);
        file.addHeader(MdmParamExcelAttributes.NAME_HEADER);
        file.addHeader(paramTitle);
        file.addLine(categoryId, "Водоросли/Фитопланктон/Диатомовые водоросли", "да");

        // заведем категорийную настройку
        CategoryParamValue paramValue = value(categoryId, param, false);
        categoryParamValueRepository.insertBatch(paramValue);

        List<String> errors = service.importExcel("", bytes(file)).getErrors();
        assertThat(errors).containsExactlyInAnyOrder(
            "Количество msku/ssku на пересчет в загружаемых категориях превышает допустимый порог");

        // проверим, что категорийные настройки не изменились после загрузки файла
        assertThat(categoryParamValueRepository.findAll()).containsExactlyInAnyOrder(paramValue);
    }

    @Test
    public void whenNumberOfMskusAndSskusInCategoriesExceedsLimitWithNoRecalcShouldNotReturnError() {
        keyValueService.putValue(MdmProperties.ACCEPTABLE_AMOUNT_OF_ENTITIES_FOR_GOLDEN_RECALC, 2);

        // подготовим маппинги
        int categoryId = 9041;
        ShopSkuKey shopSkuKey1 = new ShopSkuKey(1, "Коварный вирус");
        ShopSkuKey shopSkuKey2 = new ShopSkuKey(2, "Неизвестный вирус");
        ShopSkuKey shopSkuKey3 = new ShopSkuKey(3, "Безобидный вирус");
        MappingCacheDao mapping1 =
            new MappingCacheDao().setCategoryId(categoryId).setMskuId(1L).setShopSkuKey(shopSkuKey1).setUpdateStamp(1L);
        MappingCacheDao mapping2 =
            new MappingCacheDao().setCategoryId(categoryId).setMskuId(2L).setShopSkuKey(shopSkuKey2).setUpdateStamp(1L);
        MappingCacheDao mapping3 =
            new MappingCacheDao().setCategoryId(categoryId).setMskuId(3L).setShopSkuKey(shopSkuKey3).setUpdateStamp(1L);

        mappingsCacheRepository.insertBatch(mapping1, mapping2, mapping3);

        // подговотовим файл для импорта
        String paramTitle = "Микроорганизмы и вирусы";
        long mdmParamId = 98053L;
        mdmParamProvider.markParamAllowed(MdmParamIoType.CATEGORY_EXCEL_IMPORT, mdmParamId);

        MdmParam param = new MdmParam()
            .setId(mdmParamId)
            .setTitle(paramTitle)
            .setXslName(paramTitle)
            .setValueType(MdmParamValueType.BOOL);
        mdmParamCacheMock.add(param);

        var file = new ExcelFile.Builder();
        file.addHeader(MdmParamExcelAttributes.HID_HEADER);
        file.addHeader(MdmParamExcelAttributes.NAME_HEADER);
        file.addHeader(paramTitle);
        file.addLine(categoryId, "Водоросли/Фитопланктон/Диатомовые водоросли", "да");

        // заведем категорийную настройку
        CategoryParamValue paramValue = value(categoryId, param, false);
        categoryParamValueRepository.insertBatch(paramValue);

        var importResult = service.importExcel("", bytes(file), true);
        List<String> errors = importResult.getErrors();
        var fileStatus = importResult.getStatus();
        assertThat(errors).isEmpty();
        assertThat(fileStatus).isEqualTo(FileStatus.OK);

        // проверим, что категорийные настройки обновились после загрузки файла
        assertThat(categoryParamValueRepository.findAll()).doesNotContain(paramValue);
    }

    private byte[] bytes(ExcelFile.Builder file) {
        return ExcelFileConverter.convertToBytes(file.build());
    }

    private CategoryParamValue value(long categoryId, MdmParam param, boolean bool) {
        return (CategoryParamValue) new CategoryParamValue().setCategoryId(categoryId)
            .setXslName(param.getXslName())
            .setBool(bool)
            .setMdmParamId(param.getId())
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);
    }

    private CategoryParamValue value(long categoryId, MdmParam param, BigDecimal numeric) {
        return (CategoryParamValue) new CategoryParamValue().setCategoryId(categoryId)
            .setXslName(param.getXslName())
            .setNumeric(numeric)
            .setMdmParamId(param.getId())
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);
    }

    private CategoryParamValue value(long categoryId, MdmParam param, String option) {
        return (CategoryParamValue) new CategoryParamValue().setCategoryId(categoryId)
            .setXslName(param.getXslName())
            .setOption(param.getExternals().getOptionByRender(option))
            .setMdmParamId(param.getId())
            .setMasterDataSourceType(MasterDataSourceType.MDM_OPERATOR);
    }
}

package ru.yandex.market.mbo.mdm.common.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import ru.yandex.market.mbo.mdm.common.masterdata.model.param.CategoryParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.model.param.MdmParamValue;
import ru.yandex.market.mbo.mdm.common.masterdata.repository.param.CategoryParamValueRepository;
import ru.yandex.market.mbo.mdm.common.masterdata.services.metadata.MetadataProvider;
import ru.yandex.market.mbo.mdm.common.utils.MdmBaseDbTestClass;
import ru.yandex.market.mboc.common.services.category.CategoryCachingService;
import ru.yandex.market.mdm.http.MdmBase;
import ru.yandex.market.mdm.http.MdmEntity;

import static ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownBmdmIds.CATEGORY_SETTINGS_ID;

@Import(
    MdmEntityConverterForCategorySettings.class
)
public class MdmEntityConverterForCategorySettingsTest extends MdmBaseDbTestClass {

    @Autowired
    private MdmEntityConverterForCategorySettings converter;
    @Autowired
    private CategoryParamValueRepository categoryParamValueRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private MetadataProvider metadataProvider;
    @MockBean
    private CategoryCachingService categoryCachingService;


    //данные MdmParam'ы используется на проде.
    private static final List<Long> supportedMdmParams = List.of(19L, 50L, 410L, 411L, 412L, 413L, 414L, 415L, 416L,
        417L, 600L, 688L, 700L, 701L, 702L, 703L, 704L, 705L, 706L, 707L, 708L, 709L, 710L, 711L, 712L);

    private static final List<Integer> testingCategories = List.of(4748062, 90404, 90440, 15927546);

    @Before
    public void setUp() throws Exception {
        StringBuilder query = new StringBuilder();

        var resourceStream = getClass().getResourceAsStream("/util/CategoryParamValuesForTests");

        if (resourceStream == null) {
            throw new RuntimeException("Resource CategoryParamValuesForTests not found");
        }

        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(resourceStream))) {
            while (bufferedReader.readLine() != null) {
                query.append(bufferedReader.readLine());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        jdbcTemplate.execute(query.toString());
    }

    @Test
    public void convert() {
        testingCategories.forEach(this::testForCategory);
    }

    private void testForCategory(int categoryId) {
        List<CategoryParamValue> paramValues = categoryParamValueRepository.findCategoryParamValues(categoryId)
            .stream().filter(it -> supportedMdmParams.contains(it.getMdmParamId())).collect(Collectors.toList());
        Map<Long, CategoryParamValue> id2paramValues = paramValues.stream().
            collect(Collectors.toMap(MdmParamValue::getMdmParamId, Function.identity()));
        MdmBase.MdmEntityType entityType = getMdmEntityTypeForCategorySettings();
        MdmEntity entity = converter.convert(categoryId, paramValues, entityType);
        List<CategoryParamValue> newCategoryParamValues = converter.convert(entity, entityType);

        Assertions.assertThat(entity.getMdmId()).isEqualTo(categoryId);
        Assertions.assertThat(newCategoryParamValues).hasSize(paramValues.size());
        Assertions.assertThat(newCategoryParamValues).allMatch(newParamValue -> {
            if (!id2paramValues.containsKey(newParamValue.getMdmParamId())) {
                return false;
            }
            var paramValue = id2paramValues.get(newParamValue.getMdmParamId());
            return paramValue.getCategoryId() == newParamValue.getCategoryId() &&
                Objects.equals(paramValue.getXslName(), newParamValue.getXslName()) &&
                Objects.equals(paramValue.getStrings(), newParamValue.getStrings()) &&
                Objects.equals(paramValue.getNumerics(), newParamValue.getNumerics()) &&
                Objects.equals(paramValue.getBools(), newParamValue.getBools()) &&
                Objects.equals(paramValue.getOptions(), newParamValue.getOptions());
        });
    }

    private MdmBase.MdmEntityType getMdmEntityTypeForCategorySettings() {
        return metadataProvider.findEntityType(CATEGORY_SETTINGS_ID).orElseThrow();
    }
}

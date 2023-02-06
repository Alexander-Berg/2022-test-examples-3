package ru.yandex.mbo.tool.jira.MBO30875;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import ru.yandex.market.mbo.db.params.IParameterLoaderService;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Parameter;


public class SetTypeProductParamToolTest {
    private static final long TEST_CATEGORY1 = 90567L;
    private static final long TEST_CATEGORY2 = 90568L;
    private static final String TEST_VALUE_NAME1 = "Тест стола";
    private static final String TEST_VALUE_NAME2 = "Тест тумбочки";
    private static final String TEST_CONDITION1 = "MaxWeight:180, LightSimbols:\"true\"";
    private static final String TEST_CONDITION2 = "MaxWeight:150";
    private static final String TEST_CONDITION3 = "LightSimbols:\"false\"";

    private IParameterLoaderService parameterLoaderService;
    private SetTypeProductParamTool tool;

    @Before
    public void setUp() {
        parameterLoaderService = createMockParamLoaderService();
        tool = new SetTypeProductParamTool();
        tool.setParameterLoaderService(parameterLoaderService);
    }

    @Test
    public void testReadExcelOk() throws IOException {
        SetTypeProductParamTool.ParamChangeInfo expected = new SetTypeProductParamTool.ParamChangeInfo(
            TEST_CATEGORY1,
            List.of(TEST_VALUE_NAME1),
            TEST_VALUE_NAME1,
            Map.of(TEST_VALUE_NAME1, Map.of(1, "180", 2, "true"))
        );

        try (InputStream xlsxStream = new ByteArrayInputStream(
            makeXlsx(List.of(TEST_CATEGORY1), List.of(TEST_VALUE_NAME1), List.of(TEST_CONDITION1)))) {

            Collection<SetTypeProductParamTool.ParamChangeInfo> res = tool.buildParamChangeInfoByExcel(xlsxStream);

            Assertions.assertThat(res)
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(List.of(expected));
        }
    }

    @Test
    public void testReadExcelWithDifferentCategories() throws IOException {
        SetTypeProductParamTool.ParamChangeInfo expected1 = new SetTypeProductParamTool.ParamChangeInfo(
            TEST_CATEGORY1,
            List.of(TEST_VALUE_NAME1),
            TEST_VALUE_NAME1,
            Map.of(TEST_VALUE_NAME1, Map.of(1, "150"))
        );
        SetTypeProductParamTool.ParamChangeInfo expected2 = new SetTypeProductParamTool.ParamChangeInfo(
            TEST_CATEGORY2,
            List.of(TEST_VALUE_NAME2),
            TEST_VALUE_NAME2,
            Map.of(TEST_VALUE_NAME2, Map.of(2, "false"))
        );

        try (InputStream xlsxStream = new ByteArrayInputStream(makeXlsx(
            List.of(TEST_CATEGORY1, TEST_CATEGORY2),
            List.of(TEST_VALUE_NAME1, TEST_VALUE_NAME2),
            List.of(TEST_CONDITION2, TEST_CONDITION3)))) {

            Collection<SetTypeProductParamTool.ParamChangeInfo> res = tool.buildParamChangeInfoByExcel(xlsxStream);

            Assertions.assertThat(res)
                .usingFieldByFieldElementComparator()
                .containsExactlyInAnyOrderElementsOf(List.of(expected1, expected2));
        }
    }

    @Test
    public void testWhenWrongParamThenFail() throws IOException {
        try (InputStream xlsxStream = new ByteArrayInputStream(
            makeXlsx(List.of(TEST_CATEGORY1), List.of(TEST_VALUE_NAME1), List.of("Unknown:\"some value\"")))) {

            Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> tool.buildParamChangeInfoByExcel(xlsxStream))
                .withMessage("Unknown parameter 'Unknown' in category " + TEST_CATEGORY1);
        }
    }

    @Test
    public void testMergeParamInfo() throws IOException {
        Map<Long, SetTypeProductParamTool.ParamChangeInfo> source = Map.of(
            TEST_CATEGORY1, new SetTypeProductParamTool.ParamChangeInfo(TEST_CATEGORY1, TEST_VALUE_NAME1),
            TEST_CATEGORY2, new SetTypeProductParamTool.ParamChangeInfo(TEST_CATEGORY2, TEST_VALUE_NAME2)
        );

        SetTypeProductParamTool.ParamChangeInfo expected1 = new SetTypeProductParamTool.ParamChangeInfo(
            TEST_CATEGORY1,
            List.of(TEST_VALUE_NAME1, TEST_VALUE_NAME2),
            TEST_VALUE_NAME1,
            Map.of(
                TEST_VALUE_NAME1, Map.of(1, "150"),
                TEST_VALUE_NAME2, Map.of(2, "false")
            )
        );

        SetTypeProductParamTool.ParamChangeInfo expected2 = new SetTypeProductParamTool.ParamChangeInfo(
            TEST_CATEGORY2,
            List.of(TEST_VALUE_NAME2),
            TEST_VALUE_NAME2,
            Map.of(TEST_VALUE_NAME2, Map.of(1, "180", 2, "true"))
        );

        try (InputStream xlsxStream = new ByteArrayInputStream(makeXlsx(
            List.of(TEST_CATEGORY1, TEST_CATEGORY1, TEST_CATEGORY2),
            List.of(TEST_VALUE_NAME1, TEST_VALUE_NAME2, TEST_VALUE_NAME2),
            List.of(TEST_CONDITION2, TEST_CONDITION3, TEST_CONDITION1)))) {

            Collection<SetTypeProductParamTool.ParamChangeInfo> read = tool.buildParamChangeInfoByExcel(xlsxStream);

            Map<Long, SetTypeProductParamTool.ParamChangeInfo> res = tool.mergeParamInfo(read, source);

            Assertions.assertThat(res).containsKeys(TEST_CATEGORY1, TEST_CATEGORY2);
            Assertions.assertThat(res.get(TEST_CATEGORY1)).isEqualToComparingFieldByField(expected1);
            Assertions.assertThat(res.get(TEST_CATEGORY2)).isEqualToComparingFieldByField(expected2);
        }
    }

    private IParameterLoaderService createMockParamLoaderService() {
        CategoryParam testParam1 =  new Parameter();
        testParam1.setId(1L);
        testParam1.setXslName("MaxWeight");
        CategoryParam testParam2 =  new Parameter();
        testParam2.setId(2L);
        testParam2.setXslName("LightSimbols");
        List<CategoryParam> params = ImmutableList.of(testParam1, testParam2);

        Answer<CategoryParam> getParam = (invocation) -> {
            String name = invocation.getArgument(0);
            return params.stream()
                .filter(p -> name.equals(p.getXslName()))
                .findAny()
                .orElse(null);
        };

        CategoryEntities categoryEntities1 = Mockito.mock(CategoryEntities.class);
        Mockito.when(categoryEntities1.getHid()).thenReturn(TEST_CATEGORY1);
        Mockito.when(categoryEntities1.getParameterByName(Mockito.anyString())).thenAnswer(getParam);

        CategoryEntities categoryEntities2 = Mockito.mock(CategoryEntities.class);
        Mockito.when(categoryEntities2.getHid()).thenReturn(TEST_CATEGORY2);
        Mockito.when(categoryEntities2.getParameterByName(Mockito.anyString())).thenAnswer(getParam);

        IParameterLoaderService res = Mockito.mock(IParameterLoaderService.class);
        Mockito.when(res.loadCategoryEntitiesByHid(TEST_CATEGORY1)).thenReturn(categoryEntities1);
        Mockito.when(res.loadCategoryEntitiesByHid(TEST_CATEGORY2)).thenReturn(categoryEntities2);
        return res;
    }

    private byte[] makeXlsx(List<Long> categotyIds, List<String> values, List<String> conditions) throws IOException {
        var wb = new XSSFWorkbook();
        var sh = wb.createSheet();
        for (int i = 0; i < categotyIds.size(); i++) {
            var row = sh.createRow(i);
            var cell = row.createCell(0, CellType.NUMERIC);
            cell.setCellValue(categotyIds.get(i));
            cell = row.createCell(1);
            cell.setCellValue(values.get(i));
            cell = row.createCell(2);
            cell.setCellValue(conditions.get(i));
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            wb.write(out);
            return out.toByteArray();
        }
    }
}

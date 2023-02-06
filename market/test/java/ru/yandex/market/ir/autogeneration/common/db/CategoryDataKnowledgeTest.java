package ru.yandex.market.ir.autogeneration.common.db;


import com.google.protobuf.BytesValue;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMock;
import ru.yandex.market.mbo.export.CategoryParametersService;
import ru.yandex.market.mbo.export.CategorySizeMeasureService;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.robot.db.ParameterValueComposer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class CategoryDataKnowledgeTest {

    public static final long CATEGORY_ID = 123L;

    @Mock
    private CategorySizeMeasureService categorySizeMeasureServiceMock;

    @Mock
    private CategoryParametersService categoryParametersServiceMock;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void ifNoSizeSubTypeParameterShouldNotCallSizeMeasureService() {
        CategoryDataKnowledge categoryDataKnowledge = new CategoryDataKnowledgeMock();
        categoryDataKnowledge.setCategorySizeMeasureService(categorySizeMeasureServiceMock);

        categoryDataKnowledge.getSizeMeasuresList(CATEGORY_ID, getCategory());
        Mockito.verify(categorySizeMeasureServiceMock, Mockito.never()).getSizeMeasures(Mockito.any());
    }

    @Test
    public void testGetCategoriesBytes() {
        CategoryDataKnowledge categoryDataKnowledge = new CategoryDataKnowledge();
        categoryDataKnowledge.setCategoryParametersService(categoryParametersServiceMock);
        categoryDataKnowledge.setCategoryDataRefreshersCount(1);
        categoryDataKnowledge.afterPropertiesSet();

        when(categoryParametersServiceMock.getParametersBytes(any()))
                .thenReturn(BytesValue.newBuilder()
                        .setValue(getCategory().toByteString())
                        .build());
        CategoryData categoryData = categoryDataKnowledge.getCategoryData(CATEGORY_ID);
        assertNotNull(categoryData);
        assertEquals(123L, categoryData.getHid().longValue());
    }

    public MboParameters.Category getCategory() {
        return MboParameters.Category.newBuilder()
                .setHid(CATEGORY_ID)
                .addName(MboParameters.Word.newBuilder().setName("Category " + CATEGORY_ID).setLangId(225))
                .addParameter(MboParameters.Parameter.newBuilder()
                        .setId(ParameterValueComposer.NAME_ID).setXslName(ParameterValueComposer.NAME)
                        .setValueType(MboParameters.ValueType.STRING))
                .addParameter(MboParameters.Parameter.newBuilder()
                        .setId(ParameterValueComposer.BARCODE_ID).setXslName(ParameterValueComposer.BARCODE)
                        .setValueType(MboParameters.ValueType.STRING)
                )
                .addParameter(MboParameters.Parameter.newBuilder()
                        .setId(ParameterValueComposer.VENDOR_ID)
                        .setXslName(CategoryData.VENDOR)
                        .setValueType(MboParameters.ValueType.ENUM)
                        .build())
                .build();
    }
}

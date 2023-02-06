package ru.yandex.market.mbo.db.size;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.core.utils.TransactionTemplateMock;
import ru.yandex.market.mbo.db.SizeMeasureService;
import ru.yandex.market.mbo.db.linkedvalues.ValueLinkServiceInterface;
import ru.yandex.market.mbo.db.params.IParameterLoaderService;
import ru.yandex.market.mbo.db.params.ParameterSaveContext;
import ru.yandex.market.mbo.db.params.ParameterService;
import ru.yandex.market.mbo.db.size.model.Size;
import ru.yandex.market.mbo.db.size.model.SizeChart;
import ru.yandex.market.mbo.db.size.model.SizeChartMeasure;
import ru.yandex.market.mbo.gwt.models.gurulight.GLMeasure;
import ru.yandex.market.mbo.gwt.models.gurulight.SizeMeasureValueOptionDto;
import ru.yandex.market.mbo.gwt.models.linkedvalues.LinkDirection;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLink;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLinkSearchCriteria;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLinkType;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.WordUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DuplicateOldSizeStructureServiceTest {

    private static final Long USER_ID = 2L;
    private static final String ACTUAL_SIZE_CHART_NAME = "NEW_ADIDAS";
    private static final String OLD_SIZE_CHART_NAME = "OLD_ADIDAS";

    private static final Long UNITS_PARAM_FIRST = 45L;
    private static final Long UNITS_PARAM_SECOND = 65L;

    private static final Long OLD_VENDOR_ID = 23L;
    private static final Long NEW_VENDOR_ID = 99L;

    @Mock
    private SizeMeasureService sizeMeasureService;
    @Mock
    private IParameterLoaderService parameterLoaderService;
    @Mock
    private ParameterService parameterService;
    @Mock
    private ValueLinkServiceInterface valueLinkService;
    @Mock
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    private final TransactionTemplate transactionTemplate = new TransactionTemplateMock();

    private DuplicateOldSizeStructureService duplicateOldSizeStructureService;

    @Before
    public void setUp() {
        duplicateOldSizeStructureService = new DuplicateOldSizeStructureService(
            sizeMeasureService, parameterLoaderService, parameterService,
            valueLinkService, namedParameterJdbcTemplate, transactionTemplate
        );
        ReflectionTestUtils.setField(duplicateOldSizeStructureService, "needDuplicate", Boolean.TRUE);
        when(parameterLoaderService.loadGlobalEntitiesWithoutValues()).thenReturn(getCategoryEntities());
        when(parameterService.createDefaultSaveContext(USER_ID)).thenReturn(new ParameterSaveContext(USER_ID));
    }

    @Test
    public void duplicateUpdateSizeChartNameTest() {
        SizeChart sizeChartActual = new SizeChart();
        sizeChartActual.setName(ACTUAL_SIZE_CHART_NAME);
        sizeChartActual.setVendorId(NEW_VENDOR_ID);

        SizeChart sizeChartBefore = new SizeChart();
        sizeChartBefore.setName(OLD_SIZE_CHART_NAME);
        sizeChartBefore.setVendorId(OLD_VENDOR_ID);

        final long measureIdFirst = 1L;
        final long unitsOptionIdFirst = 11L;
        final long measureIdSecond = 2L;
        final long unitsOptionIdSecond = 22L;

        SizeChartMeasure value1 = new SizeChartMeasure();
        value1.setMeasureId(measureIdFirst);
        SizeChartMeasure value2 = new SizeChartMeasure();
        value2.setMeasureId(measureIdSecond);

        Size size = new Size();
        size.addSizeChartMeasure(value1);
        size.addSizeChartMeasure(value2);

        sizeChartBefore.addSize(size);

        ValueLinkSearchCriteria dataFirst = new ValueLinkSearchCriteria();
        dataFirst.setTargetOptionIds(OLD_VENDOR_ID);
        dataFirst.setSourceParamIds(UNITS_PARAM_FIRST);
        dataFirst.setLinkDirection(LinkDirection.REVERSE);

        ValueLinkSearchCriteria dataSecond = new ValueLinkSearchCriteria();
        dataSecond.setTargetOptionIds(OLD_VENDOR_ID);
        dataSecond.setSourceParamIds(UNITS_PARAM_SECOND);
        dataSecond.setLinkDirection(LinkDirection.REVERSE);

        GLMeasure glMeasureFirst = new GLMeasure();
        glMeasureFirst.setUnitParamId(UNITS_PARAM_FIRST);
        GLMeasure glMeasureSecond = new GLMeasure();
        glMeasureSecond.setUnitParamId(UNITS_PARAM_SECOND);

        ValueLink valueLinkForFirst = new ValueLink();
        valueLinkForFirst.setSourceOptionId(unitsOptionIdFirst);
        ValueLink valueLinkForSecond = new ValueLink();
        valueLinkForSecond.setSourceOptionId(unitsOptionIdSecond);

        when(valueLinkService.findValueLinks(eq(dataFirst))).thenReturn(Collections.singletonList(valueLinkForFirst));
        when(valueLinkService.findValueLinks(eq(dataSecond))).thenReturn(Collections.singletonList(valueLinkForSecond));
        when(sizeMeasureService.getSizeMeasure(measureIdFirst)).thenReturn(glMeasureFirst);
        when(sizeMeasureService.getSizeMeasure(measureIdSecond)).thenReturn(glMeasureSecond);

        when(valueLinkService.findValueLinks(argThat(
            criteria -> criteria.getSourceOptionIds() != null && !criteria.getSourceOptionIds().isEmpty()
        ))).thenReturn(Collections.singletonList(new ValueLink()));

        duplicateOldSizeStructureService.duplicateUpdateSizeChart(USER_ID, sizeChartActual, sizeChartBefore);

        Option optionFirst = getOption(unitsOptionIdFirst, ACTUAL_SIZE_CHART_NAME, UNITS_PARAM_FIRST);
        Option optionSecond = getOption(unitsOptionIdSecond, ACTUAL_SIZE_CHART_NAME, UNITS_PARAM_SECOND);

        InOrder parameterServiceInOrder = Mockito.inOrder(parameterService);
        InOrder valueLInkInOrder = Mockito.inOrder(valueLinkService);
        parameterServiceInOrder.verify(parameterService).saveParameter(
            any(), anyLong(), any(),
            argThat(valuesChanges -> valuesChanges.getUpdated().contains(optionFirst))
        );
        parameterServiceInOrder.verify(parameterService).saveParameter(
            any(), anyLong(), any(),
            argThat(valuesChanges -> valuesChanges.getUpdated().contains(optionSecond))
        );
        valueLInkInOrder.verify(valueLinkService).findValueLinks(eq(dataFirst));
        valueLInkInOrder.verify(valueLinkService).findValueLinks(eq(dataSecond));
        valueLInkInOrder.verify(valueLinkService).findValueLinks(
            argThat(criteria -> criteria.getTargetOptionIds().contains(OLD_VENDOR_ID)
            ));
        valueLInkInOrder.verify(valueLinkService).saveValueLink(
            argThat(vl -> vl.getTargetOptionId().equals(NEW_VENDOR_ID)
            ));
    }

    @Test
    public void duplicateRemoveSizeChartMeasureValueTestIfMeasureOnly() {
        final long measureId = 1L;
        final long unitsOptionId = 11L;
        final long valueId = 2L;

        SizeChart sizeChart = new SizeChart();
        sizeChart.setName(ACTUAL_SIZE_CHART_NAME);

        Size size = new Size();
        SizeChartMeasure sizeChartMeasureFirst = new SizeChartMeasure();
        sizeChartMeasureFirst.setMeasureId(measureId);

        size.addSizeChartMeasure(sizeChartMeasureFirst);

        sizeChart.addSize(size);

        Map<String, Object> dataFirst = new HashMap<>();
        dataFirst.put("name", ACTUAL_SIZE_CHART_NAME);
        dataFirst.put("param_id", UNITS_PARAM_FIRST);
        when(namedParameterJdbcTemplate.queryForObject(anyString(), eq(dataFirst), any(Class.class)))
            .thenReturn(unitsOptionId);

        GLMeasure glMeasureFirst = new GLMeasure();
        glMeasureFirst.setUnitParamId(UNITS_PARAM_FIRST);
        glMeasureFirst.setValueParamId(valueId);
        when(sizeMeasureService.getSizeMeasure(measureId)).thenReturn(glMeasureFirst);


        duplicateOldSizeStructureService.duplicateRemoveSizeChartMeasureValue(
            USER_ID, sizeChart, sizeChartMeasureFirst
        );

        Option option = getOption(unitsOptionId, ACTUAL_SIZE_CHART_NAME, UNITS_PARAM_FIRST);
        verify(sizeMeasureService, Mockito.times(1))
            .removeConversionsForValue(any(), any(), anyLong());
        verify(parameterService).saveParameter(
            any(), anyLong(), any(),
            argThat(valuesChanges -> valuesChanges.getDeleted().contains(option))
        );
    }

    @Test
    public void duplicateRemoveSizeChartMeasureValueTestIfMeasureNotOnly() {
        final long measureIdFirst = 1L;
        final long valueIdFirst = 2L;

        SizeChart sizeChart = new SizeChart();

        Size size = new Size();
        SizeChartMeasure sizeChartMeasureFirst = new SizeChartMeasure();
        sizeChartMeasureFirst.setMeasureId(measureIdFirst);
        SizeChartMeasure sizeChartMeasureSecond = new SizeChartMeasure();
        sizeChartMeasureSecond.setMeasureId(measureIdFirst);

        size.addSizeChartMeasure(sizeChartMeasureFirst);
        size.addSizeChartMeasure(sizeChartMeasureSecond);

        sizeChart.addSize(size);

        GLMeasure glMeasureFirst = new GLMeasure();
        glMeasureFirst.setUnitParamId(UNITS_PARAM_FIRST);
        glMeasureFirst.setValueParamId(valueIdFirst);

        when(sizeMeasureService.getSizeMeasure(measureIdFirst)).thenReturn(glMeasureFirst);

        duplicateOldSizeStructureService.duplicateRemoveSizeChartMeasureValue(
            USER_ID, sizeChart, sizeChartMeasureFirst
        );

        verify(parameterService, Mockito.times(0)).saveParameter(any(), anyLong(), any(), any());
        verify(sizeMeasureService, Mockito.times(1))
            .removeConversionsForValue(any(), any(), anyLong());
    }

    @Test
    public void duplicateCreateSizeChartMeasureValueIfMeasureAlreadyExistsInChart() {
        final long measureIdFirst = 1L;
        final long unitsOptionId = 11L;
        final long valueId = 2L;
        final long valueOptionId = 4L;
        final int maxValue = 10;
        final int minValue = 0;

        SizeChart sizeChart = new SizeChart();
        sizeChart.setName(ACTUAL_SIZE_CHART_NAME);

        Size size = new Size();
        SizeChartMeasure sizeChartMeasureFirst = new SizeChartMeasure();
        sizeChartMeasureFirst.setMeasureId(measureIdFirst);
        sizeChartMeasureFirst.setOptionId(valueOptionId);
        sizeChartMeasureFirst.setMaxValue(maxValue);
        sizeChartMeasureFirst.setMinValue(minValue);
        sizeChartMeasureFirst.setName("name");
        sizeChartMeasureFirst.setConvertedToSize(true);

        SizeChartMeasure sizeChartMeasureSecond = new SizeChartMeasure();
        sizeChartMeasureSecond.setMeasureId(measureIdFirst);

        size.addSizeChartMeasure(sizeChartMeasureSecond);
        sizeChart.addSize(size);


        GLMeasure glMeasureFirst = new GLMeasure();
        glMeasureFirst.setUnitParamId(UNITS_PARAM_FIRST);
        glMeasureFirst.setValueParamId(valueId);

        when(sizeMeasureService.getSizeMeasure(measureIdFirst)).thenReturn(glMeasureFirst);

        Map<String, Object> dataFirst = new HashMap<>();
        dataFirst.put("name", ACTUAL_SIZE_CHART_NAME);
        dataFirst.put("param_id", UNITS_PARAM_FIRST);

        when(namedParameterJdbcTemplate.queryForObject(anyString(), eq(dataFirst), any(Class.class)))
            .thenReturn(unitsOptionId);

        duplicateOldSizeStructureService.duplicateCreateSizeChartMeasureValue(
            USER_ID, sizeChart, sizeChartMeasureFirst
        );

        ValueLink expectedValueLInk = new ValueLink();

        expectedValueLInk.setLinkDirection(LinkDirection.BIDIRECTIONAL);
        expectedValueLInk.setType(ValueLinkType.GENERAL);
        expectedValueLInk.setSourceParamId(UNITS_PARAM_FIRST);
        expectedValueLInk.setSourceOptionId(unitsOptionId);
        expectedValueLInk.setTargetParamId(valueId);
        expectedValueLInk.setTargetOptionId(valueOptionId);

        SizeMeasureValueOptionDto dto = new SizeMeasureValueOptionDto();
        dto.setConversionRule(minValue + "-" + maxValue);
        Option option = getOption(
            sizeChartMeasureFirst.getOptionId(), sizeChartMeasureFirst.getName(), glMeasureFirst.getValueParamId()
        );
        dto.setOption(option);
        dto.setConvertToSize(sizeChartMeasureFirst.getConvertedToSize());


        verify(valueLinkService, times(1)).saveValueLink(expectedValueLInk);
        verify(sizeMeasureService, times(1))
            .createConversionRules(glMeasureFirst, dto, USER_ID, true);
    }

    @Test
    public void duplicateCreateSizeChartMeasureValueIfMeasureNotPresentInChart() {
        final long measureIdFirst = 1L;
        final long valueId = 2L;
        final long valueOptionId = 4L;
        final int maxValue = 10;
        final int minValue = 0;
        final long vendorId = 123L;

        SizeChart sizeChart = new SizeChart();
        sizeChart.setName(ACTUAL_SIZE_CHART_NAME);
        sizeChart.setVendorId(vendorId);

        Size size = new Size();
        SizeChartMeasure sizeChartMeasureFirst = new SizeChartMeasure();
        sizeChartMeasureFirst.setMeasureId(measureIdFirst);
        sizeChartMeasureFirst.setOptionId(valueOptionId);
        sizeChartMeasureFirst.setMaxValue(maxValue);
        sizeChartMeasureFirst.setMinValue(minValue);
        sizeChartMeasureFirst.setName("name");
        sizeChartMeasureFirst.setConvertedToSize(true);

        sizeChart.addSize(size);

        GLMeasure glMeasureFirst = new GLMeasure();
        glMeasureFirst.setUnitParamId(UNITS_PARAM_FIRST);
        glMeasureFirst.setValueParamId(valueId);

        when(sizeMeasureService.getSizeMeasure(measureIdFirst)).thenReturn(glMeasureFirst);

        duplicateOldSizeStructureService.duplicateCreateSizeChartMeasureValue(
            USER_ID, sizeChart, sizeChartMeasureFirst
        );

        SizeMeasureValueOptionDto dto = new SizeMeasureValueOptionDto();
        dto.setConversionRule(minValue + "-" + maxValue);
        Option optionForParam = getOption(null, ACTUAL_SIZE_CHART_NAME, glMeasureFirst.getUnitParamId());
        Option optionForRule = getOption(
            sizeChartMeasureFirst.getOptionId(), sizeChartMeasureFirst.getName(), glMeasureFirst.getValueParamId()
        );
        dto.setOption(optionForRule);
        dto.setConvertToSize(sizeChartMeasureFirst.getConvertedToSize());


        verify(parameterService).saveParameter(
            any(), anyLong(), any(),
            argThat(valuesChanges -> valuesChanges.getAdded().contains(optionForParam))
        );
        verify(valueLinkService, times(1)).saveValueLink(argThat(
            valueLink -> valueLink.getTargetParamId().equals(KnownIds.VENDOR_PARAM_ID))
        );
        verify(sizeMeasureService, times(1))
            .createConversionRules(glMeasureFirst, dto, USER_ID, true);
    }

    @Test
    public void duplicateUpdateSizeChartMeasureValueTest() {
        final long measureIdFirst = 1L;
        final long valueId = 2L;
        final long valueOptionId = 4L;
        final int maxValue = 10;
        final int minValue = 10;

        GLMeasure glMeasure = new GLMeasure();
        glMeasure.setUnitParamId(UNITS_PARAM_FIRST);
        glMeasure.setValueParamId(valueId);

        when(sizeMeasureService.getSizeMeasure(measureIdFirst)).thenReturn(glMeasure);

        SizeChartMeasure sizeChartMeasureValue = new SizeChartMeasure();
        sizeChartMeasureValue.setMeasureId(measureIdFirst);
        sizeChartMeasureValue.setOptionId(valueOptionId);
        sizeChartMeasureValue.setMaxValue(maxValue);
        sizeChartMeasureValue.setMinValue(minValue);
        sizeChartMeasureValue.setName("name");
        sizeChartMeasureValue.setConvertedToSize(true);

        duplicateOldSizeStructureService.duplicateUpdateSizeChartMeasureValue(USER_ID, sizeChartMeasureValue);

        Option optionForDeleteRule = getOption(
            valueOptionId, sizeChartMeasureValue.getName(), glMeasure.getValueParamId()
        );
        verify(sizeMeasureService, times(1))
            .removeConversionsForValue(glMeasure, optionForDeleteRule, USER_ID);

        SizeMeasureValueOptionDto dto = new SizeMeasureValueOptionDto();
        dto.setConversionRule(String.valueOf(minValue));
        Option optionForCreateRule = getOption(
            sizeChartMeasureValue.getOptionId(), sizeChartMeasureValue.getName(), glMeasure.getValueParamId()
        );
        dto.setOption(optionForCreateRule);
        dto.setConvertToSize(sizeChartMeasureValue.getConvertedToSize());
        verify(sizeMeasureService, times(1))
            .createConversionRules(glMeasure, dto, USER_ID, true);
    }

    private Option getOption(Long optionId, String name, Long paramId) {
        Word optionName = WordUtil.defaultWord(name);
        Option option = new OptionImpl();
        option.setParamId(paramId);
        option.setNames(Stream.of(optionName).collect(Collectors.toList()));
        option.setPublished(true);

        if (optionId != null) {
            option.setId(optionId);
        }

        return option;
    }

    private CategoryEntities getCategoryEntities() {
        CategoryEntities categoryEntities = new CategoryEntities();

        CategoryParam sizeChartParam = new Parameter();
        sizeChartParam.setId(UNITS_PARAM_FIRST);

        CategoryParam sizeParam = new Parameter();
        sizeParam.setId(UNITS_PARAM_SECOND);

        categoryEntities.getParameters().addAll(Stream.of(sizeChartParam, sizeParam)
            .collect(Collectors.toList()));

        return categoryEntities;
    }
}

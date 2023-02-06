package ru.yandex.market.mbo.db.size;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import ru.yandex.market.mbo.common.model.KnownIds;
import ru.yandex.market.mbo.core.utils.TransactionTemplateMock;
import ru.yandex.market.mbo.db.SizeMeasureService;
import ru.yandex.market.mbo.db.linkedvalues.ValueLinkServiceInterface;
import ru.yandex.market.mbo.db.params.IParameterLoaderService;
import ru.yandex.market.mbo.db.params.ParameterSaveContext;
import ru.yandex.market.mbo.db.params.ParameterService;
import ru.yandex.market.mbo.db.size.dao.SizeChartDao;
import ru.yandex.market.mbo.db.size.dao.SizeChartMeasureDao;
import ru.yandex.market.mbo.db.size.dao.SizeChartSizeDao;
import ru.yandex.market.mbo.db.size.model.Size;
import ru.yandex.market.mbo.db.size.model.SizeChart;
import ru.yandex.market.mbo.db.size.model.SizeChartMeasure;
import ru.yandex.market.mbo.db.size.processor.SetVendorChartNameProcessor;
import ru.yandex.market.mbo.db.size.validation.SizeChartSizeNameValidator;
import ru.yandex.market.mbo.db.size.validation.SizeChartUniqueNameValidator;
import ru.yandex.market.mbo.gwt.models.gurulight.GLMeasure;
import ru.yandex.market.mbo.gwt.models.gurulight.ParameterValuesChanges;
import ru.yandex.market.mbo.gwt.models.linkedvalues.InitializedValueLink;
import ru.yandex.market.mbo.gwt.models.linkedvalues.LinkDirection;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLink;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLinkType;
import ru.yandex.market.mbo.gwt.models.params.CategoryEntities;
import ru.yandex.market.mbo.gwt.models.params.CategoryParam;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionImpl;
import ru.yandex.market.mbo.gwt.models.params.Parameter;
import ru.yandex.market.mbo.gwt.models.visual.Word;
import ru.yandex.market.mbo.gwt.utils.WordUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.market.mbo.db.params.IParameterLoaderService.GLOBAL_ENTITIES_HID;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("checkstyle:magicNumber")
public class SizeChartStorageServiceTest {

    private static final String SIZE_CHART_NAME = "ADIDAS";
    private static final Long SIZE_CHART_ID = 200L;
    private static final Long SIZE_CHART_OPTION_ID = 21L;
    private static final Long LOCAL_SIZE_CHART_ID = 22L;

    private static final String SIZE_NAME = "44";
    private static final Long SIZE_OPTION_ID = 5L;
    private static final Long SIZE_ID = 6L;

    private static final Long SIZE_CHART_MEASURE_ID = 90L;
    private static final Long SIZE_CHART_MEASURE_OPTION_ID = 91L;
    private static final String SIZE_CHART_MEASURE_NAME = "44";

    private static final Long USER_ID = 1L;
    private static final Long MEASURE_ID = 33L;
    private static final Long VALUE_PARAM_ID = 12L;

    private static final Random RANDOM = new Random();

    @Mock
    private SizeChartDao sizeChartDao;
    @Mock
    private SizeChartSizeDao sizeChartSizeDao;
    @Mock
    private SizeChartMeasureDao sizeChartMeasureDao;
    @Mock
    private IParameterLoaderService parameterLoaderService;
    @Mock
    private ParameterService parameterService;
    @Mock
    private SizeMeasureService sizeMeasureService;
    @Mock
    private SizeChartSizeNameValidator sizeChartSizeNameValidator;
    @Mock
    private SizeChartUniqueNameValidator sizeChartUniqueNameValidator;
    @Mock
    private SetVendorChartNameProcessor setVendorChartNameProcessor;
    @Mock
    private DuplicateOldSizeStructureService duplicateOldSizeStructureService;
    @Mock
    private ValueLinkServiceInterface valueLinkService;

    private final TransactionTemplate transactionTemplate = new TransactionTemplateMock();

    private SizeChartStorageServiceImpl sizeChartService;

    @Before
    public void setUp() {
        sizeChartService = new SizeChartStorageServiceImpl(
            sizeChartDao, sizeChartSizeDao, sizeChartMeasureDao, parameterLoaderService,
            parameterService, transactionTemplate, sizeMeasureService, valueLinkService,
            sizeChartSizeNameValidator, sizeChartUniqueNameValidator, setVendorChartNameProcessor,
            duplicateOldSizeStructureService);
        ReflectionTestUtils.setField(sizeChartService, "needCreateValueLinks", true);

        when(parameterLoaderService.loadGlobalEntitiesWithoutValues()).thenReturn(getCategoryEntities());
        when(parameterService.createDefaultSaveContext(USER_ID)).thenReturn(new ParameterSaveContext(USER_ID));

        GLMeasure glMeasure = new GLMeasure();
        glMeasure.setValueParamId(VALUE_PARAM_ID);
        when(sizeMeasureService.getSizeMeasure(anyLong())).thenReturn(glMeasure);


        doAnswer(invocation -> {
            List<Size> sizes = invocation.getArgument(0);
            sizes.forEach(size -> size.setId(RANDOM.nextLong()));
            return null;
        }).when(sizeChartSizeDao).insertSizeChartSizes(any(), anyLong());

        SizeChart sizeChart = fillSizeChart();

        sizeChart.setId(SIZE_CHART_ID);
        sizeChart.setOptionId(SIZE_CHART_OPTION_ID);

        doAnswer(invocation -> {
            SizeChart chart = invocation.getArgument(0);
            chart.setId(SIZE_CHART_ID);
            return null;
        }).when(sizeChartDao).insertSizeChart(any(SizeChart.class));

        when(sizeChartDao.getSizeChartById(SIZE_CHART_ID)).thenReturn(Optional.of(sizeChart));
    }

    @Test
    public void getSizeChartsSimpleTest() {
        List<SizeChart> sizeCharts = Stream.of(
            new SizeChart()
        ).collect(Collectors.toList());

        when(sizeChartDao.getSizeChartsSimple()).thenReturn(sizeCharts);
        List<SizeChart> result = sizeChartService.getSizeChartsSimple();

        assertEquals(result, sizeCharts);

        verify(sizeChartDao, times(1)).getSizeChartsSimple();
    }

    @Test
    public void getSizeChartByIdTest() {
        final Long id = 123L;
        Optional<SizeChart> sizeChart = Optional.of(new SizeChart());

        when(sizeChartDao.getSizeChartById(id)).thenReturn(sizeChart);
        Optional<SizeChart> result = sizeChartService.getSizeChartById(id);

        assertEquals(sizeChart, result);

        verify(sizeChartDao, times(1)).getSizeChartById(id);
    }

    @Test
    public void createVendorSizeChartTest() {
        final long vendorId = 29L;

        SizeChart sizeChart = fillSizeChart();
        sizeChart.setVendorId(vendorId);

        Option optionForCreate = getOption(null, sizeChart.getName());
        ParameterValuesChanges parameterValuesChanges = new ParameterValuesChanges();
        parameterValuesChanges.getAdded().addAll(Collections.singletonList(optionForCreate));
        parameterValuesChanges.getAddedLinks()
            .add(new InitializedValueLink(
                optionForCreate, getOption(vendorId, KnownIds.VENDOR_PARAM_ID),
                LinkDirection.REVERSE, ValueLinkType.GENERAL
            ));

        sizeChartService.saveSizeChart(USER_ID, sizeChart);

        verify(parameterService, times(1)).saveParameter(
            any(), eq(GLOBAL_ENTITIES_HID), any(CategoryParam.class),
            argThat(valuesChanges -> valuesChanges.getAdded().equals(parameterValuesChanges.getAdded())
                && valuesChanges.getAddedLinks().equals(parameterValuesChanges.getAddedLinks()))
        );
        verify(setVendorChartNameProcessor, times(1)).process(sizeChart);
        verify(sizeChartUniqueNameValidator, times(1)).validate(sizeChart);

        verify(sizeChartDao, times(1)).insertSizeChart(sizeChart);
        verify(sizeChartDao, times(0)).updateOnlySizeChart(any());
        verify(sizeChartDao, times(1)).getSizeChartById(SIZE_CHART_ID);
    }

    @Test
    public void createStandardSizeChartTest() {
        SizeChart sizeChart = fillSizeChart();

        Option optionForCreate = getOption(null, sizeChart.getName());
        ParameterValuesChanges parameterValuesChanges = new ParameterValuesChanges();
        parameterValuesChanges.getAdded().addAll(Collections.singletonList(optionForCreate));

        sizeChartService.saveSizeChart(USER_ID, sizeChart);

        verify(parameterService, times(1)).saveParameter(
            any(), anyLong(), any(CategoryParam.class),
            argThat(valuesChanges -> valuesChanges.getAdded().equals(parameterValuesChanges.getAdded())
                && valuesChanges.getAddedLinks().isEmpty())
        );

        verify(setVendorChartNameProcessor, times(1)).process(sizeChart);
        verify(sizeChartUniqueNameValidator, times(1)).validate(sizeChart);

        verify(sizeChartDao, times(1)).insertSizeChart(sizeChart);
        verify(sizeChartDao, times(0)).updateOnlySizeChart(any());
        verify(sizeChartDao, times(1)).getSizeChartById(SIZE_CHART_ID);
    }

    @Test
    public void updateSizeChartNameTest() {
        final Long chartOptionId = 1L;

        SizeChart sizeChart = fillSizeChart();
        sizeChart.setId(SIZE_CHART_ID);
        sizeChart.setOptionId(chartOptionId);

        ParameterValuesChanges parameterValuesChanges = new ParameterValuesChanges();
        parameterValuesChanges.getUpdated()
            .addAll(Collections.singletonList(getOption(chartOptionId, sizeChart.getName())));

        sizeChartService.saveSizeChart(USER_ID, sizeChart);

        verify(parameterService, times(1)).saveParameter(
            any(), anyLong(), any(CategoryParam.class),
            argThat(valuesChanges -> valuesChanges.getUpdated().equals(parameterValuesChanges.getUpdated())
                && valuesChanges.getAdded().isEmpty() && valuesChanges.getAddedLinks().isEmpty())
        );

        verify(setVendorChartNameProcessor, times(1)).process(sizeChart);
        verify(sizeChartUniqueNameValidator, times(1)).validate(sizeChart);

        verify(sizeChartDao, times(0)).insertSizeChart(any(SizeChart.class));
        verify(sizeChartDao, times(1)).updateOnlySizeChart(any());

        // also for get sizeChart from db
        verify(sizeChartDao, times(2)).getSizeChartById(SIZE_CHART_ID);

        verify(valueLinkService, never()).findValueLinks(any());
        verify(valueLinkService, never()).saveValueLink(any());
    }

    @Test
    public void updateSizeChartVendorTest() {
        final Long chartOptionId = 1L;

        final Long vendorIdBefore = 12L;
        final Long vendorIdNew = 15L;

        SizeChart sizeChartBefore = fillSizeChart();
        sizeChartBefore.setId(SIZE_CHART_ID);
        sizeChartBefore.setOptionId(chartOptionId);
        sizeChartBefore.setVendorId(vendorIdBefore);

        SizeChart sizeChartActual = fillSizeChart();
        sizeChartActual.setId(SIZE_CHART_ID);
        sizeChartActual.setOptionId(chartOptionId);
        sizeChartActual.setVendorId(vendorIdNew);

        ParameterValuesChanges parameterValuesChanges = new ParameterValuesChanges();
        parameterValuesChanges.getUpdated()
            .addAll(Collections.singletonList(getOption(chartOptionId, sizeChartBefore.getName())));

        when(sizeChartDao.getSizeChartById(sizeChartActual.getId())).thenReturn(Optional.of(sizeChartBefore));
        when(valueLinkService.findValueLinks(any())).thenReturn(Collections.singletonList(new ValueLink()));

        sizeChartService.saveSizeChart(USER_ID, sizeChartActual);

        verify(valueLinkService).findValueLinks(argThat(
            criteria -> criteria.getTargetOptionIds().contains(vendorIdBefore)
        ));
        verify(valueLinkService).saveValueLink(argThat(
            vl -> vl.getTargetOptionId().equals(vendorIdNew)
        ));

        verify(setVendorChartNameProcessor, times(1)).process(sizeChartActual);
        verify(sizeChartUniqueNameValidator, times(1)).validate(sizeChartActual);

        verify(sizeChartDao, times(0)).insertSizeChart(any(SizeChart.class));
        verify(sizeChartDao, times(1)).updateOnlySizeChart(any());

        // also for get sizeChart from db
        verify(sizeChartDao, times(2)).getSizeChartById(SIZE_CHART_ID);
    }

    @Test
    public void createSizeChartSizeTest() {
        final String sizeName = "42";
        final Long sizeChartOptionId = 5L;

        List<Long> categoryIds = Stream.of(1L, 2L).collect(Collectors.toList());

        SizeChart sizeChart = fillSizeChart();
        Size size = fillSize(sizeName, categoryIds);

        sizeChart.setId(SIZE_CHART_ID);
        sizeChart.addSize(size);
        sizeChart.setOptionId(sizeChartOptionId);

        when(sizeChartDao.getSizeChartById(SIZE_CHART_ID)).thenReturn(Optional.of(sizeChart));

        sizeChartService.saveSize(USER_ID, SIZE_CHART_ID, size);

        Option option = getOption(null, size.getSizeName());
        ParameterValuesChanges parameterValuesChanges = new ParameterValuesChanges();
        parameterValuesChanges.getAdded()
            .add(option);
        parameterValuesChanges.getAddedLinks()
            .add(new InitializedValueLink(getOption(
                sizeChartOptionId, KnownIds.UNITED_SIZE_CHART_PARAM_ID),
                option, LinkDirection.BIDIRECTIONAL, ValueLinkType.GENERAL
            ));

        verify(parameterService, times(1)).saveParameter(
            any(), anyLong(), any(CategoryParam.class),
            argThat(valuesChanges -> valuesChanges.getUpdated().isEmpty()
                && valuesChanges.getDeleted().isEmpty()
                && valuesChanges.getAdded().size() == 1
                && valuesChanges.getAdded().contains(option)
                && valuesChanges.getAddedLinks().size() == 1
                && valuesChanges.getAddedLinks().containsAll(parameterValuesChanges.getAddedLinks()))
        );

        verify(sizeChartSizeNameValidator, times(1)).validate(sizeChart, size);

        verify(sizeChartSizeDao, times(0)).removeSizeChartSizeByIds(Collections.emptyList());
        verify(sizeChartSizeDao, times(0)).updateSizeChartSizes(Collections.emptyList());
        verify(sizeChartSizeDao, times(1))
            .insertSizeChartSizes(sizeChart.getSizes(), SIZE_CHART_ID);
    }

    @Test
    public void updateSizeChartSizeTest() {
        final String oldSizeName = "45";

        SizeChart sizeChart = fillAllSizeChart(true, false, true, false, true);
        SizeChart sizeChartFromDb = fillAllSizeChart(true, false, true, false, true);

        sizeChartFromDb.getSizes().get(0).setSizeName(oldSizeName);

        Size size = sizeChart.getSizes().get(0);

        when(sizeChartDao.getSizeChartById(LOCAL_SIZE_CHART_ID)).thenReturn(Optional.of(sizeChartFromDb));

        sizeChartService.saveSize(USER_ID, LOCAL_SIZE_CHART_ID, size);
        Option option = getOption(SIZE_OPTION_ID, SIZE_NAME);

        verify(parameterService, times(1)).saveParameter(
            any(), anyLong(), any(CategoryParam.class),
            argThat(valuesChanges -> valuesChanges.getDeleted().isEmpty()
                && valuesChanges.getAdded().isEmpty()
                && valuesChanges.getUpdated().contains(option)
                && valuesChanges.getAddedLinks().isEmpty()
            )
        );

        verify(sizeChartSizeNameValidator, times(1)).validate(sizeChart, size);

        verify(sizeChartSizeDao, times(0)).removeSizeChartSizeByIds(any());
        verify(sizeChartSizeDao, times(0)).insertSizeChartSizes(any(), anyLong());
        verify(sizeChartSizeDao, times(1))
            .updateSizeChartSizes(Collections.singletonList(size));
    }

    @Test
    public void removeSizeChartSizeTest() {
        SizeChart sizeChartFromDb = fillAllSizeChart(true, false, true, false, true);

        when(sizeChartDao.getSizeChartById(sizeChartFromDb.getId())).thenReturn(Optional.of(sizeChartFromDb));

        Long sizeId = sizeChartFromDb.getSizes().get(0).getId();

        sizeChartService.removeSize(USER_ID, sizeId, sizeChartFromDb.getId());

        Option option = getOption(SIZE_OPTION_ID, SIZE_NAME);
        ParameterValuesChanges parameterValuesChanges = new ParameterValuesChanges();
        parameterValuesChanges.getDeleted()
            .add(option);

        verify(parameterService, times(1)).saveParameter(
            any(), anyLong(), any(CategoryParam.class),
            argThat(valuesChanges -> valuesChanges.getDeleted().contains(option)
                && valuesChanges.getAdded().isEmpty()
                && valuesChanges.getUpdated().isEmpty()
                && valuesChanges.getAddedLinks().isEmpty()
            )
        );

        verify(sizeChartSizeDao, times(1))
            .removeSizeChartSizeByIds(Collections.singletonList(SIZE_ID));
    }

    @Test
    public void createSizeChartMeasure() {
        SizeChart sizeChart = fillAllSizeChart(true, true, true, false, true);
        SizeChart sizeChartFromDb = fillAllSizeChart(true, false, true, false, true);

        Size size = sizeChart.getSizes().get(0);
        Long sizeId = size.getId();
        SizeChartMeasure sizeChartMeasure = size.getMeasures().get(0);

        when(sizeChartSizeDao.getSize(sizeId)).thenReturn(Optional.of(size));
        when(sizeChartDao.getSizeChartById(LOCAL_SIZE_CHART_ID)).thenReturn(Optional.of(sizeChartFromDb));

        sizeChartService.saveSizeChartMeasureValue(USER_ID, sizeId, sizeChartMeasure);

        Option option = getOption(null, SIZE_CHART_MEASURE_NAME);
        ParameterValuesChanges parameterValuesChanges = new ParameterValuesChanges();
        parameterValuesChanges.getAdded().add(option);
        parameterValuesChanges.getAddedLinks()
            .add(new InitializedValueLink(getOption(
                SIZE_CHART_OPTION_ID, KnownIds.UNITED_SIZE_CHART_PARAM_ID),
                option, LinkDirection.BIDIRECTIONAL, ValueLinkType.GENERAL
            ));

        verify(parameterService, times(1)).saveParameter(
            any(), anyLong(), any(CategoryParam.class),
            argThat(valuesChanges ->
                valuesChanges.getDeleted().isEmpty()
                    && valuesChanges.getAdded().contains(option)
                    && valuesChanges.getUpdated().isEmpty()
                    && valuesChanges.getAddedLinks().size() == 1
                    && valuesChanges.getAddedLinks().containsAll(parameterValuesChanges.getAddedLinks())
            )
        );

        verify(sizeChartMeasureDao, times(0))
            .updateSizeChartMeasures(any());
        verify(sizeChartMeasureDao, times(1))
            .insertSizeChartMeasures(sizeChart.getSizes().get(0).getMeasures());
    }

    @Test
    public void updateSizeChartMeasure() {
        SizeChart sizeChart = fillAllSizeChart(true, true, true, true, true);
        SizeChart sizeChartFromDb = fillAllSizeChart(true, false, true, true, true);

        SizeChartMeasure sizeChartMeasureFromDb = fillSizeChartMeasure(
            "46", SIZE_ID, SIZE_CHART_OPTION_ID, true, MEASURE_ID,
            false, 10, 20
        );
        sizeChartMeasureFromDb.setId(SIZE_CHART_MEASURE_ID);

        Size size = sizeChart.getSizes().get(0);
        Long sizeId = size.getId();
        SizeChartMeasure sizeChartMeasure = size.getMeasures().get(0);

        sizeChartFromDb.getSizes().get(0).addSizeChartMeasure(sizeChartMeasureFromDb);

        when(sizeChartDao.getSizeChartById(LOCAL_SIZE_CHART_ID)).thenReturn(Optional.of(sizeChartFromDb));
        when(sizeChartSizeDao.getSize(sizeId)).thenReturn(Optional.of(size));

        sizeChartService.saveSizeChartMeasureValue(USER_ID, sizeId, sizeChartMeasure);

        Option option = getOption(SIZE_CHART_MEASURE_OPTION_ID, SIZE_CHART_MEASURE_NAME);
        ParameterValuesChanges parameterValuesChanges = new ParameterValuesChanges();
        parameterValuesChanges.getUpdated().add(option);

        verify(parameterService, times(1)).saveParameter(
            any(), anyLong(), any(CategoryParam.class),
            argThat(valuesChanges ->
                valuesChanges.getDeleted().isEmpty()
                    && valuesChanges.getAdded().isEmpty()
                    && valuesChanges.getUpdated().contains(option)
                    && valuesChanges.getAddedLinks().isEmpty()
            )
        );

        verify(sizeChartMeasureDao, times(1))
            .updateSizeChartMeasures(sizeChart.getSizes().get(0).getMeasures());
        verify(sizeChartMeasureDao, times(0))
            .insertSizeChartMeasures(any());
    }

    @Test
    public void deleteSizeChartMeasure() {
        SizeChart sizeChart = fillAllSizeChart(true, true, true, true, true);

        Size size = sizeChart.getSizes().get(0);
        Long sizeId = size.getId();
        SizeChartMeasure sizeChartMeasure = size.getMeasures().get(0);
        Long sizeChartMeasureId = sizeChartMeasure.getId();

        when(sizeChartDao.getSizeChartById(sizeChart.getId())).thenReturn(Optional.of(sizeChart));
        when(sizeChartSizeDao.getSize(sizeId)).thenReturn(Optional.of(size));
        when(sizeChartMeasureDao.getSizeChartMeasureById(sizeChartMeasureId)).thenReturn(Optional.of(sizeChartMeasure));

        sizeChartService.removeSizeChartMeasureValue(USER_ID, sizeId, sizeChartMeasureId);

        Option option = getOption(SIZE_CHART_MEASURE_OPTION_ID, SIZE_CHART_MEASURE_NAME);

        verify(parameterService, times(1)).saveParameter(
            any(), anyLong(), any(CategoryParam.class),
            argThat(valuesChanges ->
                valuesChanges.getDeleted().contains(option)
                    && valuesChanges.getAdded().isEmpty()
                    && valuesChanges.getUpdated().isEmpty()
                    && valuesChanges.getAddedLinks().isEmpty()
            )
        );

        verify(sizeChartMeasureDao, times(1))
            .removeSizeChartMeasuresByIds(sizeChart.getSizes().stream()
                .map(Size::getMeasures).flatMap(Collection::stream)
                .map(SizeChartMeasure::getId).collect(Collectors.toList())
            );
    }

    @Test(expected = SizeChartStorageServiceException.class)
    public void cantRemoveSizeChartIfSizesExistTest() {
        SizeChart sizeChart = fillAllSizeChart(true, true, true, true, true);

        when(sizeChartDao.getSizeChartById(sizeChart.getId())).thenReturn(Optional.of(sizeChart));

        sizeChartService.removeSizeChart(USER_ID, sizeChart.getId());
    }

    @Test(expected = SizeChartStorageServiceException.class)
    public void cantRemoveSizeChartSizeIfMeasuresExistTest() {
        SizeChart sizeChart = fillAllSizeChart(true, true, true, true, true);
        Size size = sizeChart.getSizes().get(0);

        when(sizeChartDao.getSizeChartById(sizeChart.getId())).thenReturn(Optional.of(sizeChart));

        sizeChartService.removeSize(USER_ID, size.getId(), sizeChart.getId());
    }

    @Test
    public void updateSizeChartMeasuresIfUpdateSizeNameTest() {
        final String oldSizeName = "45";

        SizeChart sizeChart = fillAllSizeChart(true, false, true, false, true);
        SizeChart sizeChartFromDb = fillAllSizeChart(true, true, true, true, true);

        sizeChartFromDb.getSizes().get(0).setSizeName(oldSizeName);
        sizeChartFromDb.getSizes().get(0).getMeasures().get(0).setName(oldSizeName);
        sizeChartFromDb.getSizes().get(0).getMeasures().get(0).setSetNameByUser(false);
        sizeChartFromDb.getSizes().get(0).getMeasures().add(
            fillSizeChartMeasure(
                "name", sizeChartFromDb.getSizes().get(0).getId(), 123L, false,
                232L, false, 0, 10
            )
        );
        sizeChartFromDb.getSizes().get(0).getMeasures().get(1).setSetNameByUser(true);

        Size size = sizeChart.getSizes().get(0);

        when(sizeChartDao.getSizeChartById(LOCAL_SIZE_CHART_ID)).thenReturn(Optional.of(sizeChartFromDb));

        sizeChartService.saveSize(USER_ID, LOCAL_SIZE_CHART_ID, size);

        SizeChartMeasure measureForUpdate = sizeChartFromDb.getSizes().get(0).getMeasures().get(0);
        measureForUpdate.setName(size.getSizeName());

        Option sizeOption = getOption(SIZE_OPTION_ID, SIZE_NAME);
        Option sizeMeasureOption = getOption(measureForUpdate.getOptionId(), SIZE_NAME);

        InOrder inOrder = inOrder(parameterService);
        inOrder.verify(parameterService).saveParameter(
            any(), anyLong(), any(),
            argThat(valuesChanges -> valuesChanges.getUpdated().contains(sizeMeasureOption))
        );
        inOrder.verify(parameterService).saveParameter(
            any(), anyLong(), any(CategoryParam.class),
            argThat(valuesChanges -> valuesChanges.getDeleted().isEmpty()
                && valuesChanges.getAdded().isEmpty()
                && valuesChanges.getUpdated().contains(sizeOption)
                && valuesChanges.getAddedLinks().isEmpty()
            )
        );

        verify(sizeChartSizeNameValidator, times(1)).validate(sizeChart, size);

        verify(sizeChartMeasureDao, times(1))
            .updateSizeChartMeasures(Collections.singletonList(measureForUpdate));

        verify(sizeChartSizeDao, times(0)).removeSizeChartSizeByIds(any());
        verify(sizeChartSizeDao, times(0)).insertSizeChartSizes(any(), anyLong());
        verify(sizeChartSizeDao, times(1))
            .updateSizeChartSizes(Collections.singletonList(size));
    }

    private SizeChart fillSizeChart() {
        SizeChart sizeChart = new SizeChart();
        sizeChart.setName(SIZE_CHART_NAME);
        return sizeChart;
    }

    private Option getOption(Long optionId, String name) {
        Word optionName = WordUtil.defaultWord(name);
        Option option = new OptionImpl();
        option.setNames(Stream.of(optionName).collect(Collectors.toList()));
        option.setPublished(true);

        if (optionId != null) {
            option.setId(optionId);
        }

        return option;
    }

    private SizeChart fillAllSizeChart(Boolean needAddSize, Boolean needAddSizeMeasures, Boolean setIdForSize,
                                       Boolean setIdForMeasure, Boolean setIdForChart) {
        List<Long> categoryIds = Stream.of(1L, 2L).collect(Collectors.toList());

        SizeChartMeasure sizeChartMeasure = fillSizeChartMeasure(
            SIZE_CHART_MEASURE_NAME, SIZE_ID, SIZE_CHART_MEASURE_OPTION_ID, true, MEASURE_ID,
            false, 10, 20
        );
        if (!setIdForMeasure) {
            sizeChartMeasure.setOptionId(null);
        } else {
            sizeChartMeasure.setId(SIZE_CHART_MEASURE_ID);
        }

        Size size = fillSize(SIZE_NAME, categoryIds);

        if (setIdForSize) {
            size.setSizeOptionId(SIZE_OPTION_ID);
            size.setId(SIZE_ID);
        }

        if (needAddSizeMeasures) {
            size.addSizeChartMeasure(sizeChartMeasure);
        }

        SizeChart sizeChart = fillSizeChart();

        if (setIdForChart) {
            sizeChart.setOptionId(SIZE_CHART_OPTION_ID);
            sizeChart.setId(LOCAL_SIZE_CHART_ID);
        }

        if (needAddSize) {
            sizeChart.addSize(size);
        }

        return sizeChart;
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    private SizeChartMeasure fillSizeChartMeasure(String name, Long sizeId, Long optionId, Boolean convertedToSize,
                                                  Long measureId, Boolean usedInFilter, Integer minValue,
                                                  Integer maxValue) {
        SizeChartMeasure measure = new SizeChartMeasure();
        measure.setName(name);
        measure.setSizeId(sizeId);
        measure.setOptionId(optionId);
        measure.setMeasureId(measureId);
        measure.setConvertedToSize(convertedToSize);
        measure.setUsedInFilter(usedInFilter);
        measure.setMinValue(minValue);
        measure.setMaxValue(maxValue);
        return measure;
    }

    private CategoryEntities getCategoryEntities() {
        CategoryEntities categoryEntities = new CategoryEntities();

        CategoryParam sizeChartParam = new Parameter();
        sizeChartParam.setId(KnownIds.UNITED_SIZE_CHART_PARAM_ID);

        CategoryParam sizeParam = new Parameter();
        sizeParam.setId(KnownIds.UNITED_SIZE_PARAM_ID);

        CategoryParam sizeMeasureParam = new Parameter();
        sizeMeasureParam.setId(VALUE_PARAM_ID);

        categoryEntities.getParameters().addAll(Stream.of(sizeChartParam, sizeParam, sizeMeasureParam)
            .collect(Collectors.toList()));

        return categoryEntities;
    }

    private Size fillSize(String sizeName, List<Long> categoryIds) {
        Size size = new Size();
        size.setSizeName(sizeName);
        size.setSizeChartId(LOCAL_SIZE_CHART_ID);
        size.setCategoryIds(categoryIds);
        return size;
    }

    private Option getOption(Long optionId, Long paramId) {
        Option chartOption = new OptionImpl();
        chartOption.setId(optionId);
        chartOption.setParamId(paramId);
        return chartOption;
    }

}

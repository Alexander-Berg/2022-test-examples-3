package ru.yandex.market.gutgin.tms.pipeline.dcp.xls;

import Market.DataCamp.DataCampContentMarketParameterValue;
import Market.DataCamp.DataCampContentMarketParameterValue.MarketParameterValue;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferMarketContent;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.Assertions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.gutgin.tms.service.ConvertRawSkuToDataCampOffersService;
import ru.yandex.market.ir.autogeneration.common.helpers.BookCategoryHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMock;
import ru.yandex.market.ir.excel.generator.CategoryInfo;
import ru.yandex.market.ir.excel.generator.CategoryInfoProducer;
import ru.yandex.market.ir.excel.generator.ImportContentType;
import ru.yandex.market.ir.excel.generator.param.MainParamCreator;
import ru.yandex.market.ir.excel.generator.param.MboParameterWrapper;
import ru.yandex.market.ir.excel.generator.param.ParameterInfo;
import ru.yandex.market.ir.excel.generator.param.ParameterInfoBuilder;
import ru.yandex.market.ir.http.MarketParameters;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.partner.content.common.BaseDcpExcelDBStateGenerator;
import ru.yandex.market.partner.content.common.csku.KnownParameters;
import ru.yandex.market.partner.content.common.db.dao.FileDataProcessRequestDao;
import ru.yandex.market.partner.content.common.db.dao.XlsDataCampDao;
import ru.yandex.market.partner.content.common.db.dao.goodcontent.GcRawSkuDao;
import ru.yandex.market.partner.content.common.db.jooq.enums.XlsLogbrokerStatus;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcRawSku;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.XlsDatacampOffer;
import ru.yandex.market.partner.content.common.engine.parameter.ProcessFileCountedData;
import ru.yandex.market.partner.content.common.entity.goodcontent.RawParamValue;
import ru.yandex.market.partner.content.common.entity.goodcontent.RawSku;
import ru.yandex.market.robot.db.ParameterValueComposer;

@SuppressWarnings("checkstyle:magicnumber")
public class ConvertRawSkuToDatacampOffersTaskActionTest extends BaseDcpExcelDBStateGenerator {
    @Autowired
    private XlsDataCampDao xlsDatacampDao;
    @Autowired
    private GcRawSkuDao gcRawSkuDao;
    @Autowired
    private FileDataProcessRequestDao fileDataProcessRequestDao;
    private CategoryInfoProducer categoryInfoProducer;
    private ConvertRawSkuToDatacampOffersTaskAction convertTaskAction;
    private ConvertRawSkuToDataCampOffersService convertService;

    @Before
    public void setUp() {
        super.setUp();

        categoryInfoProducer = Mockito.mock(CategoryInfoProducer.class);
        Mockito.when(categoryInfoProducer.extractCategoryInfo(anyLong(), any()))
            .thenReturn(CategoryInfo.newBuilder()
                .setMainParamCreator(new MainParamCreator(ImportContentType.DCP_EXCEL))
                .setId(1L)
                .build(ImportContentType.DCP_EXCEL));
        CategoryDataHelper categoryDataHelper = new CategoryDataHelper(new CategoryDataKnowledgeMock(), new BookCategoryHelper());
        convertService = new ConvertRawSkuToDataCampOffersService(categoryInfoProducer, categoryDataHelper, true);
        convertTaskAction = new ConvertRawSkuToDatacampOffersTaskAction(xlsDatacampDao, gcRawSkuDao,
            convertService, businessIdXlsExtractor, fileDataProcessRequestDao);
    }

    @Test
    public void shouldConvertOneRawSku() {
        List<GcRawSku> gcRawSkus = generateGcRawSkus(1);
        gcRawSkuDao.markValidForOfferUpload(gcRawSkus);

        convertTaskAction.apply(new ProcessFileCountedData(requestId, processId, 1, 1));

        List<XlsDatacampOffer> offers = xlsDatacampDao.findAll();

        assertEquals("One offer", 1, offers.size());

        XlsDatacampOffer offer = offers.get(0);
        assertEquals("Status", XlsLogbrokerStatus.NEW, offer.getStatus());
        assertNotNull("Created date", offer.getCreateDate());
        assertNotNull("Updated date", offer.getUpdateDate());
        assertEquals("Created and updated date is equals", offer.getCreateDate(), offer.getUpdateDate());
        assertNotNull("Data Camp offer", offer.getDatacampOffer());
    }

    @Test
    public void shouldConvertMoreThanOneRawSku() {
        List<GcRawSku> gcRawSkus1 = generateGcRawSkus(10);
        gcRawSkuDao.markValidForOfferUpload(gcRawSkus1);
        List<GcRawSku> gcRawSkus2 = generateGcRawSkus(10, createFileProcessId(requestId));
        gcRawSkuDao.markValidForOfferUpload(gcRawSkus2);

        convertTaskAction.apply(new ProcessFileCountedData(requestId, processId, 1, 1));

        List<XlsDatacampOffer> offers = xlsDatacampDao.findAll();

        assertThat(offers)
            .hasSize(10)
            .extracting(
                XlsDatacampOffer::getCreateDate,
                XlsDatacampOffer::getUpdateDate,
                XlsDatacampOffer::getDatacampOffer
            )
            .doesNotContainNull();

        assertThat(offers)
            .allMatch(o -> o.getStatus().equals(XlsLogbrokerStatus.NEW));
    }


    @Test
    public void shouldBeCorrectDataCampOfferIdentifiers() {
        List<GcRawSku> gcRawSkus = generateGcRawSkus(1);
        gcRawSkuDao.markValidForOfferUpload(gcRawSkus);
        RawSku rawSku = gcRawSkus.get(0).getData();

        convertTaskAction.apply(new ProcessFileCountedData(requestId, processId, 1, 1));

        List<XlsDatacampOffer> offers = xlsDatacampDao.findAll();
        assertEquals("One offer", 1, offers.size());

        DataCampOffer.Offer datacampOffer = getFirstDatacampOffer(offers);

        assertEquals(rawSku.getShopSku(), datacampOffer.getIdentifiers().getOfferId());
        assertEquals(BUSINESS_ID, datacampOffer.getIdentifiers().getBusinessId());
    }

    @Test
    public void shouldBeCorrectDataCampOfferPictures() {
        List<GcRawSku> gcRawSkus = generateGcRawSkus(1, (rawSkus -> {
            RawSku rawSku = rawSkus.get(0);
            rawSku.setMainPictureUrl("main_url");
            rawSku.setOtherPictureUrlList(Arrays.asList("other_1_url", "other_2_url"));
        }));
        gcRawSkuDao.markValidForOfferUpload(gcRawSkus);

        convertTaskAction.apply(new ProcessFileCountedData(requestId, processId, 1, 1));

        List<XlsDatacampOffer> offers = xlsDatacampDao.findAll();
        assertEquals("One offer", 1, offers.size());

        DataCampOffer.Offer datacampOffer = getFirstDatacampOffer(offers);

        assertEquals("main_url",
            datacampOffer.getPictures().getPartner().getOriginal().getSourceList().get(0).getUrl());
        assertEquals("other_1_url",
            datacampOffer.getPictures().getPartner().getOriginal().getSourceList().get(1).getUrl());
        assertEquals("other_2_url",
            datacampOffer.getPictures().getPartner().getOriginal().getSourceList().get(2).getUrl());
    }

    @Test
    public void shouldBeCorrectDataCampOfferPartnerContent() {
        List<GcRawSku> gcRawSkus = generateGcRawSkus(1, (rawSkus -> {
            rawSkus.get(0).setRawParamValues(Arrays.asList(
                new RawParamValue(ParameterValueComposer.NAME_ID, "name", "name_value"),
                new RawParamValue(MainParamCreator.DESCRIPTION_ID, "desc", "desc_value"),
                new RawParamValue(ParameterValueComposer.VENDOR_ID, "vendor", "vendor_value"),
                new RawParamValue(ParameterValueComposer.VENDOR_CODE_ID, "vendor_code", "vendor_code_value"),
                new RawParamValue(ParameterValueComposer.BARCODE_ID, "barcode", "797266714467")
            ));
        }));
        gcRawSkuDao.markValidForOfferUpload(gcRawSkus);
        RawSku rawSku = gcRawSkus.get(0).getData();

        convertTaskAction.apply(new ProcessFileCountedData(requestId, processId, 1, 1));

        List<XlsDatacampOffer> offers = xlsDatacampDao.findAll();
        assertEquals("One offer", 1, offers.size());

        DataCampOffer.Offer datacampOffer = getFirstDatacampOffer(offers);

        DataCampOfferContent.OriginalSpecification original =
            datacampOffer.getContent().getPartner().getOriginal();

        assertEquals(Integer.parseInt(rawSku.getGroupId()), original.getGroupId().getValue());
        assertEquals(rawSku.getGroupName(), original.getGroupName().getValue());

        assertEquals(getRawValue(rawSku, ParameterValueComposer.NAME_ID), original.getName().getValue());
        assertEquals(getRawValue(rawSku, MainParamCreator.DESCRIPTION_ID), original.getDescription().getValue());
        assertEquals(getRawValue(rawSku, ParameterValueComposer.VENDOR_ID), original.getVendor().getValue());
        assertEquals(getRawValue(rawSku, ParameterValueComposer.VENDOR_CODE_ID), original.getVendorCode().getValue());
        assertEquals(getRawValue(rawSku, ParameterValueComposer.BARCODE_ID), original.getBarcode().getValue(0));
    }

    @Test
    public void shouldBeCorrectDataCampStringParameter() {
        final long paramId = 1;
        final String paramName = "param_name";
        final String paramValue = "param_value";
        Mockito.when(categoryInfoProducer.extractCategoryInfo(anyLong(), any()))
            .thenReturn(CategoryInfo.newBuilder()
                .setMainParamCreator(new MainParamCreator(ImportContentType.DCP_EXCEL))
                .addParameter(
                    ParameterInfoBuilder.asString()
                        .setId(paramId)
                        .setXslName(paramName)
                        .setImportContentType(ImportContentType.DCP_EXCEL)
                        .build()
                    )
                .setId(1L)
                .build(ImportContentType.DCP_EXCEL));

        List<GcRawSku> gcRawSkus = generateGcRawSkus(1, rawSkus ->
            rawSkus.get(0).setRawParamValues(Collections.singletonList(
                new RawParamValue(paramId, paramName, paramValue)
            ))
        );
        gcRawSkuDao.markValidForOfferUpload(gcRawSkus);
        RawSku rawSku = gcRawSkus.get(0).getData();

        convertTaskAction.apply(new ProcessFileCountedData(requestId, processId, 1, 1));

        List<XlsDatacampOffer> offers = xlsDatacampDao.findAll();
        DataCampOffer.Offer datacampOffer = getFirstDatacampOffer(offers);
        DataCampOfferMarketContent.MarketParameterValues parameters = getParameterValues(datacampOffer);

        assertEquals(1, parameters.getParameterValuesCount());
        assertEquals(paramId, parameters.getParameterValues(0).getParamId());
        assertEquals(DataCampContentMarketParameterValue.MarketValueType.STRING,
            getParameterValue(parameters).getValueType());
        assertEquals(getRawValue(rawSku, paramId), getParameterValue(parameters).getStrValue());
    }

    @Test
    public void shouldBeCorrectDataCampNumericParameter() {
        final long paramId = 1;
        final String paramName = "param_name";
        final String paramValue = "123";
        Mockito.when(categoryInfoProducer.extractCategoryInfo(anyLong(), any()))
            .thenReturn(CategoryInfo.newBuilder()
                .setMainParamCreator(new MainParamCreator(ImportContentType.DCP_EXCEL))
                .addParameter(
                    ParameterInfoBuilder.asNumeric()
                        .setId(paramId)
                        .setXslName(paramName)
                        .setImportContentType(ImportContentType.DCP_EXCEL)
                        .build()
                )
                .setId(1L)
                .build(ImportContentType.DCP_EXCEL));

        List<GcRawSku> gcRawSkus = generateGcRawSkus(1, rawSkus ->
            rawSkus.get(0).setRawParamValues(Collections.singletonList(
                new RawParamValue(paramId, paramName, paramValue)
            ))
        );
        gcRawSkuDao.markValidForOfferUpload(gcRawSkus);
        RawSku rawSku = gcRawSkus.get(0).getData();

        convertTaskAction.apply(new ProcessFileCountedData(requestId, processId, 1, 1));

        List<XlsDatacampOffer> offers = xlsDatacampDao.findAll();
        DataCampOffer.Offer datacampOffer = getFirstDatacampOffer(offers);
        DataCampOfferMarketContent.MarketParameterValues parameters = getParameterValues(datacampOffer);

        assertEquals(1, parameters.getParameterValuesCount());
        assertEquals(paramId, parameters.getParameterValues(0).getParamId());
        assertEquals(DataCampContentMarketParameterValue.MarketValueType.NUMERIC,
            getParameterValue(parameters).getValueType());
        assertEquals(getRawValue(rawSku, paramId), getParameterValue(parameters).getNumericValue());
    }

    @Test
    public void shouldBeCorrectDataCampEnumParameter() {
        final long paramId = 1;
        final long optionId = 101;
        final String paramName = "enum_param_name";
        final String paramValue = "valid_option";

        Mockito.when(categoryInfoProducer.extractCategoryInfo(anyLong(), any()))
            .thenReturn(CategoryInfo.newBuilder()
                .setMainParamCreator(new MainParamCreator(ImportContentType.DCP_EXCEL))
                .addParameter(createEnumParameter(paramId, paramName, optionId, paramValue))
                .setId(1L)
                .build(ImportContentType.DCP_EXCEL));

        List<GcRawSku> gcRawSkus = generateGcRawSkus(1, rawSkus ->
            rawSkus.get(0).setRawParamValues(Collections.singletonList(
                new RawParamValue(paramId, paramName, paramValue)
            ))
        );
        gcRawSkuDao.markValidForOfferUpload(gcRawSkus);

        convertTaskAction.apply(new ProcessFileCountedData(requestId, processId, 1, 1));

        List<XlsDatacampOffer> offers = xlsDatacampDao.findAll();
        DataCampOffer.Offer datacampOffer = getFirstDatacampOffer(offers);
        DataCampOfferMarketContent.MarketParameterValues parameters = getParameterValues(datacampOffer);

        assertEquals(1, parameters.getParameterValuesCount());
        assertEquals(paramId, parameters.getParameterValues(0).getParamId());
        assertEquals(DataCampContentMarketParameterValue.MarketValueType.ENUM,
            getParameterValue(parameters).getValueType());
        assertEquals(paramValue, getParameterValue(parameters).getStrValue());
        assertEquals(optionId, getParameterValue(parameters).getOptionId());
    }

    @Test
    public void shouldBeCorrectDataCampNumericEnumParameter() {
        final long paramId = 1;
        final long optionId = 101;
        final String paramName = "enum_param_name";
        final Double paramValue = 3.14;

        Mockito.when(categoryInfoProducer.extractCategoryInfo(anyLong(), any()))
            .thenReturn(CategoryInfo.newBuilder()
                .setMainParamCreator(new MainParamCreator(ImportContentType.DCP_EXCEL))
                .addParameter(createNumericEnumParameter(paramId, paramName, optionId, paramValue))
                .setId(1L)
                .build(ImportContentType.DCP_EXCEL));

        List<GcRawSku> gcRawSkus = generateGcRawSkus(1, rawSkus ->
            rawSkus.get(0).setRawParamValues(Collections.singletonList(
                new RawParamValue(paramId, paramName, paramValue.toString())
            ))
        );
        gcRawSkuDao.markValidForOfferUpload(gcRawSkus);

        convertTaskAction.apply(new ProcessFileCountedData(requestId, processId, 1, 1));

        List<XlsDatacampOffer> offers = xlsDatacampDao.findAll();
        DataCampOffer.Offer datacampOffer = getFirstDatacampOffer(offers);
        DataCampOfferMarketContent.MarketParameterValues parameters = getParameterValues(datacampOffer);

        assertEquals(1, parameters.getParameterValuesCount());
        assertEquals(paramId, parameters.getParameterValues(0).getParamId());
        assertEquals(DataCampContentMarketParameterValue.MarketValueType.NUMERIC_ENUM,
            getParameterValue(parameters).getValueType());
        assertEquals(paramValue.toString(), getParameterValue(parameters).getStrValue());
        assertEquals(optionId, getParameterValue(parameters).getOptionId());
    }

    @Test
    public void shouldBeCorrectDataCampHypothesisParameterForEnum() {
        final long paramId = 1;
        final long optionId = 101;
        final String paramName = "enum_param_name";
        final String paramValue = "invalid_option";

        Mockito.when(categoryInfoProducer.extractCategoryInfo(anyLong(), any()))
            .thenReturn(CategoryInfo.newBuilder()
                .setMainParamCreator(new MainParamCreator(ImportContentType.DCP_EXCEL))
                .addParameter(createEnumParameter(paramId, paramName, optionId, "valid option"))
                .setId(1L)
                .build(ImportContentType.DCP_EXCEL));

        List<GcRawSku> gcRawSkus = generateGcRawSkus(1, rawSkus ->
            rawSkus.get(0).setRawParamValues(Collections.singletonList(
                new RawParamValue(paramId, paramName, paramValue)
            ))
        );
        gcRawSkuDao.markValidForOfferUpload(gcRawSkus);

        convertTaskAction.apply(new ProcessFileCountedData(requestId, processId, 1, 1));

        List<XlsDatacampOffer> offers = xlsDatacampDao.findAll();
        DataCampOffer.Offer datacampOffer = getFirstDatacampOffer(offers);
        DataCampOfferMarketContent.MarketParameterValues parameters = getParameterValues(datacampOffer);

        assertEquals(1, parameters.getParameterValuesCount());
        assertEquals(paramId, parameters.getParameterValues(0).getParamId());
        assertEquals(DataCampContentMarketParameterValue.MarketValueType.HYPOTHESIS,
            getParameterValue(parameters).getValueType());
        assertEquals(paramValue, getParameterValue(parameters).getStrValue());
    }

    @Test
    public void shouldBeCorrectDataCampHypothesisParameterForNumericEnum() {
        final long paramId = 1;
        final long optionId = 101;
        final String paramName = "enum_param_name";
        final Double paramValue = 3.14;

        Mockito.when(categoryInfoProducer.extractCategoryInfo(anyLong(), any()))
            .thenReturn(CategoryInfo.newBuilder()
                .setMainParamCreator(new MainParamCreator(ImportContentType.DCP_EXCEL))
                .addParameter(createNumericEnumParameter(paramId, paramName, optionId, paramValue * 2))
                .setId(1L)
                .build(ImportContentType.DCP_EXCEL));

        List<GcRawSku> gcRawSkus = generateGcRawSkus(1, rawSkus ->
            rawSkus.get(0).setRawParamValues(Collections.singletonList(
                new RawParamValue(paramId, paramName, paramValue.toString())
            ))
        );
        gcRawSkuDao.markValidForOfferUpload(gcRawSkus);

        convertTaskAction.apply(new ProcessFileCountedData(requestId, processId, 1, 1));

        List<XlsDatacampOffer> offers = xlsDatacampDao.findAll();
        DataCampOffer.Offer datacampOffer = getFirstDatacampOffer(offers);
        DataCampOfferMarketContent.MarketParameterValues parameters = getParameterValues(datacampOffer);

        assertEquals(1, parameters.getParameterValuesCount());
        assertEquals(paramId, parameters.getParameterValues(0).getParamId());
        assertEquals(DataCampContentMarketParameterValue.MarketValueType.HYPOTHESIS,
            getParameterValue(parameters).getValueType());
        assertEquals(paramValue.toString(), getParameterValue(parameters).getStrValue());
    }

    @Test
    public void shouldHaveRequestTimeAsDatacampOfferUpdatedTime() {
        List<GcRawSku> gcRawSkus = generateGcRawSkus(1, (rawSkus -> {
            rawSkus.get(0).setRawParamValues(Collections.singletonList(
                new RawParamValue(ParameterValueComposer.NAME_ID, "name", "name_value")
            ));
        }));
        gcRawSkuDao.markValidForOfferUpload(gcRawSkus);

        convertTaskAction.apply(new ProcessFileCountedData(requestId, processId, 1, 1));

        DataCampOffer.Offer datacampOffer = getFirstDatacampOffer(xlsDatacampDao.findAll());

        long datacampSeconds = datacampOffer.getContent()
            .getPartner().getOriginal().getName().getMeta().getTimestamp().getSeconds();
        long requestSeconds = TimeUnit.MILLISECONDS.toSeconds(
            fileDataProcessRequestDao.fetchOneByFileDataProcessRequestId(requestId).getCreateTime().getTime()
        );
        assertNotEquals(0, requestSeconds);
        assertNotEquals(0, datacampSeconds);
        assertEquals("Created date as request created date", requestSeconds, datacampSeconds);
    }

    @Test
    public void shouldHaveCorrectCategory() {
        List<GcRawSku> gcRawSkus = generateGcRawSkus(1);
        gcRawSkuDao.markValidForOfferUpload(gcRawSkus);
        RawSku rawSku = gcRawSkus.get(0).getData();

        convertTaskAction.apply(new ProcessFileCountedData(requestId, processId, 1, 1));

        List<XlsDatacampOffer> offers = xlsDatacampDao.findAll();
        assertEquals("One offer", 1, offers.size());

        DataCampOffer.Offer datacampOffer = getFirstDatacampOffer(offers);

        assertEquals(
            Math.toIntExact(rawSku.getCategoryId()),
            datacampOffer.getContent().getBinding().getPartner().getMarketCategoryId());
    }


    @Test
    public void processUnitedSizeAsHypothesis() {
        Mockito.when(categoryInfoProducer.extractCategoryInfo(anyLong(), any()))
                .thenReturn(CategoryInfo.newBuilder()
                        .setMainParamCreator(new MainParamCreator(ImportContentType.DCP_EXCEL))
                        .addParameter(createEnumParameter(KnownParameters.UNITED_SIZE.getId(),
                                KnownParameters.UNITED_SIZE.getXslName(),
                                999, "size1"))
                        .setId(1L)
                        .build(ImportContentType.DCP_EXCEL));

        List<GcRawSku> gcRawSkus = generateGcRawSkus(1, rawSkus ->
                rawSkus.get(0).setRawParamValues(Collections.singletonList(
                        new RawParamValue(KnownParameters.UNITED_SIZE.getId(),
                                KnownParameters.UNITED_SIZE.getXslName(), "size1")
                ))
        );
        gcRawSkuDao.markValidForOfferUpload(gcRawSkus);

        convertTaskAction.apply(new ProcessFileCountedData(requestId, processId, 1, 1));

        List<XlsDatacampOffer> offers = xlsDatacampDao.findAll();

        assertEquals("One offer", 1, offers.size());

        List<MarketParameterValue> parameterValuesList =
                offers.get(0).getDatacampOffer().getContent()
                        .getPartner()
                        .getMarketSpecificContent()
                        .getParameterValues()
                        .getParameterValuesList();
        assertThat(parameterValuesList).hasSize(1);
        MarketParameterValue unitedSize = parameterValuesList.get(0);
        assertThat(unitedSize).extracting(MarketParameterValue::getParamId)
                .isEqualTo(KnownParameters.UNITED_SIZE.getId());
        assertThat(unitedSize).extracting(v -> v.getValue().getStrValue())
                .isEqualTo("size1");
        assertThat(unitedSize).extracting(v -> v.getValue().getValueType().getNumber())
                .isEqualTo(MarketParameters.MarketValueType.HYPOTHESIS.getNumber());
    }

    private DataCampContentMarketParameterValue.MarketValue getParameterValue(
        DataCampOfferMarketContent.MarketParameterValues parameters
    ) {
        return parameters.getParameterValues(0).getValue();
    }

    private DataCampOfferMarketContent.MarketParameterValues getParameterValues(DataCampOffer.Offer datacampOffer) {
        return datacampOffer.getContent().getPartner().getMarketSpecificContent().getParameterValues();
    }

    private DataCampOffer.Offer getFirstDatacampOffer(List<XlsDatacampOffer> offers) {
        return offers.get(0).getDatacampOffer();
    }

    private String getRawValue(RawSku rawSku, long paramId) {
        return rawSku.getRawParamValues().stream()
            .filter(rawParam -> rawParam.getParamId().equals(paramId))
            .map(RawParamValue::getValue)
            .findFirst().get();
    }

    private ParameterInfo createEnumParameter(long paramId, String paramName, long optionId, String paramValue) {
        MboParameters.Parameter parameter = MboParameters.Parameter.newBuilder()
            .setId(paramId)
            .setXslName(paramName)
            .setValueType(MboParameters.ValueType.ENUM)
            .addOption(MboParameters.Option.newBuilder()
                .setId(optionId)
                .addName(MboParameters.Word.newBuilder().setLangId(225).setName(paramValue)))
            .build();
        MboParameterWrapper mboParameterWrapper = new MboParameterWrapper(parameter);
        return ParameterInfoBuilder
            .asMboParameter(mboParameterWrapper)
            .setImportContentType(ImportContentType.DCP_EXCEL)
            .build();
    }

    private ParameterInfo createNumericEnumParameter(long paramId, String paramName, long optionId, Double paramValue) {
        MboParameters.Parameter parameter = MboParameters.Parameter.newBuilder()
            .setId(paramId)
            .setXslName(paramName)
            .setValueType(MboParameters.ValueType.NUMERIC_ENUM)
            .addOption(MboParameters.Option.newBuilder()
                .setId(optionId)
                .addName(MboParameters.Word.newBuilder().setLangId(225).setName(paramValue.toString())))
            .build();
        MboParameterWrapper mboParameterWrapper = new MboParameterWrapper(parameter);
        return ParameterInfoBuilder
            .asMboParameter(mboParameterWrapper)
            .setImportContentType(ImportContentType.DCP_EXCEL)
            .build();
    }
}

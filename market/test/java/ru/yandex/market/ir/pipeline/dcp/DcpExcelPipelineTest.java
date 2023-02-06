package ru.yandex.market.ir.pipeline.dcp;

import Market.DataCamp.API.DatacampMessageOuterClass.DatacampMessage;
import Market.DataCamp.DataCampContentMarketParameterValue;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMarketContent;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampOfferPictures;
import Market.DataCamp.DataCampUnitedOffer;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import ru.yandex.market.extractor.ExtractorConfig;
import ru.yandex.market.gutgin.tms.assertions.GutginAssertions;
import ru.yandex.market.gutgin.tms.service.OfferContentStateMbocApiServiceMock;
import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration.common.rating.SkuRatingEvaluator;
import ru.yandex.market.ir.autogeneration_api.export.excel.DcpSkuDataGenerator;
import ru.yandex.market.ir.config.DcpExcelPipelineTestConfig;
import ru.yandex.market.ir.excel.TestExcelFileGenerator;
import ru.yandex.market.ir.excel.generator.CategoryInfo;
import ru.yandex.market.ir.excel.generator.PartnerContentConverter;
import ru.yandex.market.ir.excel.generator.XlsRow;
import ru.yandex.market.ir.excel.generator.param.MainParamCreator;
import ru.yandex.market.ir.http.PartnerContent;
import ru.yandex.market.ir.pipeline.BasePipelineTest;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.logbroker.event.LogbrokerEvent;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.partner.content.common.csku.judge.Judge;
import ru.yandex.market.partner.content.common.db.dao.FileProcessDao;
import ru.yandex.market.partner.content.common.db.dao.FileProcessMessageService;
import ru.yandex.market.partner.content.common.db.jooq.enums.FileProcessMessageType;
import ru.yandex.market.partner.content.common.db.jooq.enums.MrgrienPipelineStatus;
import ru.yandex.market.partner.content.common.db.jooq.enums.PipelineType;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.FileProcess;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.Pipeline;
import ru.yandex.market.partner.content.common.message.MessageInfo;
import ru.yandex.market.partner.content.common.service.mock.DataCampServiceMock;
import ru.yandex.market.robot.db.ParameterValueComposer;
import ru.yandex.utils.Pair;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ContextConfiguration(classes = DcpExcelPipelineTestConfig.class)
@TestPropertySource(properties = {"categoryDataRefreshersCount=1000000"})
public class DcpExcelPipelineTest extends BasePipelineTest {

    private static final Integer BUSINESS_ID = PARTNER_SHOP_ID;
    private static final String SSKU_NAME = "Some test offer";
    private static final String SSKU_DESCRIPTION = "For roundtrip testing";
    private static final String VENDOR = "Vendor name";
    private static final String VENDORCODE = "1111";
    private static final DataCampOfferMeta.StringListValue BARCODES = DataCampOfferMeta.StringListValue.newBuilder()
        .addValue("797266714467")
        .build();
    private static final int GROUP_ID = 1234;
    private static final String GROUP_NAME = "Группа";

    private static final long NUM_PARAM_ID = 99991L;
    private static final long MULTIVALUE_PARAM_ID = 99992L;
    private static final long BOOL_PARAM_ID = 99993L;
    private static final long NUMERIC_ENUM_PARAM_ID = 99994L;
    private static final long ENUM_PARAM_ID = 99995L;
    public static final String NUMERIC_PARAM_NAME = "Numeric";
    public static final String MULTIVALUE_PARAM_NAME = "MultivalueEnum";
    public static final String BOOLEAN_PARAM_NAME = "Boolean";
    public static final String ENUM_PARAM_NAME = "Enum";
    public static final String NUMERIC_ENUM_PARAM_NAME = "NumericEnum";

    @Resource
    private DataCampServiceMock dataCampServiceMock;

    @Resource
    private OfferContentStateMbocApiServiceMock offerContentStateMbocApiServiceMock;

    @Resource
    private CategoryDataHelper categoryDataHelper;

    @Resource(name = "offerLogbrokerService")
    private LogbrokerService offerLogbrokerService;

    @Resource
    private FileProcessDao fileProcessDao;

    @Resource
    private SkuRatingEvaluator skuRatingEvaluator;

    @Resource
    private FileProcessMessageService fileProcessMessageService;

    @Resource
    private ModelStorageHelper modelStorageHelper;

    @Before
    @Override
    public void setUp() {
        super.setUp();
        Mockito.reset(offerLogbrokerService);
    }


    @Test
    public void happyPath() {
        List<String> pictureUrls = IntStream.rangeClosed(1, 4)
            .mapToObj(i -> String.format("http://dcp-excel/picture-%d.jpg", i))
            .collect(Collectors.toList());

        final String offerId = "SSKU1";
        CategoryData categoryData = categoryDataHelper.getCategoryData(CATEGORY1_ID);
        DataCampOffer.Offer testOffer = createTestOffer(categoryData, offerId, pictureUrls);
        dataCampServiceMock.setOffersForBusinessId(BUSINESS_ID, testOffer);
        offerContentStateMbocApiServiceMock.setAllowModelCreateUpdate(offerId, true);
        offerContentStateMbocApiServiceMock.setCategoryIdFromUc(offerId, CATEGORY1_ID);

        PartnerContent.ProcessRequest request = createRequest();
        Pipeline pipeline = runPipeline(testOffer);

        GutginAssertions.assertThat(pipeline).hasStatus(MrgrienPipelineStatus.FINISHED);
        assertAllTasksFinished(pipeline.getId());

        List<MessageInfo> validationErrors = getValidationErrors(request.getProcessRequestId());
        assertThat(validationErrors).isEmpty();

        List<DataCampOffer.Offer> offers = getOffersForDatacamp();
        assertThat(offers).hasSize(1);
        DataCampOffer.Offer parsedOffer = offers.get(0);
        assertOfferContentIsTheSame(parsedOffer, testOffer, pictureUrls);
    }

    @Test
    public void duplicateShopSku() {
        List<String> pictureUrls = IntStream.rangeClosed(1, 4)
            .mapToObj(i -> String.format("http://dcp-excel/picture-%d.jpg", i))
            .collect(Collectors.toList());

        final String offerId = "SSKU1";
        CategoryData categoryData = categoryDataHelper.getCategoryData(CATEGORY1_ID);
        DataCampOffer.Offer testOffer = createTestOffer(categoryData, offerId, pictureUrls);
        dataCampServiceMock.setOffersForBusinessId(BUSINESS_ID, testOffer);
        offerContentStateMbocApiServiceMock.setAllowModelCreateUpdate(offerId, true);
        offerContentStateMbocApiServiceMock.setCategoryIdFromUc(offerId, CATEGORY1_ID);

        DcpSkuDataGenerator dcpSkuDataGenerator = new DcpSkuDataGenerator(
            dataCampServiceMock, sourceDao,
            new PartnerContentConverter(categoryDataHelper), modelStorageHelper, new Judge(),
                skuRatingEvaluator);

        PartnerContent.ProcessRequest request = createRequest();
        Pipeline pipeline = runPipeline(dcpSkuDataGenerator, ImmutableList.of(offerId, offerId));

        GutginAssertions.assertThat(pipeline).hasStatus(MrgrienPipelineStatus.FINISHED);
        assertAllTasksFinished(pipeline.getId());

        List<MessageInfo> validationErrors = getValidationErrors(request.getProcessRequestId());
        assertThat(validationErrors)
            .hasSize(1)
            .extracting(MessageInfo::getCode)
            .containsExactly("ir.partner_content.dcp.excel.validation.duplicateShopSku");

        verify(offerLogbrokerService, Mockito.never()).publishEvent(any());
    }

    @Test
    public void emptyShopSku() {
        List<String> pictureUrls = IntStream.rangeClosed(1, 4)
            .mapToObj(i -> String.format("http://dcp-excel/picture-%d.jpg", i))
            .collect(Collectors.toList());

        final String shopSku = "SSKU1";
        CategoryData categoryData = categoryDataHelper.getCategoryData(CATEGORY1_ID);
        DataCampOffer.Offer testOffer = createTestOffer(categoryData, shopSku, pictureUrls);
        dataCampServiceMock.setOffersForBusinessId(BUSINESS_ID, testOffer);

        // при генерации excel подменяем значение в первой колонке (shop sku) на пустую строку
        DcpSkuDataGenerator realDataGenerator = new DcpSkuDataGenerator(dataCampServiceMock, sourceDao,
            new PartnerContentConverter(categoryDataHelper), modelStorageHelper, new Judge(),
                skuRatingEvaluator);
        DcpSkuDataGenerator mockedDataGenerator = Mockito.mock(DcpSkuDataGenerator.class);
        Mockito.when(mockedDataGenerator.generateRows(eq(SOURCE_ID), any(), any(), anyList(), eq(false)))
                .thenAnswer(invocation -> {
                    CategoryInfo categoryInfo = invocation.getArgument(1);
                    CategoryData category = invocation.getArgument(2);
                    List<String> shopSkus = invocation.getArgument(3);
                    List<XlsRow> result = realDataGenerator.generateRows(
                            SOURCE_ID, categoryInfo, category, shopSkus, false);
                    result.get(0).getColumns().add(new XlsRow.XlsCol(0, "", true));
                    return result;
                });

        PartnerContent.ProcessRequest request = createRequest();
        Pipeline pipeline = runPipeline(mockedDataGenerator, Collections.singletonList(shopSku));

        GutginAssertions.assertThat(pipeline).hasStatus(MrgrienPipelineStatus.FINISHED);
        assertAllTasksFinished(pipeline.getId());

        List<MessageInfo> validationErrors = getValidationErrors(request.getProcessRequestId());
        assertThat(validationErrors)
            .hasSize(1)
            .extracting(MessageInfo::getCode)
            .containsExactly("ir.partner_content.dcp.excel.validation.noShopSkuInFile");

        verify(offerLogbrokerService, Mockito.never()).publishEvent(any());
    }

    @Test
    public void emptyFile() {
        PartnerContent.ProcessRequest request = createRequest();
        Pipeline pipeline = runPipeline();

        GutginAssertions.assertThat(pipeline).hasStatus(MrgrienPipelineStatus.FINISHED);
        assertAllTasksFinished(pipeline.getId());

        List<MessageInfo> validationErrors = getValidationErrors(request.getProcessRequestId());
        assertThat(validationErrors).isEmpty();

        verify(offerLogbrokerService, Mockito.never()).publishEvent(any());
    }


    @Override
    protected MboParameters.Category.Builder buildCategory() {
        return MboParameters.Category.newBuilder()
            .setHid(CATEGORY1_ID)
            .setLeaf(true)
            .addName(word("dcp-roundtrip-test"))
            .addUniqueName(word("dcp-roundtrip-test"))
            .addParameter(createParameter(
                ParameterValueComposer.VENDOR_ID, ParameterValueComposer.VENDOR, MboParameters.ValueType.ENUM)
                .setService(true)
                .addOption(createOption(1, "vendor 1"))
                .addOption(createOption(2, "vendor 2"))
            )
            .addParameter(
                createParameter(ParameterValueComposer.NAME_ID, ParameterValueComposer.NAME, MboParameters.ValueType.STRING)
                    .setService(true)
            )
            .addParameter(
                createParameter(MainParamCreator.DESCRIPTION_ID, "Description", MboParameters.ValueType.STRING)
                    .setService(true)
            )
            .addParameter(
                createParameter(ParameterValueComposer.BARCODE_ID, ParameterValueComposer.BARCODE, MboParameters.ValueType.STRING)
                    .setService(true)
                    .setMultivalue(true)
            )
            .addParameter(
                createParameter(ParameterValueComposer.VENDOR_CODE_ID, ParameterValueComposer.VENDOR_CODE, MboParameters.ValueType.STRING)
                    .setService(true)
            )
            .addParameter(
                createParameter(NUM_PARAM_ID, NUMERIC_PARAM_NAME, MboParameters.ValueType.NUMERIC))
            .addParameter(
                createParameter(ENUM_PARAM_ID, ENUM_PARAM_NAME, MboParameters.ValueType.ENUM)
                    .addOption(createOption(1, "value 1"))
                    .addOption(createOption(2, "value 2"))
            )
            .addParameter(
                createParameter(MULTIVALUE_PARAM_ID, MULTIVALUE_PARAM_NAME, MboParameters.ValueType.ENUM)
                    .addOption(createOption(1, "value 1"))
                    .addOption(createOption(2, "value 2"))
                    .setMultivalue(true)
            )
            .addParameter(
                createParameter(BOOL_PARAM_ID, BOOLEAN_PARAM_NAME, MboParameters.ValueType.BOOLEAN))
            .addParameter(
                createParameter(NUMERIC_ENUM_PARAM_ID, NUMERIC_ENUM_PARAM_NAME, MboParameters.ValueType.NUMERIC_ENUM)
                    .addOption(createOption(1, "99.5"))
                    .addOption(createOption(2, "199"))
            );
    }

    private PartnerContent.ProcessRequest createRequest() {
        return partnerContentFileService.addFile(
            PartnerContent.FileSource.newBuilder()
                .setFileContentType(PartnerContent.FileContentType.DCP_XLS)
                .setSourceId(SOURCE_ID)
                .setBusinessId(BUSINESS_ID)
                .setUrl(FILE_URL)
                .setIsDynamic(false)
                .build(),
            false
        );
    }

    private MboParameters.Word word(String value) {
        return MboParameters.Word.newBuilder()
            .setLangId(ExtractorConfig.Language.RUSSIAN_VALUE)
            .setName(value)
            .build();
    }

    private MboParameters.Parameter.Builder createParameter(long numParamId, String name, MboParameters.ValueType valueType) {
        return MboParameters.Parameter.newBuilder()
            .setId(numParamId).setXslName(name)
            .addName(word(name))
            .setValueType(valueType);
    }

    private MboParameters.Option createOption(int id, String name) {
        return MboParameters.Option.newBuilder()
            .setId(id)
            .addName(word(name))
            .build();
    }

    private DataCampOffer.Offer createTestOffer(CategoryData category, String offerId, List<String> pictureUrls) {

        // генерируем значения для всех категорийных параметров
        List<DataCampContentMarketParameterValue.MarketParameterValue> parameterValues =
            category.getParameterList().stream()
                .filter(param -> !param.getService())
                .map(param -> {
                    DataCampContentMarketParameterValue.MarketParameterValue.Builder builder =
                        DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                            .setParamId(param.getId())
                            .setParamName(param.getXslName());
                    switch (param.getValueType()) {
                        case ENUM:
                            builder.setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                .setValueType(DataCampContentMarketParameterValue.MarketValueType.ENUM)
                                .setOptionId(param.getOption(0).getId())
                                .setStrValue(param.getOption(0).getName(0).getName()));
                            break;
                        case NUMERIC_ENUM:
                            builder.setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                .setValueType(DataCampContentMarketParameterValue.MarketValueType.NUMERIC_ENUM)
                                .setOptionId(param.getOption(0).getId())
                                .setStrValue(param.getOption(0).getName(0).getName()));
                            break;
                        case BOOLEAN:
                            builder.setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                .setValueType(DataCampContentMarketParameterValue.MarketValueType.BOOLEAN)
                                .setBoolValue(true));
                            break;
                        case NUMERIC:
                            builder.setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                .setValueType(DataCampContentMarketParameterValue.MarketValueType.NUMERIC)
                                .setNumericValue("100500"));
                            break;
                        case STRING:
                            builder.setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                                .setValueType(DataCampContentMarketParameterValue.MarketValueType.STRING)
                                .setStrValue("Value"));
                            break;
                        default:
                            throw new RuntimeException("Unexpected param type - " + param.getValueType());
                    }
                    return builder.build();
                })
                .collect(Collectors.toList());

        // для multivalue параметров пишем два значения
        parameterValues.addAll(category.getParameterList().stream()
            .filter(p -> p.getMultivalue() && !p.getService() && p.getOptionCount() > 1)
            .map(param -> DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
                .setParamId(param.getId())
                .setParamName(param.getXslName())
                .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                    .setValueType(DataCampContentMarketParameterValue.MarketValueType.ENUM)
                    .setOptionId(param.getOption(1).getId())
                    .setStrValue(param.getOption(1).getName(0).getName())
                )
                .build())
            .collect(Collectors.toList()));

        // картинки - первые 2 делаем DIRECT_LINK, остальные UPLOAD
        // см. https://st.yandex-team.ru/MARKETIR-14126
        List<DataCampOfferPictures.SourcePicture> sourcePictures = Stream.concat(
            pictureUrls.stream()
                .limit(2)
                .map(url -> DataCampOfferPictures.SourcePicture.newBuilder()
                    .setSource(DataCampOfferPictures.PictureSource.DIRECT_LINK)
                    .setUrl(url)
                    .build()),
            pictureUrls.stream().skip(2)
                .map(url -> DataCampOfferPictures.SourcePicture.newBuilder()
                    .setSource(DataCampOfferPictures.PictureSource.UPLOAD)
                    .setUrl("key-" + url)
                    .build()))
            .collect(Collectors.toList());
        Map<String, DataCampOfferPictures.MarketPicture> actualPictures = pictureUrls.stream().skip(2)
            .collect(Collectors.toMap(
                url -> "key-" + url,
                url -> DataCampOfferPictures.MarketPicture.newBuilder()
                    .setOriginal(DataCampOfferPictures.MarketPicture.Picture.newBuilder().setUrl(url))
                    .build()));

        // собираем оффер
        return DataCampOffer.Offer.newBuilder()
            .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setBusinessId(BUSINESS_ID)
                .setOfferId(offerId))
            .setPictures(DataCampOfferPictures.OfferPictures.newBuilder()
                .setPartner(DataCampOfferPictures.PartnerPictures.newBuilder()
                    .putAllActual(actualPictures)
                    .setOriginal(DataCampOfferPictures.SourcePictures.newBuilder()
                        .addAllSource(sourcePictures))))
            .setContent(DataCampOfferContent.OfferContent.newBuilder()
                .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                    .setOriginal(DataCampOfferContent.OriginalSpecification.newBuilder()
                        .setName(stringValue(SSKU_NAME))
                        .setDescription(stringValue(SSKU_DESCRIPTION))
                        .setVendor(stringValue(VENDOR))
                        .setVendorCode(stringValue(VENDORCODE))
                        .setBarcode(BARCODES)
                        .setGroupId(intValue(GROUP_ID))
                        .setGroupName(stringValue(GROUP_NAME)))
                    .setMarketSpecificContent(DataCampOfferMarketContent.MarketSpecificContent.newBuilder()
                        .setParameterValues(DataCampOfferMarketContent.MarketParameterValues.newBuilder()
                            .addAllParameterValues(parameterValues)))))
            .build();
    }

    private DataCampOfferMeta.Ui32Value intValue(int value) {
        return DataCampOfferMeta.Ui32Value.newBuilder().setValue(value).build();
    }

    private DataCampOfferMeta.StringValue stringValue(String sskuName) {
        return DataCampOfferMeta.StringValue.newBuilder().setValue(sskuName).build();
    }

    private Pipeline runPipeline(DataCampOffer.Offer... offers) {
        TestExcelFileGenerator generator = generatorBuilder.buildDcp(sourceDao, BUSINESS_ID, offers);

        List<String> shopSkus = Arrays.stream(offers)
            .map(o -> o.getIdentifiers().getOfferId())
            .collect(Collectors.toList());
        mockMdsFileDownloadSuccess(CATEGORY1_ID, SOURCE_ID,
            shopSkus,
            MDS_FILE_BUCKET, MDS_FILE_KEY, generator);

        long pipelineId = getLastPipelineId(PipelineType.DCP_SINGLE_XLS);
        runPipeline(pipelineId);
        return getPipeline(pipelineId);
    }

    private Pipeline runPipeline(DcpSkuDataGenerator dcpSkuDataGenerator, List<String> shopSkus) {
        TestExcelFileGenerator generator = generatorBuilder.buildDcp(dcpSkuDataGenerator);

        mockMdsFileDownloadSuccess(CATEGORY1_ID, SOURCE_ID,
            shopSkus,
            MDS_FILE_BUCKET, MDS_FILE_KEY, generator);

        long pipelineId = getLastPipelineId(PipelineType.DCP_SINGLE_XLS);
        runPipeline(pipelineId);
        return getPipeline(pipelineId);
    }


    private List<DataCampOffer.Offer> getOffersForDatacamp() {
        ArgumentCaptor<LogbrokerEvent<DatacampMessage>> argument = ArgumentCaptor.forClass(LogbrokerEvent.class);
        verify(offerLogbrokerService, Mockito.times(1)).publishEvent(argument.capture());

        return argument.getValue().getPayload()
            .getUnitedOffersList().stream()
            .flatMap(b -> b.getOfferList().stream())
            .map(DataCampUnitedOffer.UnitedOffer::getBasic)
            .collect(Collectors.toList());
    }

    private List<MessageInfo> getValidationErrors(long processRequestId) {
        FileProcess fileProcess = fileProcessDao.fetchByFileDataProcessRequestId(processRequestId).get(0);
        return fileProcessMessageService.getFileProcessMessages(fileProcess.getId(), FileProcessMessageType.FILE_VALIDATION);
    }

    private void assertOfferContentIsTheSame(DataCampOffer.Offer parsed, DataCampOffer.Offer initial,
                                             List<String> pictureUrls) {
        assertEqualIds(parsed, initial, DataCampOfferIdentifiers.OfferIdentifiers::getBusinessId);
        assertEqualIds(parsed, initial, DataCampOfferIdentifiers.OfferIdentifiers::getOfferId);

        assertOriginalStringEquals(parsed, initial, o -> o.getName());
        assertOriginalStringEquals(parsed, initial, o -> o.getDescription());
        assertOriginalStringEquals(parsed, initial, o -> o.getVendor());
        assertOriginalStringEquals(parsed, initial, o -> o.getVendorCode());
        assertOriginalStringEquals(parsed, initial, o -> o.getGroupName());
        assertOriginalIntEquals(parsed, initial, o -> o.getGroupId());
        assertOriginalStringListEquals(parsed, initial, o -> o.getBarcode());

        List<Pair<Long, DataCampContentMarketParameterValue.MarketValue>> parsedParamValues =
            parsed.getContent().getPartner().getMarketSpecificContent().getParameterValues().getParameterValuesList().stream()
                .map(v -> Pair.makePair(v.getParamId(), v.getValue()))
                .collect(Collectors.toList());

        List<Pair<Long, DataCampContentMarketParameterValue.MarketValue>> initialParamValues =
            initial.getContent().getPartner().getMarketSpecificContent().getParameterValues().getParameterValuesList().stream()
                .map(v -> Pair.makePair(v.getParamId(), v.getValue()))
                .collect(Collectors.toList());
        assertThat(parsedParamValues).containsExactlyInAnyOrderElementsOf(initialParamValues);

        assertThat(parsed.getPictures().getPartner().getOriginal().getSourceList())
            .extracting(DataCampOfferPictures.SourcePicture::getUrl)
            .containsExactlyElementsOf(pictureUrls);
    }

    private <R> void assertEqualIds(DataCampOffer.Offer parsed, DataCampOffer.Offer initial,
                                    Function<DataCampOfferIdentifiers.OfferIdentifiers, R> getter) {
        assertThat(getter.apply(parsed.getIdentifiers())).isEqualTo(getter.apply(initial.getIdentifiers()));
    }


    private void assertOriginalStringEquals(DataCampOffer.Offer parsed, DataCampOffer.Offer initial,
                                            Function<DataCampOfferContent.OriginalSpecification, DataCampOfferMeta.StringValue> getter) {
        String originalValue = getter.apply(parsed.getContent().getPartner().getOriginal()).getValue();
        String newValue = getter.apply(initial.getContent().getPartner().getOriginal()).getValue();
        assertThat(originalValue).isEqualTo(newValue);
    }

    private void assertOriginalStringListEquals(DataCampOffer.Offer parsed, DataCampOffer.Offer initial,
                                                Function<DataCampOfferContent.OriginalSpecification, DataCampOfferMeta.StringListValue> getter) {
        List<String> originalValue = getter.apply(parsed.getContent().getPartner().getOriginal()).getValueList();
        List<String> newValue = getter.apply(initial.getContent().getPartner().getOriginal()).getValueList();
        assertThat(originalValue).containsExactlyElementsOf(newValue);
    }

    private void assertOriginalIntEquals(DataCampOffer.Offer parsed, DataCampOffer.Offer initial,
                                         Function<DataCampOfferContent.OriginalSpecification, DataCampOfferMeta.Ui32Value> getter) {
        Integer originalValue = getter.apply(parsed.getContent().getPartner().getOriginal()).getValue();
        Integer newValue = getter.apply(initial.getContent().getPartner().getOriginal()).getValue();
        assertThat(originalValue).isEqualTo(newValue);
    }
}

package ru.yandex.market.ir.autogeneration_api.export.excel;

import Market.DataCamp.DataCampContentMarketParameterValue;
import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMarketContent;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.ir.autogeneration.common.helpers.BookCategoryHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.ModelStorageHelper;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMock;
import ru.yandex.market.ir.autogeneration.common.rating.DefaultRatingEvaluator;
import ru.yandex.market.ir.autogeneration.common.rating.SkuRatingEvaluator;
import ru.yandex.market.ir.excel.generator.CategoryInfo;
import ru.yandex.market.ir.excel.generator.CategoryInfoProducer;
import ru.yandex.market.ir.excel.generator.CategoryParametersFormParser;
import ru.yandex.market.ir.excel.generator.ImportContentType;
import ru.yandex.market.ir.excel.generator.PartnerContentConverter;
import ru.yandex.market.ir.excel.generator.XlsRow;
import ru.yandex.market.ir.excel.generator.XslInfo;
import ru.yandex.market.ir.excel.generator.param.MainParamCreator;
import ru.yandex.market.ir.excel.generator.param.ParameterInfo;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.partner.content.common.csku.judge.Judge;
import ru.yandex.market.partner.content.common.db.dao.SourceDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.Source;
import ru.yandex.market.partner.content.common.service.mock.DataCampServiceMock;
import ru.yandex.market.robot.db.ParameterValueComposer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DcpSkuDataGeneratorTest {
    private static final Integer SOURCE_ID = 100500;
    private static final Integer BUSINESS_ID = 10500;
    private static final long HID = 91491L;

    private final Judge judge = new Judge();
    private final CategoryDataKnowledgeMock categoryDataKnowledgeMock = new CategoryDataKnowledgeMock();
    private CategoryInfoProducer categoryInfoProducer;
    private CategoryDataHelper categoryDataHelper;
    private SourceDao sourceDao;
    private SkuRatingEvaluator skuRatingEvaluator = new DefaultRatingEvaluator((categoryDataKnowledgeMock));
    private ModelStorageHelper modelStorageHelper = mock(ModelStorageHelper.class);

    @Before
    public void setUp() {
        Source source = new Source();
        source.setSourceId(SOURCE_ID);
        source.setPartnerShopId(BUSINESS_ID);
        sourceDao = mock(SourceDao.class);
        when(sourceDao.findById(any())).thenReturn(source);

        final CategoryData categoryData = CategoryData.build(MboParameters.Category.newBuilder()
            .setHid(HID)
            .setLeaf(true)
            .addParameter(MboParameters.Parameter.newBuilder()
                .setId(ParameterValueComposer.VENDOR_ID)
                .setXslName(CategoryData.VENDOR)
                .setValueType(MboParameters.ValueType.ENUM)
            )
            .addParameter(MboParameters.Parameter.newBuilder()
                .setId(ParameterValueComposer.BARCODE_ID)
                .setXslName(CategoryData.BAR_CODE)
                .setValueType(MboParameters.ValueType.STRING)
                .setMultivalue(true))
            .build());
        categoryDataKnowledgeMock.addCategoryData(HID, categoryData);

        Map<String, XslInfo> categoryAttr = buildCategoryAttr(categoryData.getParameterList());
        CategoryParametersFormParser categoryParametersFormParser = mock(CategoryParametersFormParser.class);
        when(categoryParametersFormParser.getCategoryAttr(Mockito.anyLong())).thenReturn(categoryAttr);
        BookCategoryHelper bookCategoryHelper = mock(BookCategoryHelper.class);
        categoryDataHelper = new CategoryDataHelper(categoryDataKnowledgeMock, bookCategoryHelper);
        categoryInfoProducer = new CategoryInfoProducer(categoryDataKnowledgeMock, categoryParametersFormParser);
    }

    @Test
    public void multivalueParameterValuesAreMergedWithDelimiter() {
        final String offerId = "ssku1";
        final String barcode1 = "1234", barcode2 = "5678", barcode3 = "9101112";

        List<DataCampContentMarketParameterValue.MarketParameterValue> parameterValues = ImmutableList.of(
            getBarcodeParamValue(barcode1),
            getBarcodeParamValue(barcode2),
            getBarcodeParamValue(barcode3)
        );
        DataCampOffer.Offer offer = buildOffer(offerId, parameterValues, null, null);
        DataCampServiceMock dataCampServiceMock = new DataCampServiceMock();
        dataCampServiceMock.setOffersForBusinessId(BUSINESS_ID, offer);

        DcpSkuDataGenerator dcpSkuDataGenerator = new DcpSkuDataGenerator(
            dataCampServiceMock, sourceDao,
            new PartnerContentConverter(categoryDataHelper), modelStorageHelper, judge,
                skuRatingEvaluator);

        CategoryData categoryData = categoryInfoProducer.getCategoryData(HID);
        CategoryInfo categoryInfo = categoryInfoProducer.extractCategoryInfo(HID, ImportContentType.DCP_EXCEL);
        List<XlsRow> rowValues = dcpSkuDataGenerator.generateRows(
            SOURCE_ID, categoryInfo, categoryData, Collections.singletonList(offerId), false);

        int barcodeParamIndex = categoryInfo.getParameters().stream()
            .map(ParameterInfo::getId).collect(Collectors.toList())
            .indexOf(ParameterValueComposer.BARCODE_ID);

        assertThat(rowValues).hasSize(1);
        assertThat(rowValues).element(0).extracting(m -> m.getColumns().get(0).getContent()).isEqualTo(offerId);
        assertThat(rowValues).element(0).extracting(m -> m.getColumns().stream()
                .filter(c -> c.getIndex() == barcodeParamIndex).findFirst().get().getContent())
            .isEqualTo(String.join(CategoryDataHelper.MULTI_VALUE_DELIMITER, barcode1, barcode2, barcode3));
    }

    @Test
    public void groupIdAndNameAreEmptyIfNotPresentInOffer() {
        final String offerId = "ssku1";

        DataCampOffer.Offer offer = buildOffer(offerId, Collections.emptyList(), null, null);
        DataCampServiceMock dataCampServiceMock = new DataCampServiceMock();
        dataCampServiceMock.setOffersForBusinessId(BUSINESS_ID, offer);

        DcpSkuDataGenerator dcpSkuDataGenerator = new DcpSkuDataGenerator(
            dataCampServiceMock, sourceDao,
            new PartnerContentConverter(categoryDataHelper), modelStorageHelper, judge,
                skuRatingEvaluator);

        CategoryData categoryData = categoryInfoProducer.getCategoryData(HID);
        CategoryInfo categoryInfo = categoryInfoProducer.extractCategoryInfo(HID, ImportContentType.DCP_EXCEL);
        List<XlsRow> rowValues = dcpSkuDataGenerator.generateRows(
            SOURCE_ID, categoryInfo, categoryData, Collections.singletonList(offerId), false);

        int groupIdIndex = categoryInfo.getParameters().stream()
            .map(ParameterInfo::getId).collect(Collectors.toList())
            .indexOf(((Integer)MainParamCreator.GROUP_ID_ORDER).longValue());
        int groupNameIndex = categoryInfo.getParameters().stream()
            .map(ParameterInfo::getId).collect(Collectors.toList())
            .indexOf(((Integer)MainParamCreator.GROUP_NAME_PARAM_ID).longValue());

        assertThat(rowValues).hasSize(1);
        assertThat(rowValues).element(0).extracting(m -> m.getColumns().get(0).getContent()).isEqualTo(offerId);
        assertThat(rowValues).element(0).extracting(m -> m.getColumns().stream().anyMatch(c -> c.getIndex() == groupIdIndex)).isEqualTo(false);
        assertThat(rowValues).element(0).extracting(m -> m.getColumns().stream().anyMatch(c -> c.getIndex() == groupNameIndex)).isEqualTo(false);
    }

    @Test
    public void groupIdAndNameAreExtractedFromOffer() {
        final String offerId = "ssku1";
        final Integer groupId = 100500;
        final String groupName = "group";

        DataCampOffer.Offer offer = buildOffer(offerId, Collections.emptyList(), groupId, groupName);
        DataCampServiceMock dataCampServiceMock = new DataCampServiceMock();
        dataCampServiceMock.setOffersForBusinessId(BUSINESS_ID, offer);

        DcpSkuDataGenerator dcpSkuDataGenerator = new DcpSkuDataGenerator(
            dataCampServiceMock, sourceDao,
            new PartnerContentConverter(categoryDataHelper), modelStorageHelper, judge,
                skuRatingEvaluator);

        CategoryData categoryData = categoryInfoProducer.getCategoryData(HID);
        CategoryInfo categoryInfo = categoryInfoProducer.extractCategoryInfo(HID, ImportContentType.DCP_EXCEL);
        List<XlsRow> rowValues = dcpSkuDataGenerator.generateRows(
            SOURCE_ID, categoryInfo, categoryData, Collections.singletonList(offerId), false);

        int groupIdIndex = categoryInfo.getParameters().stream()
            .map(ParameterInfo::getId).collect(Collectors.toList())
            .indexOf(((Integer)MainParamCreator.GROUP_ID_ORDER).longValue());
        int groupNameIndex = categoryInfo.getParameters().stream()
            .map(ParameterInfo::getId).collect(Collectors.toList())
            .indexOf(((Integer)MainParamCreator.GROUP_NAME_PARAM_ID).longValue());

        assertThat(rowValues).hasSize(1);
        assertThat(rowValues).element(0).extracting(m -> m.getColumns().get(0).getContent()).isEqualTo(offerId);
        assertThat(rowValues).element(0).extracting(m -> m.getColumns().stream()
                .filter(c -> c.getIndex() == groupIdIndex).findFirst().get().getContent()).isEqualTo(groupId.toString());
        assertThat(rowValues).element(0).extracting(m -> m.getColumns().stream()
                .filter(c -> c.getIndex() == groupNameIndex).findFirst().get().getContent()).isEqualTo(groupName);
    }

    private DataCampOffer.Offer buildOffer(
        String offerId,
        List<DataCampContentMarketParameterValue.MarketParameterValue> parameterValues,
        Integer groupId,
        String groupName) {

        DataCampOfferContent.PartnerContent.Builder partnerContent =
            DataCampOfferContent.PartnerContent.newBuilder()
                .setMarketSpecificContent(DataCampOfferMarketContent.MarketSpecificContent.newBuilder()
                    .setParameterValues(DataCampOfferMarketContent.MarketParameterValues.newBuilder()
                        .addAllParameterValues(parameterValues)));

        if (groupId != null) {
            partnerContent.getOriginalBuilder().getGroupIdBuilder().setValue(groupId);
        }
        if (groupName != null) {
            partnerContent.getOriginalBuilder().getGroupNameBuilder().setValue(groupName);
        }

        return DataCampOffer.Offer.newBuilder()
            .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setBusinessId(BUSINESS_ID)
                .setOfferId(offerId))
            .setContent(DataCampOfferContent.OfferContent.newBuilder()
                .setPartner(partnerContent))
            .build();
    }

    private DataCampContentMarketParameterValue.MarketParameterValue getBarcodeParamValue(String value) {
        return DataCampContentMarketParameterValue.MarketParameterValue.newBuilder()
            .setParamId(ParameterValueComposer.BARCODE_ID)
            .setParamName(CategoryData.BAR_CODE)
            .setValue(DataCampContentMarketParameterValue.MarketValue.newBuilder()
                .setValueType(DataCampContentMarketParameterValue.MarketValueType.STRING)
                .setStrValue(value))
            .build();
    }


    private Map<String, XslInfo> buildCategoryAttr(Collection<MboParameters.Parameter> parameterList) {
        return parameterList.stream()
            .map(MboParameters.Parameter::getXslName)
            .collect(Collectors.toMap(n -> n, n -> XslInfo.DEFAULT));
    }
}

package ru.yandex.market.gutgin.tms.pipeline.dcp.xls.validation;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.gutgin.tms.service.ConvertRawSkuToDataCampOffersService;
import ru.yandex.market.gutgin.tms.service.OfferContentStateMbocApiServiceMock;
import ru.yandex.market.ir.autogeneration.common.db.CategoryDataKnowledge;
import ru.yandex.market.ir.autogeneration.common.helpers.BookCategoryHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMock;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMockBuilder;
import ru.yandex.market.ir.excel.generator.CategoryInfoProducer;
import ru.yandex.market.partner.content.common.db.dao.FileDataProcessRequestDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcRawSku;
import ru.yandex.market.partner.content.common.helpers.BusinessIdXlsExtractor;
import ru.yandex.market.partner.content.common.message.MessageInfo;
import ru.yandex.market.partner.content.common.mocks.CategoryParametersFormParserMock;
import ru.yandex.market.partner.content.common.service.mock.DataCampServiceMock;

import static org.assertj.core.api.Assertions.assertThat;

public class OfferContentStateValidationTest extends BaseDcpExcelValidationTest {

    private static final long CATEGORY_ID_FROM_UC = CATEGORY_ID + 100;
    private static final String OFFER_ID = "SSKU1";

    @Autowired
    private FileDataProcessRequestDao fileDataProcessRequestDao;

    private final OfferContentStateMbocApiServiceMock offerContentStateMbocApiServiceMock =
        new OfferContentStateMbocApiServiceMock();

    @Override
    public void setUp() {
        super.setUp();

        DataCampOffer.Offer offer = DataCampOffer.Offer.newBuilder()
            .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setBusinessId(BUSINESS_ID)
                .setOfferId(OFFER_ID))
            .setContent(
                DataCampOfferContent.OfferContent.newBuilder()
                    .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                        .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                            .setMarketCategoryId(Math.toIntExact(CATEGORY_ID)))))
            .build();
        DataCampServiceMock dataCampServiceMock = new DataCampServiceMock();
        dataCampServiceMock.setOffersForBusinessId(BUSINESS_ID, offer);

        CategoryDataKnowledgeMockBuilder categoryDataKnowledgeMockBuilder = CategoryDataKnowledgeMockBuilder.builder();
        addCategory(categoryDataKnowledgeMockBuilder, CATEGORY_ID);
        addCategory(categoryDataKnowledgeMockBuilder, CATEGORY_ID_FROM_UC);
        CategoryDataKnowledge categoryDataKnowledge = categoryDataKnowledgeMockBuilder.build();

        CategoryParametersFormParserMock categoryParametersFormParserMock = new CategoryParametersFormParserMock();
        CategoryInfoProducer categoryInfoProducer =
            new CategoryInfoProducer(categoryDataKnowledge, categoryParametersFormParserMock);
        BusinessIdXlsExtractor businessIdXlsExtractor = new BusinessIdXlsExtractor(fileDataProcessRequestDao, fileProcessDao);
        CategoryDataHelper categoryDataHelper = new CategoryDataHelper(new CategoryDataKnowledgeMock(), new BookCategoryHelper());
        ConvertRawSkuToDataCampOffersService convertRawSkuToDataCampOffersService =
            new ConvertRawSkuToDataCampOffersService(categoryInfoProducer, categoryDataHelper, true);

        validation = new OfferContentStateValidation(
            offerContentStateMbocApiServiceMock,
            businessIdXlsExtractor,
            convertRawSkuToDataCampOffersService,
            categoryDataKnowledge);

        offerContentStateMbocApiServiceMock.reset();
    }

    @Test
    public void shouldPassWhenCategoryFromUcIsTheSameAndModelCreateUpdateAllowed() {
        offerContentStateMbocApiServiceMock.setAllowModelCreateUpdate(OFFER_ID, true);
        offerContentStateMbocApiServiceMock.setCategoryIdFromUc(OFFER_ID, CATEGORY_ID);

        GcRawSku gcRawSku = createGcRawSku(OFFER_ID);
        Validation.Result<GcRawSku> result = validation.validate(ImmutableList.of(gcRawSku));

        assertThat(result.getInvalidValues()).isEmpty();
        assertThat(result.getMessages()).isEmpty();
    }

    @Test
    public void shouldNotThrowErrorForInvalidGroupId() {
        offerContentStateMbocApiServiceMock.setAllowModelCreateUpdate(OFFER_ID, true);
        offerContentStateMbocApiServiceMock.setCategoryIdFromUc(OFFER_ID, CATEGORY_ID);

        GcRawSku gcRawSku = createGcRawSkuWithInvalidGroupId(OFFER_ID);
        Validation.Result<GcRawSku> result = validation.validate(ImmutableList.of(gcRawSku));

        assertThat(result.getInvalidValues()).isEmpty();
        assertThat(result.getMessages()).isEmpty();
    }

    @Test
    public void shouldFailWhenCategoryFromUcDoesNotMatchOfferCategory() {
        offerContentStateMbocApiServiceMock.setAllowModelCreateUpdate(OFFER_ID, true);
        offerContentStateMbocApiServiceMock.setCategoryIdFromUc(OFFER_ID, CATEGORY_ID_FROM_UC);

        GcRawSku gcRawSku = createGcRawSku(OFFER_ID);
        Validation.Result<GcRawSku> result = validation.validate(ImmutableList.of(gcRawSku));

        assertThat(result.getInvalidValues()).containsExactly(gcRawSku);
        assertThat(result.getMessages())
            .extracting(MessageInfo::getCode)
            .containsExactly("ir.partner_content.dcp.excel.validation.offer_matched_to_another_category");
        assertThat(result.getMessages())
            .extracting(messageInfo -> messageInfo.getParams().get("shopSku"))
            .containsExactly(OFFER_ID);
        assertThat(result.getMessages())
            .extracting(messageInfo -> messageInfo.getParams().get("rowIndex"))
            .containsExactly(ROW_INDEX);
        assertThat(result.getMessages())
            .extracting(messageInfo -> messageInfo.getParams().get("matchedCategoryName"))
            .containsExactly("Category " + CATEGORY_ID_FROM_UC);
    }

    @Test
    public void shouldFailWhenModelCreateUpdateIsNotAllowed() {
        offerContentStateMbocApiServiceMock.setAllowModelCreateUpdate(OFFER_ID, false);
        offerContentStateMbocApiServiceMock.setCategoryIdFromUc(OFFER_ID, CATEGORY_ID);

        GcRawSku gcRawSku = createGcRawSku(OFFER_ID);
        Validation.Result<GcRawSku> result = validation.validate(ImmutableList.of(gcRawSku));

        assertThat(result.getInvalidValues()).containsExactly(gcRawSku);
        assertThat(result.getMessages())
            .extracting(messageInfo -> messageInfo.getParams().get("shopSku"))
            .containsExactly(OFFER_ID);
        assertThat(result.getMessages())
            .extracting(messageInfo -> messageInfo.getParams().get("rowIndex"))
            .containsExactly(ROW_INDEX);
        assertThat(result.getMessages())
            .extracting(MessageInfo::getCode)
            .containsExactly("ir.partner_content.dcp.excel.validation.model_create_update_is_not_allowed");
    }


    private void addCategory(CategoryDataKnowledgeMockBuilder builder, long categoryId) {
        builder
            .startCategory(categoryId)
            .setName("Category " + categoryId)
            .setUniqueName("Category " + categoryId)
            .vendorParameterBuilder().build()
            .build();
    }
}

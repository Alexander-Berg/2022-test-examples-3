package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.databucket_validations;

import java.util.List;
import java.util.Optional;

import Market.DataCamp.DataCampOfferMeta;
import com.google.common.collect.ImmutableList;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.gutgin.tms.engine.task.ProcessTaskResult;
import ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.BarcodeGtinValidation;
import ru.yandex.market.gutgin.tms.service.GlobalVendorsCachingService;
import ru.yandex.market.ir.autogeneration.common.db.CategoryData;
import ru.yandex.market.ir.autogeneration.common.helpers.BookCategoryHelper;
import ru.yandex.market.ir.autogeneration.common.helpers.CategoryDataHelper;
import ru.yandex.market.ir.autogeneration.common.mocks.CategoryDataKnowledgeMock;
import ru.yandex.market.mbo.export.MboParameters;
import ru.yandex.market.mbo.http.MboVendors;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DatacampOffer;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.entity.goodcontent.FailData;
import ru.yandex.market.partner.content.common.entity.goodcontent.ParamInfo;
import ru.yandex.market.partner.content.common.entity.goodcontent.TicketValidationResult;
import ru.yandex.market.partner.content.common.message.MessageInfo;
import ru.yandex.market.robot.db.ParameterValueComposer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline.DcpOfferUtils.initOffer;

public class BarcodeGtinValidationTest extends DBDcpStateGenerator {

    private static final String VENDOR_NAME = "vendor1";
    private static final long BOOK_CATEGORY_ID = 90928L;
    private static final long COMMON_CATEGORY_ID = 1234L;
    private BarcodeGtinValidation barcodeGtinValidation;

    @Before
    public void setUp() {
        super.setUp();
        CategoryDataKnowledgeMock categoryDataKnowledgeMock = new CategoryDataKnowledgeMock();
        CategoryData category = createCategory();
        categoryDataKnowledgeMock.addCategoryData(COMMON_CATEGORY_ID, category);
        categoryDataKnowledgeMock.addCategoryData(BOOK_CATEGORY_ID, category);
        BookCategoryHelper bookCategoryHelper = new BookCategoryHelper();
        CategoryDataHelper categoryDataHelper = new CategoryDataHelper(categoryDataKnowledgeMock, bookCategoryHelper);
        GlobalVendorsCachingService globalVendorsCachingService = mock(GlobalVendorsCachingService.class);
        doReturn(Optional.of(createGlobalVendor())).when(globalVendorsCachingService).getVendor(anyLong());
        this.barcodeGtinValidation = new BarcodeGtinValidation(
            gcSkuValidationDao,
            gcSkuTicketDao,
            globalVendorsCachingService,
            categoryDataHelper,
            true);
    }

    @Test
    public void okBarcode() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(1, offers ->
            offerWithBarcode(offers, "87280500", 0));
        gcSkuTickets.forEach(ticket -> setCategory(ticket, COMMON_CATEGORY_ID));

        ProcessTaskResult<List<TicketValidationResult>> validationResult = barcodeGtinValidation.validate(gcSkuTickets);

        assertThat(validationResult.getResult()).hasSize(1);
        assertThat(validationResult.getResult().get(0).isValid()).isTrue();
        assertThat(validationResult.getResult().get(0).getFailData()).isNull();
    }

    @Test
    public void notValidBarcodes() {
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(2, offers -> {
            offerWithBarcode(offers, "899121530013513485", 0); //too long
            offerWithBarcode(offers, "aaaaaaaaa", 1); //not a barcode
        });
        gcSkuTickets.forEach(ticket -> setCategory(ticket, COMMON_CATEGORY_ID));

        ProcessTaskResult<List<TicketValidationResult>> validationResult = barcodeGtinValidation.validate(gcSkuTickets);

        List<TicketValidationResult> result = validationResult.getResult();
        assertThat(result).hasSize(2);
        assertMessageResult(result, 0);
        assertMessageResult(result, 1);
    }

    @Test
    public void testIsbn() {
        String validIsbn = "978-5-9909805-1-8";
        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(1, offers ->
                offerWithBarcode(offers, validIsbn, 0));
        setCategory(gcSkuTickets.get(0), BOOK_CATEGORY_ID);

        ProcessTaskResult<List<TicketValidationResult>> validationResult = barcodeGtinValidation.validate(gcSkuTickets);
        assertThat(validationResult.getResult()).hasSize(1);
        assertThat(validationResult.getResult().get(0).isValid()).isTrue();

        String invalidIsbn = "3-16-148410-X";
        gcSkuTickets = generateDBDcpInitialState(1, offers ->
                offerWithBarcode(offers, invalidIsbn, 0));
        setCategory(gcSkuTickets.get(0), BOOK_CATEGORY_ID);

        validationResult = barcodeGtinValidation.validate(gcSkuTickets);
        assertThat(validationResult.getResult()).hasSize(1);
        assertThat(validationResult.getResult().get(0).isValid()).isFalse();
    }

    private void assertMessageResult(List<TicketValidationResult> result, int index) {
        TicketValidationResult ticketValidationResult1 = result.get(index);
        assertThat(ticketValidationResult1.isValid()).isFalse();
        ImmutableList<MessageInfo> validationMessages = ticketValidationResult1.getValidationMessages();
        assertThat(validationMessages).hasSize(1);
        MessageInfo messageInfo = validationMessages.get(0);
        assertThat(messageInfo.getCode()).isEqualTo("ir.partner_content.goodcontent.validation.barcode_required_gtin");
        assertThat(messageInfo.getLevel()).isEqualTo(MessageInfo.Level.ERROR);
        FailData failData = ticketValidationResult1.getFailData();
        assertThat(failData).isNotNull();
        assertThat(failData.getParams()).containsExactly(
                new ParamInfo(ParameterValueComposer.BARCODE_ID, ParameterValueComposer.BARCODE,
                        false));
    }

    private void setCategory(GcSkuTicket gcSkuTicket, long category) {
        gcSkuTicket.setCategoryId(category);
    }

    private MboVendors.GlobalVendor createGlobalVendor() {
        return MboVendors.GlobalVendor.newBuilder()
            .setIsRequireGtinBarcodes(true)
            .build();
    }

    private CategoryData createCategory() {
        return CategoryData.build(
            CATEGORY.toBuilder()
                .addParameter(
                    MboParameters.Parameter.newBuilder()
                        .setId(ParameterValueComposer.VENDOR_ID)
                        .setXslName(ParameterValueComposer.VENDOR)
                        .setValueType(MboParameters.ValueType.ENUM)
                        .addOption(
                            MboParameters.Option.newBuilder()
                                .setId(1)
                                .addName(
                                    MboParameters.Word.newBuilder()
                                        .setName(VENDOR_NAME)
                                )
                                .setIsGuruVendor(false)
                        )
                )
        );
    }

    private void offerWithBarcode(List<DatacampOffer> offers, String barcode, int index) {
        initOffer(CATEGORY_ID, offers.get(index), builder -> builder
            .getContentBuilder()
            .getPartnerBuilder()
            .getActualBuilder()
            .setVendor(
                DataCampOfferMeta.StringValue.newBuilder().setValue(VENDOR_NAME)
            )
            .setBarcode(DataCampOfferMeta.StringListValue.newBuilder().addValue(barcode))
        );
    }
}

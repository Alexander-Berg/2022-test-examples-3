package ru.yandex.market.gutgin.tms.pipeline.good.taskaction.datacamp_pipeline;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferMeta;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.gutgin.tms.engine.task.ProcessTaskResult;
import ru.yandex.market.partner.content.common.DBDcpStateGenerator;
import ru.yandex.market.partner.content.common.csku.KnownParameters;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.DatacampOffer;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;
import ru.yandex.market.partner.content.common.entity.goodcontent.TicketValidationResult;
import ru.yandex.market.partner.content.common.message.Messages;

import static org.assertj.core.api.Assertions.assertThat;

public class VendorCodeDuplicatesValidationTest extends DBDcpStateGenerator {
    private static final String VENDOR_CODE_EXIST = "vendor_code_exist";

    private VendorCodeDuplicatesValidation vendorCodeDuplicatesValidation;

    @Before
    public void init() {
        vendorCodeDuplicatesValidation = new VendorCodeDuplicatesValidation(
                gcSkuValidationDao, gcSkuTicketDao, Collections.emptySet(), true);
    }

    @Test
    public void whenEmptyVendorCodeThenValid() {
        String vendor1 = "vendor1";
        String vendor2 = "vendor2";

        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(3, datacampOffers -> {
            initOffer(datacampOffers.get(0), vendor1, "");
            initOffer(datacampOffers.get(1), vendor2, "");
            initOffer(datacampOffers.get(2), vendor2, "");
        });

        ProcessTaskResult<List<TicketValidationResult>> validate =
                vendorCodeDuplicatesValidation.validate(gcSkuTickets);

        assertThat(validate.getResult().get(0).isValid()).isTrue();
        assertThat(validate.getResult().get(1).isValid()).isTrue();
        assertThat(validate.getResult().get(2).isValid()).isTrue();
    }

    @Test
    public void whenSameVendorCodeInDifferentVendorThenValid() {
        String vendor1 = "vendor1";
        String vendor2 = "vendor2";

        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(2, datacampOffers -> {
            initOffer(datacampOffers.get(0), vendor1, VENDOR_CODE_EXIST);
            initOffer(datacampOffers.get(1), vendor2, VENDOR_CODE_EXIST);
        });

        ProcessTaskResult<List<TicketValidationResult>> validate =
                vendorCodeDuplicatesValidation.validate(gcSkuTickets);

        assertThat(validate.getResult().get(0).isValid()).isTrue();
        assertThat(validate.getResult().get(1).isValid()).isTrue();
    }

    @Test
    public void whenDuplicateWithTicketThenMarketMessage() {
        String vendor1 = "vendor1";

        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(3, datacampOffers -> {
            initOffer(datacampOffers.get(0), vendor1, "anotherVendorCode");
            initOffer(datacampOffers.get(1), vendor1, VENDOR_CODE_EXIST);
            initOffer(datacampOffers.get(2), vendor1, VENDOR_CODE_EXIST);
        });

        ProcessTaskResult<List<TicketValidationResult>> validate =
                vendorCodeDuplicatesValidation.validate(gcSkuTickets);

        assertThat(validate.getResult().get(0).isValid()).isTrue();

        TicketValidationResult invalid2 = validate.getResult().get(1);
        TicketValidationResult invalid3 = validate.getResult().get(2);

        String shopSku2 = gcSkuTickets.get(1).getShopSku();
        String shopSku3 = gcSkuTickets.get(2).getShopSku();

        assertThat(validate.getResult().get(0).isValid()).isTrue();

        assertThat(invalid2.isValid()).isFalse();
        assertThat(invalid2.getValidationMessages())
                .hasSize(1)
                .containsOnly(Messages.get().dcpDuplicateTicketVendorCode(
                        shopSku2,
                        VENDOR_CODE_EXIST,
                        new String[]{shopSku3},
                        KnownParameters.VENDOR_CODE.getId()));

        assertThat(invalid3.isValid()).isFalse();
        assertThat(invalid3.getValidationMessages())
                .hasSize(1)
                .containsOnly(Messages.get().dcpDuplicateTicketVendorCode(
                        shopSku3,
                        VENDOR_CODE_EXIST,
                        new String[]{shopSku2},
                        KnownParameters.VENDOR_CODE.getId()));
    }

    @Test
    public void whenDuplicateWithHignNumberOfTicketThenCutMarketMessage() {
        String vendor1 = "vendor1";

        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(13, datacampOffers -> {
            datacampOffers.forEach(datacampOffer ->  initOffer(datacampOffer, vendor1, VENDOR_CODE_EXIST));
        });

        ProcessTaskResult<List<TicketValidationResult>> validate =
                vendorCodeDuplicatesValidation.validate(gcSkuTickets);

        assertThat(validate.getResult().get(0).isValid()).isFalse();

        TicketValidationResult invalidResult = validate.getResult().get(1);

        assertThat(invalidResult.isValid()).isFalse();
        assertThat(invalidResult.getValidationMessages())
                .hasSize(1);
        Map<String, Object> params = invalidResult.getValidationMessages().get(0).getParams();
        String[] otherOfferIds = (String[]) params.get("otherOfferIds");
        assertThat(otherOfferIds[otherOfferIds.length - 1]).isEqualTo("так же в 2 других");
    }

    @Test
    public void whenDuplicateAndDisabledThenValid() {
        VendorCodeDuplicatesValidation validation = new VendorCodeDuplicatesValidation(
                gcSkuValidationDao, gcSkuTicketDao, Collections.emptySet(), false);
        String vendor1 = "vendor1";

        List<GcSkuTicket> gcSkuTickets = generateDBDcpInitialState(3, datacampOffers -> {
            initOffer(datacampOffers.get(0), vendor1, "anotherVendorCode");
            initOffer(datacampOffers.get(1), vendor1, VENDOR_CODE_EXIST);
            initOffer(datacampOffers.get(2), vendor1, VENDOR_CODE_EXIST);
        });

        ProcessTaskResult<List<TicketValidationResult>> validate = validation.validate(gcSkuTickets);

        assertThat(validate.getResult().get(0).isValid()).isTrue();
        assertThat(validate.getResult().get(1).isValid()).isTrue();
        assertThat(validate.getResult().get(2).isValid()).isTrue();
    }

    private void initOffer(DatacampOffer datacampOffer, String vendor, String vendorCode) {
        DataCampOffer.Offer.Builder builder = DataCampOffer.Offer.newBuilder();

        builder.getIdentifiersBuilder()
                .setBusinessId(datacampOffer.getBusinessId())
                .setOfferId(datacampOffer.getOfferId());

        builder
                .getContentBuilder().getPartnerBuilder().getActualBuilder()
                .setVendor(DataCampOfferMeta.StringValue.newBuilder().setValue(vendor))
                .setVendorCode(DataCampOfferMeta.StringValue.newBuilder().setValue(vendorCode));

        builder
                .getContentBuilder().getBindingBuilder().getApprovedBuilder()
                .setMarketCategoryId(Math.toIntExact(CATEGORY_ID));

        datacampOffer.setData(builder.build());
    }
}

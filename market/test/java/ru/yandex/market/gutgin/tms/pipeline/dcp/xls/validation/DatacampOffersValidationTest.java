package ru.yandex.market.gutgin.tms.pipeline.dcp.xls.validation;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMapping;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.market.partner.content.common.db.dao.FileDataProcessRequestDao;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcRawSku;
import ru.yandex.market.partner.content.common.helpers.BusinessIdXlsExtractor;
import ru.yandex.market.partner.content.common.message.MessageInfo;
import ru.yandex.market.partner.content.common.service.mock.DataCampServiceMock;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class DatacampOffersValidationTest extends BaseDcpExcelValidationTest {

    private static final String OFFER_ID = "SSKU1";

    @Autowired
    private FileDataProcessRequestDao fileDataProcessRequestDao;

    private DatacampOffersValidation validation;

    @Override
    public void setUp() {
        super.setUp();

        DataCampOffer.Offer offer = DataCampOffer.Offer.newBuilder()
            .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                .setBusinessId(123)
                .setOfferId(OFFER_ID))
            .setContent(
                DataCampOfferContent.OfferContent.newBuilder()
                    .setBinding(DataCampOfferMapping.ContentBinding.newBuilder()
                        .setApproved(DataCampOfferMapping.Mapping.newBuilder()
                            .setMarketCategoryId(Math.toIntExact(CATEGORY_ID)))))
            .build();
        DataCampServiceMock dataCampServiceMock = new DataCampServiceMock();
        dataCampServiceMock.setOffersForBusinessId(BUSINESS_ID, offer);

        BusinessIdXlsExtractor businessIdXlsExtractor = new BusinessIdXlsExtractor(fileDataProcessRequestDao, fileProcessDao);
        validation = new DatacampOffersValidation(dataCampServiceMock, businessIdXlsExtractor);
    }

    @Test
    public void shouldPassWhenOfferHasNoShopSku() {
        GcRawSku withShopSku = createGcRawSku(OFFER_ID);
        GcRawSku withoutShopSku = createGcRawSku(null);
        Validation.Result<GcRawSku> result = validation.validate(ImmutableList.of(withShopSku, withoutShopSku));

        assertThat(result.getInvalidValues()).isEmpty();
        assertThat(result.getMessages()).isEmpty();
    }

    @Test
    public void shouldPassWhenOfferExists() {
        GcRawSku gcRawSku = createGcRawSku(OFFER_ID);
        Validation.Result<GcRawSku> result = validation.validate(ImmutableList.of(gcRawSku));

        assertThat(result.getInvalidValues()).isEmpty();
        assertThat(result.getMessages()).isEmpty();
    }

    @Test
    public void shouldFailWhenOfferDoesNotExist() {
        GcRawSku gcRawSku = createGcRawSku("NON_EXISTING");
        Validation.Result<GcRawSku> result = validation.validate(ImmutableList.of(gcRawSku));

        assertThat(result.getInvalidValues()).containsExactly(gcRawSku);
        assertThat(result.getMessages()).extracting(MessageInfo::getCode)
                .containsExactly("ir.partner_content.dcp.excel.validation.no_offer_in_dc");
        assertThat(result.getMessages()).extracting(MessageInfo::getLevel)
                .containsExactly(MessageInfo.Level.ERROR);
    }

    @Test
    public void shouldPassWhenNoOffers() {
        Validation.Result<GcRawSku> result = validation.validate(Collections.emptyList());

        assertThat(result.getInvalidValues()).isEmpty();
        assertThat(result.getMessages()).isEmpty();
    }
}

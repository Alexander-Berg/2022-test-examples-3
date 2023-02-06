package ru.yandex.market.rg.asyncreport.fulfillment.supply;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import Market.DataCamp.DataCampOffer;
import Market.DataCamp.DataCampOfferContent;
import Market.DataCamp.DataCampOfferIdentifiers;
import Market.DataCamp.DataCampOfferMeta;
import Market.DataCamp.DataCampUnitedOffer;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.core.asyncreport.ReportState;
import ru.yandex.market.core.asyncreport.worker.model.ReportResult;
import ru.yandex.market.core.fulfillment.supply.FFSupplyTemplateParams;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.rg.asyncreport.supply.FFSupplyTemplateGenerator;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;

@DbUnitDataSet(before = "FFSupplyGeneratorTest.uCat.before.csv")
public class FFSupplyTemplateGeneratorTest extends FunctionalTest {

    @Autowired
    private FFSupplyTemplateGenerator ffSupplyTemplateGenerator;
    @Autowired
    private MdsS3Client mdsS3Client;
    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @BeforeEach
    void init() throws MalformedURLException {
        willReturn(new URL("http://mds.yandex.net/test.xlsx"))
                .given(mdsS3Client).getUrl(any());

        willReturn(
                SearchBusinessOffersResult.builder()
                        .setOffers(List.of(
                                generateDataCampOffer("Pelmeshka1234", "Pelmeshka iz gribov",
                                        List.of("123456", "123567")),
                                generateDataCampOffer("Varennik123", "Varennik iz kartoshki",
                                        List.of())
                        ))
                        .build())
                .given(dataCampShopClient).searchBusinessOffers(any());
    }

    @Test
    void testSuccess() {
        ReportResult report = ffSupplyTemplateGenerator.generate("report1",
                FFSupplyTemplateParams.newBuilder().withPartnerId(10103L).build());
        Assert.assertEquals(report.getNewState(), ReportState.DONE);
        Assert.assertEquals(report.getReportGenerationInfo().getUrlToDownload(),
                "http://mds.yandex.net/test.xlsx");
    }

    @Test
    void testEmpty() {
        willReturn(SearchBusinessOffersResult.builder().build())
                .given(dataCampShopClient).searchBusinessOffers(any());
        ReportResult report = ffSupplyTemplateGenerator.generate("report1",
                FFSupplyTemplateParams.newBuilder().withPartnerId(10103L).build());
        then(mdsS3Client).shouldHaveNoInteractions();
        Assert.assertEquals(report.getNewState(), ReportState.DONE);
        Assert.assertNull(report.getReportGenerationInfo().getUrlToDownload());
        Assert.assertEquals("Report is empty", report.getReportGenerationInfo().getDescription());
    }

    private DataCampUnitedOffer.UnitedOffer generateDataCampOffer(
            String offerId,
            String title,
            List<String> barcodes
    ) {
        return DataCampUnitedOffer.UnitedOffer.newBuilder()
                .setBasic(DataCampOffer.Offer.newBuilder()
                        .setIdentifiers(DataCampOfferIdentifiers.OfferIdentifiers.newBuilder()
                                .setOfferId(offerId)
                                .build())
                        .setContent(DataCampOfferContent.OfferContent.newBuilder()
                                .setPartner(DataCampOfferContent.PartnerContent.newBuilder()
                                        .setOriginal(DataCampOfferContent.OriginalSpecification.newBuilder()
                                                .setName(DataCampOfferMeta.StringValue.newBuilder()
                                                        .setValue(title)
                                                        .build())
                                                .setBarcode(DataCampOfferMeta.StringListValue.newBuilder()
                                                        .addAllValue(barcodes)
                                                        .build())
                                                .build())
                                        .build())
                                .build())
                        .build())

                .build();
    }
}

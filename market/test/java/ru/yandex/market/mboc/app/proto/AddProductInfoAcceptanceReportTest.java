package ru.yandex.market.mboc.app.proto;

import java.util.Map;
import java.util.stream.Collectors;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.mboc.common.offers.model.report.AcceptanceReport;
import ru.yandex.market.mboc.common.services.category.CategoryRepository;
import ru.yandex.market.mboc.common.services.category_info.info.CategoryInfo;
import ru.yandex.market.mboc.common.services.proto.datacamp.DatacampContext;
import ru.yandex.market.mboc.common.test.YamlTestUtil;
import ru.yandex.market.mboc.common.utils.OfferTestUtils;
import ru.yandex.market.mboc.http.MboMappings;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.market.mboc.common.offers.model.Offer.ProcessingStatus.OPEN;

public class AddProductInfoAcceptanceReportTest extends AddProductInfoHelperServiceTestBase {

    @Autowired
    private CategoryRepository categoryRepository;

    @Before
    public void setupCategory() {
        categoryRepository.insert(OfferTestUtils.defaultCategory());

        categoryInfoRepository.insert(OfferTestUtils.categoryInfoWithManualAcceptance()
            .setFbyAcceptanceMode(CategoryInfo.AcceptanceMode.AUTO_ACCEPT)
            .setFbyPlusAcceptanceMode(CategoryInfo.AcceptanceMode.AUTO_REJECT)
            .setFbsAcceptanceMode(CategoryInfo.AcceptanceMode.MANUAL)
            .setDsbsAcceptanceMode(CategoryInfo.AcceptanceMode.AUTO_ACCEPT)
            .setExpressAcceptanceMode(CategoryInfo.AcceptanceMode.AUTO_ACCEPT)
        );

        categoryKnowledgeService.addCategory(OfferTestUtils.TEST_CATEGORY_INFO_ID);
    }

    @Test
    public void testReportForming() throws InvalidProtocolBufferException {
        var json = YamlTestUtil.readAsString("offers/acceptance_report_request.json");

        var request = MboMappings.ProviderProductInfoRequest.newBuilder();
        JsonFormat.parser().merge(json, request);
        request.getProviderProductInfoBuilder(0).setMarketCategoryId((int) OfferTestUtils.TEST_CATEGORY_INFO_ID);

        var reports = service.formPreliminaryReport(request.build(), DatacampContext.builder().build());
        assertThat(reports).hasSize(1);

        var report = reports.get(0);
        var acceptanceReports = report.getReports();
        assertThat(acceptanceReports).hasSize(5);

        var compactReports = acceptanceReports.stream()
            .collect(
                Collectors.toMap(
                    AcceptanceReport::getTradeModel,
                    AcceptanceReport::getResolution
                )
            );
        assertThat(compactReports).containsAllEntriesOf(Map.of(
            AcceptanceReport.TradeModel.FBY,        AcceptanceReport.Resolution.OK,
            AcceptanceReport.TradeModel.FBY_PLUS,   AcceptanceReport.Resolution.TRASH,
            AcceptanceReport.TradeModel.FBS,        AcceptanceReport.Resolution.MANUAL,
            AcceptanceReport.TradeModel.DSBS,       AcceptanceReport.Resolution.OK,
            AcceptanceReport.TradeModel.EXPRESS,    AcceptanceReport.Resolution.OK
        ));

        var offer = report.getOffer();
        assertThat(offer).isNotNull();
        assertThat(offer.getProcessingStatus()).isEqualTo(OPEN);
    }
}

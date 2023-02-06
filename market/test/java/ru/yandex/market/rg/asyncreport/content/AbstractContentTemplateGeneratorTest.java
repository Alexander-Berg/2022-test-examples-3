package ru.yandex.market.rg.asyncreport.content;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;

import javax.annotation.Nonnull;

import Market.DataCamp.SyncAPI.SyncGetOffer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.asyncreport.worker.model.ReportResult;
import ru.yandex.market.core.offer.content.PartnerContentException;
import ru.yandex.market.ir.http.PartnerContent;
import ru.yandex.market.ir.http.PartnerContentService;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersRequest;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersResult;
import ru.yandex.market.mbi.datacamp.model.search.filter.PartnerSupplyPlan;
import ru.yandex.market.mbi.datacamp.model.search.filter.ResultContentStatus;
import ru.yandex.market.mbi.datacamp.model.search.filter.ResultOfferStatus;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.DataCampStrollerConversions;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * Тесты для генераторов категорийных шаблонов.
 * {@link AbstractContentTemplateGenerator}.
 *
 * @author Kirill Batalin (batalin@yandex-team.ru)
 */
class AbstractContentTemplateGeneratorTest extends FunctionalTest {

    @Autowired
    private MdsS3Client mdsS3Client;

    @Autowired
    @Qualifier("dataCampShopClient")
    private DataCampClient dataCampShopClient;

    @Autowired
    private PartnerContentService marketProtoPartnerContentService;

    @BeforeEach
    void init() throws MalformedURLException {
        Mockito.doReturn(new URL("http://example.com/"))
                .when(mdsS3Client).getUrl(any());
    }

    @AfterEach
    void checkMocks() {
        Mockito.verifyNoMoreInteractions(dataCampShopClient, marketProtoPartnerContentService);
    }

    protected SearchBusinessOffersRequest testSuccessAndGetRequest(AbstractContentTemplateGenerator generator,
                                                                   long partnerId) {
        mockDataCamp();
        mockMbo("testSuccess");

        PartnerContentParams params = getParams(partnerId);
        ReportResult result = generator.generate("report_id", params);

        Assertions.assertThat(result)
                .usingRecursiveComparison()
                .ignoringAllOverriddenEquals()
                .isEqualTo(ReportResult.done("http://example.com/"));

        checkMboMock("testSuccess");

        var captor = ArgumentCaptor.forClass(SearchBusinessOffersRequest.class);
        verify(dataCampShopClient)
                .searchBusinessOffers(captor.capture());

        SearchBusinessOffersRequest actualDataCampRequest = captor.getValue();
        Assertions.assertThat(actualDataCampRequest.getCategoryIds())
                .containsExactlyInAnyOrder(100L, 101L);
        Assertions.assertThat(actualDataCampRequest.getMarketCategoryIds())
                .containsExactlyInAnyOrder(99L);
        Assertions.assertThat(actualDataCampRequest.getAllowModelCreateUpdate())
                .isTrue();
        return actualDataCampRequest;
    }

    void testMboFail(AbstractContentTemplateGenerator generator, long partnerId) {
        mockDataCamp();
        mockMbo("testFail");

        PartnerContentParams params = getParams(partnerId);

        Assertions.assertThatThrownBy(() -> generator.generate("report_id", params))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Can't get content template")
                .getCause()
                .isInstanceOf(PartnerContentException.class)
                .hasMessage("UNKNOWN_CATEGORY: unknown category");

        checkMboMock("testFail");
        verify(dataCampShopClient).searchBusinessOffers(any());
        Mockito.verifyNoMoreInteractions(mdsS3Client);
    }

    private void mockDataCamp() {
        SyncGetOffer.GetUnitedOffersResponse rawResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "PartnerContentTemplateGeneratorTest/proto/testSuccess.datacamp.json",
                getClass()
        );
        SearchBusinessOffersResult response = DataCampStrollerConversions.fromStrollerResponse(rawResponse);
        Mockito.doReturn(response)
                .when(dataCampShopClient)
                .searchBusinessOffers(any());
    }

    private void checkMboMock(String path) {
        PartnerContent.GetFileTemplateRequest mboRequest = getMboRequest(path);
        verify(marketProtoPartnerContentService).getFileTemplate(mboRequest);
    }

    private void mockMbo(String path) {
        PartnerContent.GetFileTemplateRequest mboRequest = getMboRequest(path);
        PartnerContent.GetFileTemplateResponse mboResponse = ProtoTestUtil.getProtoMessageByJson(
                PartnerContent.GetFileTemplateResponse.class,
                "PartnerContentTemplateGeneratorTest/proto/" + path + ".mbo.response.json",
                getClass()
        );
        Mockito.doReturn(mboResponse)
                .when(marketProtoPartnerContentService)
                .getFileTemplate(mboRequest);
    }

    private PartnerContent.GetFileTemplateRequest getMboRequest(String path) {
        return ProtoTestUtil.getProtoMessageByJson(
                PartnerContent.GetFileTemplateRequest.class,
                "PartnerContentTemplateGeneratorTest/proto/" + path + ".mbo.request.json",
                getClass()
        );
    }

    @Nonnull
    private PartnerContentParams getParams(long partnerId) {
        PartnerContentParams params = new PartnerContentParams();
        params.setEntityId(partnerId);
        params.setTemplateCategoryId(99);
        params.setCategoryIds(Set.of(100L, 101L));
        params.setVendors(Set.of("vendor1", "vendor2"));
        params.setSupplyPlans(Set.of(PartnerSupplyPlan.WILL_SUPPLY));
        params.setResultOfferStatuses(Set.of(ResultOfferStatus.NOT_PUBLISHED_CHECKING));
        params.setResultContentStatuses(Set.of(ResultContentStatus.HAS_CARD_MARKET));
        return params;
    }
}

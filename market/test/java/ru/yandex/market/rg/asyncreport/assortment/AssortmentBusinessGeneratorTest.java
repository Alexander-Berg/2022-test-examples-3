package ru.yandex.market.rg.asyncreport.assortment;

import java.util.List;
import java.util.Set;

import Market.DataCamp.SyncAPI.GetVerdicts;
import Market.DataCamp.SyncAPI.SyncGetOffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.indexer.model.IndexerErrorLevel;
import ru.yandex.market.core.supplier.model.IndexerErrorInfo;
import ru.yandex.market.core.supplier.model.OfferInfo;
import ru.yandex.market.mbi.datacamp.stroller.DataCampStrollerConversions;
import ru.yandex.market.rg.asyncreport.assortment.model.AssortmentParams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;

@DbUnitDataSet(before = {"csv/AssortmentFunctionalTest.csv", "csv/AssortmentUniteEnv.csv"})
class AssortmentBusinessGeneratorTest extends AbstractAssortmentGeneratorTest {

    private static final long SUPPLIER_ID = 774L;

    public AssortmentBusinessGeneratorTest() {
        super(true, false);
    }

    @BeforeEach
    void mockVerdicts() {
        GetVerdicts.GetVerdictsBatchResponse verdictsResponse = ProtoTestUtil.getProtoMessageByJson(
                GetVerdicts.GetVerdictsBatchResponse.class,
                "proto/AssortmentGeneratorTest.business.verdicts.json",
                getClass()
        );
        doReturn(verdictsResponse)
                .when(dataCampShopClient).getVerdicts(any(), anyLong(), any());
    }

    @Test
    @DbUnitDataSet(before = {
            "csv/AssortmentGeneratorTest.unitedCatalogForPartner.csv",
            "csv/testVerdictsFromStroller.before.csv"
    })
    void testVerdicts() {

        SyncGetOffer.GetUnitedOffersResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/AssortmentGeneratorTest.DataCampGetUnitedOffersResponseWithFullBasic.json",
                getClass()
        );
        doReturn(DataCampStrollerConversions.fromStrollerResponse(getUnitedOffersResponse))
                .when(dataCampShopClient).searchBusinessOffers(any());

        AssortmentParams assortmentParams = new AssortmentParams();
        assortmentParams.setEntityId(SUPPLIER_ID);
        assortmentParams.setIncludeServiceIds(Set.of(1001L, 1002L));


        List<OfferInfo> capturedOffers = runGeneratorAndCaptureOfferInfo(assortmentParams);
        assertThat(capturedOffers).hasSize(1);
        assertThat(capturedOffers.get(0).getIndexerErrorInfos()).containsAll(
                List.of(
                        IndexerErrorInfo.builder()
                                .withLevel(IndexerErrorLevel.ERROR)
                                .withDescription("text_2")
                                .build(),
                        IndexerErrorInfo.builder()
                                .withLevel(IndexerErrorLevel.ERROR)
                                .withDescription("Не заполнено обязательное поле")
                                .withRecommendation("Заполните поле «Вес в упаковке в килограммах» в каталоге.")
                                .withCountShopsInfo("Относится к 1 магазину")
                                .build(),
                        IndexerErrorInfo.builder()
                                .withLevel(IndexerErrorLevel.ERROR)
                                .withDescription("text_1")
                                .withCountShopsInfo("Относится к 2 магазинам")
                                .build()
                )
        );
    }

    @Test
    @DbUnitDataSet(before = {
            "csv/AssortmentGeneratorTest.unitedCatalogForPartner.csv",
            "csv/AssortmentEmptyBasicEnv.csv"
    })
    void testFilterEmptyBasicPositive() {

        SyncGetOffer.GetUnitedOffersResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/AssortmentGeneratorTest.DataCampGetUnitedOffersResponseWithFullBasic.json",
                getClass()
        );
        doReturn(DataCampStrollerConversions.fromStrollerResponse(getUnitedOffersResponse))
                .when(dataCampShopClient).searchBusinessOffers(any());

        List<OfferInfo> capturedOffers = runGeneratorAndCaptureOfferInfo(SUPPLIER_ID);
        assertThat(capturedOffers).hasSize(1);
    }

    @Test
    @DbUnitDataSet(before = {
            "csv/AssortmentGeneratorTest.unitedCatalogForPartner.csv",
            "csv/AssortmentEmptyBasicEnv.csv"
    })
    void testFilterEmptyBasicNegative() {

        SyncGetOffer.GetUnitedOffersResponse getUnitedOffersResponse = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/AssortmentGeneratorTest.DataCampGetUnitedOffersResponse_774.json",
                getClass()
        );
        doReturn(DataCampStrollerConversions.fromStrollerResponse(getUnitedOffersResponse))
                .when(dataCampShopClient).searchBusinessOffers(any());

        List<OfferInfo> capturedOffers = runGeneratorAndCaptureOfferInfo(SUPPLIER_ID);
        assertThat(capturedOffers).isEmpty();
    }
}

package ru.yandex.market.mbi.affiliate.promo.service;

import java.util.concurrent.atomic.AtomicInteger;

import Market.DataCamp.DataCampPromo;
import Market.DataCamp.SyncAPI.SyncGetPromo;
import okhttp3.ResponseBody;
import org.dbunit.database.DatabaseConfig;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import retrofit2.Response;

import ru.yandex.market.common.test.db.DbUnitDataBaseConfig;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.common.test.db.DbUnitTestExecutionListener;
import ru.yandex.market.loyalty.api.exception.MarketLoyaltyException;
import ru.yandex.market.loyalty.api.model.MarketLoyaltyErrorCode;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeCheckRequest;
import ru.yandex.market.loyalty.api.model.promocode.PromocodeCheckResponse;
import ru.yandex.market.loyalty.client.MarketLoyaltyClient;
import ru.yandex.market.mbi.affiliate.promo.common.AffiliatePromoException;
import ru.yandex.market.mbi.affiliate.promo.config.FunctionalTestConfig;
import ru.yandex.market.mbi.affiliate.promo.random.RandomStringGenerator;
import ru.yandex.market.mbi.affiliate.promo.stroller.StrollerJsonApi;
import ru.yandex.market.mbi.affiliate.promo.stroller.StrollerProtoApi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = FunctionalTestConfig.class)
@TestExecutionListeners(listeners = {
        DbUnitTestExecutionListener.class,
}, mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
@DbUnitDataBaseConfig(
        @DbUnitDataBaseConfig.Entry(
                name = DatabaseConfig.FEATURE_ALLOW_EMPTY_FIELDS,
                value = "true"))
@DbUnitDataSet(dataSource = "promoDataSource", before = "db/promocode_generation_before.csv")
public class PromocodeGenerationServiceTest {
    private static final long START_DATE = 1656670634;
    private static final long END_DATE = 1656929834;

    @Autowired
    private RandomStringGenerator mockRandomStringGenerator;

    @Autowired
    private MarketLoyaltyClient marketLoyaltyClient;

    @Autowired
    private StrollerProtoApi strollerProtoApi;
    @Autowired
    private StrollerJsonApi strollerJsonApi;

    @Autowired
    private PromocodeGenerationService service;

    @After
    public void tearDown() {
        Mockito.reset(mockRandomStringGenerator, marketLoyaltyClient, strollerProtoApi, strollerJsonApi);
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", after = "db/promocode_generation_new_gen_after.csv")
    public void testGeneratePromocodes() throws Exception {
        when(mockRandomStringGenerator.nextString())
                .thenReturn("OOOO")
                .thenReturn("XXXA")
                .thenReturn("XXXB")
                .thenReturn("XXXC")
                .thenReturn("XXXD")
                .thenReturn("XXXE");
        ArgumentCaptor<SyncGetPromo.GetPromoBatchRequest> strollerRequestCaptor =
                ArgumentCaptor.forClass(SyncGetPromo.GetPromoBatchRequest.class);
        when(strollerProtoApi.getPromosByIds(strollerRequestCaptor.capture()).execute())
                .thenReturn(Response.success(getParentStrollerResponse()));
        ArgumentCaptor<PromocodeCheckRequest> checkRequestCaptor =
                ArgumentCaptor.forClass(PromocodeCheckRequest.class);
        when(marketLoyaltyClient.checkPromocode(checkRequestCaptor.capture()))
                .thenReturn(new PromocodeCheckResponse(true, null));

        service.generatePromocodes(10001, 12345, 5);
        verify(mockRandomStringGenerator, times(6)).nextString();
        assertThat(strollerRequestCaptor.getValue().getEntries(0).getPromoId()).isEqualTo("aff_parent_10001");
        assertThat(checkRequestCaptor.getAllValues().stream().allMatch(r -> r.getStartDate().getTime() == START_DATE));
        assertThat(checkRequestCaptor.getAllValues().stream().allMatch(r -> r.getEndDate().getTime() == END_DATE));
        assertThat(checkRequestCaptor.getAllValues().stream().map(PromocodeCheckRequest::getCode))
                .containsExactly("XXXA-AG-AF", "XXXB-AG-AF", "XXXC-AG-AF", "XXXD-AG-AF", "XXXE-AG-AF");
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", after = "db/promocode_generation_new_gen_after2.csv")
    public void testGenerateCheckFails() throws Exception {
        when(mockRandomStringGenerator.nextString())
                .thenReturn("OOOO")
                .thenReturn("XXXA")
                .thenReturn("XXXB")
                .thenReturn("XXXC");
        when(strollerProtoApi.getPromosByIds(any()).execute())
                .thenReturn(Response.success(getParentStrollerResponse()));
        when(marketLoyaltyClient.checkPromocode(any()))
                .thenReturn(new PromocodeCheckResponse(true, null))
                .thenReturn(new PromocodeCheckResponse(false, MarketLoyaltyErrorCode.PROMOCODE_IS_OCCUPIED_NOW))
                .thenReturn(new PromocodeCheckResponse(true, null));
        service.generatePromocodes(10001, 12345, 2);
        verify(mockRandomStringGenerator, times(4)).nextString();
    }

    @Test
    public void testGenerateCheckAlwaysFails() throws Exception {
        AtomicInteger i = new AtomicInteger();
        doAnswer(invocation -> "S" + i.incrementAndGet()).when(mockRandomStringGenerator).nextString();

        when(strollerProtoApi.getPromosByIds(any()).execute())
                .thenReturn(Response.success(getParentStrollerResponse()));
        when(marketLoyaltyClient.checkPromocode(any()))
                .thenReturn(new PromocodeCheckResponse(false, MarketLoyaltyErrorCode.PROMOCODE_IS_OCCUPIED_NOW));
        assertThrows(AffiliatePromoException.class,
                () -> service.generatePromocodes(10001, 12345, 2));
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", after = "db/promocode_generation_new_gen_after2.csv")
    public void testGenerateReservationFails() throws Exception {
        when(mockRandomStringGenerator.nextString())
                .thenReturn("OOOO")
                .thenReturn("XXXA")
                .thenReturn("XXXB")
                .thenReturn("XXXC");
        when(strollerProtoApi.getPromosByIds(any()).execute())
                .thenReturn(Response.success(getParentStrollerResponse()));
        when(marketLoyaltyClient.checkPromocode(any())).thenReturn(new PromocodeCheckResponse(true, null));
        when(marketLoyaltyClient.reservePromocode("XXXB-AG-AF")).thenThrow(new MarketLoyaltyException("something bad happened"));
        service.generatePromocodes(10001, 12345, 2);
        verify(mockRandomStringGenerator, times(4)).nextString();
    }

    @Test
    public void testGenerateBadPromoDescription() {
        assertThrows(AffiliatePromoException.class,
                () -> service.generatePromocodes(10008, 12345, 100));
    }

    @Test
    public void testGenerateBadClid() {
        assertThrows(AffiliatePromoException.class,
                () -> service.generatePromocodes(10001, 88888, 87));

    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", after = "db/promocode_generation_try_upload_after.csv")
    public void testTryUpload() throws Exception {
        ArgumentCaptor<SyncGetPromo.GetPromoBatchRequest> strollerGetRequestCaptor =
                ArgumentCaptor.forClass(SyncGetPromo.GetPromoBatchRequest.class);
        when(strollerProtoApi.getPromosByIds(strollerGetRequestCaptor.capture()).execute())
                .thenReturn(Response.success(getParentStrollerResponse()));
        ArgumentCaptor<DataCampPromo.PromoDescription> strollerUpdateRequestCaptor =
                ArgumentCaptor.forClass(DataCampPromo.PromoDescription.class);
        when(strollerJsonApi.addPromo(strollerUpdateRequestCaptor.capture()).execute())
                .thenReturn(Response.success(DataCampPromo.PromoDescription.newBuilder().build()));

        service.tryUpload(100);

        assertThat(strollerGetRequestCaptor.getValue().getEntries(0).getPromoId()).isEqualTo("aff_parent_10000");
        assertThat(strollerUpdateRequestCaptor.getAllValues().stream().map(p -> p.getMechanicsData().getBluePromocode().getPromoCode()))
                .containsExactlyInAnyOrder("AAAA-AG-AF", "AAAB-AG-AF", "AAAC-AG-AF");
        assertThat(strollerUpdateRequestCaptor.getAllValues().stream().allMatch(p -> p.getConstraints().getStartDate() == START_DATE));
        assertThat(strollerUpdateRequestCaptor.getAllValues().stream().allMatch(p -> p.getConstraints().getEndDate() == END_DATE));
    }

    @Test
    @DbUnitDataSet(dataSource = "promoDataSource", after = "db/promocode_generation_try_upload_failure_after.csv")
    public void testTryUploadWithFailure() throws Exception {
        when(strollerProtoApi.getPromosByIds(any()).execute())
                .thenReturn(Response.success(getParentStrollerResponse()));

        ArgumentCaptor<DataCampPromo.PromoDescription> strollerUpdateRequestCaptor =
                ArgumentCaptor.forClass(DataCampPromo.PromoDescription.class);
        when(strollerJsonApi.addPromo(strollerUpdateRequestCaptor.capture()).execute())
                .thenReturn(Response.success(DataCampPromo.PromoDescription.newBuilder().build()))
                .thenReturn(Response.error(500, ResponseBody.create(null, "internal-error")))
                .thenReturn(Response.success(DataCampPromo.PromoDescription.newBuilder().build()));

        service.tryUpload(100);
    }

    private static SyncGetPromo.GetPromoBatchResponse getParentStrollerResponse() {
        return SyncGetPromo.GetPromoBatchResponse.newBuilder()
                .setPromos(DataCampPromo.PromoDescriptionBatch.newBuilder()
                        .addPromo(DataCampPromo.PromoDescription.newBuilder()
                                .setConstraints(DataCampPromo.PromoConstraints.newBuilder()
                                        .setStartDate(START_DATE)
                                        .setEndDate(END_DATE)
                                )
                        ))
                .build();
    }
}
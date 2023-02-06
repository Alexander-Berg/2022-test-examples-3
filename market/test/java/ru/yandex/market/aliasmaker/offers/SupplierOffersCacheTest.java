package ru.yandex.market.aliasmaker.offers;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.googlecode.protobuf.format.JsonFormat;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import ru.yandex.market.aliasmaker.TestFileUtils;
import ru.yandex.market.aliasmaker.cache.offers.SupplierOfferConverter;
import ru.yandex.market.aliasmaker.cache.offers.SupplierOffersCache;
import ru.yandex.market.ir.http.Formalizer;
import ru.yandex.market.ir.http.Formalizer.FormalizerResponse;
import ru.yandex.market.ir.http.FormalizerService;
import ru.yandex.market.ir.http.FormalizerServiceStub;
import ru.yandex.market.ir.http.Offer.YmlParam;
import ru.yandex.market.mboc.http.MboCategory;
import ru.yandex.market.mboc.http.MboCategoryService;
import ru.yandex.market.mboc.http.MboCategoryServiceStub;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.SupplierOffer;
import ru.yandex.market.mdm.http.MasterDataProto;
import ru.yandex.market.mdm.http.MasterDataService;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("checkstyle:magicnumber")
public class SupplierOffersCacheTest {
    public static final int TEST_CATEGORY_ID = 90787;
    public static final Long WRONG_ID = -123L;
    private SupplierOffersCache cache;

    @Before
    public void setup() {
        MboMappings.SearchMappingsResponse supplierOffers =
                TestFileUtils.load("/supplierOffers.json", MboMappings.SearchMappingsResponse.newBuilder()).build();

        FormalizerResponse formalizerResponse =
                TestFileUtils.load("/formalizerResponse.json", FormalizerResponse.newBuilder()).build();

        FormalizerService formalizerService = mock(FormalizerService.class);
        doAnswer((Answer<Object>) invocation -> {
            Formalizer.FormalizerRequest request = invocation.getArgument(0, Formalizer.FormalizerRequest.class);
            Formalizer.Offer offer = request.getOffer(0);
            Assert.assertEquals(supplierOffers.getOffers(0).getTitle(), offer.getTitle());
            return formalizerResponse;

        }).when(formalizerService).formalize(any());

        MasterDataService masterDataService = mock(MasterDataService.class);
        MasterDataProto.SearchSskuMasterDataResponse masterDataResponse =
                TestFileUtils.load("/masterData.json", MasterDataProto.SearchSskuMasterDataResponse.newBuilder()).build();
        when(masterDataService.searchSskuMasterData(any()))
                .thenReturn(masterDataResponse);

        MboCategoryService mboCategoryService = mock(MboCategoryService.class);
        cache = new SupplierOffersCache(mboCategoryService, formalizerService, masterDataService);
    }

    @Test
    public void toXmlTest() {
        List<YmlParam> ymlParams = Arrays.asList(
                YmlParam.newBuilder().setName("Vendor_id").setValue("12774998").build(),
                YmlParam.newBuilder().setName("Категория Маркета").setValue("Пазлы").build()
        );
        String xml = SupplierOfferConverter.toXml(ymlParams);
        Assert.assertEquals("<?xml version='1.0' encoding='UTF-8'?>" +
                        "<offer_params>" +
                        "<param name=\"Vendor_id\" unit=\"\">12774998</param>" +
                        "<param name=\"Категория Маркета\" unit=\"\">Пазлы</param" +
                        "></offer_params>",
                xml
        );
    }

    @Test
    public void toOfferTest() {
        SupplierOffer.Offer supplierOffer =
                TestFileUtils.load("/supplierOffers.json", SupplierOffer.Offer.newBuilder()).build();

        Offer offer = SupplierOfferConverter.toOffer(supplierOffer, Collections.emptyList(), null);
        System.out.println(offer.toString());
    }

    @Test
    public void getOffersShouldFall() {
        try {
            List<Offer> offers = cache.getOffers(List.of(WRONG_ID));
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("failed after 3 retries"));
        }
    }

    /**
     * Дергает формализатор, для получения образца ответа формализатора.
     */
    @Test
    @Ignore
    public void runFormalize() throws JSONException {
        Formalizer.Offer.Builder offer =
                TestFileUtils.load("/formalizerRequest.json", Formalizer.Offer.newBuilder());

        FormalizerServiceStub formalizer = new FormalizerServiceStub();
        formalizer.setHost("http://cs-formalizer.tst.vs.market.yandex.net:34512/");

        FormalizerResponse response = formalizer.formalize(
                Formalizer.FormalizerRequest.newBuilder()
                        .addOffer(offer)
                        .build()
        );

        System.out.println(
                new JSONObject(JsonFormat.printToString(response)).toString(2)
        );
    }

    /**
     * Дергает ручку MBOC для получения образца синего оффера.
     */
    @Test
    @Ignore
    public void runMboCategoryService() throws Exception {
        MboCategoryServiceStub mboCategoryService = new MboCategoryServiceStub();
        mboCategoryService.setHost("https://cm-testing.market.yandex-team.ru/proto/mboCategoryService/");

        final long offerId = 4201132;
        List<SupplierOffer.Offer> offers = mboCategoryService.searchMappingsBulkByOfferId(
                MboCategory.SearchMappingsBulkAMRequest.newBuilder().addOfferIds(offerId).build()
        ).getOffersList();

        System.out.println(
                new JSONObject(JsonFormat.printToString(offers.get(0))).toString(2));
    }
}

package ru.yandex.vendor.csbilling;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import ru.yandex.cs.billing.api.client.CsBillingApiRetrofitClient;
import ru.yandex.cs.billing.campaign.model.CampaignInfo;
import ru.yandex.cs.billing.campaign.model.ContractType;
import ru.yandex.cs.billing.cutoff.model.DsCutoff;
import ru.yandex.cs.billing.cutoff.model.DsCutoffType;
import ru.yandex.vendor.exception.BadParamException;
import ru.yandex.vendor.products.model.VendorProduct;
import ru.yandex.vendor.vendors.CampaignProductInfoRetriever;
import ru.yandex.vendor.vendors.VendorServiceSql;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.cs.billing.cutoff.model.DsCutoffType.ADMIN;
import static ru.yandex.cs.billing.cutoff.model.DsCutoffType.CLIENT;
import static ru.yandex.cs.billing.cutoff.model.DsCutoffType.FINANCE;
import static ru.yandex.cs.billing.cutoff.model.DsCutoffType.OFFER_DOCUMENTS;
import static ru.yandex.vendor.VendorTestUtils.assertParamInvalid;
import static ru.yandex.vendor.products.model.VendorProduct.RECOMMENDED_SHOPS;
import static ru.yandex.vendor.util.Utils.mapToList;

@RunWith(JUnit4.class)
public class HttpCsBillingApiServiceTest {

    private static final long UID = 123456;
    private static final long VENDOR_ID = 42;
    private static final long CAMPAIGN_ID = 101;
    private static final long DATASOURCE_ID = 202;
    private static final VendorProduct PRODUCT = RECOMMENDED_SHOPS;
    private static final String PRODUCT_KEY = PRODUCT.getProductKey();

    @Test
    public void request_payment_fails_on_illegal_product_key() throws Exception {
        HttpCsBillingApiService service = new HttpCsBillingApiService(null, null, null, null);
        try {
            service.requestPayment(VENDOR_ID, "qwe", 100, UID);
            fail();
        } catch (BadParamException e) {
            assertParamInvalid(e, "productKey");
        }
    }

    @Test
    public void request_payment_fails_on_illegal_sum() throws Exception {
        HttpCsBillingApiService service = new HttpCsBillingApiService(null, null, null, null);
        try {
            service.requestPayment(VENDOR_ID, PRODUCT_KEY, -1, UID);
            fail();
        } catch (BadParamException e) {
            assertParamInvalid(e, "sumInCents");
        }
        try {
            service.requestPayment(VENDOR_ID, PRODUCT_KEY, 3_333_333_334L, UID);
            fail();
        } catch (BadParamException e) {
            assertParamInvalid(e, "sumInCents");
        }
    }

    @Test
    public void request_payment_fails_on_non_offer_campaign() throws Exception {
        // given:
        HttpCsBillingApiService service = createNonOfferService();
        try {
            // when
            service.requestPayment(VENDOR_ID, PRODUCT_KEY, 100, UID);
            fail();
        } catch (BadParamException e) {
            // then
            assertParamInvalid(e, "NON_PREPAID_CONTRACT_TYPE_PRODUCT_PAYMENT", "productKey");
        }
    }

    @Test
    public void request_payment_fails_on_some_cutoffs() throws Exception {
        test_request_NOT_fails_on_cutoffs(singletonList(ADMIN));
        test_request_NOT_fails_on_cutoffs(singletonList(OFFER_DOCUMENTS));
        test_request_NOT_fails_on_cutoffs(asList(ADMIN, OFFER_DOCUMENTS));
    }

    private void test_request_NOT_fails_on_cutoffs(List<DsCutoffType> cutoffs) {
        // given:
        HttpCsBillingApiService service = createService(cutoffs);
        try {
            // when
            service.requestPayment(VENDOR_ID, PRODUCT_KEY, 100, UID);
        } catch (BadParamException e) {
            // then
            fail();
        }
    }

    @Test
    public void request_payment_calls_api_client() throws Exception {
        test_request_payment_does_not_fail_with_some_cutoffs(emptyList(), 345);
        test_request_payment_does_not_fail_with_some_cutoffs(singletonList(CLIENT), 333);
        test_request_payment_does_not_fail_with_some_cutoffs(singletonList(FINANCE), 876);
        test_request_payment_does_not_fail_with_some_cutoffs(asList(CLIENT, FINANCE), 12);

    }

    private void test_request_payment_does_not_fail_with_some_cutoffs(List<DsCutoffType> cutoffs, long sum) {
        // given:
        HttpCsBillingApiService service = createService(cutoffs);
        // when
        String response = service.requestPayment(VENDOR_ID, PRODUCT_KEY, sum, UID);
        // then
        assertEquals("mock_success:" + sum, response);
    }

    private HttpCsBillingApiService createNonOfferService() {
        return createService(false, emptyList());
    }

    private HttpCsBillingApiService createService(List<DsCutoffType> cutoffs) {
        return createService(true, cutoffs);
    }

    private HttpCsBillingApiService createService(boolean offer, List<DsCutoffType> cutoffTypes) {
        CampaignInfo info = new CampaignInfo();
        info.setOffer(offer);
        info.setCampaignId(CAMPAIGN_ID);
        info.setDatasourceId(DATASOURCE_ID);
        if (offer) {
            info.setContractType(ContractType.PREPAID.getId());
        }
        CampaignProductInfoRetriever retriever = mock(CampaignProductInfoRetriever.class);
        VendorServiceSql vendorServiceSql = mock(VendorServiceSql.class);
        CsBillingApiRetrofitClient retrofitClient = mock(CsBillingApiRetrofitClient.class);
        when(retriever.getActiveCampaignInfo(VENDOR_ID, PRODUCT)).thenReturn(of(info));
        CsBillingApiClient apiClient = mock(CsBillingApiClient.class);
        List<DsCutoff> cutoffs = mapToList(cutoffTypes, HttpCsBillingApiServiceTest::createDsCutoff);
        when(apiClient.listActualCutoffsPerDatasource(DATASOURCE_ID, PRODUCT.getServiceId())).thenReturn(cutoffs);
        when(apiClient.requestPayment(eq(CAMPAIGN_ID), eq(PRODUCT.getServiceId()), anyLong(), eq(UID)))
                .then(inv -> "mock_success:" + inv.getArguments()[2]);
        return new HttpCsBillingApiService(retrofitClient, apiClient, retriever, vendorServiceSql);
    }

    private static DsCutoff createDsCutoff(DsCutoffType t) {
        DsCutoff cutoff = new DsCutoff();
        cutoff.setType(t);
        return cutoff;
    }
}

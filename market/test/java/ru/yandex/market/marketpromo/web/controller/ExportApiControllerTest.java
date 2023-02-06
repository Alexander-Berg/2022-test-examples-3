package ru.yandex.market.marketpromo.web.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.marketpromo.core.application.context.ProcessingQueueType;
import ru.yandex.market.marketpromo.core.dao.DatacampOfferDao;
import ru.yandex.market.marketpromo.core.dao.PromoDao;
import ru.yandex.market.marketpromo.filter.AssortmentRequest;
import ru.yandex.market.marketpromo.filter.PromoFilter;
import ru.yandex.market.marketpromo.filter.PromoRequest;
import ru.yandex.market.marketpromo.model.OfferDisabledSource;
import ru.yandex.market.marketpromo.model.Promo;
import ru.yandex.market.marketpromo.model.processing.ProcessingRequestStatus;
import ru.yandex.market.marketpromo.model.processing.ProcessingRequestType;
import ru.yandex.market.marketpromo.processing.ProcessId;
import ru.yandex.market.marketpromo.processing.ProcessingController;
import ru.yandex.market.marketpromo.processing.ProcessingTask;
import ru.yandex.market.marketpromo.service.ProcessingQueueService;
import ru.yandex.market.marketpromo.test.MockedWebTestBase;
import ru.yandex.market.marketpromo.utils.IdentityUtils;
import ru.yandex.market.marketpromo.web.model.response.ExportResultResponse;
import ru.yandex.market.marketpromo.web.model.response.ExportTokenResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.basePrice;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.categoryId;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.datacampOffer;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.disabled;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.disabledSource;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.name;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.potentialPromo;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.price;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.shopSku;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.stocks;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.warehouse;
import static ru.yandex.market.marketpromo.core.test.generator.PromoMechanics.minimalDiscountPercentSize;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.directDiscount;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promo;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promoId;
import static ru.yandex.market.marketpromo.core.test.generator.Promos.promoName;
import static ru.yandex.market.marketpromo.model.processing.ProcessingRequestType.EXPORT_ASSORTMENT;
import static ru.yandex.market.marketpromo.model.processing.ProcessingRequestType.EXPORT_PROMO;

public class ExportApiControllerTest extends MockedWebTestBase {

    private static final long WAREHOUSE_ID = 123L;
    private static final String SSKU_1 = "ssku-123";
    private static final String DD_PROMO_ID = "#21098";
    private static final String DD_PROMO_NAME = "DD Promo";

    @Autowired
    private DatacampOfferDao datacampOfferDao;
    @Autowired
    private ProcessingQueueService processingQueueService;
    @Autowired
    private PromoDao promoDao;
    @Autowired
    @ProcessingQueueType(EXPORT_ASSORTMENT)
    private ProcessingController<ProcessingRequestStatus, ProcessingRequestType> exportAssortmentController;
    @Autowired
    @ProcessingQueueType(EXPORT_PROMO)
    private ProcessingController<ProcessingRequestStatus, ProcessingRequestType> exportPromoController;

    private Promo directDiscount;

    @BeforeEach
    void configure() {
        directDiscount = promoDao.replace(promo(
                promoId(DD_PROMO_ID),
                promoName(DD_PROMO_NAME),
                directDiscount(
                        minimalDiscountPercentSize(10)
                )
        ));
        datacampOfferDao.replace(List.of(
                datacampOffer(
                        name(SSKU_1),
                        shopSku(SSKU_1),
                        warehouse(WAREHOUSE_ID),
                        price(1000),
                        basePrice(1500),
                        disabledSource(OfferDisabledSource.MARKET_ABO),
                        disabledSource(OfferDisabledSource.MARKET_IDX),
                        disabled(true),
                        stocks(15L),
                        categoryId(123L),
                        potentialPromo(DD_PROMO_ID, BigDecimal.valueOf(150))
                )
        ));
    }

    @Test
    void shouldRespondOnAssortmentGetExport() throws Exception {
        List<ProcessingTask<ProcessingRequestStatus>> requestsInQueue = exportAssortmentController.processesOf(Set.of(
                ProcessingRequestStatus.IN_QUEUE,
                ProcessingRequestStatus.PROCESSING
        ), 10);
        assertThat(requestsInQueue, empty());

        ExportTokenResponse exportResponse = objectMapper.readValue(mockMvc.perform(
                get("/v1/promos/{promoId}/assortment/export", IdentityUtils.encodePromoId(directDiscount))
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(), ExportTokenResponse.class);

        requestsInQueue = exportAssortmentController.processesOf(Set.of(
                ProcessingRequestStatus.IN_QUEUE,
                ProcessingRequestStatus.PROCESSING
        ), 10);
        assertThat(requestsInQueue.size(), is(1));

        assertThat(IdentityUtils.encodeProcessId(requestsInQueue.get(0).getProcessId()), is((exportResponse.getToken())));
    }

    @Test
    void shouldRespondOnPromoGetExport() throws Exception {
        List<ProcessingTask<ProcessingRequestStatus>> requestsInQueue = exportPromoController.processesOf(Set.of(
                ProcessingRequestStatus.IN_QUEUE,
                ProcessingRequestStatus.PROCESSING
        ), 10);
        assertThat(requestsInQueue, empty());

        ExportTokenResponse exportResponse = objectMapper.readValue(mockMvc.perform(
                get("/v1/promos/export", IdentityUtils.encodePromoId(directDiscount))
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(), ExportTokenResponse.class);

        requestsInQueue = exportPromoController.processesOf(Set.of(
                ProcessingRequestStatus.IN_QUEUE,
                ProcessingRequestStatus.PROCESSING
        ), 10);
        assertThat(requestsInQueue.size(), is(1));

        assertThat(IdentityUtils.encodeProcessId(requestsInQueue.get(0).getProcessId()), is((exportResponse.getToken())));
    }

    @Test
    void shouldRespondOnSameAssortmentGetExport() throws Exception {
        List<ProcessingTask<ProcessingRequestStatus>> requestsInQueue = exportAssortmentController.processesOf(Set.of(
                ProcessingRequestStatus.IN_QUEUE,
                ProcessingRequestStatus.PROCESSING
        ), 10);
        assertThat(requestsInQueue, empty());

        ExportTokenResponse exportResponse = objectMapper.readValue(mockMvc.perform(
                get("/v1/promos/{promoId}/assortment/export", IdentityUtils.encodePromoId(directDiscount))
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(), ExportTokenResponse.class);

        requestsInQueue = exportAssortmentController.processesOf(Set.of(
                ProcessingRequestStatus.IN_QUEUE,
                ProcessingRequestStatus.PROCESSING
        ), 10);
        assertThat(requestsInQueue.size(), is(1));

        assertThat(IdentityUtils.encodeProcessId(requestsInQueue.get(0).getProcessId()), is((exportResponse.getToken())));

        ExportTokenResponse exportResponseOnSameRequest = objectMapper.readValue(mockMvc.perform(
                get("/v1/promos/{promoId}/assortment/export", IdentityUtils.encodePromoId(directDiscount))
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(), ExportTokenResponse.class);

        requestsInQueue = exportAssortmentController.processesOf(Set.of(
                ProcessingRequestStatus.IN_QUEUE,
                ProcessingRequestStatus.PROCESSING
        ), 10);
        assertThat(requestsInQueue.size(), is(1));

        assertThat(IdentityUtils.encodeProcessId(requestsInQueue.get(0).getProcessId()), is((exportResponse.getToken())));
    }

    @Test
    void shouldReturnPromoExportStatus() throws Exception {
        ProcessId processId = processingQueueService.enqueue(
                ProcessId.of(ProcessingRequestType.EXPORT_PROMO, UUID.randomUUID().toString()),
                PromoRequest.builder().filter(PromoFilter.NAME, directDiscount.getName()).build());

        ExportResultResponse exportResponse = objectMapper.readValue(mockMvc.perform(
                get("/v1/export/{token}/result",
                        IdentityUtils.encodeProcessId(processId))
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(), ExportResultResponse.class);

        assertThat(exportResponse.getProcessingRequestStatus(), is(ProcessingRequestStatus.IN_QUEUE));
        assertThat(exportResponse.getResultUrl(), nullValue());
    }

    @Test
    void shouldReturnAssormentExportStatus() throws Exception {
        ProcessId processId = processingQueueService.enqueue(
                ProcessId.of(EXPORT_ASSORTMENT, UUID.randomUUID().toString()),
                AssortmentRequest.builder(directDiscount.toPromoKey()).build());

        ExportResultResponse exportResponse = objectMapper.readValue(mockMvc.perform(
                get("/v1/export/{token}/result",
                        IdentityUtils.encodeProcessId(processId))
                        .contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString(), ExportResultResponse.class);

        assertThat(exportResponse.getProcessingRequestStatus(), is(ProcessingRequestStatus.IN_QUEUE));
        assertThat(exportResponse.getResultUrl(), nullValue());
    }

}

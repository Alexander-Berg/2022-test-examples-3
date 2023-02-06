package ru.yandex.market.tpl.billing.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import lombok.SneakyThrows;
import okhttp3.MediaType;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import ru.yandex.common.util.date.TestableClock;
import ru.yandex.market.common.test.db.DbUnitDataSet;
import ru.yandex.market.mbi.tariffs.client.model.BillingUnitEnum;
import ru.yandex.market.mbi.tariffs.client.model.CommonJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.ModelType;
import ru.yandex.market.mbi.tariffs.client.model.Partner;
import ru.yandex.market.mbi.tariffs.client.model.PvzBrandingTypeEnum;
import ru.yandex.market.mbi.tariffs.client.model.PvzTariffRewardJsonSchema;
import ru.yandex.market.mbi.tariffs.client.model.PvzTariffZoneEnum;
import ru.yandex.market.mbi.tariffs.client.model.ServiceTypeEnum;
import ru.yandex.market.mbi.tariffs.client.model.TariffDTO;
import ru.yandex.market.mbi.tariffs.client.model.TariffFindQuery;
import ru.yandex.market.tpl.billing.AbstractFunctionalTest;
import ru.yandex.market.tpl.billing.service.tariff.TariffService;
import ru.yandex.market.tpl.billing.service.tariff.TariffsIterator;
import ru.yandex.market.tpl.billing.util.BillingConstants;
import ru.yandex.yadoc.FileResource;
import ru.yandex.yadoc.YaDocClient;
import ru.yandex.yadoc.client.model.DocumentMeta;
import ru.yandex.yadoc.client.model.DocumentsContractsRequest;
import ru.yandex.yadoc.client.model.DocumentsMeta;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class PvzControllerTest extends AbstractFunctionalTest {

    @Autowired
    YaDocClient yaDocClient;

    @Autowired
    private PvzController pvzController;

    @Autowired
    TestableClock clock;

    @Autowired
    private TariffService tariffService;

    private MockMvc restMockMvc;

    @BeforeEach
    void setUp() {
        restMockMvc = MockMvcBuilders.standaloneSetup(pvzController).build();

        doAnswer(invocation -> new TariffsIterator((page, size) -> {
            if (page != 0) {
                return List.of();
            }

            TariffDTO generalTariff = createTariff(1L, null);
            generalTariff.setMeta(List.of(
                    createMeta(20, 0, 1_000_000),
                    createMeta(15, 1_000_000, 1_500_000),
                    createMeta(10, 1_500_000, 3_500_000),
                    createMeta(8, 3_500_000, 6_000_000),
                    createMeta(5, 6_000_000, null)
            ));

            return List.of(generalTariff);

        })).when(tariffService).findTariffs(any(TariffFindQuery.class));
    }

    private TariffDTO createTariff(long tariffId, Partner partner) {
        TariffDTO tariffDTO = new TariffDTO();
        tariffDTO.setId(tariffId);
        tariffDTO.setIsActive(true);
        tariffDTO.setDateFrom(BillingConstants.Pvz.BILLING_START_DAY_OF_BILLING_BY_GMV);
        tariffDTO.setModelType(ModelType.THIRD_PARTY_LOGISTICS_PVZ);
        tariffDTO.setPartner(partner);
        tariffDTO.setServiceType(ServiceTypeEnum.PVZ_REWARD);
        return tariffDTO;
    }

    private PvzTariffRewardJsonSchema createMeta(int percentAmount, int fromGmv, Integer toGmv) {
        PvzTariffRewardJsonSchema rewardJsonSchema = new PvzTariffRewardJsonSchema();
        rewardJsonSchema.setFromMonthAge(0);
        rewardJsonSchema.setPvzBrandingType(PvzBrandingTypeEnum.FULL);
        rewardJsonSchema.setPvzTariffZone(PvzTariffZoneEnum.MOSCOW);
        rewardJsonSchema.setAmount(BigDecimal.valueOf(percentAmount));
        rewardJsonSchema.setType(CommonJsonSchema.TypeEnum.RELATIVE);
        rewardJsonSchema.setCurrency("RUB");
        rewardJsonSchema.setBillingUnit(BillingUnitEnum.ORDER);
        rewardJsonSchema.setGmvFrom(fromGmv);
        rewardJsonSchema.setGmvTo(toGmv);
        return rewardJsonSchema;
    }

    @Test
    @DbUnitDataSet(before = "/database/service/yadocservice/before/two_partners.csv")
    public void emptyReport() throws Exception {
        when(yaDocClient.getDocumentsMeta(getDocumentsRequest())).thenReturn(getDocumentMeta());
        when(yaDocClient.getDocument(1853095L)).thenReturn(getFileResource());
        Object responseBytes = mockMvc.perform(get("/pvz/ACT/2021-01-31?campaignId=1"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        Assertions.assertThat(responseBytes).isEqualTo(new byte[12]);
    }

    @SneakyThrows
    @Test
    @DbUnitDataSet(before = "/database/service/yadocservice/before/two_partners.csv")
    public void notFoundByDateTest() {
        when(yaDocClient.getDocumentsMeta(getDocumentsRequest())).thenReturn(new DocumentsMeta());
        String massage = mockMvc.perform(get("/pvz/ACT/2021-01-31?campaignId=1"))
                .andExpect(status().is4xxClientError()).andReturn().getResolvedException().getMessage();
        Assertions.assertThat(massage).isEqualTo("404 NOT_FOUND \"No documents were found for this date: 2021-01-31\"");
    }

    @SneakyThrows
    @Test
    public void notFoundByCampaignIdTest() {
        when(yaDocClient.getDocumentsMeta(getDocumentsRequest())).thenReturn(new DocumentsMeta());
        String massage = mockMvc.perform(get("/pvz/ACT/2021-01-31?campaignId=1"))
                .andExpect(status().is4xxClientError()).andReturn().getResolvedException().getMessage();
        Assertions.assertThat(massage).isEqualTo("404 NOT_FOUND \"No partner were found with this campaignId: 1\"");
    }

    private FileResource getFileResource() {

        FileResource fileResource = new FileResource(
                new byte[12],
                "name",
                MediaType.parse("Text/plain"),
                1L);
        return fileResource;
    }

    private DocumentsContractsRequest getDocumentsRequest() {
        DocumentsContractsRequest documentsRequest = new DocumentsContractsRequest();
        documentsRequest.setContractIds(List.of(1932363L));
        documentsRequest.setDateTo(LocalDate.parse("2021-01-31"));
        documentsRequest.setDateFrom(LocalDate.parse("2021-01-31"));
        return documentsRequest;
    }

    private DocumentsMeta getDocumentMeta() {
        DocumentMeta inv = new DocumentMeta();
        inv.setDocDate(OffsetDateTime.parse("2021-01-31T00:00:00+03:00"));
        inv.setDocType(DocumentMeta.DocTypeEnum.INV);
        inv.setDocNumber("139361782");
        inv.setDocId(139361782L);
        inv.contractId(1932363L);
        inv.partyId(11234549L);
        inv.isSentByEmail(true);
        inv.isReversed(false);

        DocumentMeta act = new DocumentMeta();
        act.setDocDate(OffsetDateTime.parse("2021-01-31T00:00:00+03:00"));
        act.setDocType(DocumentMeta.DocTypeEnum.PARTNER_ACT);
        act.setDocNumber("139361782");
        act.setDocId(1853095L);
        act.contractId(1932363L);
        act.partyId(11234549L);
        act.isSentByEmail(true);
        act.isReversed(false);

        DocumentsMeta documentsMeta = new DocumentsMeta();
        documentsMeta.addDocumentsItem(inv);
        documentsMeta.addDocumentsItem(act);

        return documentsMeta;
    }

    @Test
    @DbUnitDataSet(before = "/controller/calculate/before/when_whole_month_branded.csv")
    void whenWholeMonthBranded() throws Exception {
        clock.setFixed(Instant.parse("2022-02-22T12:00:00Z"), ZoneOffset.ofHours(0));
        restMockMvc.perform(get("/pvz/calculator/123/pvz-params?date=2022-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billingBrandDate").value("2022-02-01"))
                .andExpect(jsonPath("$.gmv").value(6000))
                .andExpect(jsonPath("$.region").value("MOSCOW"));
    }

    @Test
    @DbUnitDataSet(before = "/controller/calculate/before/when_part_month_branded.csv")
    void whenPartMonthBranded() throws Exception {
        restMockMvc.perform(get("/pvz/calculator/123/pvz-params?date=2022-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billingBrandDate").value("2022-03-01"))
                .andExpect(jsonPath("$.gmv").value(6000))
                .andExpect(jsonPath("$.region").value("MOSCOW"));
    }

    @Test
    @DbUnitDataSet(before = "/controller/calculate/before/when_whole_month_branded.csv")
    void whenEmptyMonthAndYear() throws Exception {
        clock.setFixed(Instant.parse("2022-02-22T12:00:00Z"), ZoneOffset.ofHours(0));
        restMockMvc.perform(get("/pvz/calculator/123/pvz-params"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billingBrandDate").value("2022-02-01"))
                .andExpect(jsonPath("$.gmv").value(6000))
                .andExpect(jsonPath("$.region").value("MOSCOW"));
    }

    @Test
    @DbUnitDataSet(before = "/controller/calculate/before/when_whole_month_branded.csv")
    void whenDateInFuture() throws Exception {
        restMockMvc.perform(get("/pvz/calculator/123/pvz-params?date=2022-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billingBrandDate").value("2022-02-01"))
                .andExpect(jsonPath("$.gmv").value(0))
                .andExpect(jsonPath("$.region").value("MOSCOW"));
    }

    @Test
    @DbUnitDataSet(before = "/controller/calculate/before/when_whole_month_branded.csv")
    void whenIncorrectPickupPoint() throws Exception {
        restMockMvc.perform(get("/pvz/calculator/321/pvz-params"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DbUnitDataSet(before = "/controller/calculate/before/when_date_before_branded.csv")
    void whenDateBeforeBranded() throws Exception {
        restMockMvc.perform(get("/pvz/calculator/123/pvz-params?date=2022-02"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @DbUnitDataSet(before = "/controller/calculate/before/tariffs.csv")
    void checkSmallLegalPartnerReward() throws Exception {
        restMockMvc.perform(get(
                "/pvz/calculator/reward?date=2022-02&billingBrandDate=2022-03-01&gmv=1000&region=MOSCOW"
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legalPartnerReward").value(200));
    }

    @Test
    @DbUnitDataSet(before = "/controller/calculate/before/tariffs.csv")
    void checkLargeLegalPartnerReward() throws Exception {
        restMockMvc.perform(get(
                "/pvz/calculator/reward?date=2022-02&billingBrandDate=2022-03-01&gmv=10000000&region=MOSCOW"
                        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legalPartnerReward").value(875000));
    }

    @Test
    @DbUnitDataSet(before = "/controller/calculate/before/tariffs.csv")
    void checkLargeLegalPartnerRewardWithIncorrectRegion() throws Exception {
        restMockMvc.perform(get(
                "/pvz/calculator/reward?date=2022-02&billingBrandDate=2022-03-01&gmv=10000000&region=Piter"
                        ))
                .andExpect(status().is4xxClientError());
    }

}

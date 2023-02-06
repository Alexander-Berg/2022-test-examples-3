package ru.yandex.market.logistics.nesu.controller.own.delivery;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerStatus;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.tarifficator.client.TarifficatorClient;
import ru.yandex.market.logistics.tarifficator.model.dto.MdsFileDto;
import ru.yandex.market.logistics.tarifficator.model.dto.PriceListFileDto;
import ru.yandex.market.logistics.tarifficator.model.dto.PriceListRestrictionsDto;
import ru.yandex.market.logistics.tarifficator.model.enums.FileExtension;
import ru.yandex.market.logistics.tarifficator.model.enums.FileType;
import ru.yandex.market.logistics.tarifficator.model.enums.PriceListStatus;

import static java.lang.ClassLoader.getSystemResourceAsStream;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static ru.yandex.market.logistics.nesu.controller.own.delivery.TestOwnDeliveryUtils.OWN_PARTNER_ID;
import static ru.yandex.market.logistics.nesu.controller.own.delivery.TestOwnDeliveryUtils.PRICE_LIST_ID;
import static ru.yandex.market.logistics.nesu.controller.own.delivery.TestOwnDeliveryUtils.TARIFF_ID;
import static ru.yandex.market.logistics.nesu.controller.own.delivery.TestOwnDeliveryUtils.partnerBuilder;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Тесты АПИ OwnTariffPriceListController")
@DatabaseSetup("/repository/own-delivery/own_delivery.xml")
class OwnPriceListControllerTest extends AbstractContextualTest {

    private static final String XLSX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Autowired
    private LMSClient lmsClient;
    @Autowired
    private TarifficatorClient tarifficatorClient;

    @Test
    @DisplayName("Загрузить файл прайс-листа для тарифа")
    void uploadPriceList() throws Exception {
        mockCheckTariff(partnerBuilder());
        when(tarifficatorClient.uploadPriceListFile(
            TARIFF_ID,
            "minimal-price-list.xlsx",
            new byte[0],
            new PriceListRestrictionsDto()
                .setDirectionCountRestriction(1)
                .setWeightBreaksCountRestriction(10)
        ))
            .thenReturn(priceList(122L, 44L, PriceListStatus.SUCCESS));

        execUpload(defaultMockFile())
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/own-delivery/own_price_list_response.json"));
    }

    @Test
    @DisplayName("Загрузить файл прайс-листа, тариф не найден")
    void uploadPriceListTariffNotFound() throws Exception {
        execUpload(defaultMockFile())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [TARIFF] with ids [12]"));

        verifyZeroInteractions(lmsClient);
    }

    @Test
    @DisplayName("Загрузить файл прайс-листа, СД не принадлежит магазину")
    void uploadPriceListNotOwnedPartner() throws Exception {
        mockCheckTariff(partnerBuilder().marketId(2L).businessId(100L));

        execUpload(defaultMockFile())
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [TARIFF] with ids [12]"));
    }

    @Test
    @DisplayName("Загрузить файл прайс-листа, СД не активна")
    void uploadPriceListNotActivePartner() throws Exception {
        mockCheckTariff(partnerBuilder().status(PartnerStatus.INACTIVE));

        execUpload(defaultMockFile())
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Partner with id 45 is inactive."));
    }

    @Test
    @DisplayName("Получение прайс-листов по тарифу")
    void getPriceLists() throws Exception {
        mockCheckTariff(partnerBuilder());
        when(tarifficatorClient.getPriceListFiles(TARIFF_ID))
            .thenReturn(List.of(
                priceList(122L, 44L, PriceListStatus.SUCCESS),
                priceList(123L, 25L, PriceListStatus.PARTIAL_SUCCESS)
            ));

        execGet()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/own-delivery/own_price_list_by_tariff_response.json"));
    }

    @Test
    @DisplayName("Получение прайс-листов, не найден тариф")
    void getPriceListsTariffNotFound() throws Exception {
        execGet()
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [TARIFF] with ids [12]"));
    }

    @Test
    @DisplayName("Получение прайс-листов, СД не принадлежит магазину")
    void getPriceListsNotOwnedPartner() throws Exception {
        mockCheckTariff(partnerBuilder().marketId(2L).businessId(100L));

        execGet()
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [TARIFF] with ids [12]"));
    }

    @Test
    @DisplayName("Активация прайс-листа")
    void activatePriceList() throws Exception {
        PriceListFileDto priceList = priceList(PRICE_LIST_ID, 46L, PriceListStatus.PARTIAL_SUCCESS);
        when(tarifficatorClient.getPriceListFile(PRICE_LIST_ID))
            .thenReturn(priceList);
        mockCheckTariff(partnerBuilder());

        priceList.setActive(true);
        when(tarifficatorClient.activatePriceListFile(PRICE_LIST_ID))
            .thenReturn(priceList);

        execActivate()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/own-delivery/own_active_price_list_response.json"));
    }

    @Test
    @DisplayName("Активация прайс-листа, СД не принадлежит магазину")
    void activatePriceListsNotOwnedPartner() throws Exception {
        when(tarifficatorClient.getPriceListFile(PRICE_LIST_ID))
            .thenReturn(priceList(PRICE_LIST_ID, 46L, PriceListStatus.PARTIAL_SUCCESS));
        mockCheckTariff(partnerBuilder().marketId(2L).businessId(100L));

        execActivate()
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [PRICE_LIST] with ids [125]"));
    }

    @Test
    @DisplayName("Получение прайс-листов, СД не активна")
    void getPriceListsNotActivePartner() throws Exception {
        mockCheckTariff(partnerBuilder().status(PartnerStatus.INACTIVE));
        when(tarifficatorClient.getPriceListFiles(TARIFF_ID))
            .thenReturn(List.of(
                priceList(122L, 44L, PriceListStatus.SUCCESS),
                priceList(123L, 25L, PriceListStatus.PARTIAL_SUCCESS)
            ));

        execGet()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/own-delivery/own_price_list_by_tariff_response.json"));
    }

    @Test
    @DisplayName("Получение активного прайс-листа тарифа")
    void getActivePriceList() throws Exception {
        mockCheckTariff(partnerBuilder());
        when(tarifficatorClient.getPriceListFiles(TARIFF_ID))
            .thenReturn(List.of(
                priceList(122L, 44L, PriceListStatus.SUCCESS),
                priceList(123L, 25L, PriceListStatus.PARTIAL_SUCCESS)
            ));

        execGetActive()
            .andExpect(status().isOk())
            .andExpect(jsonContent("controller/own-delivery/active_own_price_list_by_tariff_response.json"));
    }

    @Test
    @DisplayName("Активный прайс-лист не найден")
    void getActivePriceListNotFound() throws Exception {
        mockCheckTariff(partnerBuilder());
        when(tarifficatorClient.getPriceListFiles(TARIFF_ID))
            .thenReturn(List.of(priceList(123L, 25L, PriceListStatus.PARTIAL_SUCCESS)));

        execGetActive()
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Active price-list of tariff 12 not found"));
    }

    private void mockCheckTariff(PartnerResponse.PartnerResponseBuilder partner) {
        when(tarifficatorClient.getOptionalTariff(TestOwnDeliveryUtils.TARIFF_ID))
            .thenReturn(Optional.of(TestOwnDeliveryUtils.tariffBuilder().build()));
        when(lmsClient.getPartner(OWN_PARTNER_ID))
            .thenReturn(Optional.of(partner.build()));
    }

    @Nonnull
    private PriceListFileDto priceList(long id, long fileId, PriceListStatus status) {
        PriceListFileDto priceList = new PriceListFileDto()
            .setTariffId(TARIFF_ID);
        priceList
            .setId(id)
            .setFile(new MdsFileDto(
                fileId,
                "http://url-mds",
                "price-list",
                FileType.PRICE_LIST,
                FileExtension.EXCEL
            ))
            .setCreatedAt(Instant.parse("2019-07-22T11:00:00Z"))
            .setUpdatedAt(Instant.parse("2019-07-22T12:00:00Z"))
            .setStatus(status);
        return priceList;
    }

    @Nonnull
    private ResultActions execUpload(MockMultipartFile file) throws Exception {
        return mockMvc.perform(
            multipart("/back-office/own-price-list/" + TARIFF_ID)
                .file(file)
                .param("userId", "1")
                .param("shopId", "1")
        );
    }

    @Nonnull
    private ResultActions execGet() throws Exception {
        return mockMvc.perform(
            get("/back-office/own-price-list/" + TARIFF_ID)
                .param("userId", "1")
                .param("shopId", "1")
        );
    }

    @Nonnull
    private ResultActions execActivate() throws Exception {
        return mockMvc.perform(
            post("/back-office/own-price-list/" + PRICE_LIST_ID + "/activate")
                .param("userId", "1")
                .param("shopId", "1")
        );
    }

    @Nonnull
    private ResultActions execGetActive() throws Exception {
        return mockMvc.perform(
            get("/back-office/own-price-list/" + TARIFF_ID + "/active")
                .param("userId", "1")
                .param("shopId", "1")
        );
    }

    @Nonnull
    private MockMultipartFile defaultMockFile() throws IOException {
        return newMockFile(getSystemResourceAsStream("controller/own-deliver/minimal-price-list.xlsx"));
    }

    @Nonnull
    private MockMultipartFile newMockFile(InputStream fileStream) throws IOException {
        return new MockMultipartFile("file", "minimal-price-list.xlsx", XLSX_MIME_TYPE, fileStream);
    }
}

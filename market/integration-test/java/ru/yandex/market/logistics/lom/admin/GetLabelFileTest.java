package ru.yandex.market.logistics.lom.admin;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.werewolf.client.WwClient;
import ru.yandex.market.logistics.werewolf.model.entity.LabelInfo;
import ru.yandex.market.logistics.werewolf.model.enums.DocumentFormat;
import ru.yandex.market.logistics.werewolf.model.enums.PageSize;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;

@DisplayName("Получение файла с ярлыками для заказа")
@DatabaseSetup("/controller/admin/label_file/before/prepare.xml")
class GetLabelFileTest extends AbstractContextualTest {

    private static final byte[] PDF_LABELS_FILE_CONTENT = "this is a PDF labels file".getBytes(StandardCharsets.UTF_8);

    @Autowired
    private WwClient wwClient;

    @AfterEach
    void tearDown() {
        verifyNoMoreInteractions(wwClient);
    }

    @Test
    @DisplayName("Успех")
    void success() throws Exception {
        when(wwClient.generateLabels(List.of(expectedLabelInfo()), DocumentFormat.PDF, PageSize.A4))
            .thenReturn(PDF_LABELS_FILE_CONTENT);

        mockMvc.perform(get("/admin/orders/1/label-file"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_PDF_VALUE))
            .andExpect(content().bytes(PDF_LABELS_FILE_CONTENT));

        verify(wwClient).generateLabels(any(), any(), any());
    }

    @Test
    @DisplayName("Заказ не найден")
    void orderNotFound() throws Exception {
        mockMvc.perform(get("/admin/orders/2/label-file"))
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [ORDER] with id [2]"));
    }

    @Test
    @DisplayName("У заказа нет коробок")
    @DatabaseSetup(
        value = "/controller/admin/label_file/before/remove_place_unit.xml",
        type = DatabaseOperation.DELETE
    )
    void noPlacesInOrder() throws Exception {
        mockMvc.perform(get("/admin/orders/1/label-file"))
            .andExpect(status().isBadRequest())
            .andExpect(errorMessage("Order has no units with a type PLACE. No labels to generate."));
    }

    @Nonnull
    private LabelInfo expectedLabelInfo() {
        return LabelInfo.builder()
            .platformClientId(6L)
            .barcode("LOinttest-1")
            .sortingCenter(
                LabelInfo.PartnerInfo.builder()
                    .legalName("Horns-n-Hooves Ltd.")
                    .readableName("Честный Сортцентр")
                    .build()
            )
            .deliveryService(
                LabelInfo.PartnerInfo.builder()
                    .legalName("Someday Delivery Inc.")
                    .readableName("Своевременная Доставка")
                    .build()
            )
            .place(
                LabelInfo.PlaceInfo.builder()
                    .externalId("place-external-id")
                    .placeNumber(1)
                    .placesCount(1)
                    .weight(BigDecimal.valueOf(1.2))
                    .build()
            )
            .recipient(
                LabelInfo.RecipientInfo.builder()
                    .firstName("recipient-first-name")
                    .middleName("recipient-middle-name")
                    .lastName("recipient-last-name")
                    .phoneNumber("+78889990000")
                    .build()
            )
            .address(
                LabelInfo.AddressInfo.builder()
                    .country("country-0")
                    .federalDistrict("federal-district-0")
                    .region("region-0")
                    .locality("locality-0")
                    .subRegion("sub-region-0")
                    .settlement("settlement-0")
                    .street("street-0")
                    .house("house-0")
                    .housing("housing-0")
                    .room("room-0")
                    .zipCode("zip-code-0")
                    .build()
            )
            .shipmentDate(LocalDate.of(2022, 3, 4))
            .seller(
                LabelInfo.SellerInfo.builder()
                    .legalName("Snake's Oils")
                    .readableName("Аптека №1")
                    .number("order-external-id")
                    .build()
            )
            .build();
    }
}

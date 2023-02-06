package ru.yandex.market.logistics.lom.service;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistics.lom.AbstractContextualTest;
import ru.yandex.market.logistics.lom.entity.MdsFile;
import ru.yandex.market.logistics.lom.entity.enums.FileType;
import ru.yandex.market.logistics.lom.entity.enums.ResourceType;
import ru.yandex.market.logistics.lom.exception.LomException;
import ru.yandex.market.logistics.lom.exception.http.ResourceNotFoundException;
import ru.yandex.market.logistics.lom.jobs.model.OrderIdPartnerIdPayload;
import ru.yandex.market.logistics.lom.jobs.processor.GetLabelWwService;
import ru.yandex.market.logistics.lom.service.mds.MdsS3Service;
import ru.yandex.market.logistics.werewolf.client.WwClient;
import ru.yandex.market.logistics.werewolf.model.entity.LabelInfo;
import ru.yandex.market.logistics.werewolf.model.enums.DocumentFormat;
import ru.yandex.market.logistics.werewolf.model.enums.PageSize;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static ru.yandex.market.logistics.lom.utils.jobs.PayloadFactory.createOrderIdPartnerIdPayload;
import static ru.yandex.market.logistics.lom.utils.lgw.CreateLgwCommonEntitiesUtils.createPartner;

class GetLabelWwServiceTest extends AbstractContextualTest {
    private static final Partner PARTNER = createPartner();
    private static final OrderIdPartnerIdPayload PAYLOAD = createOrderIdPartnerIdPayload(1L, PARTNER.getId(), "123");
    private static final OrderIdPartnerIdPayload PAYLOAD_2 = createOrderIdPartnerIdPayload(2L, PARTNER.getId(), "123");

    @Autowired
    private GetLabelWwService getLabelService;

    @Autowired
    private WwClient wwClient;

    @Autowired
    private MdsS3Service mdsS3Service;

    @Test
    @DatabaseSetup("/service/documents/before/ww_no_label_valid_case.xml")
    @ExpectedDatabase(
        value = "/service/documents/after/ww_label_generation_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Успешное получение ярлыка из WW и его сохранение, доставка до двери")
    void getSuccess() throws Exception {
        LabelInfo labelInfo = getLabelInfoDefaultBuilder().build();

        mockGenerateLabel(labelInfo);
        mockUploadMdsFile();

        getLabelService.processPayload(PAYLOAD);
        verifyGenerateLabel(labelInfo);
    }

    @Test
    @DatabaseSetup("/service/documents/before/ww_no_label_valid_case_pickup_point.xml")
    @ExpectedDatabase(
        value = "/service/documents/after/ww_label_generation_success_pickup_point.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Успешное получение ярлыка из WW и его сохранение, доставка в ПВЗ")
    void getSuccessPickupPoint() throws Exception {
        LabelInfo labelInfo = getLabelInfoDefaultBuilder().build();

        mockGenerateLabel(labelInfo);
        mockUploadMdsFile();

        getLabelService.processPayload(PAYLOAD);
        verifyGenerateLabel(labelInfo);
    }

    @Test
    @DatabaseSetup("/service/documents/before/ww_no_label_return_sc.xml")
    @ExpectedDatabase(
        value = "/service/documents/after/ww_label_generation_success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Успешное получение ярлыка из WW и его сохранение, прямой СЦ не указан, а обратный указан")
    void getSuccessScIsNullReturnScNotNull() throws Exception {
        LabelInfo labelInfo = getLabelInfoDefaultBuilder()
            .sortingCenter(null)
            .build();

        mockGenerateLabel(labelInfo);
        mockUploadMdsFile();

        getLabelService.processPayload(PAYLOAD);
        verifyGenerateLabel(labelInfo);
    }

    @Test
    @DisplayName("Ярлык уже существует")
    @DatabaseSetup("/service/documents/before/ww_label_exists_with_mds_file.xml")
    void labelExists() {
        getLabelService.processPayload(PAYLOAD_2);
        verifyZeroInteractions(wwClient);
    }

    @Test
    @DisplayName("Заказ не найден")
    void orderNotFound() {
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
            () -> getLabelService.processPayload(PAYLOAD));
        assertEquals(exception.getType(), ResourceType.ORDER);
        verifyZeroInteractions(wwClient);
    }

    @Test
    @DatabaseSetup("/service/documents/before/ww_no_label_no_recipient_contact.xml")
    @DisplayName("Не указаны контактные данные получателя в заказе")
    void noContact() {
        LomException exception = assertThrows(LomException.class,
            () -> getLabelService.processPayload(PAYLOAD));
        assertEquals(exception.getMessage(), "No contact with type RECIPIENT found for order with id = 1");
        verifyZeroInteractions(wwClient);
    }

    @Test
    @DatabaseSetup("/service/documents/before/ww_label_exists_with_mds_file.xml")
    @ExpectedDatabase(
        value = "/service/documents/before/ww_label_exists_with_mds_file.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Ошибка при генерации файла в WW")
    void wwFileGenerationError() {
        LabelInfo labelInfo = getLabelInfoDefaultBuilder().build();

        when(wwClient.generateLabels(List.of(labelInfo), DocumentFormat.PDF, PageSize.A6))
            .thenThrow(new RuntimeException("FileGenerationError"));

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> getLabelService.processPayload(PAYLOAD));
        assertEquals(exception.getMessage(), "FileGenerationError");

        verifyGenerateLabel(labelInfo);
    }

    @Test
    @DatabaseSetup("/service/documents/before/ww_label_exists_with_mds_file.xml")
    @ExpectedDatabase(
        value = "/service/documents/before/ww_label_exists_with_mds_file.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @DisplayName("Ошибка при сохранении mds-файла")
    void mdsFileUploadError() {
        LabelInfo labelInfo = getLabelInfoDefaultBuilder().build();

        mockGenerateLabel(labelInfo);
        when(mdsS3Service.uploadFile(getMdsFile(), any())).thenThrow(new RuntimeException("FileUploadError"));

        RuntimeException exception = assertThrows(RuntimeException.class,
            () -> getLabelService.processPayload(PAYLOAD));
        assertEquals(exception.getMessage(), "FileUploadError");
        verifyGenerateLabel(labelInfo);
    }

    @Nonnull
    private LabelInfo.LabelInfoBuilder getLabelInfoDefaultBuilder() {
        return LabelInfo.builder()
            .platformClientId(3L)
            .barcode("2-LOinttest-1")
            .deliveryService(
                LabelInfo.PartnerInfo.builder()
                    .legalName("ООО DPD")
                    .readableName("DPD")
                    .build()
            )
            .place(
                LabelInfo.PlaceInfo.builder()
                    .externalId("storage_unit_place_external_id")
                    .placeNumber(1)
                    .placesCount(1)
                    .weight(BigDecimal.ONE)
                    .build()
            )
            .recipient(
                LabelInfo.RecipientInfo.builder()
                    .firstName("Иван")
                    .lastName("Иванов")
                    .phoneNumber("+79876543210")
                    .build()
            )
            .address(
                LabelInfo.AddressInfo.builder()
                    .country("Россия")
                    .locality("Москва")
                    .street("Ленина")
                    .house("5")
                    .zipCode("123321")
                    .build()
            )
            .shipmentDate(LocalDate.of(2020, 1, 1))
            .seller(
                LabelInfo.SellerInfo.builder()
                    .number("order_1_external_id")
                    .readableName("Магазин")
                    .legalName("ООО Магазин")
                    .build()
            );
    }

    private void mockGenerateLabel(@Nonnull LabelInfo labelInfo) {
        when(wwClient.generateLabels(List.of(labelInfo), DocumentFormat.PDF, PageSize.A6)).thenReturn(new byte[10]);
    }

    private void mockUploadMdsFile() throws Exception {
        when(mdsS3Service.uploadFile(getMdsFile(), any())).thenReturn(new URL("https://mds.url/lom-doc-test/1"));
    }

    @Nonnull
    private MdsFile getMdsFile() {
        return new MdsFile()
            .setId(1L)
            .setFileName("ww_label_for_order_1_partner_20.pdf")
            .setFileType(FileType.ORDER_LABEL)
            .setMimeType(MediaType.APPLICATION_PDF_VALUE)
            .setUrl("");
    }

    private void verifyGenerateLabel(@Nonnull LabelInfo labelInfo) {
        verify(wwClient).generateLabels(List.of(labelInfo), DocumentFormat.PDF, PageSize.A6);
    }
}

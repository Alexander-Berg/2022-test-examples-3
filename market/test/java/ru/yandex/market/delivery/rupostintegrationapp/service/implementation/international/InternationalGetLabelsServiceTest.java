package ru.yandex.market.delivery.rupostintegrationapp.service.implementation.international;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lowagie.text.pdf.PdfReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import utils.FixtureRepository;

import ru.yandex.market.delivery.entities.common.ResourceId;
import ru.yandex.market.delivery.entities.request.ds.DsGetLabelsRequest;
import ru.yandex.market.delivery.entities.response.ds.implementation.DsGetLabelsResponseContent;
import ru.yandex.market.delivery.rupostintegrationapp.BaseTest;
import ru.yandex.market.delivery.rupostintegrationapp.dao.international.CreatedEmsOrderRepository;
import ru.yandex.market.delivery.rupostintegrationapp.dao.international.CreatedRmOrderRepository;
import ru.yandex.market.delivery.rupostintegrationapp.model.entity.CreatedEmsItem;
import ru.yandex.market.delivery.rupostintegrationapp.model.entity.CreatedEmsOrder;
import ru.yandex.market.delivery.rupostintegrationapp.model.entity.CreatedInternationalItem;
import ru.yandex.market.delivery.rupostintegrationapp.model.entity.CreatedInternationalOrder;
import ru.yandex.market.delivery.rupostintegrationapp.model.entity.CreatedRmItem;
import ru.yandex.market.delivery.rupostintegrationapp.model.entity.CreatedRmOrder;
import ru.yandex.market.delivery.rupostintegrationapp.model.entity.enums.TariffCode;
import ru.yandex.market.delivery.russianpostapiclient.client.RussianPostApiClient;
import ru.yandex.market.pdf.generator.PdfGeneratorService;
import ru.yandex.market.pdf.generator.PdfGeneratorServiceImpl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Тест для {@link InternationalGetLabelsService}.
 */
@ExtendWith(MockitoExtension.class)
class InternationalGetLabelsServiceTest extends BaseTest {

    private static final Long YANDEX_ID = 1000L;
    private static final String TRACK_CODE = "RE0004955B4RU";
    private static final String RECEIVER_NAME = "Konstantinopolskij Konstantin Konstantinovich";
    private static final String RECEIVER_REGION = "Sankt-Peterburg i Leningradskaya oblastʹ";
    private static final String RECEIVER_CITY = "Saint Petersburg, Saint Petersburg, Saint Petersburg";
    private static final String RECEIVER_STREET = "Pushkinskaya ulitsa, mikrorajon Yubilejnyj";
    private static final String RECEIVER_HOUSE = "53";
    private static final String RECEIVER_APARTMENT = "55";
    private static final String RECEIVER_ZIP_CODE = "620149";
    private static final String RECEIVER_PHONE = "+79025836308";
    private static final String SENDER_NAME = "Bringly";
    private static final String SENDER_ZIP_CODE = "14057";
    private static final String SENDER_COUNTRY = "Germany";
    private static final String SENDER_CITY = "Berlin";
    private static final String SENDER_STREET = "Heusingerstrasse";
    private static final String SENDER_HOUSE = "12-16";
    private static final String USER_ORDER_ID = "Y515151";
    private static final Integer WEIGHT_KG = 1;
    private static final Integer WEIGHT_G = 2;
    @Mock
    private RussianPostApiClient client;

    @Mock
    private CreatedRmOrderRepository createdRmOrderRepository;

    @Mock
    private CreatedEmsOrderRepository createdEmsOrderRepository;

    @Mock
    private TokenValidationService tokenValidationService;

    private PdfGeneratorService pdfGeneratorService = new PdfGeneratorServiceImpl();
    private ObjectMapper objectMapper = new ObjectMapper();

    private InternationalGetLabelsService service;

    @BeforeEach
    void init() {
        service = spy(new InternationalGetLabelsService(
            client,
            createdRmOrderRepository,
            createdEmsOrderRepository,
            tokenValidationService,
            pdfGeneratorService,
            objectMapper
        ));
    }

    @Test
    void testRmLabel() throws Exception {
        int itemsCount = 5;
        CreatedRmOrder order = (CreatedRmOrder) new CreatedRmOrder()
            .setOrderMade(LocalDateTime.now());
        order.setItems(Stream.generate(CreatedRmItem::new)
            .map(this::fillItem).limit(itemsCount)
            .collect(Collectors.toList()));
        when(createdRmOrderRepository.findByOrderId(YANDEX_ID)).thenReturn(Optional.of(order));
        doReturn(FixtureRepository.getRmLabel())
            .when(service)
            .getBarcodeInBase64(any(), any(), any());
        testLabel(order, TariffCode.REGISTERED_SMALL_PACKET, 1);
    }

    @Test
    void testEmsLabel() throws Exception {
        CreatedEmsOrder order = (CreatedEmsOrder) new CreatedEmsOrder()
            .setOrderMade(LocalDateTime.now());
        int itemsCount = 4;
        order.setItems(Stream.generate(CreatedEmsItem::new)
            .map(this::fillItem).limit(itemsCount)
            .collect(Collectors.toList()));
        when(createdRmOrderRepository.findByOrderId(YANDEX_ID)).thenReturn(Optional.of(order));
        doReturn(FixtureRepository.getEmsLabel())
            .when(service)
            .getBarcodeInBase64(any(), any(), any());
        testLabel(order, TariffCode.EMS, 4);
    }

    private void testLabel(
        CreatedInternationalOrder order,
        TariffCode tariffCode,
        Integer expectedPages
    ) throws Exception {
        DsGetLabelsRequest dsGetLabelsRequest = new DsGetLabelsRequest();
        DsGetLabelsRequest.RequestContent requestContent = dsGetLabelsRequest.new RequestContent();
        dsGetLabelsRequest.setRequestContent(requestContent);

        ResourceId resourceId = new ResourceId();
        resourceId.setYandexId(String.valueOf(YANDEX_ID));
        resourceId.setPartnerId(TRACK_CODE);

        order
            .setTrackCode(TRACK_CODE)
            .setReceiverName(RECEIVER_NAME)
            .setReceiverRegion(RECEIVER_REGION)
            .setReceiverCity(RECEIVER_CITY)
            .setReceiverStreet(RECEIVER_STREET)
            .setReceiverHouse(RECEIVER_HOUSE)
            .setReceiverApartment(RECEIVER_APARTMENT)
            .setReceiverZipCode(RECEIVER_ZIP_CODE)
            .setReceiverMobilePhoneNumber(RECEIVER_PHONE)
            .setSenderName(SENDER_NAME)
            .setSenderZipCode(SENDER_ZIP_CODE)
            .setSenderCountry(SENDER_COUNTRY)
            .setSenderCity(SENDER_CITY)
            .setSenderStreet(SENDER_STREET)
            .setSenderHouse(SENDER_HOUSE)
            .setUserOrderId(USER_ORDER_ID)
            .setWeightInKilograms(WEIGHT_KG)
            .setWeightInGrams(WEIGHT_G)
            .setTariffCode(tariffCode);

        requestContent.setOrdersId(Collections.singletonList(resourceId));

        DsGetLabelsResponseContent dsGetLabelsResponseContent = service.doJob(dsGetLabelsRequest);

        softly.assertThat(dsGetLabelsResponseContent.getPdf()).as("PDF generated and encoded")
            .isNotBlank();

        byte[] pdfBytes = Base64.getDecoder().decode(dsGetLabelsResponseContent.getPdf());
        PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfBytes));
        softly.assertThat(reader.getNumberOfPages()).isEqualTo(expectedPages);
    }

    private <T extends CreatedInternationalItem> T fillItem(T item) {
        item.setName("Square Aviation Plotter")
            .setWeightInKilograms(0)
            .setWeightInGrams(501)
            .setPriceInEuro(10L)
            .setPriceInEuroCent(33)
            .setQuantity(3);
        return item;
    }
}

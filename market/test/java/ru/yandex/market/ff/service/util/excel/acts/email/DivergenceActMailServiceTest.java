package ru.yandex.market.ff.service.util.excel.acts.email;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.ff.base.IntegrationTest;
import ru.yandex.market.ff.client.dto.ActDataRowDTO;
import ru.yandex.market.ff.client.dto.PrimaryDivergenceActDto;
import ru.yandex.market.ff.client.enums.UnredeemedPrimaryDivergenceType;
import ru.yandex.market.ff.repository.ServiceEmailRepository;
import ru.yandex.market.ff.service.ConcreteEnvironmentParamService;
import ru.yandex.market.ff.service.util.excel.acts.PrimaryUnredeemedDivergenceActBuilder;
import ru.yandex.market.ff.service.util.excel.acts.email.sender.SenderMail;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerExternalParamResponse;
import ru.yandex.market.logistics.management.entity.response.partner.PartnerResponse;
import ru.yandex.market.logistics.management.entity.type.PartnerExternalParamType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DivergenceActMailServiceTest extends IntegrationTest {

    private static final String FROM_EMAIL = "shipmentclaims@yandex-team.ru";

    private final PrimaryDivergenceActDto primaryDivergenceActDto =
            new PrimaryDivergenceActDto("106", "1123_T",
                    LocalDate.parse("2020-06-18"),
                    LocalDateTime.parse("18.06.2020 10:00", DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                    LocalDateTime.parse("19.06.2020 11:18", DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                    "140126, Московская область, Раменский городской округ, " +
                            "Логистический технопарк Софьино, строение 3/1",
                    Arrays.asList(
                            new ActDataRowDTO("1dg6", "BnU8-d",
                                    UnredeemedPrimaryDivergenceType.EXPIRED, 2),
                            new ActDataRowDTO("12w4r", "gtm1HY5",
                                    UnredeemedPrimaryDivergenceType.DELIVERED, 1),
                            new ActDataRowDTO("h8==ed", "1234",
                                    UnredeemedPrimaryDivergenceType.NOT_SUPPLIED, 1),
                            new ActDataRowDTO("2563", "wfb",
                                    UnredeemedPrimaryDivergenceType.NOT_SUPPLIED, 3),
                            new ActDataRowDTO("12w4r", "gtm1HY5",
                                    UnredeemedPrimaryDivergenceType.NOT_SUPPLIED, 2),
                            new ActDataRowDTO("1v6HNv", "fb446",
                                    UnredeemedPrimaryDivergenceType.WAREHOUSE, 1)
                    ));

    @Autowired
    private PrimaryUnredeemedDivergenceActBuilder actBuilderService;

    @Autowired
    private ServiceEmailRepository serviceEmailRepository;

    @Autowired
    private DivergenceActMailService divergenceActMailService;

    @Autowired
    private ConcreteEnvironmentParamService paramService;

    private final RestTemplate mockRestTemplate = mock(RestTemplate.class);

    @BeforeEach
    public void setFields() {
        ReflectionTestUtils.setField(divergenceActMailService, "restTemplate", mockRestTemplate);
    }

    @Test
    @DatabaseSetup("classpath:service/xlsx-report/email/emails.xml")
    void sendEmailTest() {
        PartnerResponse partnerResponse = PartnerResponse.newBuilder().name("Боксберри").build();
        when(lmsClient.getPartner(eq(106L))).thenReturn(java.util.Optional.of(partnerResponse));
        PartnerExternalParamResponse paramResponse =
                PartnerExternalParamResponse.newBuilder().value("somemail@yandex-team.ru").build();
        when(lmsClient.getPartnerExternalParam(51L, PartnerExternalParamType.SERVICE_EMAILS))
                .thenReturn(paramResponse);
        when(mockRestTemplate.postForObject(anyString(), any(ObjectNode.class), any())).thenReturn(new Object());
        actBuilderService.setPrimaryDivergenceActDto(primaryDivergenceActDto);
        Workbook divergenceAct = actBuilderService.getDivergenceAct();

        SenderMail senderMail = divergenceActMailService.sendEmail(divergenceAct,
                actBuilderService.getGeneratedActName(), "51");

        assertNotNull(divergenceAct);
        assertEquals(FROM_EMAIL, senderMail.getFrom());
        assertEquals("somemail@yandex-team.ru", senderMail.getTo().get(0));
    }

    @Test
    void sendEmailNotFoundTest() {
        PartnerResponse partnerResponse = PartnerResponse.newBuilder().name("Боксберри").build();
        when(lmsClient.getPartner(106L)).thenReturn(java.util.Optional.of(partnerResponse));
        when(mockRestTemplate.postForObject(anyString(), any(ObjectNode.class), any())).thenReturn(new Object());
        actBuilderService.setPrimaryDivergenceActDto(primaryDivergenceActDto);
        Workbook divergenceAct = actBuilderService.getDivergenceAct();

        SenderMail senderMail = divergenceActMailService.sendEmail(divergenceAct,
                actBuilderService.getGeneratedActName(), "1");

        assertNotNull(divergenceAct);
        assertEquals(FROM_EMAIL, senderMail.getFrom());
        assertEquals(FROM_EMAIL, senderMail.getTo().get(0));
    }
}

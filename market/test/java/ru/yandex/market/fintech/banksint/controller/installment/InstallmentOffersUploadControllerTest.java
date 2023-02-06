package ru.yandex.market.fintech.banksint.controller.installment;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import ru.yandex.market.fintech.banksint.FunctionalTest;
import ru.yandex.market.fintech.banksint.mybatis.installment.InstallmentResourceMapper;
import ru.yandex.market.fintech.banksint.mybatis.installment.model.InstallmentsResourceInfo;
import ru.yandex.market.fintech.banksint.mybatis.installment.model.ResourceStatus;
import ru.yandex.market.fintech.banksint.mybatis.installment.model.ResourceType;
import ru.yandex.market.fintech.banksint.service.mds.MdsS3Service;
import ru.yandex.market.fintech.instalment.model.InlineResponse200;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class InstallmentOffersUploadControllerTest extends FunctionalTest {

    private static final String REQUEST_URL = "/supplier/fin-service/installments/offers/upload/xlsx/async" +
            "?shop_id={shopId}&business_id={businessId}";
    private static final String URL = "url";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MdsS3Service mdsS3Service;

    @Autowired
    private InstallmentResourceMapper installmentResourceMapper;

    @BeforeEach
    public void setUp() {
        jdbcTemplate.execute(readClasspathFile("InstallmentOffersResource.sql"));
        Mockito.doReturn(URL)
                .when(mdsS3Service)
                .uploadFile(Mockito.anyString(), Mockito.any(MultipartFile.class));
    }

    @Test
    void uploadXlsxWithInstallmentsAndOffersOkTest() {
        String filename = "InstallmentOffersResource.sql";
        InlineResponse200 expected = new InlineResponse200().resourceId(null).name(filename);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        LinkedMultiValueMap<String, Object> parameters = new LinkedMultiValueMap<>();
        parameters.add("file", new ClassPathResource("ru/yandex/market/fintech/banksint/controller/installment/" +
                filename));
        HttpEntity<LinkedMultiValueMap<String, Object>> httpEntity = new HttpEntity<>(parameters, headers);

        ResponseEntity<InlineResponse200> response = testRestTemplate.exchange(
                REQUEST_URL,
                HttpMethod.POST,
                httpEntity,
                new ParameterizedTypeReference<>() {
                },
                Map.of(
                        "shopId", "1",
                        "businessId", "2"
                )
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        InlineResponse200 body = response.getBody();
        assertNotNull(body.getResourceId());
        assertEquals(filename, body.getName());

        InstallmentsResourceInfo info = installmentResourceMapper.getInstallmentsResourceInfoByResourceId(
                body.getResourceId());
        assertNotNull(info.getResourceId());
        assertNotNull(info.getShopId());
        assertNotNull(info.getName());
        assertEquals(0, info.getCorrectSelectedOffers());
        assertEquals(0, info.getInvalidOffers());
        assertEquals(ResourceStatus.PENDING, info.getStatus());
        assertEquals(ResourceType.INSTALLMENT, info.getType());
        assertEquals(0, info.getTotalOffers());
        assertEquals(URL, info.getUrlToDownload());
        assertNotNull(info.getCreatedAt());
    }
}

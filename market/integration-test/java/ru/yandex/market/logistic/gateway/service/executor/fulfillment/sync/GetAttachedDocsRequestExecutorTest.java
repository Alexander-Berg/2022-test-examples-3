package ru.yandex.market.logistic.gateway.service.executor.fulfillment.sync;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.google.common.collect.ImmutableList;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.logistic.api.model.common.PartnerType;
import ru.yandex.market.logistic.gateway.AbstractIntegrationTest;
import ru.yandex.market.logistic.gateway.common.model.common.Partner;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DateTime;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.DocumentFormat;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.request.GetAttachedDocsRequest;
import ru.yandex.market.logistic.gateway.common.model.fulfillment.response.GetAttachedDocsResponse;
import ru.yandex.market.logistic.gateway.service.converter.fulfillment.DocumentFormatConverter;
import ru.yandex.market.logistic.gateway.service.converter.fulfillment.OrdersDataConverter;
import ru.yandex.market.logistic.gateway.service.util.PropertiesService;
import ru.yandex.market.logistic.gateway.service.util.S3FileHandler;
import ru.yandex.market.logistics.werewolf.client.WwClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

public class GetAttachedDocsRequestExecutorTest extends AbstractIntegrationTest {
    @Autowired
    private AmazonS3 amazonS3;

    @Autowired
    private S3FileHandler s3FileHandler;

    @Autowired
    private WwClient wwClientJson;

    @Autowired
    private OrdersDataConverter ordersDataConverter;

    @Autowired
    private DocumentFormatConverter documentFormatConverter;

    @Autowired
    private PropertiesService propertiesService;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private GetAttachedDocsRequest createRequest(DocumentFormat documentFormat, PartnerType partnerType) {
        return new GetAttachedDocsRequest(
            new Partner(1L),
            new GetAttachedDocsRequest.OrdersData(
                ImmutableList.of(
                    new GetAttachedDocsRequest.DocOrder(
                        ResourceId.builder().setYandexId("1234-LO-31337").setPartnerId("000000003243423423").build(),
                        BigDecimal.valueOf(3.14),
                        BigDecimal.valueOf(123),
                        2
                    ),
                    new GetAttachedDocsRequest.DocOrder(
                        ResourceId.builder().setYandexId("1235-LO-131337").setPartnerId("000000003243423424").build(),
                        BigDecimal.valueOf(11.11),
                        BigDecimal.valueOf(1.1),
                        1
                    )
                ),
                ResourceId.builder().setYandexId("1234").setPartnerId("000000003243423423").build(),
                new DateTime("2019-11-26"),
                new GetAttachedDocsRequest.DocSender(
                    "ООО Пыш Пыш Ололо",
                    "1"
                ),
                new GetAttachedDocsRequest.DocPartner(
                    "ООО Абырвалг",
                    partnerType
                )
            ),
            documentFormat
        );
    }

    @Test
    public void createHtml() throws Exception {
        GetAttachedDocsRequest request = createRequest(DocumentFormat.HTML, PartnerType.MARKET_SORTING_CENTER);
        GetAttachedDocsRequestExecutor requestExecutor = new GetAttachedDocsRequestExecutor(
            s3FileHandler,
            wwClientJson,
            ordersDataConverter,
            documentFormatConverter,
            propertiesService
        );

        File htmlFile = temporaryFolder.newFile("attachedDoc.html");
        URL fileUrl = new URL(String.format("file:///%s", htmlFile.getAbsolutePath()));
        when(amazonS3.getUrl(anyString(), anyString())).thenReturn(fileUrl);

        byte[] actResult = StringUtils.repeat("0", 1000).getBytes();
        when(wwClientJson.generateReceptionTransferAct(any(), any())).thenReturn(actResult);

        doAnswer(i -> {
            ByteArrayInputStream stream = (ByteArrayInputStream) i.getArgument(2, InputStream.class);

            FileWriter writer = new FileWriter(htmlFile);
            IOUtils.copy(stream, writer, StandardCharsets.UTF_8);
            writer.flush();
            return null;
        })
            .when(amazonS3).putObject(anyString(), anyString(), any(InputStream.class), any(ObjectMetadata.class));

        GetAttachedDocsResponse response = requestExecutor.execute(request);
        assertThat(response.getFileUrl()).isEqualTo(fileUrl.toString());
        assertThat(response.getFormat()).isEqualTo(DocumentFormat.HTML);
        assertThat(htmlFile).isFile();
        assertThat(FileUtils.sizeOf(htmlFile)).isGreaterThan(0);
    }

    @Test
    public void createPdf() throws Exception {
        GetAttachedDocsRequest request = createRequest(DocumentFormat.PDF, PartnerType.MARKET_SORTING_CENTER);
        GetAttachedDocsRequestExecutor requestExecutor = new GetAttachedDocsRequestExecutor(
            s3FileHandler,
            wwClientJson,
            ordersDataConverter,
            documentFormatConverter,
            propertiesService
        );

        File pdfFile = temporaryFolder.newFile("attachedDoc.pdf");
        URL fileUrl = new URL(String.format("file://%s", pdfFile.getAbsolutePath()));
        when(amazonS3.getUrl(anyString(), anyString())).thenReturn(fileUrl);

        byte[] actResult = StringUtils.repeat("0", 1000).getBytes();
        when(wwClientJson.generateReceptionTransferAct(any(), any())).thenReturn(actResult);

        doAnswer(i -> {
            ByteArrayInputStream stream = (ByteArrayInputStream) i.getArgument(2, InputStream.class);

            FileWriter writer = new FileWriter(pdfFile);
            IOUtils.copy(stream, writer, StandardCharsets.UTF_8);
            writer.flush();
            return null;
        })
            .when(amazonS3).putObject(anyString(), anyString(), any(InputStream.class), any(ObjectMetadata.class));

        GetAttachedDocsResponse response = requestExecutor.execute(request);
        assertThat(response.getFileUrl()).isEqualTo(fileUrl.toString());
        assertThat(response.getFormat()).isEqualTo(DocumentFormat.PDF);

        assertThat(pdfFile).isFile();
        assertThat(FileUtils.sizeOf(pdfFile)).isGreaterThan(0);
    }

    @Test
    public void unsupportedPartnerTypeTest() {
        for (PartnerType partnerType : partnersParams()) {
            Throwable exception = catchThrowable(() -> {
                unsupportedPartnerType(partnerType);
            });
            assertThat(exception).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("PartnerType (%s) is not supported", partnerType);
        }
    }

    public void unsupportedPartnerType(PartnerType partnerType) throws IOException {
        GetAttachedDocsRequest request = createRequest(DocumentFormat.PDF, partnerType);
        GetAttachedDocsRequestExecutor requestExecutor = new GetAttachedDocsRequestExecutor(
            s3FileHandler,
            wwClientJson,
            ordersDataConverter,
            documentFormatConverter,
            propertiesService
        );

        File pdfFile = temporaryFolder.newFile();
        URL fileUrl = new URL(String.format("file://%s", pdfFile.getAbsolutePath()));
        when(amazonS3.getUrl(anyString(), anyString())).thenReturn(fileUrl);

        byte[] actResult = StringUtils.repeat("0", 1000).getBytes();
        when(wwClientJson.generateReceptionTransferAct(any(), any())).thenReturn(actResult);

        doAnswer(i -> {
            ByteArrayInputStream stream = (ByteArrayInputStream) i.getArgument(2, InputStream.class);

            IOUtils.copy(stream, new FileWriter(pdfFile), StandardCharsets.UTF_8);
            return null;
        })
            .when(amazonS3).putObject(anyString(), anyString(), any(InputStream.class), any(ObjectMetadata.class));

        requestExecutor.execute(request);
    }

    private List<PartnerType> partnersParams() {
        List<PartnerType> supportedPartnerTypes = ImmutableList.of(PartnerType.MARKET_SORTING_CENTER);
        return Arrays.stream(PartnerType.values())
            .filter(pt -> !supportedPartnerTypes.contains(pt))
            .collect(Collectors.toList());
    }
}

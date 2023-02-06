package ru.yandex.market.api.partner.controllers.offers.mapping;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import ru.yandex.market.api.partner.context.Format;
import ru.yandex.market.api.partner.context.FunctionalTest;
import ru.yandex.market.api.partner.context.FunctionalTestHelper;
import ru.yandex.market.core.campaign.model.PartnerId;
import ru.yandex.market.core.model.ModelService;
import ru.yandex.market.core.model.ModelWithNameAndVendorName;
import ru.yandex.market.logbroker.LogbrokerService;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.yt.YtTemplate;
import ru.yandex.market.mboc.http.MboMappings;
import ru.yandex.market.mboc.http.MboMappingsService;
import ru.yandex.market.mboc.http.SupplierOffer;

import static java.util.stream.Collectors.toList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class OfferMappingEntriesTest extends FunctionalTest {

    protected static final long CAMPAIGN_ID = 10774;
    protected static final long SUPPLIER_CAMPAIGN_ID = 1111;
    protected static final int SUPPLIER_ID = 111;

    //language=xml
    protected static final String XML_OK = "<response><status>OK</status></response>";

    //language=xml
    protected static final String XML_BAD_REQUEST = "" +
            "<response>\n" +
            "    <status>ERROR</status>\n" +
            "    <errors>\n" +
            "        <error code=\"BAD_REQUEST\" message=\"OfferMappingEntries array is empty\"/>\n" +
            "    </errors>\n" +
            "</response>";

    //language=json
    protected static final String JSON_OK = "{\"status\": \"OK\"}";

    //language=json
    protected static final String JSON_BAD_REQUEST = "" +
            "{\n" +
            "  \"status\": \"ERROR\",\n" +
            "  \"errors\": [\n" +
            "    {\n" +
            "      \"code\": \"BAD_REQUEST\",\n" +
            "      \"message\": \"OfferMappingEntries array is empty\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    @Autowired
    private ModelService modelService;
    @Autowired
    private YtTemplate ultraControllerYtTemplate;
    @Autowired
    private ObjectMapper jsonMapper;
    @Autowired
    protected MboMappingsService patientMboMappingsService;
    @Autowired
    @Qualifier("assortmentLogbrokerService")
    protected LogbrokerService assortmentLogbrokerService;
    @Autowired
    @Qualifier("dataCampShopClient")
    protected DataCampClient dataCampClient;

    @BeforeEach
    void setUp() {
        Mockito.doAnswer(this::invokeYtCallback)
                .when(ultraControllerYtTemplate).selectRows(Mockito.anyString(), Mockito.any(), Mockito.any());
    }

    private <T> List<T> invokeYtCallback(InvocationOnMock invocation) throws IOException {
        List<JsonNode> data = readData();
        Function<JsonNode, T> callback = invocation.getArgument(2);
        return data.stream().map(callback).collect(Collectors.toList());
    }

    @Nonnull
    private List<JsonNode> readData() throws IOException {
        List<JsonNode> result = new ArrayList<>();
        Class<?> klass = OfferMappingEntriesControllerTest.class;
        String fileName = klass.getSimpleName() + ".data.txt";
        try (InputStream stream = new BufferedInputStream(klass.getResourceAsStream(fileName))) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    result.add(jsonMapper.readValue(line, JsonNode.class));
                }
                return result;
            }
        }
    }

    @Nonnull
    protected ResponseEntity<String> getEntries(Format format, String urlQueryString) {
        String url = String.format(
                "%s/campaigns/%d/offer-mapping-entries.%s%s",
                urlBasePrefix, SUPPLIER_CAMPAIGN_ID, format.formatName(), urlQueryString);
        try {
            return FunctionalTestHelper.makeRequest(new URI(url), HttpMethod.GET, format);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    protected void prepareMocks(List<Long> modelIds, List<String> committed) {
        doAnswer(invocation -> {
            modelIds.stream()
                    .map(id -> new ModelWithNameAndVendorName(id, "test", "test"))
                    .forEach(invocation.getArgument(2));
            return null;
        }).when(modelService).getModels(anyIterable(), eq(ModelWithNameAndVendorName.class), any());

        MboMappings.SearchMappingsResponse response =
                MboMappings.SearchMappingsResponse.newBuilder().addAllOffers(
                        committed.stream()
                                .map(sku -> SupplierOffer.Offer.newBuilder()
                                        .setSupplierId(PartnerId.datasourceId(774).toLong())
                                        .setShopSkuId(Objects.toString(sku))
                                        .setApprovedMapping(
                                                SupplierOffer.Mapping.newBuilder()
                                                        .setSkuId(1L)
                                                        .build()
                                        )
                                        .build())
                                .collect(toList())
                ).build();

        doReturn(response)
                .doReturn(MboMappings.SearchMappingsResponse.newBuilder().build())
                .when(patientMboMappingsService)
                .searchMappingsByShopId(any());
    }

    protected void verifyAssortmentMarketQuickLogbrokerService() {
        verify(assortmentLogbrokerService, never()).publishEvent(any());
    }
}

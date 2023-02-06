package ru.yandex.market.mboc.common.services.idxapi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.util.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import ru.yandex.market.Magics;
import ru.yandex.market.mboc.common.offers.ImportedOffer;
import ru.yandex.market.mboc.common.services.converter.LineWith;
import ru.yandex.market.mboc.common.services.converter.models.OffersParseResult;
import ru.yandex.market.proto.indexer.v2.BlueAssortment;
import ru.yandex.market.proto.indexer.v2.ProcessLog;
import ru.yandex.market.protobuf.streams.YandexSnappyOutputStream;
import ru.yandex.market.protobuf.tools.NumberConvertionUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * @author galaev@yandex-team.ru
 * @since 19/09/2018.
 */
public class IndexerApiServiceTest {

    private IndexerApiService indexerApiService;
    private RestTemplate restTemplate;

    @Before
    public void setUp() {
        restTemplate = Mockito.mock(RestTemplate.class);
        indexerApiService = new IndexerApiServiceImpl(restTemplate, "http://idxapi");
    }

    @Test
    public void testCorrectResponse() {
        Mockito.when(restTemplate.getForObject(anyString(), any()))
            .thenAnswer(i -> {
                String url = i.getArgument(0).toString();
                if (url.startsWith("http://idxapi")) {
                    return textResponse("indexer/good_text_response.txt");
                } else if (url.startsWith("http://s3/protobuf")) {
                    return goodProtoResponse();
                }
                return null;
            });
        OffersParseResult<ImportedOffer> result = indexerApiService.parseYmlOffers("http://yml");
        Assertions.assertThat(result.isFailed()).isFalse();

        List<LineWith<ImportedOffer>> offers = result.getOffersWithLines();
        Assertions.assertThat(offers).hasSize(2);
        Assertions.assertThat(LineWith.unwrapAsList(offers))
            .extracting(ImportedOffer::getTitle)
            .containsExactlyInAnyOrder("test title", "other title");
    }

    @Test
    public void testIncorrectProtoResponse() {
        Mockito.when(restTemplate.getForObject(anyString(), any()))
            .thenAnswer(i -> {
                String url = i.getArgument(0).toString();
                if (url.startsWith("http://idxapi")) {
                    return textResponse("indexer/good_text_response.txt");
                } else if (url.startsWith("http://s3/protobuf")) {
                    return badProtoResponse();
                }
                return null;
            });
        OffersParseResult<ImportedOffer> result = indexerApiService.parseYmlOffers("http://yml");
        Assertions.assertThat(result.isFailed()).isTrue();

        // result is failed, but offers are parsed anyway
        List<LineWith<ImportedOffer>> offers = result.getOffersWithLines();
        Assertions.assertThat(offers).hasSize(2);
        Assertions.assertThat(LineWith.unwrapAsList(offers))
            .extracting(ImportedOffer::getTitle)
            .containsExactlyInAnyOrder("test title", "other title");
    }

    @Test(expected = IndexerApiException.class)
    public void testIncorrectTextResponse() {
        Mockito.when(restTemplate.getForObject(anyString(), any()))
            .thenAnswer(i -> {
                String url = i.getArgument(0).toString();
                if (url.startsWith("http://idxapi")) {
                    return textResponse("indexer/bad_text_response.txt");
                } else if (url.startsWith("http://s3/protobuf")) {
                    return goodProtoResponse();
                }
                return null;
            });
        OffersParseResult<ImportedOffer> result = indexerApiService.parseYmlOffers("http://yml");
    }

    @Test(expected = IndexerApiException.class)
    public void testIdxApiException() {
        Mockito.when(restTemplate.getForObject(anyString(), any()))
            .thenAnswer(i -> {
                throw new RestClientException("error");
            });
        OffersParseResult<ImportedOffer> result = indexerApiService.parseYmlOffers("http://yml");
    }

    @Test(expected = IndexerApiException.class)
    public void testIdxApiExceptionForNullResponse() {
        Mockito.when(restTemplate.getForObject(anyString(), any())).thenAnswer(i -> null);
        OffersParseResult<ImportedOffer> result = indexerApiService.parseYmlOffers("http://yml");
    }

    private byte[] goodProtoResponse() throws IOException {
        return createProtoResponse(false);
    }

    private byte[] badProtoResponse() throws IOException {
        return createProtoResponse(true);
    }

    private byte[] createProtoResponse(boolean withErrors) throws IOException {
        BlueAssortment.CheckResult protoResponse = BlueAssortment.CheckResult.newBuilder()
            .addAllLogMessage(createLogMessagesProto(withErrors))
            .setInputFeed(createInputFeedProto())
            .build();
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        YandexSnappyOutputStream snappyStream = new YandexSnappyOutputStream(byteStream);
        snappyStream.write(Magics.MagicConstants.FCHR.name().getBytes(Charset.forName("ASCII")));
        snappyStream.write(NumberConvertionUtils.toByteArrayInReversedOrder(protoResponse.getSerializedSize()));
        protoResponse.writeTo(snappyStream);
        snappyStream.flush();
        return byteStream.toByteArray();
    }

    private BlueAssortment.InputFeed.Builder createInputFeedProto() {
        return BlueAssortment.InputFeed.newBuilder()
            .addOffer(BlueAssortment.InputOffer.newBuilder()
                .addRawFields(createKvEntry("title", "test title"))
                .addRawFields(createKvEntry("price", "1000"))
                .addRawFields(createKvEntry("url", "http://shop.buy"))
                .addRawFields(createKvEntry("shop_category", "Стулья"))
                .addRawFields(createKvEntry("barcode", "654321"))
                .addRawFields(createKvEntry("barcode", "123456"))
                .addRawFields(createKvEntry("country_of_origin", "ru"))
                .setPosition("1:1"))
            .addOffer(BlueAssortment.InputOffer.newBuilder()
                .addRawFields(createKvEntry("title", "other title"))
                .addRawFields(createKvEntry("price", "10"))
                .addRawFields(createKvEntry("url", "http://shop.buy"))
                .addRawFields(createKvEntry("shop_category", "Кресла"))
                .addRawFields(createKvEntry("country_of_origin", "tr"))
                .setPosition("20:1"));
    }

    private BlueAssortment.KvEntry createKvEntry(String key, String value) {
        return BlueAssortment.KvEntry.newBuilder()
            .setKey(key)
            .setValue(value)
            .build();
    }

    private List<ProcessLog.LogMessage> createLogMessagesProto(boolean withErrors) {
        List<ProcessLog.LogMessage> messages = new ArrayList<>();
        messages.add(ProcessLog.LogMessage.newBuilder()
            .setCode("200")
            .setText("Feed accepted.")
            .setLevelEnum(NMarket.NProcessLog.ProcessLog.Level.MESSAGE)
            .setPosition("10:0")
            .build());
        if (withErrors) {
            messages.add(ProcessLog.LogMessage.newBuilder()
                .setCode("21f")
                .setText("Error parsing XML tags, something wrong with offer")
                .setLevelEnum(NMarket.NProcessLog.ProcessLog.Level.ERROR)
                .setPosition("5:0")
                .build());
        }
        return messages;
    }

    private String textResponse(String file) throws IOException {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(file)) {
            return IOUtils.toString(stream);
        }
    }
}

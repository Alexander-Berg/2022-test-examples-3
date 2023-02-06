package ru.yandex.market.ultracontroller.onetime;

import com.googlecode.protobuf.format.JsonFormat;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import ru.yandex.common.util.StringUtils;
import ru.yandex.ir.util.ImmutableMonitoringResult;
import ru.yandex.kikimr.persqueue.LogbrokerClientFactory;
import ru.yandex.kikimr.persqueue.auth.Credentials;
import ru.yandex.kikimr.persqueue.producer.AsyncProducer;
import ru.yandex.kikimr.persqueue.producer.async.AsyncProducerConfig;
import ru.yandex.kikimr.persqueue.proxy.ProxyBalancer;
import ru.yandex.market.CategoryTree;
import ru.yandex.market.ir.http.Matcher;
import ru.yandex.market.ir.http.Offer.YmlParam;
import ru.yandex.market.ir.http.UltraController;
import ru.yandex.market.ir.http.UltraControllerService;
import ru.yandex.market.ir.http.UltraControllerServiceStub;
import ru.yandex.market.mbo.http.OfferStorageService;
import ru.yandex.market.mbo.http.OfferStorageServiceStub;
import ru.yandex.market.mbo.http.OffersStorage;
import ru.yandex.market.ultracontroller.dao.OfferEntity;
import ru.yandex.market.ultracontroller.ext.MatcherWorker;
import ru.yandex.market.ultracontroller.utils.Dumper;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Набор методов, чтобы потестировать UC с Логброкером руками.
 */
@Ignore
public class TestUC4Logbroker {
    /**
     * Ваш личный токен.
     * можно получить по ссылке:
     * http://oauth.yandex-team.ru/authorize?response_type=token&client_id=11515c5e5e994dfe8196ccfd6eb42dd8
     */
    private static final String LOGBROKER_OAUTH_TOKEN = ""; // FIX IT BEFORE RUN!

    private static final String HTTP_USER_AGENT = "test-USERNAME"; // FIX IT BEFORE RUN!
    private static final String ULTRA_CONTROLLER_AIDS_HOST = "http://aida.yandex.ru:34500/"; // FIX IT BEFORE RUN!

    private static final String MBO_OFFERS_API_HOST =
        "https://mbo-offers-api.vs.market.yandex.net:33722/offerStorageYtRpc/";
    private static final String ULTRA_CONTROLLER_TESTING_HOST =
        "http://ultra-controller.tst.vs.market.yandex.net:34563/";
    private static final String ULTRA_CONTROLLER_PROD_HOST =
        "http://ultracontroller.vs.market.yandex.net:34563/";

    private static final int MBO_PRICE = 35;
    private static final Pattern YML_PARAMS_PATTERN = Pattern.compile(
        "<param name=\"([^\"]*)\" unit=\"([^\"]*)\">([^<]*)<", Pattern.CASE_INSENSITIVE
    );
    private static final int YML_PARAM_NAME_GROUP = 1;
    private static final int YML_PARAM_UNIT_GROUP = 2;
    private static final int YML_PARAM_VALUE_GROUP = 3;

    private static final int DEFAULT_RED_STATUS = 1;
    private static final int DEFAULT_SHOP_COUNTRY = 0;
    private static final int DEFAULT_EXTERNAL_CATEGORY_TYPE = 0;

    private static final int TEST1_CATEGORY_HID = 14960839;
    private static final String TEST1_CLASSIFIER_MAGIC_ID = "13d8ea46cdd3750e6b0c93f32af3a159";
    //    private static final String TEST1_CLASSIFIER_MAGIC_ID = "0208f6e95ed750b629870f3a7e75afa1";
    //    private static final String TEST1_CLASSIFIER_MAGIC_ID = "3882f900288ab33f97b617c83b43acbe";

    private static final String LOGBROKER_HOST = "vla.logbroker.yandex.net";
    private static final String TOPIC_NAME = "/marketir/dev/ultracontroller/feedparser/input";

    @Test
    public void testUC() throws JsonFormat.ParseException {
        UltraController.Offer offer = prepareRequest();
        UltraControllerService service = getUltraControllerService();
        UltraController.DataRequest dataRequest = UltraController.DataRequest.newBuilder()
            .addOffers(offer)
            .setOutputTopicName("feedparser/output")
            .build();
        UltraController.DataResponse enrichedOffers = service.enrich(dataRequest);
        System.out.println(JsonFormat.printToString(enrichedOffers));
    }

    @Test
    public void generateMatcherRequest() throws JsonFormat.ParseException, UnsupportedEncodingException {
        UltraController.Offer offer = prepareRequest();

        OfferEntity offerEntity = new OfferEntity(offer, ImmutableMonitoringResult.OK);
        offerEntity.setCategoryId(TEST1_CATEGORY_HID);

        CategoryTree.CategoryTreeNode node =
            CategoryTree.newCategoryTreeNodeBuilder()
                .setName("test")
                .setHyperId(TEST1_CATEGORY_HID)
                .build();
        node.addLinkedCategory(TEST1_CATEGORY_HID);
        CategoryTree tree = Mockito.mock(CategoryTree.class);
        when(tree.getByHyperId(any())).thenReturn(node);
        MatcherWorker matcherWorker = new MatcherWorker();
        matcherWorker.setCategoryTree(tree);

        Matcher.Offer.Builder matcherRequestBuilder = matcherWorker.generateMatcherRequest(
            Collections.singletonList(offerEntity)
        ).getOffers().get(0).toBuilder();
        matcherRequestBuilder
            .clearGuruCategoryId()
            .setHid(TEST1_CATEGORY_HID)
            .setUseFormalizedMatcher(true);
        String matcherRequest = JsonFormat.printToString(matcherRequestBuilder.build());

        System.out.println(JsonFormat.printToString(offer));
        System.out.println(matcherRequest);
        System.out.println(URLEncoder.encode(matcherRequest, "UTF-8"));
        Matcher.OfferBatch matchBatchRequest = Matcher.OfferBatch.newBuilder().addOffer(matcherRequestBuilder).build();
        System.out.println(URLEncoder.encode(JsonFormat.printToString(matchBatchRequest), "UTF-8"));
    }

    @Test
    public void saveUltraControllerRequest() throws IOException {
        UltraController.Offer request = prepareRequest();
        Path path = Paths.get("ultraControllerRequest.pb");
        try (
            OutputStream outputStream = Files.newOutputStream(path)
        ) {
            request.writeTo(outputStream);
        }
        System.out.println("Written to " + path.toAbsolutePath());

    }

    @Test
    public void loadUltraControllerRequest() throws IOException {
        Path path = Paths.get("ultraControllerRequest.pb");
        System.out.println(path.toAbsolutePath());
        byte[] data = Files.readAllBytes(path);

        UltraController.Offer offer = UltraController.Offer.newBuilder().mergeFrom(data).build();

        System.out.println(JsonFormat.printToString(offer));
        System.out.println(Dumper.dump(data));
    }

    @Test
    public void saveEnrichCacheRecord() throws IOException {
        UltraController.Offer request = prepareRequest();
        UltraControllerService service = getUltraControllerService();

        long requestTsSec = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        UltraController.EnrichedOffer enrichedOffer = service.enrichSingleOffer(request);
        long responseTsSec = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());

        System.out.println(JsonFormat.printToString(enrichedOffer));
        System.out.println("Spent " + (responseTsSec - requestTsSec) + " seconds.");

        UltraController.EnrichCacheRecord enrichCacheRecord = UltraController.EnrichCacheRecord.newBuilder()
            .setShopId(request.getShopId())
            .setOfferId(request.getShopOfferId())
            .setFeedId(0)
            .setRequestTsSec(requestTsSec)
            .setRequest(request)
            .setResponseTsSec(responseTsSec)
            .setResponse(enrichedOffer)
            .build();

        Path path = Paths.get("enrichCacheRecord2.pb");
        try (
            OutputStream outputStream = Files.newOutputStream(path)
        ) {
            enrichCacheRecord.writeTo(outputStream);
        }
        System.out.println("Written to " + path.toAbsolutePath());
    }

    /**
     * Файл с запросом можно сделать методом saveUltraControllerRequest().
     * Ошибка "StatusRuntimeException: UNAVAILABLE: HTTP/2 error code: NO_ERROR" означает,
     * что не верный LOGBROKER_OAUTH_TOKEN.
     */
    @Test
    public void sendRequestToLogbroker() throws Exception {
        Path path = Paths.get("ultraControllerRequest.pb");
        byte[] data = Files.readAllBytes(path);

        ProxyBalancer proxyBalancer = new ProxyBalancer(LOGBROKER_HOST);
        LogbrokerClientFactory logbrokerClientFactory = new LogbrokerClientFactory(proxyBalancer);
        byte[] sourceId = UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8);
        AsyncProducerConfig producerConfig = AsyncProducerConfig
            .builder(TOPIC_NAME, sourceId)
            .setCredentialsProvider(() -> Credentials.oauth(LOGBROKER_OAUTH_TOKEN))
            .build();
        AsyncProducer producer = logbrokerClientFactory.asyncProducer(producerConfig);

        producer.init();
        producer.write(data, 1);
        producer.close();
    }


    private static UltraController.Offer prepareRequest() throws JsonFormat.ParseException {
        OffersStorage.GenerationDataOffer offer = obtainOffer(TEST1_CLASSIFIER_MAGIC_ID);
        return toUltraControllerRequest(offer);
    }

    private static OffersStorage.GenerationDataOffer obtainOffer(String classifierMagicId) {
        OfferStorageService offerStorageService = getOfferStorageService();
        OffersStorage.GetOffersResponse response = offerStorageService.getOffersByIds(
            OffersStorage.GetOffersRequest.newBuilder()
                .addClassifierMagicIds(classifierMagicId)
                .build()
        );
        return response.getOffers(0);
    }

    private static UltraController.Offer toUltraControllerRequest(OffersStorage.GenerationDataOffer offer) {
        UltraController.Offer.Builder builder = UltraController.Offer.newBuilder();
        if (!StringUtils.isEmpty(offer.getClassifierGoodId())) {
            builder.setClassifierGoodId(offer.getClassifierGoodId());
        }
        return builder.setClassifierMagicId(offer.getClassifierMagicId())
            .setFeedId((int) offer.getFeedId())
            .setOffer(offer.getOffer())
            .setDescription(offer.getDescription())
            .setIsbn(offer.getIsbn())
            .setVendorCode(offer.getVendorCode())
            .setParams(offer.getParams())
            .addAllYmlParam(
                parseOfferParamsToYmlParam(offer.getOfferParams())
            )
            .setMarketCategory(offer.getMarketCategory())
            .setPrice(offer.getPrice() * MBO_PRICE)
            .setShopId((int) offer.getShopId())
            .setShopName(offer.getDatasource())
            .setShopCategoryName(offer.getShopCategoryName())
            .setPicUrls(offer.getPicUrls())
            .setLocale(offer.getLocale())
            .setAdult(offer.getAdult() > 0)
            .setReturnMarketNames(false)
            .setSkuShop(offer.getSkuShop())
            .setBarcode(offer.getBarcode())
            .setShopCountry(orDefault(offer.getShopCountry(), DEFAULT_SHOP_COUNTRY))
            .setSkipCache(true)
            .setExternalCategoryName(offer.getExternalCategoryName())
            .setExternalCategoryType(orDefault(offer.getExternalCategoryType(), DEFAULT_EXTERNAL_CATEGORY_TYPE))
            .setIsBlueAssortment(false)
            .build();
    }

    private static List<YmlParam> parseOfferParamsToYmlParam(String xml) {
        if (xml == null) {
            return Collections.emptyList();
        }
        List<YmlParam> ymlParamList = new ArrayList<>();
        xml = xml.replaceAll("\\s+", " ");
        java.util.regex.Matcher matcher = YML_PARAMS_PATTERN.matcher(xml);
        while (matcher.find()) {
            String name = matcher.group(YML_PARAM_NAME_GROUP);
            String value = matcher.group(YML_PARAM_VALUE_GROUP);
            String unit = matcher.group(YML_PARAM_UNIT_GROUP);
            ymlParamList.add(
                YmlParam.newBuilder()
                    .setName(StringUtils.emptyIfNull(name))
                    .setValue(StringUtils.emptyIfNull(value))
                    .setUnit(StringUtils.emptyIfNull(unit))
                    .build()
            );
        }
        return ymlParamList;
    }

    private static int orDefault(int value, int defaultValue) {
        if (value == 0) {
            return defaultValue;
        }
        return value;
    }

    private static OfferStorageService getOfferStorageService() {
        OfferStorageServiceStub stub = new OfferStorageServiceStub();
        stub.setUserAgent(HTTP_USER_AGENT);
        stub.setHost(MBO_OFFERS_API_HOST);
        return stub;
    }

    private static UltraControllerService getUltraControllerService() {
        UltraControllerServiceStub ultraControllerService = new UltraControllerServiceStub();
        ultraControllerService.setUserAgent(HTTP_USER_AGENT);
        ultraControllerService.setHost(ULTRA_CONTROLLER_PROD_HOST);
//        ultraControllerService.setHost(ULTRA_CONTROLLER_TESTING_HOST);
//        ultraControllerService.setHost(ULTRA_CONTROLLER_AIDS_HOST);
        return ultraControllerService;
    }
}

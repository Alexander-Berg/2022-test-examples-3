package ru.yandex.market.rg.asyncreport.assortment;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import Market.DataCamp.SyncAPI.SyncGetOffer;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import ru.yandex.market.common.mds.s3.client.service.api.MdsS3Client;
import ru.yandex.market.common.test.util.ProtoTestUtil;
import ru.yandex.market.core.asyncreport.worker.model.ReportResult;
import ru.yandex.market.core.feed.validation.result.FeedXlsService;
import ru.yandex.market.core.supplier.model.OfferInfo;
import ru.yandex.market.mbi.datacamp.model.search.SearchBusinessOffersRequest;
import ru.yandex.market.mbi.datacamp.stroller.DataCampClient;
import ru.yandex.market.mbi.datacamp.stroller.DataCampStrollerConversions;
import ru.yandex.market.rg.asyncreport.assortment.model.AssortmentParams;
import ru.yandex.market.rg.config.FunctionalTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

public abstract class AbstractAssortmentGeneratorTest extends FunctionalTest {

    private static final int CAPTURED_OFFERS_MAX_SIZE = 10;

    private final boolean isBusiness;
    private final boolean isPriceMode;

    public AbstractAssortmentGeneratorTest(boolean isBusiness, boolean isPriceMode) {
        this.isBusiness = isBusiness;
        this.isPriceMode = isPriceMode;
    }

    @Autowired
    protected MdsS3Client mdsS3Client;

    @Autowired
    private FeedXlsService<OfferInfo> unitedFeedTemplateXlsService;

    @Autowired
    @Qualifier("assortmentGenerator")
    protected AbstractAssortmentGenerator assortmentGenerator;

    @Autowired
    @Qualifier("assortmentPriceGenerator")
    protected AbstractAssortmentGenerator assortmentPriceGenerator;

    @Autowired
    @Qualifier("assortmentBusinessGenerator")
    protected AbstractAssortmentGenerator assortmentBusinessGenerator;

    @Autowired
    protected DataCampClient dataCampShopClient;

    protected AbstractAssortmentGenerator getAssortmentGenerator() {
        if (isBusiness) {
            return assortmentBusinessGenerator;
        } else if (isPriceMode) {
            return assortmentPriceGenerator;
        } else {
            return assortmentGenerator;
        }
    }

    protected List<OfferInfo> runGeneratorAndCaptureOfferInfo(long partnerId) {
        AssortmentParams assortmentParams = new AssortmentParams();
        assortmentParams.setEntityId(partnerId);

        return runGeneratorAndCaptureOfferInfo(assortmentParams);
    }

    @SuppressWarnings("unchecked")
    protected List<OfferInfo> runGeneratorAndCaptureOfferInfo(
            AssortmentParams assortmentParams
    ) {
        AtomicInteger counter = new AtomicInteger(0);
        AtomicReferenceArray<OfferInfo> capturedOffers =
                new AtomicReferenceArray<>(CAPTURED_OFFERS_MAX_SIZE);

        // вытаскиваем значения из стрима и не пишем excel
        doAnswer(invocation -> {
            ((Stream<OfferInfo>) invocation.getArgument(1)).forEach(
                    offerInfo -> capturedOffers.set(
                            counter.getAndIncrement() % CAPTURED_OFFERS_MAX_SIZE,
                            offerInfo
                    )
            );
            Consumer<Path> pathConsumer = invocation.getArgument(2);
            pathConsumer.accept(Path.of("somePath"));
            return null;
        }).when(unitedFeedTemplateXlsService)
                .fillTemplate(any(), any(), any());

        prepareAndRunGenerator(assortmentParams);

        return IntStream.range(0, CAPTURED_OFFERS_MAX_SIZE)
                .mapToObj(capturedOffers::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    protected SearchBusinessOffersRequest runGeneratorAndCaptureDataCampRequest(
            AssortmentParams assortmentParams
    ) {
        var dataCampEmptyResponseMock = ProtoTestUtil.getProtoMessageByJson(
                SyncGetOffer.GetUnitedOffersResponse.class,
                "proto/AssortmentGeneratorTest.testAssortmentParamsToDatacampRequestConversion.json",
                getClass()
        );
        doReturn(DataCampStrollerConversions.fromStrollerResponse(dataCampEmptyResponseMock))
                .when(dataCampShopClient).searchBusinessOffers(any());
        try {
            prepareAndRunGenerator(assortmentParams);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        var dataCampRequestCaptor = ArgumentCaptor.forClass(SearchBusinessOffersRequest.class);
        verify(dataCampShopClient).searchBusinessOffers(dataCampRequestCaptor.capture());
        return dataCampRequestCaptor.getValue();
    }


    protected void prepareAndRunGenerator(AssortmentParams assortmentParams) {
        try {
            doReturn(new URL("http://path/to"))
                    .when(mdsS3Client)
                    .getUrl(any());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        ReportResult reportResult = getAssortmentGenerator().generate("1", assortmentParams);
        assertThat(reportResult.getReportGenerationInfo().getUrlToDownload()).isEqualTo("http://path/to");
    }
}

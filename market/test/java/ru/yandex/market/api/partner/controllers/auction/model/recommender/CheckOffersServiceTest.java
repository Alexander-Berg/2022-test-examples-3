package ru.yandex.market.api.partner.controllers.auction.model.recommender;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import name.falgout.jeffrey.testing.junit.mockito.MockitoExtension;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;

import ru.yandex.market.api.partner.controllers.auction.model.report.ReportError;
import ru.yandex.market.api.partner.controllers.auction.model.report.check_offers.CheckOffersRecord;
import ru.yandex.market.api.partner.controllers.auction.model.report.check_offers.CheckOffersResponse;
import ru.yandex.market.common.report.AsyncMarketReportService;
import ru.yandex.market.common.report.model.MarketReportPlace;
import ru.yandex.market.common.report.model.MarketSearchRequest;
import ru.yandex.market.core.auction.model.AuctionOfferId;
import ru.yandex.market.mbi.util.MbiCollectors;
import ru.yandex.market.mbi.util.MbiMatchers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static ru.yandex.market.core.auction.matchers.MarketSearchRequestMatchers.hasPlace;

/**
 * Тесты для {@link CheckOffersService}.
 *
 * @author vbudnev
 */
@ExtendWith(MockitoExtension.class)
class CheckOffersServiceTest {

    private static final int URL_MAX_LENGTH_BIG = Integer.MAX_VALUE;
    private static final Set<AuctionOfferId> SOME_IRRELEVANT_IDS = ImmutableSet.of(
            new AuctionOfferId(100L, "someOfferId1"),
            new AuctionOfferId(200L, "someOfferId2")
    );
    @Captor
    private ArgumentCaptor<MarketSearchRequest> requestArgumentCaptor;
    @Mock
    private AsyncMarketReportService reportService;
    private CheckOffersService checkOffersService;

    @BeforeEach
    void beforeEach() {
        checkOffersService = new CheckOffersService(reportService);
    }

    @DisplayName("Сообщение об ошибке в ответе репорта")
    @Test
    void testParseError() {
        mockReportAnswer("check_offers_error.json");
        CheckOffersResponse response = loadChecks(SOME_IRRELEVANT_IDS);
        assertThat(
                response.getError(),
                MbiMatchers.<ReportError>newAllOfBuilder()
                        .add(ReportError::getCode, "EMPTY_REQUEST", "code")
                        .add(ReportError::getMessage, "Request is empty", "message")
                        .build()
        );

        assertTrue(response.hasError());
        assertThat(response.getResults(), nullValue());
    }

    @DisplayName("Передача идентификаторов ТП в base64")
    @Test
    void testBase64Conversion() {
        mockReportAnswer("check_offers_response.json");
        loadChecks(ImmutableSet.of(
                new AuctionOfferId(123L, "абвгд,abcde=-"),
                new AuctionOfferId(456L, "someofferId")
                )
        );

        Mockito.verify(reportService).async(requestArgumentCaptor.capture(), any());
        MarketSearchRequest request = requestArgumentCaptor.getValue();
        Collection<String> values = request.getParams().get("feed_shoffer_id_base64");

        assertThat(request, hasPlace(MarketReportPlace.CHECK_OFFERS));

        assertThat(
                values,
                Matchers.contains(
                        ImmutableList.of(Matchers.is("MTIzLdCw0LHQstCz0LQsYWJjZGU9LQ==,NDU2LXNvbWVvZmZlcklk"))
                )
        );

    }

    /**
     * 5 идентификаторов должны быть побиты на 3 блока с учтоем ограничения на 50 так как:
     * "1_id_of_length_15" дает base64 свертку в 24 символа.
     */
    @DisplayName("N запросов в репорт при ограничении url запроса")
    @Test
    void test_lengthSplit() {
        mockReportAnswer("check_offers_response.json");

        final Set<AuctionOfferId> offerIds = ImmutableSet.of(
                new AuctionOfferId(1L, "id_of_length_15"),
                new AuctionOfferId(2L, "id_of_length_15"),
                new AuctionOfferId(3L, "id_of_length_15"),
                new AuctionOfferId(4L, "id_of_length_15"),
                new AuctionOfferId(5L, "id_of_length_15")
        );

        final List<CheckOffersResponse> responses = checkOffersService.loadChecks(offerIds, 50)
                .stream()
                .collect(MbiCollectors.toFutureList())
                .join();

        assertThat(responses, hasSize(3));

        Mockito.verify(reportService, times(3)).async(requestArgumentCaptor.capture(), any());
        List<MarketSearchRequest> requests = requestArgumentCaptor.getAllValues();

        //первый блок
        Collection<String> values = requests.get(0).getParams().get("feed_shoffer_id_base64");

        assertThat(requests.get(0), hasPlace(MarketReportPlace.CHECK_OFFERS));
        assertThat(
                values,
                Matchers.contains(
                        //1-id_of_length_15 и 2-id_of_length_15
                        ImmutableList.of(Matchers.is("Mi1pZF9vZl9sZW5ndGhfMTU=,MS1pZF9vZl9sZW5ndGhfMTU="))
                )
        );

        //второй блок
        values = requests.get(1).getParams().get("feed_shoffer_id_base64");

        assertThat(requests.get(1), hasPlace(MarketReportPlace.CHECK_OFFERS));
        assertThat(
                values,
                Matchers.contains(
                        //3-id_of_length_15 и 4-id_of_length_15
                        ImmutableList.of(Matchers.is("NC1pZF9vZl9sZW5ndGhfMTU=,My1pZF9vZl9sZW5ndGhfMTU="))
                )
        );

        //третий блок
        values = requests.get(2).getParams().get("feed_shoffer_id_base64");

        assertThat(requests.get(2), hasPlace(MarketReportPlace.CHECK_OFFERS));
        assertThat(
                values,
                Matchers.contains(
                        //5-id_of_length_15
                        ImmutableList.of(Matchers.is("NS1pZF9vZl9sZW5ndGhfMTU="))
                )
        );

    }

    @DisplayName("Валидный ответ репорта")
    @Test
    void testParse() {
        mockReportAnswer("check_offers_response.json");
        CheckOffersResponse response = loadChecks(SOME_IRRELEVANT_IDS);
        assertThat(
                response.getResults(),
                Matchers.contains(
                        ImmutableList.of(
                                MbiMatchers.<CheckOffersRecord>newAllOfBuilder()
                                        .add(CheckOffersRecord::getFeedId, 475690L, "feedId")
                                        .add(CheckOffersRecord::getOfferId, "493303.000284.MN0W2RU/A", "offerId")
                                        .add(CheckOffersRecord::getModelId, 14209841L, "modelId")
                                        .add(CheckOffersRecord::hasModel, true, "hasModel")
                                        .build(),
                                MbiMatchers.<CheckOffersRecord>newAllOfBuilder()
                                        .add(CheckOffersRecord::getFeedId, 475690L, "feedId")
                                        .add(CheckOffersRecord::getOfferId, "510689.YNDX-000SB", "offerId")
                                        .add(CheckOffersRecord::getModelId, nullValue(), "modelId")
                                        .add(CheckOffersRecord::hasModel, false, "hasModel")
                                        .build()
                        )
                )
        );

        assertThat(response.getError(), nullValue());
        assertFalse(response.hasError());
    }

    private void mockReportAnswer(String fileName) {
        when(reportService.async(any(), any()))
                .thenAnswer(ignored ->
                        {
                            final CheckOffersResponseParser parser = new CheckOffersResponseParser();
                            parser.parse(CheckOffersServiceTest.class.getResourceAsStream(fileName));
                            return CompletableFuture.completedFuture(parser.getResult());
                        }
                );

    }

    private CheckOffersResponse loadChecks(Set<AuctionOfferId> offerIds) {
        return checkOffersService.loadChecks(offerIds, URL_MAX_LENGTH_BIG)
                .stream()
                .collect(MbiCollectors.toFutureList())
                .join()
                .get(0);
    }
}
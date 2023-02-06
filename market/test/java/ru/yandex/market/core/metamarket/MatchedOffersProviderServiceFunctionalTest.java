package ru.yandex.market.core.metamarket;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import ru.yandex.market.core.FunctionalTest;
import ru.yandex.market.core.offer.OfferId;

@ParametersAreNonnullByDefault
public class MatchedOffersProviderServiceFunctionalTest extends FunctionalTest {
    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Test
    public void test0() {
        NamedParameterJdbcTemplate mockYqlTemplate = mockedYqlTemplate(Collections.emptyList());
        MatchedOffersProviderService matchedOffersProviderService =
                new MatchedOffersProviderService(mockYqlTemplate, "hahn.[//test1]", "hahn.//test2");
        List<BlueWhitePair<OfferId>> matchedOffers = new ArrayList<>();
        matchedOffersProviderService.streamMatchedOfferPairsFromYt(matchedOfferStream -> {
            matchedOffers.addAll(matchedOfferStream.collect(Collectors.toList()));
        });
        Assert.assertEquals(0, matchedOffers.size());
    }


    @Test
    public void test1() {
        NamedParameterJdbcTemplate mockYqlTemplate = mockedYqlTemplate(Arrays.asList(
                BlueWhitePair.of(OfferId.of(810661, "BLU123"), OfferId.of(810774, "WHI123"))
        ));
        MatchedOffersProviderService matchedOffersProviderService =
                new MatchedOffersProviderService(mockYqlTemplate, "hahn.[//test1]", "hahn.//test2");
        List<BlueWhitePair<OfferId>> matchedOffers = new ArrayList<>();
        matchedOffersProviderService.streamMatchedOfferPairsFromYt(matchedOfferStream -> {
            matchedOffers.addAll(matchedOfferStream.collect(Collectors.toList()));
        });
        Assert.assertEquals(1, matchedOffers.size());
        Assert.assertEquals(OfferId.of(810661, "BLU123"), matchedOffers.get(0).blue());
        Assert.assertEquals(OfferId.of(810774, "WHI123"), matchedOffers.get(0).white());
    }

    @Test
    public void test3() {
        NamedParameterJdbcTemplate mockYqlTemplate = mockedYqlTemplate(Arrays.asList(
                BlueWhitePair.of(OfferId.of(810661, "BLU123"), OfferId.of(810774, "WHI123")),
                BlueWhitePair.of(OfferId.of(810661, "BLU124"), OfferId.of(810774, "WHI124")),
                BlueWhitePair.of(OfferId.of(810662, "BLU123"), OfferId.of(810775, "WHI123"))
        ));
        MatchedOffersProviderService matchedOffersProviderService =
                new MatchedOffersProviderService(mockYqlTemplate, "hahn.[//test1]", "hahn.//test2");
        List<BlueWhitePair<OfferId>> matchedOffers = new ArrayList<>();
        matchedOffersProviderService.streamMatchedOfferPairsFromYt(matchedOfferStream -> {
            matchedOffers.addAll(matchedOfferStream.collect(Collectors.toList()));
        });
        Assert.assertEquals(3, matchedOffers.size());
        Assert.assertEquals(OfferId.of(810661, "BLU123"), matchedOffers.get(0).blue());
        Assert.assertEquals(OfferId.of(810774, "WHI123"), matchedOffers.get(0).white());

        Assert.assertEquals(OfferId.of(810661, "BLU124"), matchedOffers.get(1).blue());
        Assert.assertEquals(OfferId.of(810774, "WHI124"), matchedOffers.get(1).white());

        Assert.assertEquals(OfferId.of(810662, "BLU123"), matchedOffers.get(2).blue());
        Assert.assertEquals(OfferId.of(810775, "WHI123"), matchedOffers.get(2).white());
    }

    @Nonnull
    private NamedParameterJdbcTemplate mockedYqlTemplate(List<BlueWhitePair<OfferId>> pairs) {
        String mockQuery;
        if (pairs.isEmpty()) {
            mockQuery = " SELECT 1 FROM dual WHERE 0 = 1 ";
        } else {
            String formatString = "" +
                    " SELECT" +
                    "     %d blue_feed_id," +
                    "     '%s' blue_shop_sku," +
                    "     %d white_feed_id," +
                    "     '%s' white_offer_id" +
                    " FROM dual";
            mockQuery = pairs.stream()
                    .map(pair -> String.format(formatString,
                            pair.blue().feedId(),
                            pair.blue().offerId(),
                            pair.white().feedId(),
                            pair.white().offerId()))
                    .collect(Collectors.joining(" UNION ALL "));
        }
        NamedParameterJdbcTemplate mockYqlTemplate = Mockito.mock(NamedParameterJdbcTemplate.class);
        Mockito.when(mockYqlTemplate.query(Mockito.anyString(), Mockito.any(ResultSetExtractor.class)))
                .thenAnswer((Answer<Boolean>) invocation -> {
                    ResultSetExtractor<Boolean> resultSetExtractor = invocation.getArgument(1);
                    return namedParameterJdbcTemplate.query(mockQuery, resultSetExtractor);
                });
        return mockYqlTemplate;
    }
}

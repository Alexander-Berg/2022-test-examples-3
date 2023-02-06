package ru.yandex.market.api.internal.report.parsers.json;

import javax.inject.Inject;

import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.domain.OfferId;
import ru.yandex.market.api.domain.PageInfo;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.report.InternalModelsResult;
import ru.yandex.market.api.internal.report.ReportRequestContext;
import ru.yandex.market.api.internal.report.parsers.ReportParserFactory;
import ru.yandex.market.api.matchers.OfferMatcher;
import ru.yandex.market.api.test.infrastructure.prerequisites.annotations.WithContext;
import ru.yandex.market.api.util.ResourceHelpers;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
@WithContext
public class InternalModelsResultParserTest extends BaseTest {
    @Inject
    private ReportParserFactory factory;

    @Test
    public void shouldParsePromoOffers() {
        InternalModelsResult result = parse("model-with-additional-offers.json");
        Matcher<OfferV2> offerMatcher = OfferMatcher.offer(
                OfferMatcher.offerId(
                        Matchers.is(
                                new OfferId("Kj1tbMS153I4wfwbts3WgQ",
                                        "aUQL_bjSov2lVsVWkvd8NdnCKMUO")
                        )
                )
        );
        Assert.assertThat(
                result.getPromoOffers(),
                Matchers.contains(offerMatcher)
        );
    }


    public InternalModelsResult parse(String filename) {
        ReportRequestContext context = new ReportRequestContext();
        InternalModelsResultParser parser =
                factory.getInternalModelsResultParser(context, PageInfo.DEFAULT);
        return parser.parse(ResourceHelpers.getResource(filename));
    }
}

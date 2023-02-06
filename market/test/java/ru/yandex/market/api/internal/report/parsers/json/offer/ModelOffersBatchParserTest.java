package ru.yandex.market.api.internal.report.parsers.json.offer;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.report.ReportRequestContext;
import ru.yandex.market.api.internal.report.parsers.ReportParserFactory;
import ru.yandex.market.api.internal.report.parsers.json.ModelOffersV2BatchParser;
import ru.yandex.market.api.util.ResourceHelpers;

/**
 * @author dimkarp93
 */
public class ModelOffersBatchParserTest extends BaseTest {
    @Inject
    private ReportParserFactory reportParserFactory;

    @Test
    public void modelBatchOffers() {
        List<OfferV2> result = parse("offer-model-offers-batch.json");
        Assert.assertThat(
                result.stream().map(OfferV2::getWareMd5).collect(Collectors.toList()),
                Matchers.containsInAnyOrder(
                        "hOATIldS1-HdJ_K1Q5IkCg",
                        "qDBDsLjOMpeCrFx3EWRmIA",
                        "9WG30XGSZuybCP9d-LZFQQ",
                        "uuU2JlyoTCcCmdATKJYglw",
                        "TL_7SZaImjzKRF4z2VI8Qw",
                        "h0L9GpzJbbDpvTQxNlDlnQ",
                        "4UmnThroA9uMJbJ3dtMhJg",
                        "Ocyk4WWooShVNLGVFRm0fA",
                        "5Oadr3yA8jLFKQs5v5c93w",
                        "instV_Y9ul1aT1vSrqvDiw",
                        "LrBk1uLBrIoTxsHaTlYv_Q",
                        "1wRFs22Iqgkp98BIqVfkWg",
                        "phVaFuIPmg6SiZA406WNHw",
                        "Gy1UQtFsDGs5penXkF78mg",
                        "ocYFI1_-_c29OV5aVIFlUQ",
                        "13Tfw49ozqmzMhjQnFHriw",
                        "nAiffgDxKFOMBOQCqeCMWw",
                        "ejIUv3gCiaIIddKic1R5_A",
                        "t24NRknnXO7DsebczGp2Bw",
                        "nl17fiqdHdMGcD9oIHqebg"
                )
        );
    }

    @Test
    public void modelOriginIdBatchOffers() {
        List<OfferV2> result = parse("offer-model-offers-batch.json");
        Assert.assertThat(result.get(0).getModel().getId(), Matchers.is(12299000L));
        Assert.assertThat(result.get(0).getModel().getOriginalId(), Matchers.is(12299057L));
        Assert.assertThat(result.get(1).getModel().getId(), Matchers.is(12299057L));
        Assert.assertThat(result.get(1).getModel().getOriginalId(), Matchers.is(12299057L));
    }

    @Test
    public void modelBatchOffersNPE() {
        List<OfferV2> result = parse("offer-model-offers-batch-NPE.json");
        Assert.assertThat(result, Matchers.empty());
    }

    public List<OfferV2> parse(String filename) {
        ModelOffersV2BatchParser offersParser =
                reportParserFactory.getModelOffersBatchParser(new ReportRequestContext());
        return offersParser.parse(ResourceHelpers.getResource(filename));
    }
}

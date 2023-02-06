package ru.yandex.autotests.market.partner.api.campaigns.bids;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.partner.api.steps.ApplyingRecommendedBidsSteps;
import ru.yandex.autotests.market.partner.beans.Format;
import ru.yandex.autotests.market.partner.beans.PartnerApiRequestData;
import ru.yandex.autotests.market.partner.beans.Target;
import ru.yandex.autotests.market.partner.beans.api.bids.recommended.body.Offers;
import ru.yandex.autotests.market.report.util.ReportAvailabilityRule;

import java.util.Arrays;
import java.util.Collection;

import static ru.yandex.autotests.market.partner.api.data.bids.RecommendedBidsData.*;

/**
 * @author stille
 * @since 29.08.13
 */
@Feature("Campaign recommended bids put")
@Aqua.Test(title = "Установка рекомендованных ставок для кампании")
@RunWith(Parameterized.class)
public class PutRecommendedTest {

    @Rule
    public final ReportAvailabilityRule reportAvailability = new ReportAvailabilityRule();

    private ApplyingRecommendedBidsSteps tester = new ApplyingRecommendedBidsSteps();

    private Offers offers;
    private PartnerApiRequestData requestData;
    private Target target;
    private int position;

    public PutRecommendedTest(String caseName,
                              PartnerApiRequestData requestData, Offers offers, Target target, int position) {
        this.offers = offers;
        this.requestData = requestData;
        this.target = target;
        this.position = position;
    }

    @Parameterized.Parameters(name = "Case: {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[]{
                        "Put Recom by Id on GUARANTEE_POSITION to MODEL_CARD",
                        basicPutRecommendedRequestById(),
                        defaultOffersBodyById(),
                        Target.MODEL_CARD,
                        GUARANTEE_POSITION
                },
                new Object[]{
                        "Put Recom by Id on GUARANTEE_POSITION to SEARCH",
                        basicPutRecommendedRequestById(),
                        defaultOffersBodyById(),
                        Target.SEARCH,
                        GUARANTEE_POSITION
                }
        );
    }

    @Test
    public void isResponseCorrect() {
        tester.isResponseCorrect(offers, requestData
                .withTarget(target)
                .withPosition(position));
    }

    @Test
    public void isResponseCorrectJSON() {
        tester.isResponseCorrectJSON(offers, requestData
                .withTarget(target)
                .withPosition(position));
    }


    @Test
    public void isResponseCorrectXML() {
        tester.isResponseCorrect(offers, requestData
                .withTarget(target)
                .withPosition(position)
                .withFormat(Format.XML));
    }

    @Test
    public void areBidsApplied() {
        tester.areBidsApplied(offers,
                requestData
                        .withTarget(target)
                        .withPosition(position),
                target, position);
    }

    @Test
    public void areBidsAppliedJSON() {
        tester.areBidsAppliedJSON(offers,
                requestData.withTarget(target)
                        .withPosition(position)
                        .withFormat(Format.JSON),
                target, position);
    }

    @Test
    public void areBidsAppliedXML() {
        tester.areBidsApplied(offers,
                requestData.withBody(offers)
                        .withTarget(target)
                        .withFormat(Format.XML)
                        .withPosition(position),
                target, position);
    }

}

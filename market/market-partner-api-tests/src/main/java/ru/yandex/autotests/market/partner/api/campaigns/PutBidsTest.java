package ru.yandex.autotests.market.partner.api.campaigns;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.aqua.annotations.project.Aqua;
import ru.yandex.aqua.annotations.project.Feature;
import ru.yandex.autotests.market.partner.api.data.bids.PutBidsTestCase;
import ru.yandex.autotests.market.partner.api.steps.ApplyingBidsSteps;
import ru.yandex.autotests.market.partner.beans.ApiVersion;
import ru.yandex.autotests.market.partner.beans.Format;
import ru.yandex.autotests.market.partner.beans.PartnerApiRequestData;
import ru.yandex.autotests.market.partner.beans.api.bids.body.OfferBids;
import ru.yandex.autotests.market.report.util.ReportAvailabilityRule;
import ru.yandex.qatools.hazelcast.HazelcastAnnotations;

import java.util.Arrays;
import java.util.Collection;

import static ru.yandex.autotests.market.partner.api.data.bids.PutBidsData.campaignIdWithIdIdentification;
import static ru.yandex.autotests.market.partner.api.data.bids.PutBidsData.campaignIdWithTitleIdentification;
import static ru.yandex.autotests.market.partner.api.data.bids.PutBidsData.notNullOfferBidsById;
import static ru.yandex.autotests.market.partner.api.data.bids.PutBidsData.notNullOfferBidsByTitle;
import static ru.yandex.autotests.market.partner.api.data.bids.PutBidsData.nullOfferBidsById;
import static ru.yandex.autotests.market.partner.api.data.bids.PutBidsData.nullOfferBidsByTitle;
import static ru.yandex.autotests.market.partner.api.data.bids.PutBidsData.particularOfferBidsById;
import static ru.yandex.autotests.market.partner.api.data.bids.PutBidsData.particularOfferBidsByTitle;
import static ru.yandex.autotests.market.partner.api.data.bids.PutBidsData.requestForApplyingById;
import static ru.yandex.autotests.market.partner.api.data.bids.PutBidsData.requestForApplyingByTitle;

/**
 * Created with IntelliJ IDEA.
 * User: stille
 * Date: 05.07.13
 * Time: 15:49
 * To change this template use File | Settings | File Templates.
 */
@Feature("Campaign bids put")
@Aqua.Test(title = "Установка ставок для кампании")
@RunWith(Parameterized.class)
public class PutBidsTest {

    @Rule
    public final ReportAvailabilityRule reportAvailability = new ReportAvailabilityRule();

    private ApplyingBidsSteps tester = new ApplyingBidsSteps();

    private OfferBids offerBids;
    private int campaignId;
    private PartnerApiRequestData requestData;
    private String caseName;

    @Parameterized.Parameters(name = "Case: {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {new PutBidsTestCase(
                        campaignIdWithTitleIdentification(),
                        particularOfferBidsByTitle(),
                        requestForApplyingByTitle(),
                        "particularOfferBidsByTitle"
                )},
                {new PutBidsTestCase(
                        campaignIdWithIdIdentification(),
                        particularOfferBidsById(),
                        requestForApplyingById(),
                        "particularOfferBidsById"
                )}
        });
    }

    @HazelcastAnnotations.Lock("Campaign bids put")
    public PutBidsTest(PutBidsTestCase bidsTestCase) {
        this.campaignId = bidsTestCase.getCampaignId();
        this.offerBids = bidsTestCase.getOfferBids();
        this.requestData = bidsTestCase.getRequestData();
        this.caseName = bidsTestCase.getCaseName();
    }

    @Test
    public void testResponse() {
        tester.isResponseCorrect(campaignId, offerBids, requestData);
    }

    @Test
    public void testRecordingIntoStorage() {
        tester.areBidsPresentInStorage(campaignId, offerBids, requestData);
    }


    @Test
    public void testResponseV2() {
        tester.isResponseCorrect(campaignId, offerBids,
                requestData.withVersion(ApiVersion.V2));
    }

    @Test
    public void testResponseV2Json() {
        tester.isJsonResponseCorrect(campaignId, offerBids,
                requestData
                        .withFormat(Format.JSON)
                        .withVersion(ApiVersion.V2));
    }

    @Test
    public void testRecordingIntoStorageV2() {
        tester.areBidsPresentInStorage(campaignId, offerBids,
                requestData
                        .withVersion(ApiVersion.V2));
    }

    @Test
    public void testRecordingIntoStorageV2Json() {
        tester.areJsonBidsPresentInStorage(campaignId, offerBids,
                requestData
                        .withFormat(Format.JSON)
                        .withVersion(ApiVersion.V2));
    }

}

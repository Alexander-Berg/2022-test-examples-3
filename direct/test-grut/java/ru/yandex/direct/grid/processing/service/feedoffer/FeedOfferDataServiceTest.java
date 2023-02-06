package ru.yandex.direct.grid.processing.service.feedoffer;

import java.math.BigDecimal;
import java.util.List;

import Market.DataCamp.DataCampContractTypes;
import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.feed.service.FeedService;
import ru.yandex.direct.core.entity.feedoffer.service.FeedOfferService;
import ru.yandex.direct.core.entity.image.repository.ImageDataRepository;
import ru.yandex.direct.core.testing.info.BannerImageFormat;
import ru.yandex.direct.core.testing.info.UserInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.UserSteps;
import ru.yandex.direct.core.testing.steps.uac.GrutSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.configuration.GrutGridProcessingTest;
import ru.yandex.direct.grid.processing.model.feedoffer.GdAddOrUpdateFeedOffer;
import ru.yandex.direct.grid.processing.model.feedoffer.GdAddOrUpdateFeedOfferPayload;
import ru.yandex.direct.grid.processing.model.feedoffer.GdFeedOffer;
import ru.yandex.direct.grid.processing.model.feedoffer.GdFeedOfferAddItem;
import ru.yandex.direct.grid.processing.model.feedoffer.GdFeedOffersContext;
import ru.yandex.direct.grid.processing.model.feedoffer.GdRetailFeedOfferParams;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.grid.processing.service.feedoffer.converter.GdFeedOfferConverter.fromGdFeedOfferAddItem;
import static ru.yandex.direct.grid.processing.service.feedoffer.converter.GdFeedOfferConverter.toGdFeedOffer;

@GrutGridProcessingTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FeedOfferDataServiceTest {
    @Autowired
    private Steps steps;
    @Autowired
    private UserSteps userSteps;
    @Autowired
    private GrutSteps grutSteps;
    @Autowired
    private FeedOfferService feedOfferService;
    @Autowired
    private FeedService feedService;
    @Autowired
    private ImageDataRepository imageDataRepository;
    @Autowired
    private FeedOfferDataService feedOfferDataService;

    private UserInfo userInfo;
    private ClientId clientId;
    private BannerImageFormat image;

    @Before
    public void before() {
        userInfo = userSteps.createDefaultUser();
        clientId = userInfo.getClientId();
        var clientInfo = userInfo.getClientInfo();
        grutSteps.createClient(clientInfo);
        image = steps.bannerSteps().createRegularImageFormat(clientInfo);
    }

    @Test
    public void getFeedOffer_success() {
        GdFeedOfferAddItem offer = createDefaultOffer();
        GdAddOrUpdateFeedOffer input = new GdAddOrUpdateFeedOffer().withManualFeedOffers(List.of(offer));
        GdAddOrUpdateFeedOfferPayload payload = feedOfferDataService
                .addOrUpdateFeedOffers(clientId, userInfo.getChiefUid(), userInfo.getUid(), input);

        offer.withId(payload.getRowset().get(0).getId());
        List<GdFeedOffer> expected = List.of(toGdFeedOffer(fromGdFeedOfferAddItem(offer, clientId)));

        GdFeedOffersContext actual = feedOfferDataService.getFeedOffers(clientId);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(actual.getFeedId()).isNotNull();
            softly.assertThat(actual.getRowset()).isEqualTo(expected);
        });
    }

    @Test
    public void getFeedOffer_success_noOffers() {
        GdFeedOffersContext actual = feedOfferDataService.getFeedOffers(clientId);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(actual.getFeedId()).isNull();
            softly.assertThat(actual.getRowset()).isEmpty();
        });
    }

    @Test
    public void addFeedOffer_success() {
        GdFeedOfferAddItem offer = createDefaultOffer();
        GdAddOrUpdateFeedOffer input = new GdAddOrUpdateFeedOffer().withManualFeedOffers(List.of(offer));
        GdAddOrUpdateFeedOfferPayload payload = feedOfferDataService
                .addOrUpdateFeedOffers(clientId, userInfo.getChiefUid(), userInfo.getUid(), input);

        assertThat(payload.getValidationResult()).isNull();
    }

    @Test
    public void addFeedOffer_validationFailed() {
        GdFeedOfferAddItem offer = createDefaultOffer().withLabel("");
        GdAddOrUpdateFeedOffer input = new GdAddOrUpdateFeedOffer().withManualFeedOffers(List.of(offer));
        GdAddOrUpdateFeedOfferPayload payload = feedOfferDataService
                .addOrUpdateFeedOffers(clientId, userInfo.getChiefUid(), userInfo.getUid(), input);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(payload.getFeedId()).isNull();
            softly.assertThat(payload.getValidationResult()).isNotNull();
        });
    }

    @Test
    public void updateFeedOffer_sameManualFeed_success() {
        GdFeedOfferAddItem offer = createDefaultOffer();
        GdAddOrUpdateFeedOffer input = new GdAddOrUpdateFeedOffer().withManualFeedOffers(List.of(offer));
        GdAddOrUpdateFeedOfferPayload payload = feedOfferDataService
                .addOrUpdateFeedOffers(clientId, userInfo.getChiefUid(), userInfo.getUid(), input);
        Long feedId = payload.getFeedId();

        offer.setId(payload.getRowset().get(0).getId());
        input.setManualFeedOffers(List.of(offer));
        payload = feedOfferDataService
                .addOrUpdateFeedOffers(clientId, userInfo.getChiefUid(), userInfo.getUid(), input);

        assertThat(payload.getFeedId()).isEqualTo(feedId);
    }

    private GdFeedOfferAddItem createDefaultOffer() {
        return new GdFeedOfferAddItem()
                .withId(null)
                .withLabel("label")
                .withDescription("description")
                .withHref("http://example.com")
                .withImages(List.of(String.format("https://%s/get-%s/%s/%s/x450",
                        image.getAvatarHost(), image.getAvatarNamespace(), image.getMdsGroupId(), image.getImageHash())))
                .withCurrency(DataCampContractTypes.Currency.RUR)
                .withCurrentPrice(BigDecimal.valueOf(1000))
                .withIsAvailable(true)
                .withRetailParams(
                        new GdRetailFeedOfferParams()
                );
    }
}

package ru.yandex.direct.core.entity.feedoffer.validation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import Market.DataCamp.DataCampContractTypes.Currency;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.configuration.GrutCoreTest;
import ru.yandex.direct.core.entity.feedoffer.model.FeedOffer;
import ru.yandex.direct.core.entity.feedoffer.model.RetailFeedOfferParams;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.uac.GrutSteps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.testing.matchers.validation.Matchers;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.core.entity.banner.service.validation.defects.BannerDefects.restrictedCharsInField;
import static ru.yandex.direct.core.entity.feedoffer.validation.FeedOfferDefects.clientNotExistInGrut;
import static ru.yandex.direct.core.entity.feedoffer.validation.FeedOfferDefects.hrefNotUnique;
import static ru.yandex.direct.core.entity.feedoffer.validation.FeedOfferDefects.imageCountIsTooLarge;
import static ru.yandex.direct.core.entity.feedoffer.validation.FeedOfferDefects.noImages;
import static ru.yandex.direct.core.entity.feedoffer.validation.FeedOfferDefects.noOffers;
import static ru.yandex.direct.core.entity.feedoffer.validation.FeedOfferDefects.offerHasInvalidHref;
import static ru.yandex.direct.core.entity.feedoffer.validation.FeedOfferDefects.offerIdNotUnique;
import static ru.yandex.direct.core.entity.feedoffer.validation.FeedOfferDefects.oldPriceLessOrEqualThanCurrent;
import static ru.yandex.direct.core.entity.feedoffer.validation.FeedOfferDefects.unknownOfferId;
import static ru.yandex.direct.core.validation.assertj.ValidationResultConditions.error;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectDefinitionWith;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.defect.StringDefects.notEmptyString;

@GrutCoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FeedOfferValidationServiceTest {
    private static final Integer MAX_IMAGE_COUNT = 5;

    @Autowired
    private Steps steps;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GrutSteps grutSteps;
    @Autowired
    private FeedOfferValidationService feedOfferValidationService;

    private ClientInfo clientInfo;
    private Long clientId;
    private User operator;

    @Before
    public void before() {
        clientInfo = steps.clientSteps().createDefaultClient();
        clientId = clientInfo.getClientId().asLong();
        var shard = clientInfo.getShard();
        operator = userRepository.fetchByUids(shard, singletonList(clientInfo.getUid())).get(0);
        grutSteps.createClient(clientInfo);
    }

    @Test
    public void validate_success() {
        FeedOffer offer = createDefaultOffer();
        var vr = feedOfferValidationService
                .validate(clientInfo.getClientId(), clientInfo.getUid(), operator.getUid(), List.of(offer));

        MatcherAssert.assertThat(vr, Matchers.hasNoDefectsDefinitions());
    }

    @Test
    public void validate_noOffers() {
        var vr = feedOfferValidationService
                .validate(clientInfo.getClientId(), clientInfo.getUid(), operator.getUid(), List.of());

        assertThat(vr).has(error(noOffers()));
    }

    @Test
    public void validate_unknownOfferId() {
        FeedOffer offer = createDefaultOffer().withId(123L);
        var vr = feedOfferValidationService
                .validate(clientInfo.getClientId(), clientInfo.getUid(), operator.getUid(), List.of(offer));

        assertThat(vr).has(error(unknownOfferId()));
    }

    @Test
    public void validate_duplicateOfferId() {
        FeedOffer offer = createDefaultOffer().withId(123L);
        var vr = feedOfferValidationService
                .validate(clientInfo.getClientId(), clientInfo.getUid(), operator.getUid(), List.of(offer, offer));

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(validationError(offerIdNotUnique().defectId()))));
    }

    @Test
    public void validate_clientNotExistInGrut() {
        Long notExistingClientId = clientId + 1;
        FeedOffer offer = createDefaultOffer().withClientId(notExistingClientId);
        var vr = feedOfferValidationService
                .validate(ClientId.fromLong(notExistingClientId), clientInfo.getUid(), operator.getUid(),
                        List.of(offer));

        assertThat(vr).has(error(clientNotExistInGrut()));
    }

    @Test
    public void validate_offerWithoutLabel() {
        FeedOffer offer = createDefaultOffer().withLabel("");
        var vr = feedOfferValidationService
                .validate(clientInfo.getClientId(), clientInfo.getUid(), operator.getUid(), List.of(offer));

        assertThat(vr).has(error(notEmptyString()));
    }

    @Test
    public void validate_offerWithNotAllowedChars() {
        FeedOffer offer = createDefaultOffer().withDescription("|");
        var vr = feedOfferValidationService
                .validate(clientInfo.getClientId(), clientInfo.getUid(), operator.getUid(), List.of(offer));

        assertThat(vr).has(error(restrictedCharsInField()));
    }

    @Test
    public void validate_offerHasInvalidHref() {
        FeedOffer offer = createDefaultOffer().withHref("htt://example.com");
        var vr = feedOfferValidationService
                .validate(clientInfo.getClientId(), clientInfo.getUid(), operator.getUid(), List.of(offer));

        assertThat(vr).has(error(offerHasInvalidHref()));
    }

    @Test
    public void validate_hrefNotUnique() {
        FeedOffer offer1 = createDefaultOffer();
        FeedOffer offer2 = createDefaultOffer();
        var vr = feedOfferValidationService
                .validate(clientInfo.getClientId(), clientInfo.getUid(), operator.getUid(), List.of(offer1, offer2));

        assertThat(vr).is(matchedBy(hasDefectDefinitionWith(validationError(hrefNotUnique().defectId()))));
    }

    @Test
    public void validate_noImages() {
        FeedOffer offer = createDefaultOffer().withImages(List.of());
        var vr = feedOfferValidationService
                .validate(clientInfo.getClientId(), clientInfo.getUid(), operator.getUid(), List.of(offer));

        assertThat(vr).has(error(noImages()));
    }

    @Test
    public void validate_imageCountIsTooLarge() {
        var images = new ArrayList<String>();
        for (int i = 1; i <= MAX_IMAGE_COUNT + 1; ++i) {
            images.add("img" + i);
        }
        FeedOffer offer = createDefaultOffer().withImages(images);
        var vr = feedOfferValidationService
                .validate(clientInfo.getClientId(), clientInfo.getUid(), operator.getUid(), List.of(offer));

        assertThat(vr).has(error(imageCountIsTooLarge(MAX_IMAGE_COUNT)));
    }

    @Test
    public void validate_oldPriceLessThanCurrent() {
        FeedOffer offer = createDefaultOffer().withOldPrice(BigDecimal.valueOf(500));
        var vr = feedOfferValidationService
                .validate(clientInfo.getClientId(), clientInfo.getUid(), operator.getUid(), List.of(offer));

        assertThat(vr).has(error(oldPriceLessOrEqualThanCurrent(offer.getCurrentPrice())));
    }

    private FeedOffer createDefaultOffer() {
        return new FeedOffer()
                .withId(null)
                .withClientId(clientId)
                .withLabel("label")
                .withDescription("description")
                .withHref("http://example.com")
                .withImages(List.of("img"))
                .withCurrency(Currency.RUR)
                .withCurrentPrice(BigDecimal.valueOf(1000))
                .withIsAvailable(true)
                .withRetailFeedOfferParams(
                        new RetailFeedOfferParams()
                );
    }
}

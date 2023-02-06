package ru.yandex.direct.core.entity.feedoffer.service;

import java.math.BigDecimal;
import java.util.List;

import Market.DataCamp.DataCampContractTypes;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.configuration.GrutCoreTest;
import ru.yandex.direct.core.entity.feedoffer.converter.FeedOfferConverter;
import ru.yandex.direct.core.entity.feedoffer.model.FeedOffer;
import ru.yandex.direct.core.entity.feedoffer.model.RetailFeedOfferParams;
import ru.yandex.direct.core.entity.uac.grut.GrutContext;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.entity.user.repository.UserRepository;
import ru.yandex.direct.core.grut.api.GrutApiProperties;
import ru.yandex.direct.core.grut.api.OfferGrutApi;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.core.testing.steps.uac.GrutSteps;
import ru.yandex.direct.result.MassResult;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isFullySuccessful;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@GrutCoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FeedOfferServiceTest {

    @Autowired
    private Steps steps;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GrutSteps grutSteps;
    @Autowired
    private GrutContext grutContext;
    @Autowired
    private GrutApiProperties grutApiProperties;
    @Autowired
    private FeedOfferService feedOfferService;

    private OfferGrutApi offerGrutApi;

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

        offerGrutApi = new OfferGrutApi(grutContext, grutApiProperties);
    }

    @Test
    public void addFeedOffer_success() {
        FeedOffer offer = createDefaultOffer();
        MassResult<Long> result = feedOfferService.addOrUpdateFeedOffers(clientInfo.getClientId(), clientInfo.getUid(),
                operator.getUid(), List.of(offer));
        assumeThat(result, isFullySuccessful());

        List<FeedOffer> actual = mapList(offerGrutApi.selectOffersByClientId(clientId),
                FeedOfferConverter::fromFeedOfferGrut);
        offer.setId(actual.get(0).getId());
        offer.setUpdateTime(actual.get(0).getUpdateTime());

        assertThat(actual).isEqualTo(List.of(offer));
    }

    @Test
    public void updateFeedOffer_success() {
        FeedOffer offer = createDefaultOffer();
        feedOfferService.addOrUpdateFeedOffers(clientInfo.getClientId(), clientInfo.getUid(),
                operator.getUid(), List.of(offer));
        List<FeedOffer> addedOffers = mapList(offerGrutApi.selectOffersByClientId(clientId),
                FeedOfferConverter::fromFeedOfferGrut);

        offer.setId(addedOffers.get(0).getId());
        offer.setIsAvailable(false);
        MassResult<Long> result = feedOfferService.addOrUpdateFeedOffers(clientInfo.getClientId(), clientInfo.getUid(),
                operator.getUid(), List.of(offer));
        assumeThat(result, isFullySuccessful());

        List<FeedOffer> actual = mapList(offerGrutApi.selectOffersByClientId(clientId),
                FeedOfferConverter::fromFeedOfferGrut);
        offer.setUpdateTime(actual.get(0).getUpdateTime());

        assertThat(actual).isEqualTo(List.of(offer));
    }

    @Test
    public void addFeedOfferTwice_oneOffer_success() {
        FeedOffer offer = createDefaultOffer();
        feedOfferService.addOrUpdateFeedOffers(clientInfo.getClientId(), clientInfo.getUid(),
                operator.getUid(), List.of(offer));
        MassResult<Long> result = feedOfferService.addOrUpdateFeedOffers(clientInfo.getClientId(), clientInfo.getUid(),
                operator.getUid(), List.of(offer));
        assumeThat(result, isFullySuccessful());

        List<FeedOffer> actual = mapList(offerGrutApi.selectOffersByClientId(clientId),
                FeedOfferConverter::fromFeedOfferGrut);
        offer.setId(actual.get(0).getId());
        offer.setUpdateTime(actual.get(0).getUpdateTime());

        assertThat(actual).isEqualTo(List.of(offer));
    }

    private FeedOffer createDefaultOffer() {
        return new FeedOffer()
                .withId(null)
                .withClientId(clientId)
                .withLabel("label")
                .withDescription("description")
                .withHref("http://example.com")
                .withImages(List.of("img"))
                .withCurrency(DataCampContractTypes.Currency.RUR)
                .withCurrentPrice(BigDecimal.valueOf(1000))
                .withIsAvailable(true)
                .withRetailFeedOfferParams(
                        new RetailFeedOfferParams()
                );
    }
}

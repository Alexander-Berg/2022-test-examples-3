package ru.yandex.market.api.controller.v2.offer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.hamcrest.Matchers;
import org.junit.Test;

import ru.yandex.market.api.controller.v2.OffersControllerV2;
import ru.yandex.market.api.domain.OfferId;
import ru.yandex.market.api.domain.v2.OfferFieldV2;
import ru.yandex.market.api.domain.v2.OfferV2;
import ru.yandex.market.api.domain.v2.OfferV2ListResult;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.internal.common.GenericParams;
import ru.yandex.market.api.internal.common.PartnerInfo;
import ru.yandex.market.api.matchers.OfferMatcher;
import ru.yandex.market.api.server.context.ContextHolder;
import ru.yandex.market.api.server.sec.client.Client;
import ru.yandex.market.api.server.sec.client.internal.TestTariffs;
import ru.yandex.market.api.util.httpclient.clients.ReportTestClient;

import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Created by apershukov on 16.02.17.
 */
public class OffersControllerV2Test extends BaseTest {
    @Inject
    private ReportTestClient reportClient;

    @Inject
    private OffersControllerV2 offerController;

    @Test
    public void offerWithoutModelHasNoOffersLink() {
        OfferId offerId = new OfferId("1", null);
        reportClient.getOfferInfo(offerId, "offer-without-model_model-offers-link.json");

        OfferV2 offer = (OfferV2) offerController.getOffer(
            offerId,
            false,
            Collections.singleton(OfferFieldV2.OFFERS_LINK),
            genericParams
        ).waitResult().getOffer();

        assertThat(offer.getOffersLink(), isEmptyOrNullString());
    }

    @Test
    public void offerWithoutModelHasOffersLink() {
        OfferId offerId = new OfferId("1", null);
        reportClient.getOfferInfo(offerId, "offer-with-model_model-offers-link.json");

        ContextHolder.update(ctx ->
           ctx.setPartnerInfo(PartnerInfo.create("12", null, null, 7L, null))
        );

        OfferV2 offer = (OfferV2) offerController.getOffer(
            offerId,
            false,
            Collections.singleton(OfferFieldV2.OFFERS_LINK),
            genericParams
        ).waitResult().getOffer();

        assertEquals("https://market.yandex.ru/product--elephone-s1-8gb-dual-sim-chernyi/13/offers?pp=37&clid=12&distr_type=7&fesh=666", offer.getOffersLink());
    }

    @Test
    public void offerWithCheckoutLink() {
        OfferId offerId = new OfferId("1", null);
        reportClient.getOfferInfo(offerId, "offer-with-checkout-link.json");

        ContextHolder.update(ctx ->
                ctx.setPartnerInfo(PartnerInfo.create("12", null, null, 7L, null))
        );

        OfferV2 offer = (OfferV2) offerController.getOffer(
                offerId,
                false,
                Collections.singleton(OfferFieldV2.CHECKOUT_LINK),
                genericParams
        ).waitResult().getOffer();

        assertEquals("https://market-click2.yandex.ru/redir/338FT8NBgRvqwGz9uOZxgZnKqINkPHHY9DupcUn1FELKSLnpOhvVTaa4I2vixrGl0G4E0Pw08BtunRwNcoqSLnDoQ8-TIGwO4U3PADnYtoLwYX6WTw-PlH3ykaKMh3l4EYIFW3CskGnP3M9hnD9D7XLlEQ1sOZ5bGOlyv2WqCBirnyVvgGvyY6QVX7jYlO21af2xukptys6JACCQret_T9iK2-aymr3gIkUUsmcRL16kzA1cE-jeSx8JvFY67zoSR4f-seSKypQfdK__tbfFjNS6TfkXZagjsEaPCcDT4m_uCoDWoM0fq7xGKOkDM4aJJMjM0qvmqf8m7yBI5hwj_kJZ-KjUA8Kk7zm5e5CHCdDlARP9doCeuLnfsHasnUkGjFz5ZtjA1gCrfunaxggmfsEG7as3It9rA2koE1QMtf-s9Gm6bF5g4E_Y4KHOhe3tsHgzpDthr1uPUiW4LxpeiDXeN3yld52vcL6M33Pr6xLitqCqttgulYaAvQ-tyfy7KOZjWWJmiY6XAbFcItulIKF6pUPNRcyfRnC9-6XFd3CnSHfMaZWE8ged3kT6YyFWsMpWYx5o9UD_oirfIYQmeqKoHYp_8HLzABR6ZjMoBIiGcBG0AUnloRNKuDNRsVpb76KPKviQyBMKuv4G0KnyWPipy2kbxdbvb4AOjmumHUzUd8ekwYLoB-BfL2l0FNuuEVZsaOWkbwKBHIEYghKv2t6Ut0H-J0Vw4XYISTY1XO-toW6NZlqHZAiEY3H_JlfFiN3vR0pBoreN95i3j0DIuo-vPCFYcBQ_be7Tkox8BZE-hSOPmYjgV25C3E9Qb0vR3rdcFPtPRm5v9ttUgouwWhCDexhVXxuTX7koWJBe6DDNtZAIsjM6-i_ZkA0VVHxjZKeOlh6O-8lPzw7UuQaREpKXe-DoDQPbHzOWSSYY44zphcYJll1QO_ryO79WvlE9vwbEsnZC_fhm9dr4lESWEbuqNlOMxrKz7xyyxbI21WzagWp4qQhgcomGr5UboYjyYz6tV3syK2o,?data=QVyKqSPyGQwNvdoowNEPjTlzjvrlsHN5U6XZB8OGOYxMzHWtqUaP61zhfozjIm9hkhZpMp8_izQU6lFzU5MUfOJTjFGAEfL3hebhu47l8oMpuIt3OkjOKh-L5l77vKOn4zhVM--EW9z4NaQCYGo0wiigNaSXzv6KO7O1LnkkjrKEEHo3h9iQHC-533GlCy4_IyayzJqIpKiq0RWfNzOdb0SCvTaN1ZtV0TgQuWmLZvxHI6Oms2AoonEVrTWf_BXP_iwF6x-pMBv4FBoQ0ec_V5kvbZeHpgqeU8T2ILho2ibQiustMZnH1xGxG2qKuqEzMFkIc6f_3327Jx7cPW_pOpDBySrQKGu-AsMgqoJgMdumRNu63ZW6E1SxGHPHLNSz0CefPPwK5yvDVMuR5sMPveE3qrg0G3P9S5go0IrVKKAmxHkJTybbOSrhbe5_oioyba_8ZjHKh8d506M-nJuEx6c4PdGoWEfLsF1yyr4eHNTuEKZg-BbxmNwuoeDk0K17tGIrFyuD-QI,&b64e=1&sign=b304ac759f503638484b904238ae909f&keyno=1", offer.getBeruOrderLink());
    }

    @Test
    public void testGetOffersSorting() {
        OfferId[] offerIds = new OfferId[] {
            new OfferId("cbXRUnxaVye9cs5Kux5Amw", null),
            new OfferId("NxlzKMUuKiN2fUUqgP5VHw", null),
            new OfferId("yW8zH_g0L5uNzecxggINIw", null),
            new OfferId("ro8bfPX7LHAZzpBXxTM2Jw", null)
        };

        reportClient.getOffersV2(offerIds, "get-offers.json");

        List<String> offersWareMd5 = Arrays.stream(offerIds)
            .map(OfferId::getWareMd5)
            .collect(Collectors.toList());
        offersWareMd5.add("cbXRUnxaVye9cs5Kux5Amw"); // Test duplicate values

        OfferV2ListResult result = offerController.getOffers(offersWareMd5,
            false,
            Collections.emptyList(),
            GenericParams.DEFAULT).waitResult();

        assertThat(result.getOffers(),
            Matchers.contains(
                OfferMatcher.offer(
                    OfferMatcher.offerId(
                        OfferMatcher.wareMd5("cbXRUnxaVye9cs5Kux5Amw")
                    ),
                    OfferMatcher.name("Pav.Agata Natural Plaza")
                ),
                OfferMatcher.offer(
                    OfferMatcher.offerId(
                        OfferMatcher.wareMd5("NxlzKMUuKiN2fUUqgP5VHw")
                    ),
                    OfferMatcher.name("Смартфон Samsung Galaxy S8 SM-G950FD черный бриллиант")
                ),
                OfferMatcher.offer(
                    OfferMatcher.offerId(
                        OfferMatcher.wareMd5("yW8zH_g0L5uNzecxggINIw")
                    ),
                    OfferMatcher.name("Смартфон Samsung Galaxy S8 SM-G950FD мистический аметист")
                ),
                OfferMatcher.offer(
                    OfferMatcher.offerId(
                        OfferMatcher.wareMd5("ro8bfPX7LHAZzpBXxTM2Jw")
                    ),
                    OfferMatcher.name("Часы Casio")
                )
            )
        );
    }
}

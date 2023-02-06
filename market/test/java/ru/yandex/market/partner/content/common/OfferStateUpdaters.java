package ru.yandex.market.partner.content.common;

import java.util.Collections;
import java.util.List;

import ru.yandex.market.partner.content.common.utils.DcpPartnerPictureUtils;

/**
 * @author Nur-Magomed Dzhamiev <a href="mailto:n-mago@yandex-team.ru"></a>
 */
public class OfferStateUpdaters {

    private OfferStateUpdaters() {

    }

    public static void addStandardPictures(List<PreparedOfferState> states) {
        states.forEach(state -> {
            String url = DcpPartnerPictureUtils.testImgUrlForOffer(state.getDatacampOffer().getBusinessId(),
                state.getDatacampOffer().getOfferId());
            state.getDcpOfferBuilder().withPictures(Collections.singletonList(url));
        });
    }

}

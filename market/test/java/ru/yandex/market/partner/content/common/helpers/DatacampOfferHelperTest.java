package ru.yandex.market.partner.content.common.helpers;


import Market.DataCamp.DataCampOffer;
import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import ru.yandex.market.partner.content.common.db.jooq.tables.pojos.GcSkuTicket;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static ru.yandex.market.ir.autogeneration.common.util.DcpOfferUtils.createDcpOfferWithPics;

public class DatacampOfferHelperTest {

    @Test
    public void testCollectPicturesFromTicket() {
        // urls to mbo in offer will be without leading slashes, see DcpOfferFromPskuEnricher.updatePictures
        String mboAvatarUrl = "avatars.mds.yandex.net/get-mpic/1961245/img_id8835968184976353325.jpeg/orig";
        String supplierUrl = "https://kupitcveti.com/uploads/store/product/91661319a170fcc1c9e3bafb9a7a8a7f.jpg";
        String idxUrl = "//avatars.mds.yandex.net/get-marketpic/1570741/market_ahovdeUyPyE_CkY_EpzbXA/orig";
        DataCampOffer.Offer offer = createDcpOfferWithPics(Arrays.asList(mboAvatarUrl, supplierUrl),
            ImmutableMap.of(mboAvatarUrl, "someurl", supplierUrl, idxUrl));
        GcSkuTicket skuTicket = new GcSkuTicket();
        skuTicket.setDatacampOffer(offer);
        Map<String, Boolean> result = new HashMap<>();
        DatacampOfferHelper.collectPicturesFromTicket(skuTicket, (result::put));
        // mboAvatarUrl should be taken as is with //, but supplierUrl should be used to extract and return idxUrl
        ImmutableMap<String, Boolean> expected = ImmutableMap.of("//" + mboAvatarUrl, true, idxUrl, false);
        Assertions.assertThat(result).isEqualTo(expected);
    }
}

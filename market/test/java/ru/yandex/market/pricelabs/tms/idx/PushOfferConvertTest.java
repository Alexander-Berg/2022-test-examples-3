package ru.yandex.market.pricelabs.tms.idx;

import Market.DataCamp.API.ExportMessageOuterClass.ExportMessage;
import com.google.protobuf.TextFormat;
import org.junit.jupiter.api.Test;

import ru.yandex.market.pricelabs.misc.TimingUtils;
import ru.yandex.market.pricelabs.misc.Utils;
import ru.yandex.market.pricelabs.model.types.Status;
import ru.yandex.market.pricelabs.tms.processing.offers.OffersHolderPush;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static ru.yandex.market.pricelabs.tms.processing.TmsTestUtils.offer;

public class PushOfferConvertTest {

    @Test
    void testConvertOffer() throws Exception {
        var now = TimingUtils.getInstant();
        var expected = offer("hid.1562596", o -> {
            o.setShop_id(10781933);
            o.setFeed_id(200838639);
            o.setStatus(Status.ACTIVE);
            o.setCreated_at(now);
            o.setName("Накопительный электрический водонагреватель Gorenje OGB 30 N");
            o.setCategory_id(1519704234);
            o.setMarket_category_id(90575);
            o.setModel_id(1562596);
            o.setVendor_name("Gorenje");
            o.setPrice(350000);
            o.setOldprice(500050);
            o.setOffer_version(123);
        });

        var pushMessage = TextFormat.parse(
                Utils.readResource("tms/idx/datacamp-external-message.txt"), ExportMessage.class);
        var offerPush = OffersHolderPush.convertMessage(pushMessage);
        var actual = OffersHolderPush.convertOffer(offerPush, false, now, null);

        assertEquals(expected, actual);
    }
}

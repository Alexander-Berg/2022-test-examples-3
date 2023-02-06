package ru.yandex.market.marketpromo.core.data.serialization;

import java.util.List;
import java.util.stream.Collectors;

import Market.DataCamp.API.DatacampMessageOuterClass;
import Market.DataCamp.DataCampUnitedOffer;
import com.google.common.io.BaseEncoding;
import com.google.protobuf.Any;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.market.marketpromo.core.data.source.offerstorage.util.OfferDataConverter;
import ru.yandex.market.marketpromo.core.test.ServiceTestBase;
import ru.yandex.market.marketpromo.model.LocalOffer;

import static org.hamcrest.MatcherAssert.assertThat;
import static ru.yandex.market.marketpromo.core.test.generator.Offers.generateLocalOfferList;

public class OffersPublishingSerialisationTest extends ServiceTestBase {

    @Autowired
    private OfferDataConverter offerDataConverter;

    @Test
    void shouldSerializeOffers() {
        List<LocalOffer> localOffers = generateLocalOfferList(1000);
        Any message = Any.pack(DatacampMessageOuterClass.DatacampMessage.newBuilder()
                .addUnitedOffers(DataCampUnitedOffer.UnitedOffersBatch.newBuilder()
                        .addAllOffer(localOffers.stream()
                                .map(offerDataConverter::convertToDataCampOffer)
                                .collect(Collectors.toUnmodifiableSet())))
                .build());
        assertThat(BaseEncoding.base64().encode(message.getValue().toByteArray()).getBytes().length / 1024,
                Matchers.lessThan(2 * 1024));
    }

}

package ru.yandex.market.aliasmaker.cache.offers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.aliasmaker.offers.Offer;
import ru.yandex.market.ir.http.FormalizerParam;
import ru.yandex.market.ir.http.Matcher;
import ru.yandex.market.mbo.http.OffersStorage;
import ru.yandex.market.mbo.http.SkuBDApi;

/**
 * @author york
 * @since 09.10.2019
 */
public class OffersSerializerTest {
    private static final int CATEGORY_ID = 1;
    private int idSeq = 1;

    @Test
    public void testSer() {
        Map<String, OffersStorage.GenerationDataOffer> offersBeforeSerialization = new HashMap<>();
        Map<String, OffersStorage.GenerationDataOffer> offersAfterSerialization = new HashMap<>();
        Map<String, SkuBDApi.OfferInfo> offersInfoAfterSerialization = new HashMap<>();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Output output = new Output(outputStream);
        Kryo kryo = OfferSessionSerializer.initKryo();
        Offer offer1 = generate(offersBeforeSerialization);
        Offer offer2 = generate(offersBeforeSerialization);
        OfferSessionSerializer.serializeOffer(kryo, output, offer1);
        OfferSessionSerializer.serializeOffer(kryo, output, offer2);
        output.close();

        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        Input input = new Input(inputStream);
        kryo = OfferSessionSerializer.initKryo();
        List<Offer> after = new ArrayList<>();
        BiFunction<OffersStorage.GenerationDataOffer, SkuBDApi.OfferInfo, Offer> composer = (gdoffer, offerInfo) -> {
            offersAfterSerialization.put(gdoffer.getClassifierMagicId(), gdoffer);
            offersInfoAfterSerialization.put(gdoffer.getClassifierMagicId(), offerInfo);
            return OfferSessionSerializer.composeOffer(gdoffer, offerInfo);
        };

        after.add(OfferSessionSerializer.deserializeOffer(kryo, input, composer));
        after.add(OfferSessionSerializer.deserializeOffer(kryo, input, composer));

        for (Offer offer : after) {
            String offerId = offer.getClassifierMagicId();
            Assertions.assertThat(offer.getSkutcherOfferRequestBase())
                    .isEqualTo(offersInfoAfterSerialization.get(offerId));

            Assertions.assertThat(offersBeforeSerialization.get(offerId))
                    .isEqualTo(offersAfterSerialization.get(offerId));
        }
        Assertions.assertThat(after).extracting(Offer::getClassifierMagicId)
                .containsExactly(offer1.getClassifierMagicId(), offer2.getClassifierMagicId());
    }

    @SuppressWarnings("checkstyle:magicnumber")
    private Offer generate(Map<String, OffersStorage.GenerationDataOffer> offersBeforeSerialization) {
        int id = idSeq++;
        OffersStorage.GenerationDataOffer gen = OffersStorage.GenerationDataOffer.newBuilder()
                .setClassifierGoodId("good" + id)
                .setClassifierMagicId("magic" + id)
                .setVendorCode("vc" + id)
                .setBarcode("bc" + id)
                .setOffer("offer" + id)
                .setDescription("desc" + 1)
                .setCategoryId(CATEGORY_ID)
                .setDatasource("datasource" + id)
                .setPrice(1d + id)
                .setModel("model" + id)
                .setShopOfferId("shopOfferId" + id)
                .setShopId(10 + id)
                .setGlobalVendorId(id)
                .setLongClusterId(10000 + id)
                .setPicUrls("picUrls" + id)
                .setUrl("url" + id)
                .setOfferParams("offerParams" + id)
                .setSkuShop("skushop" + id)
                .setShopCategoryName("shopCatName" + id)
                .setMatchedType(Matcher.MatchType.values()[id % 16].getNumber())
                .build();

        offersBeforeSerialization.put(gen.getClassifierMagicId(), gen);

        return new Offer(
                gen,
                Arrays.asList(
                        generateFP(100 * id),
                        generateFP(200 * id)
                ),
                Arrays.asList(generateYP(100000 + id), generateYP(100001 + id)));
    }

    private FormalizerParam.FormalizedParamPosition generateFP(int id) {
        return FormalizerParam.FormalizedParamPosition.newBuilder()
                .setOptionId(id)
                .setParamId(id + 1)
                .setNumberValue(id + 2)
                .build();
    }

    private ru.yandex.market.ir.http.Offer.YmlParam generateYP(int id) {
        return ru.yandex.market.ir.http.Offer.YmlParam.newBuilder()
                .setName("name" + id)
                .setUnit("unit" + id)
                .setValue("value" + id)
                .build();
    }
}

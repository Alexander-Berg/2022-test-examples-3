package ru.yandex.market.markup2.utils;


import org.mockito.stubbing.Answer;
import ru.yandex.market.aliasmaker.AliasMaker;
import ru.yandex.market.ir.http.Formalizer;
import ru.yandex.market.ir.http.FormalizerParam;
import ru.yandex.market.markup2.utils.offer.Offer;
import ru.yandex.market.markup2.utils.offer.OfferStorageService;
import ru.yandex.market.markup2.utils.offer.YtOffersReader;
import ru.yandex.market.mbo.http.OffersStorage;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * @author anmalysh
 */
public class OfferTestUtils {

    private OfferTestUtils() {

    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public static OffersStorage.GenerationDataOffer createOffer(Integer categoryId, Long vendorId,
                                                                String offerId, String offer,
                                                                String goodId, Long modelId,
                                                                Long clusterId, String wareMd5,
                                                                String pictureUrl) {
        OffersStorage.GenerationDataOffer.Builder result = OffersStorage.GenerationDataOffer.newBuilder()
            .setClassifierMagicId(offerId)
            .setClassifierGoodId(goodId);

        if (categoryId != null) {
            result.setCategoryId(categoryId);
        }
        if (offer != null) {
            result.setOffer(offer);
        }
        if (vendorId != null) {
            result.setGlobalVendorId(vendorId.intValue());
        }
        if (modelId != null) {
            result.setModelId(modelId);
        }
        if (clusterId != null) {
            result.setClusterId(clusterId);
        }
        if (wareMd5 != null) {
            result.setWareMd5(wareMd5);
        }
        if (pictureUrl != null) {
            result.setPicUrls(pictureUrl);
        }

        return result.build();
    }

    @SuppressWarnings("checkstyle:ParameterNumber")
    public static OffersStorage.GenerationDataOffer createOffer(Integer categoryId,
                                                                String offerId,
                                                                String goodId,
                                                                String offer,
                                                                String picurl,
                                                                Long modelId,
                                                                Long clusterId,
                                                                String wareMd5) {
        return createOffer(categoryId, null, offerId, offer, goodId, modelId, clusterId, wareMd5, picurl);
    }

    public static OffersStorage.GenerationDataOffer createOffer(String offerId, String goodId, Long modelId,
                                                                Long clusterId, String wareMd5) {
        return createOffer(null, null, offerId, null, goodId, modelId, clusterId, wareMd5, null);
    }

    public static OffersStorage.GenerationDataOffer createOffer(Integer categoryId, Long vendorId,
                                                                String offerId, String goodId,
                                                                Long modelId, Long clusterId) {
        return createOffer(categoryId, vendorId, offerId, null, goodId, modelId, clusterId, null, null);
    }

    public static Formalizer.FormalizedOffer createFormalizedOffer(
            FormalizerParam.FormalizedParamPosition... positions) {
        return Formalizer.FormalizedOffer.newBuilder()
            .addAllPosition(Arrays.asList(positions))
            .build();
    }

    public static FormalizerParam.FormalizedParamPosition createParamPosition(
            int paramId, Integer valueId, Double nValue) {
        FormalizerParam.FormalizedParamPosition.Builder result = FormalizerParam.FormalizedParamPosition.newBuilder()
            .setParamId(paramId);
        if (valueId != null) {
            result.setValueId(valueId);
        }
        if (nValue != null) {
            result.setNumberValue(nValue);
        }
        return result.build();
    }

    public static AliasMaker.Offer createMatchedOffer(Long modelId, String offerId) {
        return AliasMaker.Offer.newBuilder()
            .setModelId(modelId)
            .setOfferId(offerId)
            .build();
    }

    public static Offer createOffer(String offerId,
                                    long modelId,
                                    long clusterId) {
        Offer.OfferBuilder offerBuilder = Offer.newBuilder();
        offerBuilder.setWareMd5(offerId);
        offerBuilder.setCategoryId(1);
        offerBuilder.setOfferId(offerId);
        offerBuilder.setDescription(String.valueOf(offerId));
        offerBuilder.setPicturesUrls(new String[]{"http://external-" + offerId});
        offerBuilder.setModelId(modelId);
        offerBuilder.setClusterId(clusterId);
        offerBuilder.setTitle(String.valueOf(offerId));
        offerBuilder.setYmlParams(new ru.yandex.market.ir.http.Offer.YmlParam[]{});
        return offerBuilder.build();
    }

    public static void mockOfferStorageService(OfferStorageService service,
                                               Collection<OffersStorage.GenerationDataOffer> offers) {
        when(service.getRawOffers(anyCollection())).thenAnswer(i -> {
            Set<String> offersIds = new HashSet<>(i.getArgument(0));
            return offers.stream()
                .filter(o -> offersIds.contains(o.getClassifierMagicId()))
                .collect(Collectors.toList());
        });
        when(service.getOffers(anyCollection())).thenCallRealMethod();
    }

    public static void mockOffersReader(YtOffersReader reader,
                                        Collection<OffersStorage.GenerationDataOffer> offers) {
        doAnswer((Answer<Void>) invocation -> {
            Consumer<Iterator<Offer>> callback = invocation.getArgument(0);

            Iterator<OffersStorage.GenerationDataOffer> it = offers.iterator();

            callback.accept(new Iterator<Offer>() {
                @Override
                public boolean hasNext() {
                    return it.hasNext();
                }

                @Override
                public Offer next() {
                    return new Offer(it.next());
                }
            });
            return null;
        })
            .when(reader).readOffers(any());
    }
}

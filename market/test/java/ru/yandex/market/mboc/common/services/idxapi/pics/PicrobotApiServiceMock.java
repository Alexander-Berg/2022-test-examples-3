package ru.yandex.market.mboc.common.services.idxapi.pics;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.idxapi.pics.dto.ImageSignatureCollection;
import ru.yandex.market.mboc.common.services.idxapi.pics.dto.ImageSignatureLayer;

public class PicrobotApiServiceMock implements PicrobotApiService {
    private final Map<String, Map<ImageSignatureLayer, String>> imageSignatureByPicUrl = new HashMap<>();

    @Override
    public ImageSignatureCollection getImageEmbeddings(List<String> picIds) {
        ImageSignatureCollection result = new ImageSignatureCollection(picIds.size());
        picIds.forEach(picId -> {
            var signatures = imageSignatureByPicUrl.get(picId);
            if (signatures != null) {
                result.putImageSignatures(picId, signatures);
            }
        });
        return result;
    }

    @Override
    public ImageSignatureCollection getImageEmbeddingsByOffers(Collection<Offer> offers) {
        List<String> picIds = offers.stream()
            .map(Offer::extractOfferContent)
            .map(content ->
                content.getSourcePicUrls() != null ? content.splitSourcePicUrls() : content.splitPicUrls()
            )
            .flatMap(Collection::stream)
            .collect(Collectors.toList());

        return getImageEmbeddings(picIds);
    }

    public void putSignature(String picUrl, Map<ImageSignatureLayer, String> value) {
        imageSignatureByPicUrl.put(picUrl, value);
    }
}

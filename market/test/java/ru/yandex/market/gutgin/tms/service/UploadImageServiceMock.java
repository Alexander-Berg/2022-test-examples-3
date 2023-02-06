package ru.yandex.market.gutgin.tms.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import ru.yandex.market.mbo.http.ModelStorage;
import ru.yandex.market.partner.content.common.message.Messages;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author s-ermakov
 */
public class UploadImageServiceMock implements UploadImageService {
    private final ArrayListMultimap<String, String> externalUrlToInnerUrlMap = ArrayListMultimap.create();

    @Override
    public Map<String, Response> uploadImages(
        int sourceId, List<PictureData> pictures, Boolean ignoreWhiteBackgroundCheck
    ) {
        return pictures.stream()
            .collect(Collectors.groupingBy(PictureData::getUrl)).entrySet().stream()
            .map(e -> {
                String url = e.getKey();
                List<PictureData> values = e.getValue();
                String innerUrl;

                try {
                    innerUrl = nextInnerUrl(url);
                } catch (IllegalArgumentException ex) {
                    return Response.invalidData(url, Messages.get().pictureInvalid(
                        url,
                        false,
                        true,
                        false,
                        false,
                        values.stream()
                            .map(PictureData::collectShopSKUWhenPicturePresent)
                            .flatMap(List::stream).toArray(String[]::new)
                    ));
                }

                if (innerUrl == null) {
                    return Response.success(url, null);
                } else {
                    return Response.success(url, pic(innerUrl, url));
                }
            })
            .collect(Collectors.toMap(Response::getUrl, Function.identity()));
    }

    public void putMapping(@Nullable String innerUrl, String externalUrl) {
        Preconditions.checkArgument(externalUrl.startsWith("http://") || externalUrl.startsWith("https://"),
            "External url should start with http:// or https://. Actual: " + externalUrl);
        Preconditions.checkArgument(innerUrl == null ||
                !(innerUrl.startsWith("http://") || innerUrl.startsWith("https://")),
            "Inner url should be null or starts with http:// or https://. Actual: " + innerUrl);

        String existentExternalUrl = getExternalUrl(innerUrl);
        if (existentExternalUrl != null) {
            throw new IllegalArgumentException("Inner url '" + innerUrl + "' already exists in store " +
                "by external url '" + existentExternalUrl + "'");
        }

        externalUrlToInnerUrlMap.put(externalUrl, innerUrl);
    }

    @Nullable
    private String nextInnerUrl(String externalUrl) {
        List<String> values = externalUrlToInnerUrlMap.get(externalUrl);
        if (values.isEmpty()) {
            throw new IllegalArgumentException("Empty list");
        } else {
            // remove returned value
            // in order to simulate multi upload of equal pictures
            String value = values.get(0);
            values.remove(0);
            return value;
        }
    }

    private String getExternalUrl(String innerUrl) {
        return externalUrlToInnerUrlMap.asMap().entrySet().stream()
            .filter(entry -> entry.getValue().contains(innerUrl))
            .map(Map.Entry::getKey)
            .findFirst().orElse(null);
    }

    private static ModelStorage.Picture pic(String innerUrl, String url) {
        return ModelStorage.Picture.newBuilder()
            .setUrl(innerUrl)
            .setUrlSource(url)
            .build();
    }
}

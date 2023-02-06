package ru.yandex.market.markup2.utils.cards;

import java.util.List;
import java.util.Optional;

public class ClusterImageFinderMock extends ClusterImageFinder {
    public static final String GOOD_IMAGE_URL = "http://good";

    public ClusterImageFinderMock() {
        super(null);
    }

    @Override
    public Optional<String> findSuitableImageForCluster(List<String> pictureUrls) {
        for (String url: pictureUrls) {
            if (url.equals(GOOD_IMAGE_URL)) {
                return Optional.of(url);
            }
        }

        return Optional.empty();
    }
}

package ru.yandex.market.logistics.tarifficator.mds;

import java.net.MalformedURLException;
import java.net.URL;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.jupiter.api.Assertions;

import ru.yandex.market.common.mds.s3.client.model.ResourceLocation;

@ParametersAreNonnullByDefault
public final class MdsFactory {

    private static final String BUCKET_NAME = "tarifficator";
    private static final String URL_PREFIX = "http://localhost:8080/";

    private MdsFactory() {
        throw new UnsupportedOperationException();
    }

    public static ResourceLocation buildDatasetLocation(long priceListId) {
        return ResourceLocation.create(BUCKET_NAME, buildDatasetFilename(priceListId));
    }

    public static String buildDatasetFilename(long priceListId) {
        return String.format("delivery_calculator_dataset_document_%s.xml", priceListId);
    }

    public static URL buildDatasetUrl(long priceListId) {
        try {
            return new URL(URL_PREFIX + buildDatasetFilename(priceListId));
        } catch (MalformedURLException e) {
            Assertions.fail(e.getMessage());
        }
        return null;
    }
}

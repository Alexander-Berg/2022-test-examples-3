package ru.yandex.market.fulfillment.wrap.marschroute.service.geo;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ru.yandex.market.fulfillment.wrap.core.exception.FulfillmentWrapException;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.cache.GeoFileCacheKey;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.model.GeoFile;
import ru.yandex.market.fulfillment.wrap.marschroute.service.geo.model.GeoInformation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GeoInformationProviderTest {

    private static final String EXPECTED_KLADR = "11111111111";

    @Mock
    private LoadingCache<GeoFileCacheKey, GeoFile> cache;

    @InjectMocks
    private GeoInformationProvider geoInformationProvider;


    /**
     * Нашли КЛАДР сразу в искомом geoId.
     */
    @Test
    void findKladrDirectlyFromGeoId() throws Exception {
        GeoFile geoFile = new GeoFile(
            ImmutableMap.of(
                1L, new GeoInformation(1L, "", EXPECTED_KLADR, "locality", 0L),
                213L, new GeoInformation(213L, "", EXPECTED_KLADR, "locality", 1L)
            )
        );

        assertValidKladrReturned(geoFile, 213L, EXPECTED_KLADR, 1);
    }

    /**
     * Нашли КЛАДР сразу в родительском geoId.
     */
    @Test
    void findKladrFromDirectParent() throws Exception {
        GeoFile geoFile = new GeoFile(
            ImmutableMap.of(
                1L, new GeoInformation(1L, "", EXPECTED_KLADR, "locality", 0L),
                213L, new GeoInformation(213L, "", null, "locality", 1L)
            )
        );

        assertValidKladrReturned(geoFile, 213L, EXPECTED_KLADR, 2);
    }

    /**
     * Нашли КЛАДР у предка (в данном случае через 1).
     */
    @Test
    void findKladrFromAncestor() throws Exception {
        GeoFile geoFile = new GeoFile(
            ImmutableMap.of(
                0L, new GeoInformation(0L, "", EXPECTED_KLADR, "locality", null),
                1L, new GeoInformation(1L, "", null, "locality", 0L),
                213L, new GeoInformation(213L, "", null, "locality", 1L)
            )
        );

        assertValidKladrReturned(geoFile, 213L, EXPECTED_KLADR, 3);
    }


    /**
     * Нету родителей и КЛАДР'а.
     */
    @Test
    void foundNoKladrAndNoParent() throws Exception {

        GeoFile geoFile = new GeoFile(
            ImmutableMap.of(
                213L, new GeoInformation(213L, "", null, "locality", 1L)
            )
        );

        assertThat(callFindKladr(213L, geoFile, 2))
            .as("GeoInfo without kladr and without parent is empty")
            .isEmpty();
    }


    /**
     * У нужного geoId нету КЛАДР как и у родительского geoId.
     */
    @Test
    void bothChildAndParentHaveNoKladr() throws Exception {

        GeoFile geoFile = new GeoFile(
            ImmutableMap.of(
                1L, new GeoInformation(1L, "", null, "locality", 0L),
                213L, new GeoInformation(213L, "", null, "locality", 1L)
            )
        );

        assertThat(callFindKladr(213L, geoFile, 3))
            .as("GeoInfo without kladr and with parent which has no kladr  must be  empty")
            .isEmpty();
    }

    @Test
    void getShortKladrTestMakeShorterKladr() {
        GeoInformation geoInformation = new GeoInformation(1L, "", "0123456789012345", "", 2L);
        assertThat(geoInformation.getShortKladr()).isPresent();
        assertThat(geoInformation.getShortKladr().get()).isEqualToIgnoringCase("01234567890");
    }

    @Test
    void getShortMarschrouteKladr() {
        GeoInformation geoInformation = new GeoInformation(1L, "", "0123456789012345", "789789789789789", null, "", 2L);
        assertThat(geoInformation.getShortKladr()).isPresent();
        assertThat(geoInformation.getShortKladr().get()).isEqualToIgnoringCase("78978978978");
    }

    @Test
    void getShortKladrWhenMarschrouteKladrIsEmptyString() {
        GeoInformation geoInformation = new GeoInformation(1L, "", "0123456789012345", "", null, "", 2L);
        assertThat(geoInformation.getShortKladr()).isPresent();
        assertThat(geoInformation.getShortKladr().get()).isEqualToIgnoringCase("01234567890");
    }

    @Test
    void provideUnsafeFailOnAbsentGeoFile() throws ExecutionException {
        given(cache.get(GeoFileCacheKey.GEO_FILE)).willReturn(null);
        assertThatThrownBy(() -> geoInformationProvider.findWithKladr(1L))
            .isInstanceOf(FulfillmentWrapException.class);
    }

    private void assertValidKladrReturned(GeoFile geoFile, Long geoId, String expectedKladr, int timesCalled) throws Exception {
        given(cache.get(GeoFileCacheKey.GEO_FILE)).willReturn(geoFile);

        String actual = geoInformationProvider.findWithKladr(geoId).map(GeoInformation::getKladrId).orElse("");

        assertThat(actual).as("Asserting kladr values match").isEqualTo(expectedKladr);

        verify(cache, times(timesCalled)).get(GeoFileCacheKey.GEO_FILE);
    }

    private Optional<GeoInformation> callFindKladr(Long geoId, GeoFile geoFile, int timesCalled) throws Exception {
        given(cache.get(GeoFileCacheKey.GEO_FILE)).willReturn(geoFile);

        try {
            return geoInformationProvider.findWithKladr(geoId);
        } finally {
            verify(cache, times(timesCalled)).get(GeoFileCacheKey.GEO_FILE);
        }
    }
}

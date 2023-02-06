package ru.yandex.direct.core.testing.stub;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.asynchttp.ParallelFetcherFactory;
import ru.yandex.direct.geosearch.GeosearchClient;
import ru.yandex.direct.geosearch.GeosearchClientSettings;
import ru.yandex.direct.geosearch.model.Address;
import ru.yandex.direct.geosearch.model.GeoObject;
import ru.yandex.direct.geosearch.model.Kind;
import ru.yandex.direct.geosearch.model.Precision;
import ru.yandex.direct.tvm.TvmIntegration;
import ru.yandex.direct.tvm.TvmService;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.mockito.Mockito.mock;

@ParametersAreNonnullByDefault
public class GeosearchClientStub extends GeosearchClient {

    private List<GeoObject> getGeoObjectsForAddressResult = singletonList(new GeoObject.Builder()
            .withX(BigDecimal.valueOf(10.001)).withY(BigDecimal.valueOf(15.208))
            .withX1(BigDecimal.valueOf(10.002)).withY1(BigDecimal.valueOf(15.206))
            .withX2(BigDecimal.valueOf(10.003)).withY2(BigDecimal.valueOf(15.207))
            .withPrecision(Precision.EXACT)
            .withKind(Kind.HOUSE)
            .build());

    private Map<CoordKindKey, GeoObject> getMostExactGeoObjectOfKindResults = new HashMap<>();

    public GeosearchClientStub() {
        super(new GeosearchClientSettings("http://addrs-testing.search.yandex.net/search/stable", null, 3, Duration.ofSeconds(1), 1, 1),
                TvmService.DUMMY, mock(ParallelFetcherFactory.class), mock(TvmIntegration.class));
    }

    @Override
    public List<GeoObject> searchAddress(@Nonnull Address address) {
        return getGeoObjectsForAddressResult;
    }

    @Override
    public Optional<GeoObject> getMostExactGeoObjectOfKind(String coordinates, Kind kind) {
        return Optional.ofNullable(getMostExactGeoObjectOfKindResults.get(new CoordKindKey(coordinates, kind)));
    }

    @Override
    public Map<Long, Set<Long>> getMergedPermalinks(Collection<Long> permalinks) {
        return emptyMap();
    }

    public GeosearchClientStub putResultForGetMostExactGeoObjectOfKind(String coordinates, Kind kind,
                                                                       GeoObject getMostExactGeoObjectOfKindResult) {
        this.getMostExactGeoObjectOfKindResults.put(new CoordKindKey(coordinates, kind),
                getMostExactGeoObjectOfKindResult);
        return this;
    }

    public GeosearchClientStub clearResultsForGetMostExactGeoObjectOfKind() {
        this.getMostExactGeoObjectOfKindResults.clear();
        return this;
    }

    private static class CoordKindKey {
        private final String coords;
        private final Kind kind;

        public CoordKindKey(String coords, Kind kind) {
            this.coords = coords;
            this.kind = kind;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CoordKindKey that = (CoordKindKey) o;
            return Objects.equals(coords, that.coords) &&
                    kind == that.kind;
        }

        @Override
        public int hashCode() {
            return Objects.hash(coords, kind);
        }
    }
}

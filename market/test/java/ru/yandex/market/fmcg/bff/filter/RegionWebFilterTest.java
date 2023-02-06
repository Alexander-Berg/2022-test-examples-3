package ru.yandex.market.fmcg.bff.filter;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.Test;

import ru.yandex.market.fmcg.bff.region.RegionService;
import ru.yandex.market.fmcg.bff.region.model.Coords;
import ru.yandex.market.fmcg.bff.region.model.GeobaseRegion;
import ru.yandex.market.fmcg.bff.util.Const;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static ru.yandex.market.fmcg.bff.test.TestUtil.requestWithHeadersMock;

/**
 * @author valter
 */
public class RegionWebFilterTest {
    @Test
    public void doFilterSuggestRegionByIp() throws Exception {
        GeobaseRegion region = new GeobaseRegion(1, "my_reg", new Coords(213.435, 984.077),
            "Europe/Moscow", 11
        );
        String ip = "213.345.12.124";
        RegionWebFilter regionWebFilter = new RegionWebFilter(regionByIpGeobase(region, ip));
        checkHeader(regionWebFilter, requestWithHeadersMock(Collections.singletonMap(Const.IP_HEADER, ip)),
            Const.USER_REGION_SUGGEST_ZOOM, "11");
        checkHeader(regionWebFilter, requestWithHeadersMock(Collections.singletonMap(Const.IP_HEADER, ip)),
            Const.USER_REGION_SUGGEST_HEADER, "1");
        checkHeader(regionWebFilter, requestWithHeadersMock(Collections.singletonMap(Const.IP_HEADER, ip)),
            Const.USER_REGION_SUGGEST_COORDS, "213.435;984.077");
        checkHeader(regionWebFilter, requestWithHeadersMock(Collections.singletonMap(Const.IP_HEADER, ip)),
            Const.USER_REGION_SUGGEST_NAME,
            Base64.getEncoder().encodeToString("my_reg".getBytes(StandardCharsets.UTF_8)));
    }

    private void checkHeader(RegionWebFilter regionWebFilter, HttpServletRequest request, String name, String value)
        throws Exception {
        HttpServletResponse responseMock = mock(HttpServletResponse.class);
        regionWebFilter.doFilter(
            request,
            responseMock,
            mock(FilterChain.class)
        );
        verify(responseMock, atLeastOnce()).addHeader(eq(name), eq(value));
    }

    private RegionService regionByIpGeobase(GeobaseRegion region, String expectedIp) {
        return new RegionService() {
            @Override
            public Optional<GeobaseRegion> getFmcgRegion(Coords coords) {
                return Optional.empty();
            }

            @Override
            public Optional<GeobaseRegion> getFmcgRegion(String ip) {
                return Objects.equals(expectedIp, ip) ? Optional.of(region) : Optional.empty();
            }

            @Override
            public Optional<GeobaseRegion> getFmcgRegion(int regionId) {
                return Optional.empty();
            }
        };
    }
}

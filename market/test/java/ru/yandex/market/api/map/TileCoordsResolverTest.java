package ru.yandex.market.api.map;

import org.junit.Test;
import ru.yandex.common.util.collections.Maybe;
import ru.yandex.market.api.common.Result;
import ru.yandex.market.api.domain.v2.GeoCoordinatesV2;
import ru.yandex.market.api.integration.UnitTestBase;
import ru.yandex.market.api.util.parser2.resolver.errors.ResolverError;

import static org.junit.Assert.*;

/**
 * @author Kirill Sulim sulim@yandex-team.ru
 */
public class TileCoordsResolverTest extends UnitTestBase {

    private TileCoordsResolver resolver = new TileCoordsResolver();

    @Test
    public void shouldResolveCoordinates() throws Exception {
        Result<Maybe<TileCoords>, ResolverError> tileResult = resolver.apply("-1.1,2.;3,-4");

        assertTrue(tileResult.isOk());

        TileCoords value = tileResult.getValue().getValue();
        assertEquals(new GeoCoordinatesV2(2.0, -1.1), value.getLbCoords());
        assertEquals(new GeoCoordinatesV2(-4.0, 3.0), value.getRtCoords());
    }

    @Test
    public void shouldReturnErrorForRangeViolation() throws Exception {
        Result<Maybe<TileCoords>, ResolverError> tileResult = resolver.apply("181,2;3,4");

        assertFalse(tileResult.isOk());
    }

    @Test
    public void shouldReturnerrorOnWrongFormat() throws Exception {
        assertNotNull(resolver.apply("123").getError());
        assertNotNull(resolver.apply("123,456").getError());
        assertNotNull(resolver.apply("123;456").getError());
        assertNotNull(resolver.apply("1.1.1,2.2.2;3.3.3,4.4.4").getError());
        assertNotNull(resolver.apply("1.a,2.b;3.b,4.v").getError()
        );
    }
}

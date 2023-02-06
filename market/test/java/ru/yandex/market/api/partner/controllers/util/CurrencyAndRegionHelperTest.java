package ru.yandex.market.api.partner.controllers.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;

import ru.yandex.common.util.collections.Pair;
import ru.yandex.common.util.currency.Bank;
import ru.yandex.common.util.currency.Currency;
import ru.yandex.market.api.partner.controllers.region.model.Region;
import ru.yandex.market.core.geobase.RegionService;
import ru.yandex.market.core.geobase.model.RegionConstants;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.market.api.partner.controllers.util.RegionTestFixtures.FRANCE;
import static ru.yandex.market.api.partner.controllers.util.RegionTestFixtures.RUSSIA;
import static ru.yandex.market.api.partner.controllers.util.RegionTestFixtures.US;

/**
 * Created with IntelliJ IDEA.
 * User: snoop
 * Date: 10.03.16
 * Time: 19:38
 */
public class CurrencyAndRegionHelperTest {

    @Test
    public void testFindRegions() {
        List<RegionTestFixtures.Path> paths = Arrays.asList(FRANCE, US, RUSSIA);

        List<List<ru.yandex.market.core.geobase.model.Region>> regions =
                paths.stream().map(p -> p.regions).collect(Collectors.toList());

        CurrencyAndRegionHelper helper = new CurrencyAndRegionHelper();
        RegionService regionService = mock(RegionService.class);
        helper.setRegionService(regionService);

        when(regionService.findRegions(RegionTestFixtures.PARIS, 10)).thenReturn(regions);

        final List<Region> regionList =
                helper.findRegions(RegionTestFixtures.PARIS, 10);
        assertSame(paths.size(), regionList.size());

        for (int i = 0; i < regions.size(); i++) {
            final List<Region> actualRegionList = Stream.iterate(regionList.get(i), r -> r != null ? r.getParent() : null).
                    limit(regions.get(i).size()).filter(Objects::nonNull).collect(Collectors.toList());
            assertEquals(paths.get(i).location, paths.get(i).ids,
                    actualRegionList.stream().map(Region::getId).collect(Collectors.toList()));
            assertEquals(paths.get(i).location, paths.get(i).names,
                    actualRegionList.stream().map(Region::getName).collect(Collectors.toList()));
        }
    }

    @Test
    public void testDetectCurrencyByRegion_belarus_new_currency() {
        final Currency expectedCurrency = Currency.BYN;
        final Map<Long, Pair<Currency, Bank>> regionCurrencies =
                Collections.singletonMap(RegionConstants.BELARUS, Pair.of(expectedCurrency, Bank.NBRB));
        RegionService regionService = mock(RegionService.class);
        when(regionService.getRegionCurrencies(any())).thenReturn(regionCurrencies);

        CurrencyAndRegionHelper helper = new CurrencyAndRegionHelper();
        helper.setRegionService(regionService);

        final Currency actualCurrency = helper.detectCurrencyByRegion(RegionConstants.BELARUS);
        assertSame("Invalid currency detected for Belarus region", expectedCurrency, actualCurrency);
    }
}
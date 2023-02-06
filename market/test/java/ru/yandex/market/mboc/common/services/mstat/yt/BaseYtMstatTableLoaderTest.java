package ru.yandex.market.mboc.common.services.mstat.yt;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mboc.common.offers.model.Offer;
import ru.yandex.market.mboc.common.services.mstat.DiffState;
import ru.yandex.market.mboc.common.services.mstat.MstatOfferState;

/**
 * @author apluhin
 * @created 2/20/21
 */
public class BaseYtMstatTableLoaderTest {


    BaseYtMstatTableLoader loader;

    @Before
    public void setUp() throws Exception {
        loader = new StubLoader(null, null, null);
    }

    @Test
    public void testExtractRemovedKeys() {
        MstatOfferState before1 = MstatOfferState.builder()
            .offerId(1L)
            .serviceOffers(Arrays.asList(new Offer.ServiceOffer(1), new Offer.ServiceOffer(2)))
            .build();
        MstatOfferState before2 = MstatOfferState.builder()
            .offerId(2L)
            .serviceOffers(Arrays.asList(new Offer.ServiceOffer(1), new Offer.ServiceOffer(2)))
            .build();
        DiffState diffState1 = new DiffState(before1, null);
        //unchanged
        DiffState diffState2 = new DiffState(before2, before2);

        List<Map<String, Object>> maps = loader.extractDeletedKeys(Arrays.asList(diffState1, diffState2));
        Assertions.assertThat(maps).isEqualTo(List.of(
            Map.of(
                "key1", 1L,
                "key2", 1
            ),
            Map.of(
                "key1", 1L,
                "key2", 2
            )
        ));

    }

    @Test
    public void testExtractPartlyRemovedKeys() {
        MstatOfferState before1 = MstatOfferState.builder()
            .offerId(1L)
            .serviceOffers(Arrays.asList(new Offer.ServiceOffer(1), new Offer.ServiceOffer(2)))
            .build();
        MstatOfferState before2 = MstatOfferState.builder()
            .offerId(2L)
            .serviceOffers(Arrays.asList(new Offer.ServiceOffer(1), new Offer.ServiceOffer(2)))
            .build();
        MstatOfferState after1 = MstatOfferState.builder()
            .offerId(1L)
            .serviceOffers(Arrays.asList(new Offer.ServiceOffer(1)))
            .build();
        DiffState diffState1 = new DiffState(before1, after1);
        //unchanged
        DiffState diffState2 = new DiffState(before2, before2);

        List<Map<String, Object>> maps = loader.extractDeletedKeys(Arrays.asList(diffState1, diffState2));
        Assertions.assertThat(maps).isEqualTo(List.of(
            Map.of(
                "key1", 1L,
                "key2", 2
            )
        ));
    }

    @Test
    public void testIgnoreAddedKey() {
        MstatOfferState after1 = MstatOfferState.builder()
            .offerId(1L)
            .serviceOffers(Arrays.asList(new Offer.ServiceOffer(1)))
            .build();
        DiffState diffState1 = new DiffState(null, after1);

        List<Map<String, Object>> maps = loader.extractDeletedKeys(Arrays.asList(diffState1));
        Assertions.assertThat(maps).isEmpty();
    }

    @Test
    public void testExtractBaseName() {
        String baseName = BaseYtMstatTableLoader.convertToBaseName("//some/path/table_name/end");
        Assertions.assertThat(baseName).isEqualTo("table_name");
    }
}

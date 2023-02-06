package ru.yandex.market.common.report.indexer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.common.report.indexer.model.OfferDetails;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

/**
 * @author artemmz
 * created on 06.03.17.
 */
public class IdxApiServiceTest {
    private String content;

    @Before
    public void init() throws IOException {
        content = IOUtils.toString(this.getClass().getResourceAsStream("/files/feed/feed-dispatcher.xml"));
    }

    @Test
    public void testFindOffer() {
        IdxApiService idxApiService = spy(new IdxApiService());
        doReturn(content).when(idxApiService).getContent(anyString());

        Map<String, Boolean> offerIds = new HashMap<String, Boolean>() {{
            put(null, false);
            put("", false);
            put("-1", false);
            put("-2", true);
            put("test", true);
        }};
        Map<String, Boolean> wareMd5s = new HashMap<String, Boolean>() {{
            put(null, false);
            put("", false);
            put("test", true);
        }};

        offerIds.forEach((offerId, notNullForOffer) ->
                wareMd5s.forEach((wareMd5, notNullForWare) -> {
                    OfferDetails offer = idxApiService.findOffer(0L, offerId, wareMd5);
                    assertEquals(
                            String.format("Wrong result = %s for offerId = %s and wareMd5 = %s", offer, offerId,
                                    wareMd5),
                            offer != null,
                            notNullForOffer || notNullForWare
                    );

                }));
    }
}

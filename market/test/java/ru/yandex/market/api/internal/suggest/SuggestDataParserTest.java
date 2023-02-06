package ru.yandex.market.api.internal.suggest;

import junit.framework.Assert;
import org.junit.Test;
import ru.yandex.market.api.domain.v2.suggest.SuggestData;
import ru.yandex.market.api.integration.BaseTest;
import ru.yandex.market.api.util.ResourceHelpers;

/**
 * Created by fettsery on 27.06.19.
 */
public class SuggestDataParserTest extends BaseTest {

    @Test
    public void checkParse() {
        SuggestData data = new SuggestDataParser().parse(ResourceHelpers.getResource("suggest-data.json"));
        Assert.assertEquals("model", data.getType());
        Assert.assertEquals("https://avatars.mds.yandex.net/get-mpic/1521939/img_id5065720375753535966.jpeg/orig&size=2", data.getLogo());
    }
}

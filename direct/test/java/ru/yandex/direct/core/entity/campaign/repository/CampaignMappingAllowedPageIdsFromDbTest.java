package ru.yandex.direct.core.entity.campaign.repository;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

/**
 * Created by hmepas on 01.10.2018
 */

@RunWith(Parameterized.class)
public class CampaignMappingAllowedPageIdsFromDbTest {

    @Parameterized.Parameter
    public String json;

    @Parameterized.Parameter(1)
    public List<Long> expected;

    @Parameterized.Parameters(name = "json: {0}")
    public static Collection params() {
        return Arrays.asList(new Object[][]{
                {"", Collections.emptyList()},
                {"[]", Collections.emptyList()},
                {null, Collections.emptyList()},
                {"[-123, 1235, 456, 0]", Arrays.asList(-123L, 1235L, 456L, 0L)},
                {"[\"1235\", 456]", Arrays.asList(1235L, 456L)},
        });
    }

    @Test
    public void test() {
        assertEquals(CampaignMappings.allowedPageIdsFromDb(json), expected);
    }
}

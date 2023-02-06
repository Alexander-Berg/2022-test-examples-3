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
public class CampaignMappingAllowedPageIdsToDbTest {
    @Parameterized.Parameter
    public List<Long> request;

    @Parameterized.Parameter(1)
    public String expectedJson;

    @Parameterized.Parameters(name = "json: {0}")
    public static Collection params() {
        return Arrays.asList(new Object[][]{
                {null, null},
                {Collections.emptyList(), null},
                {Arrays.asList(-123L, 1235L, null, 0L), "[-123,1235,null,0]"},
                {Arrays.asList(-123L, 1235L, 456L, 0L), "[-123,1235,456,0]"},
                {Arrays.asList(-123L, 1235L, 12345L, 12345L, 456L, 12345L, 0L, 12345L), "[-123,1235,12345,456,0]"}
        });
    }

    @Test
    public void test() {
        assertEquals(CampaignMappings.pageIdsToDb(request), expectedJson);
    }
}

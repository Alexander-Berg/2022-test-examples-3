package ru.yandex.direct.dbutil.sharding;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ShardHelperGetBannerIdsByShardTest {

    @Mock
    private ShardSupport shardSupport;

    @InjectMocks
    private ShardHelper shardHelper;

    @Test
    public void checkGetBannerIdsByShardTest_AllIdsValid() {
        prepareTestData(List.of(12, 2, 12));

        assertThat(shardHelper.getBannerIdsByShard(List.of(1L, 2L, 10000L)),
                Matchers.equalTo(Map.of(
                        2, List.of(2L),
                        12, List.of(1L, 10000L)
                )));
    }

    @Test
    public void checkGetBannerIdsByShardTest_AllIdsInvalid() {
        prepareTestData(Arrays.asList(null, null));

        assertThat(shardHelper.getBannerIdsByShard(List.of(121L, 2222L)),
                Matchers.equalTo(Collections.emptyMap()));
    }

    @Test
    public void checkGetBannerIdsByShardTest_SomeIdsValid() {
        prepareTestData(Arrays.asList(19, 19, null, 5));

        assertThat(shardHelper.getBannerIdsByShard(List.of(11L, 22322441L, 10000L, 99999L)),
                Matchers.equalTo(Map.of(
                        5, List.of(99999L),
                        19, List.of(11L, 22322441L)
                )));
    }

    private void prepareTestData(List<Integer> getShardsList) {
        when(shardSupport.getShards(eq(ShardKey.BID), anyList()))
                .thenReturn(getShardsList);
    }

}

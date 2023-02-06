package ru.yandex.direct.jobs.promocodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.direct.core.entity.promocodes.model.CampPromocodes;
import ru.yandex.direct.core.entity.promocodes.model.PromocodeInfo;
import ru.yandex.direct.core.entity.promocodes.repository.CampPromocodesRepository;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HasPromocodeTest {
    private static final int SHARD = 1;
    private static final int SERVICE_ID = 7;
    private static final Map<Long, List<Long>> promocodeRepositoryData = Map.of(
            100L, List.of(6000L, 7000L, 8000L),
            200L, List.of(),
            300L, List.of(7000L, 9000L)
    );
    static SafeSearchTearOffPromocodesJob safeSearchTearOffPromocodesJob;

    static Stream<Arguments> testData() {
        return Stream.of(
                arguments(400L, 6000L, false),
                arguments(200L, 7000L, false),
                arguments(100L, 9000L, false),
                arguments(300L, 7000L, true),
                arguments(100L, 6000L, true)
        );
    }

    @BeforeAll
    static void init() {
        CampPromocodesRepository campPromocodesRepository = mock(CampPromocodesRepository.class);
        ShardHelper shardHelper = mock(ShardHelper.class);
        when(shardHelper.getShardByCampaignId(any())).thenReturn(SHARD);

        for (Map.Entry<Long, List<Long>> entry : promocodeRepositoryData.entrySet()) {
            List<PromocodeInfo> promocodeInfos = new ArrayList<>();
            entry.getValue().forEach(id -> promocodeInfos.add(new PromocodeInfo().withId(id)));
            CampPromocodes promocodes = promocodeInfos.isEmpty() ?
                    null : new CampPromocodes().withPromocodes(promocodeInfos);
            when(campPromocodesRepository.getCampaignPromocodes(SHARD, entry.getKey())).thenReturn(promocodes);
        }

        safeSearchTearOffPromocodesJob = new SafeSearchTearOffPromocodesJob(SERVICE_ID, null, null, null, null,
                campPromocodesRepository, shardHelper);
    }

    @ParameterizedTest(name = "cid = {0}, promocodeId = {1}, expectedResult = {2}")
    @MethodSource("testData")
    void test(long cid, long promocodeId, boolean expectedResult) {
        assertEquals(safeSearchTearOffPromocodesJob.hasPromocode(cid, promocodeId), expectedResult);
    }
}

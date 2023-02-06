package ru.yandex.direct.core.testing.steps;

import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.testing.repository.TestWarnplaceRepository;
import ru.yandex.direct.dbschema.ppc.enums.WarnplaceDone;

@ParametersAreNonnullByDefault
public class WarnplaceSteps {

    private static final long DEFAULT_STATUS_PLACE = 1;
    private static final long DEFAULT_OLD_PLACE = 1;
    private static final long DEFAULT_CLICKS = 10;
    private static final long DEFAULT_SHOWS = 10;
    private static final WarnplaceDone DEFAULT_DONE = WarnplaceDone.No;

    @Autowired
    private TestWarnplaceRepository repository;

    public long createDefaultWarnplaceWithAddTime(LocalDateTime addTime, int shard) {
        long cid = RandomUtils.nextLong(1, Integer.MAX_VALUE);
        long bid = RandomUtils.nextLong(1, Integer.MAX_VALUE);
        long id = RandomUtils.nextLong(1, Integer.MAX_VALUE);
        long pid = RandomUtils.nextLong(1, Integer.MAX_VALUE);
        createWarnplace(null, cid, bid, DEFAULT_STATUS_PLACE, DEFAULT_OLD_PLACE, id, DEFAULT_CLICKS, DEFAULT_SHOWS,
                null, null, DEFAULT_DONE, addTime, pid, shard);
        return id;
    }

    private void createWarnplace(@Nullable Long uid, Long cid, Long bid, Long statusPlace, Long oldPlace, Long id,
                                 Long clicks, Long shows, @Nullable Long managerUid, @Nullable Long agencyUid, WarnplaceDone done,
                                 LocalDateTime addTime, Long pid, int shard) {
        repository.createWarnplace(shard, uid, cid, bid, statusPlace, oldPlace, id, clicks, shows, managerUid, agencyUid, done,
                addTime, pid);
    }

    public List<Long> getAllWarnplaceIds(int shard) {
        return repository.getAllWarnplaceIds(shard);
    }

    public void clearWarnplaceInShard(int shard) {
        repository.clearWarnplaceInShard(shard);
    }

}

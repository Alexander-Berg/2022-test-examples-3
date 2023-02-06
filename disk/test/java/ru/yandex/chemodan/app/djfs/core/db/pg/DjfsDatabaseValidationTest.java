package ru.yandex.chemodan.app.djfs.core.db.pg;

import org.junit.Test;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.chemodan.app.djfs.core.db.DjfsShardInfo;
import ru.yandex.chemodan.app.djfs.core.test.DjfsTestBase;
import ru.yandex.chemodan.test.DatabaseValidationUtils;
import ru.yandex.misc.spring.jdbc.JdbcTemplate3;

public class DjfsDatabaseValidationTest extends DjfsTestBase {

    @Test
    public void everyForeignKeyShouldHaveIndex() {
        JdbcTemplate3 shard = pgShardResolver.resolve(new DjfsShardInfo.Pg(PG_SHARD_1));

        DatabaseValidationUtils.checkIndexesExistsForAllFK(shard, Cf.set(
                "group_links_gid_fkey", //table not used
                "group_invites_gid_fkey" //table not used
        ));
    }

}

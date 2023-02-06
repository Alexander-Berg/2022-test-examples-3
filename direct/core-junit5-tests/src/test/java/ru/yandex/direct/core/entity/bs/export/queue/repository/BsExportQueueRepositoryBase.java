package ru.yandex.direct.core.entity.bs.export.queue.repository;

import java.util.HashSet;
import java.util.Set;

import org.jooq.DSLContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppc.Tables.BS_EXPORT_QUEUE;
import static ru.yandex.direct.dbschema.ppc.Tables.BS_EXPORT_SPECIALS;

@CoreTest
@ExtendWith(SpringExtension.class)
abstract class BsExportQueueRepositoryBase {
    protected static final int TEST_SHARD = 2;

    @Autowired
    protected BsExportQueueRepository queueRepository;

    @Autowired
    protected BsExportSpecialsRepository specialsRepository;

    @Autowired
    protected DslContextProvider dslContextProvider;

    @Autowired
    protected Steps steps;

    protected ClientInfo clientInfo;
    protected DSLContext testShardContext;

    private Set<Long> ids = new HashSet<>();


    @BeforeEach
    void prepare() {
        testShardContext = dslContextProvider.ppc(TEST_SHARD);

        clientInfo = steps.clientSteps().createClient(new ClientInfo().withShard(TEST_SHARD));
    }

    @AfterEach
    void cleanup() {
        if (testShardContext == null) {
            return;
        }
        testShardContext.delete(BS_EXPORT_QUEUE).where(BS_EXPORT_QUEUE.CID.in(ids));
        testShardContext.delete(BS_EXPORT_SPECIALS).where(BS_EXPORT_SPECIALS.CID.in(ids));
    }

    Long createCampaign() {
        Long campaignId = steps.campaignSteps().createActiveTextCampaign(clientInfo).getCampaignId();
        ids.add(campaignId);
        return campaignId;
    }
}

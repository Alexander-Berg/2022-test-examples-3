package ru.yandex.chemodan.app.djfs.core.test;

import java.util.function.Function;

import javax.annotation.PostConstruct;

import com.mongodb.DB;
import com.mongodb.MockMongoClient;
import com.mongodb.MongoClient;
import org.joda.time.DateTimeUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;

import ru.yandex.chemodan.app.djfs.core.album.MockGeobase;
import ru.yandex.chemodan.app.djfs.core.billing.BillingManager;
import ru.yandex.chemodan.app.djfs.core.changelog.ChangelogDao;
import ru.yandex.chemodan.app.djfs.core.client.ActivateInMemoryClient;
import ru.yandex.chemodan.app.djfs.core.client.MockBlackbox2;
import ru.yandex.chemodan.app.djfs.core.client.MockDataApiHttpClient;
import ru.yandex.chemodan.app.djfs.core.client.MockDiskSearchHttpClient;
import ru.yandex.chemodan.app.djfs.core.client.MockLogReaderHttpClient;
import ru.yandex.chemodan.app.djfs.core.client.MockMpfsClient;
import ru.yandex.chemodan.app.djfs.core.client.MockOperationCallbackHttpClient;
import ru.yandex.chemodan.app.djfs.core.db.mongo.ActivateInMemoryMongo;
import ru.yandex.chemodan.app.djfs.core.db.mongo.MongoShardResolver;
import ru.yandex.chemodan.app.djfs.core.db.pg.InMemorySharpeiClient;
import ru.yandex.chemodan.app.djfs.core.db.pg.PgShardResolver;
import ru.yandex.chemodan.app.djfs.core.db.pg.SharpeiShardResolver;
import ru.yandex.chemodan.app.djfs.core.db.pg.TransactionUtils;
import ru.yandex.chemodan.app.djfs.core.diskinfo.DiskInfoDao;
import ru.yandex.chemodan.app.djfs.core.diskinfo.DiskInfoManager;
import ru.yandex.chemodan.app.djfs.core.filesystem.DjfsResourceDao;
import ru.yandex.chemodan.app.djfs.core.filesystem.Filesystem;
import ru.yandex.chemodan.app.djfs.core.filesystem.PgTrashCleanQueueDao;
import ru.yandex.chemodan.app.djfs.core.filesystem.QuotaManager;
import ru.yandex.chemodan.app.djfs.core.globalgallery.DeletionLogDao;
import ru.yandex.chemodan.app.djfs.core.legacy.LegacyFilesystemActions;
import ru.yandex.chemodan.app.djfs.core.lock.LockManager;
import ru.yandex.chemodan.app.djfs.core.operations.OperationDao;
import ru.yandex.chemodan.app.djfs.core.publication.LinkDataDao;
import ru.yandex.chemodan.app.djfs.core.share.GroupDao;
import ru.yandex.chemodan.app.djfs.core.share.GroupLinkDao;
import ru.yandex.chemodan.app.djfs.core.share.MongoGroupDao;
import ru.yandex.chemodan.app.djfs.core.share.MongoGroupLinkDao;
import ru.yandex.chemodan.app.djfs.core.share.PgGroupDao;
import ru.yandex.chemodan.app.djfs.core.share.PgGroupLinkDao;
import ru.yandex.chemodan.app.djfs.core.share.ShareInfoManager;
import ru.yandex.chemodan.app.djfs.core.share.ShareManager;
import ru.yandex.chemodan.app.djfs.core.test.util.DjfsTestUtil;
import ru.yandex.chemodan.app.djfs.core.test.util.DjfsUserTestHelper;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.chemodan.app.djfs.core.user.OrganizationDao;
import ru.yandex.chemodan.app.djfs.core.user.UserDao;
import ru.yandex.chemodan.app.djfs.core.user.UserData;
import ru.yandex.chemodan.util.test.AbstractTest;
import ru.yandex.commune.bazinga.test.BazingaTaskManagerStub;
import ru.yandex.misc.db.embedded.ActivateEmbeddedPg;
import ru.yandex.misc.db.embedded.PreparedDbProvider;
import ru.yandex.misc.web.servlet.HttpServletRequestX;
import ru.yandex.misc.web.servlet.mock.MockHttpServletRequest;

import static ru.yandex.chemodan.app.djfs.core.user.UserData.USER_OBJ;

/**
 * @author eoshch
 */
@ContextConfiguration(classes = {
        TestContextConfiguration.class,
})
@ActiveProfiles({
        ActivateInMemoryClient.PROFILE,
        ActivateInMemoryMongo.PROFILE,
        ActivateEmbeddedPg.EMBEDDED_PG,
})
@RunWith(JUnit4.class)
public abstract class DjfsTestBase extends AbstractTest {
    @ClassRule
    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

    @Rule
    public final SpringMethodRule springMethodRule = new SpringMethodRule();

    @Autowired
    @Qualifier("common")
    protected MongoClient commonMongoClient;

    @Autowired
    @Qualifier("blockings")
    protected MongoClient blockingsMongoClient;

    @Autowired
    protected SharpeiShardResolver sharpeiShardResolver;

    @Autowired
    protected InMemorySharpeiClient sharpeiClient;

    @Autowired
    protected BazingaTaskManagerStub bazingaStub;

    @Autowired
    protected MockDataApiHttpClient dataApiHttpClient;

    @Autowired
    protected MockDiskSearchHttpClient diskSearchHttpClient;

    @Autowired
    protected MockOperationCallbackHttpClient operationCallbackHttpClient;

    @Autowired
    protected MockLogReaderHttpClient logReaderHttpClient;

    @Autowired
    protected MongoShardResolver mongoShardResolver;

    @Autowired
    protected GroupDao groupDao;

    @Autowired
    protected MongoGroupDao mongoGroupDao;

    @Autowired
    protected PgGroupDao pgGroupDao;

    @Autowired
    protected MongoGroupLinkDao mongoGroupLinkDao;

    @Autowired
    protected PgGroupLinkDao pgGroupLinkDao;

    @Autowired
    protected GroupLinkDao groupLinkDao;

    @Autowired
    protected LinkDataDao linkDataDao;

    @Autowired
    protected PgShardResolver pgShardResolver;

    @Autowired
    protected TransactionUtils transactionUtils;

    @Autowired
    protected DjfsResourceDao djfsResourceDao;

    @Autowired
    protected ChangelogDao changelogDao;

    @Autowired
    protected OperationDao operationDao;

    @Autowired
    protected MockCeleryTaskManagerContextConfiguration.MockCeleryTaskManager mockCeleryTaskManager;

    @Autowired
    protected MockHistoryContextConfiguration.MockEventHistoryLogger mockEventHistoryLogger;

    @Autowired
    protected UserDao userDao;

    @Autowired
    protected DeletionLogDao deletionLogDao;

    @Autowired
    protected DiskInfoDao diskInfoDao;

    @Autowired
    protected DiskInfoManager diskInfoManager;

    @Autowired
    protected OrganizationDao organizationDao;

    @Autowired
    protected PgTrashCleanQueueDao pgTrashCleanQueueDao;

    @Autowired
    protected Filesystem filesystem;

    @Autowired
    protected LegacyFilesystemActions legacyFilesystemActions;

    @Autowired
    protected ShareManager shareManager;

    @Autowired
    protected ShareInfoManager shareInfoManager;

    @Autowired
    protected QuotaManager quotaManager;

    @Autowired
    protected LockManager lockManager;

    @Autowired
    protected BillingManager billingManager;

    @Autowired
    protected MockBlackbox2 blackbox2;

    @Autowired
    protected MockMpfsClient mpfsClient;

    @Autowired
    protected TestContextConfiguration.EventInterceptor eventInterceptor;

    @Autowired
    protected ApplicationContext applicationContext;

    @Autowired
    protected RandomFailingInvocationHandler.ProbabilitySource randomFailuresProbabilitySource;

    @Autowired
    protected DjfsTestUtil util;

    @Autowired
    protected MockGeobase geobase;

    private DjfsUserTestHelper djfsUserTestHelper;

    protected static int PG_SHARD_1 = 1;
    protected static int PG_SHARD_2 = 2;
    protected static int PG_COMMON_SHARD_1 = 3;
    protected static int[] PG_SHARDS = {PG_SHARD_1, PG_SHARD_2, PG_COMMON_SHARD_1};

    @PostConstruct
    public void init() {
        this.djfsUserTestHelper = new DjfsUserTestHelper(sharpeiClient, userDao, diskInfoManager, filesystem);
    }

    @Before
    public void setUp() {
        randomFailuresProbabilitySource.setFailureProbability(0);
        DateTimeUtils.setCurrentMillisSystem();
        sharpeiClient.createUser(DjfsUid.COMMON_UID, PG_COMMON_SHARD_1);
    }

    @After
    public void tearDown() {
        randomFailuresProbabilitySource.setFailureProbability(0);
        for (MongoClient mongoClient : new MongoClient[]{commonMongoClient, blockingsMongoClient}) {
            if (mongoClient instanceof MockMongoClient) {
                for (DB db : mongoClient.getUsedDatabases()) {
                    mongoClient.dropDatabase(db.getName());
                }
            }
        }
        sharpeiClient.clear();
        blackbox2.clear();
        mpfsClient.clear();
        mockCeleryTaskManager.submitted.clear();
        mockEventHistoryLogger.messageData.clear();
        eventInterceptor.events.clear();
        dataApiHttpClient.resetBlocks();
        diskSearchHttpClient.resetItems();
        operationCallbackHttpClient.clearCallParams();
        geobase.reset();
        PreparedDbProvider.truncateDatabases("disk");
    }

    protected void initializeUser(DjfsUid uid, int shardIndex) {
        initializePgUser(uid, PG_SHARDS[shardIndex]);
    }

    protected void initializePgUser(DjfsUid uid, int shard) {
        initializePgUser(uid, shard, Function.identity());
    }

    protected void initializePgUser(DjfsUid uid, int shard,
            Function<UserData.UserDataBuilder, UserData.UserDataBuilder> customization) {
        getDjfsUserTestHelper().initializePgUser(uid, shard, customization);
    }

    protected HttpServletRequestX createRequestWithUserObj(UserData USER) {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setAttribute(USER_OBJ, USER);
        return new HttpServletRequestX(req);
    }

    protected DjfsUserTestHelper getDjfsUserTestHelper() {
        return djfsUserTestHelper;
    }
}

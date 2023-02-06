package ru.yandex.chemodan.app.djfs.core.z;

import com.mongodb.MongoClient;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.EventManagerContextConfiguration;
import ru.yandex.chemodan.app.djfs.core.changelog.Changelog;
import ru.yandex.chemodan.app.djfs.core.changelog.ChangelogContextConfiguration;
import ru.yandex.chemodan.app.djfs.core.changelog.MongoChangelogDao;
import ru.yandex.chemodan.app.djfs.core.client.ActivateRealClient;
import ru.yandex.chemodan.app.djfs.core.client.DjfsRealClientContextConfiguration;
import ru.yandex.chemodan.app.djfs.core.db.DjfsDbContextConfiguration;
import ru.yandex.chemodan.app.djfs.core.db.mongo.ActivateRealMongo;
import ru.yandex.chemodan.app.djfs.core.db.mongo.MongoShardResolver;
import ru.yandex.chemodan.app.djfs.core.db.pg.ActivateRealPg;
import ru.yandex.chemodan.app.djfs.core.diskinfo.DiskInfo;
import ru.yandex.chemodan.app.djfs.core.diskinfo.DiskInfoDao;
import ru.yandex.chemodan.app.djfs.core.filesystem.DjfsPrincipal;
import ru.yandex.chemodan.app.djfs.core.filesystem.DjfsResourceDao;
import ru.yandex.chemodan.app.djfs.core.filesystem.Filesystem;
import ru.yandex.chemodan.app.djfs.core.filesystem.FilesystemContextConfiguration;
import ru.yandex.chemodan.app.djfs.core.filesystem.MongoDjfsResourceDao;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResource;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.lock.LockContextConfiguration;
import ru.yandex.chemodan.app.djfs.core.operations.Operation;
import ru.yandex.chemodan.app.djfs.core.operations.OperationContextConfiguration;
import ru.yandex.chemodan.app.djfs.core.operations.OperationDao;
import ru.yandex.chemodan.app.djfs.core.share.Group;
import ru.yandex.chemodan.app.djfs.core.share.GroupDao;
import ru.yandex.chemodan.app.djfs.core.share.GroupLink;
import ru.yandex.chemodan.app.djfs.core.share.GroupLinkDao;
import ru.yandex.chemodan.app.djfs.core.share.ShareContextConfiguration;
import ru.yandex.chemodan.app.djfs.core.share.ShareInfo;
import ru.yandex.chemodan.app.djfs.core.share.ShareInfoManager;
import ru.yandex.chemodan.app.djfs.core.share.ShareManager;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.chemodan.app.djfs.core.user.User;
import ru.yandex.chemodan.app.djfs.core.user.UserContextConfiguration;
import ru.yandex.chemodan.app.djfs.core.user.UserDao;
import ru.yandex.chemodan.app.djfs.core.user.UserData;
import ru.yandex.chemodan.util.AppNameHolder;
import ru.yandex.chemodan.util.sharpei.SharpeiHttpClient;
import ru.yandex.chemodan.util.sharpei.SharpeiShardInfo;
import ru.yandex.chemodan.util.sharpei.SharpeiUserInfo;
import ru.yandex.chemodan.util.test.AbstractTest;
import ru.yandex.devtools.test.annotations.YaIgnore;
import ru.yandex.inside.passport.blackbox2.Blackbox2;
import ru.yandex.inside.passport.blackbox2.protocol.request.params.EmailsParameterValue;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxAttributes;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxCorrectResponse;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxDbFields;
import ru.yandex.misc.net.LocalhostUtils;
import ru.yandex.misc.version.SimpleAppName;

/**
 * @author eoshch
 */
@ContextConfiguration(classes = {
        ChangelogContextConfiguration.class,
        DjfsDbContextConfiguration.class,
        DjfsRealClientContextConfiguration.class,
        EventManagerContextConfiguration.class,
        FilesystemContextConfiguration.class,
        LockContextConfiguration.class,
        OperationContextConfiguration.class,
        ShareContextConfiguration.class,
        UserContextConfiguration.class,
})
@YaIgnore
@Ignore
@ActiveProfiles({
        ActivateRealClient.PROFILE,
        ActivateRealMongo.PROFILE,
        ActivateRealPg.PROFILE,
})
public class DebugTest extends AbstractTest {
    @Autowired
    private UserDao userDao;

    @Autowired
    private SharpeiHttpClient sharpeiHttpClient;

    @Autowired
    private MongoShardResolver mongoShardResolver;

    @Autowired
    private MongoDjfsResourceDao mongoMpfsResourceDao;

    @Autowired
    private DjfsResourceDao mpfsResourceDao;

    @Autowired
    private Filesystem filesystem;

    @Autowired
    private MongoChangelogDao mongoChangelogDao;

    @Autowired
    private GroupDao groupDao;

    @Autowired
    private GroupLinkDao groupLinkDao;

    @Autowired
    private ShareInfoManager shareInfoManager;

    @Autowired
    private ShareManager shareManager;

    @Autowired
    private DiskInfoDao diskInfoDao;

    @Autowired
    private OperationDao operationDao;

    @Autowired
    private Blackbox2 blackbox;

    private DjfsUid mongoUid = DjfsUid.cons("4006383560");  // eoshch-test
    // private DjfsUid pgUid = DjfsUid.cons("4007787466");  // eoshch-testpg
    private DjfsUid pgUid = DjfsUid.cons("4008574338");  // eoshch-test-pg
    private DjfsUid uid = DjfsUid.cons("4001102343");
    private DjfsUid deletedUid = DjfsUid.cons("1130000000246493");

    @BeforeClass
    public static void setup() {
        AppNameHolder.setIfNotPresent(new SimpleAppName("mfps", "core"));
        AbstractTest.setup();
    }

    @Test
    public void resourceDebug() {
        // ListF<MpfsResource> diskResources = mongoMpfsResourceDao.findAll(mongoUid, MpfsResourceArea.DISK);
        // ListF<MpfsResource> trashResources = mongoMpfsResourceDao.findAll(mongoUid, MpfsResourceArea.TRASH);
        Option<DjfsResource> diskRoot = mongoMpfsResourceDao.find(DjfsResourcePath.cons(mongoUid, "/disk"));
        Option<DjfsResource> attachRoot = mongoMpfsResourceDao.find(DjfsResourcePath.cons(mongoUid, "/attach"));
        Option<DjfsResource> diskDoc = mongoMpfsResourceDao.find(DjfsResourcePath.cons(mongoUid, "/disk/tt.doc"));
        Option<DjfsResource> diskNonexistent = mongoMpfsResourceDao
                .find(DjfsResourcePath.cons(mongoUid, "/disk/nonexistent"));
    }

    @Test
    public void mongoMkdir() {
        filesystem.createFolder(DjfsPrincipal.cons(mongoUid), DjfsResourcePath.cons(mongoUid, "/disk/with_changelog4"));
    }

    @Test
    public void pgMkdir() {
        filesystem.createFolder(DjfsPrincipal.cons(pgUid), DjfsResourcePath.cons(pgUid, "/disk/with_chagelog2"));
    }

    @Test
    public void pgResourceDebug() {
        // ListF<MpfsResource> diskResources = mpfsResourceDao.findAll(pgUid, MpfsResourceArea.DISK);
        Option<DjfsResource> diskRoot = mpfsResourceDao.find(DjfsResourcePath.cons(pgUid, "/disk"));
        Option<DjfsResource> diskDdd = mpfsResourceDao.find(DjfsResourcePath.cons(pgUid, "/disk/ddd"));
        Option<DjfsResource> diskPdf = mpfsResourceDao.find(DjfsResourcePath.cons(pgUid, "/disk/16,10 обед.pdf"));
        Option<DjfsResource> diskDddPdf =
                mpfsResourceDao.find(DjfsResourcePath.cons(pgUid, "/disk/ddd/16,10 обед.pdf"));
    }

    @Test
    public void changelogDebug() {
        ListF<Changelog> mongoChangelogs = mongoChangelogDao.findAll(mongoUid);
    }

    @Test
    public void diskInfoDebug() {
        ListF<DiskInfo> allMongo = diskInfoDao.find(mongoUid);
        ListF<DiskInfo> allPg = diskInfoDao.find(pgUid);
    }

    @Test
    public void shareDebug() {
        ListF<Group> mongoGroups = groupDao.findAll(mongoUid);
        ListF<Group> pgGroups = groupDao.findAll(pgUid);
        ListF<GroupLink> mongoGroupLinks = groupLinkDao.findAll(mongoUid);
        ListF<GroupLink> pgGroupLinks = groupLinkDao.findAll(pgUid);

        DjfsResourcePath participantSharePath = DjfsResourcePath.cons(mongoUid, "/disk/shared-pg/subfolder/file");
        DjfsResourcePath ownerSharePath = DjfsResourcePath.cons(pgUid, "/disk/shared/shared-pg/subfolder/file");
        Option<ShareInfo> participantShareInfo = shareInfoManager.get(participantSharePath);
        Option<ShareInfo> ownerShareInfo = shareInfoManager.get(ownerSharePath);
        Option<DjfsResourcePath> resolved1 = participantShareInfo.get().participantPathToOwnerPath(ownerSharePath);
        Option<DjfsResourcePath> resolved2 = ownerShareInfo.get().participantPathToOwnerPath(ownerSharePath);
        Option<DjfsResourcePath> resolved3 = participantShareInfo.get().participantPathToOwnerPath(participantSharePath);
        Option<DjfsResourcePath> resolved4 = ownerShareInfo.get().participantPathToOwnerPath(participantSharePath);
    }

    @Test
    public void operationDebug() {
        Option<Operation> mongoOperation = operationDao.find(
                DjfsUid.cons(4006383560L), "ab9f1b85f086570db5f5b2a633ca4626c31cad011e9398a92bf6605a46e53460");

        Option<Operation> pgOperation = operationDao.find
                (DjfsUid.cons(4010326924L), "71e81866d2ff0b315e9f47083938e34d98fb6724b55afc700152177caae50f6a");
    }

    @Test
    public void passportDebug() {
        BlackboxCorrectResponse blackboxCorrectResponse =
                blackbox.query().userInfo(LocalhostUtils.localAddress(), mongoUid.asPassportUid(),
                        Cf.list(BlackboxDbFields.FIO, BlackboxDbFields.FIRSTNAME, BlackboxDbFields.LASTNAME,
                                BlackboxDbFields.LANG, BlackboxDbFields.USER_ENABLED_STATUS),
                        Option.of(EmailsParameterValue.GET_ALL));

        BlackboxCorrectResponse blackboxCorrectResponse2 =
                blackbox.query().userInfo(LocalhostUtils.localAddress(), mongoUid.asPassportUid(),
                        Cf.list(BlackboxDbFields.FIO, BlackboxDbFields.USER_ENABLED_STATUS));

        BlackboxCorrectResponse blackboxCorrectResponseEmpty =
                blackbox.query().userInfo(LocalhostUtils.localAddress(),
                        DjfsUid.cons("1111111111111111111").asPassportUid(),
                        Cf.list(BlackboxDbFields.FIO, BlackboxDbFields.USER_ENABLED_STATUS));
        blackboxCorrectResponseEmpty.getUid();

        BlackboxCorrectResponse blackboxCorrectResponseBlocked = blackbox.query()
                .userInfo(LocalhostUtils.localAddress(), "yndx-disk-blocked-on-del",
                        Cf.list(BlackboxDbFields.FIO, BlackboxDbFields.USER_ENABLED_STATUS),
                        Option.of(EmailsParameterValue.GET_ALL));

        BlackboxCorrectResponse blackboxCorrectResponse3 = blackbox.query().userInfo(LocalhostUtils.localAddress(),
                mongoUid.asPassportUid(), Cf.list(BlackboxAttributes.ACCOUNT_IS_AVAILABLE));

        BlackboxCorrectResponse blackboxCorrectResponse4 = blackbox.query().userInfo(LocalhostUtils.localAddress(),
                mongoUid.asPassportUid(), Cf.<String>list());

        BlackboxCorrectResponse blackboxCorrectResponse5 = blackbox.query().userInfo(LocalhostUtils.localAddress(),
                DjfsUid.cons("1111111111111111111").asPassportUid(), Cf.list(BlackboxAttributes.ACCOUNT_IS_AVAILABLE));
    }

    @Test
    public void simple() {
        ListF<SharpeiShardInfo> pgShards = sharpeiHttpClient.getShards();
        Option<SharpeiUserInfo> sharpeiUser = sharpeiHttpClient.findUser(() -> pgUid.asString());
        MongoClient mc = mongoShardResolver.resolve(uid);
        Option<User> user = userDao.find(uid).map(UserData::toUser);

        Option<User> mongoUser = userDao.find(mongoUid).map(UserData::toUser);
        Option<User> pgUser = userDao.find(pgUid).map(UserData::toUser);

        // deleted: ISODate("2016-11-29T16:00:04.961Z")
        Option<User> deletedUser = userDao.find(deletedUid).map(UserData::toUser);
    }
}


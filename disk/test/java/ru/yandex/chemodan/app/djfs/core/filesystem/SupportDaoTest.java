package ru.yandex.chemodan.app.djfs.core.filesystem;

import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.chemodan.app.djfs.core.test.DjfsTestBase;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.misc.test.Assert;
import ru.yandex.misc.test.Parameterized;

/**
 * @author m-messiah
 */
@RunWith(Parameterized.class)
public class SupportDaoTest extends DjfsTestBase {
    private static final DjfsUid UID = DjfsUid.cons("12345");
    public static final String STID_1 = "1000004.yadisk:1260145.3983296384113870897333404302967";
    public static final String STID_2 = "1000004.yadisk:preview.249690056375959106114196944070";
    public static final SupportComment SUPPORT_COMMENT = SupportComment.builder()
            .id(new ObjectId())
            .uid(UID)
            .dataId(Option.of("test_id"))
            .dataHash(Option.of("test_hash"))
            .dataStids(Cf.list(STID_1, STID_2))
            .data(new JsonSupportData(
                    Option.of(UID.toString()),
                    Option.of(1333607510L),
                    Option.of(UID.toString() + ":/disk/test.jpg"),
                    Option.of("comment"),
                    Option.of("user"),
                    Option.empty(),
                    Option.empty(),
                    Option.empty(),
                    Option.empty(),
                    Cf.list()
            ))
            .build();

    private final Boolean usePgDao;
    protected SupportDao supportDao;
    @Autowired
    protected PgSupportDao pgSupportDao;
    @Autowired
    protected MongoSupportDao mongoSupportDao;

    @org.junit.runners.Parameterized.Parameters
    public static ListF<Object[]> data() {
        return Cf.list(new Object[]{false}, new Object[]{true});
    }

    public SupportDaoTest(Boolean usePgDao) {
        this.usePgDao = usePgDao;
    }

    @Before
    public void setUp() {
        super.setUp();
        initializeUser(UID, 0);
        supportDao = usePgDao ? pgSupportDao : mongoSupportDao;
        supportDao.insert(SUPPORT_COMMENT);
    }

    @Test
    public void findByHash() {
        Option<SupportComment> supportCommentO = supportDao.find(SUPPORT_COMMENT.getDataHash().get());
        Assert.notEmpty(supportCommentO);
        SupportComment supportComment = supportCommentO.get();
        Assert.equals(SUPPORT_COMMENT, supportComment);
    }

    @Test
    public void getAllBlockingStids() {
        SetF<String> blockedStids = supportDao.getAllBlockedStids();
        Assert.in(STID_1, blockedStids);
        Assert.in(STID_2, blockedStids);
    }
}

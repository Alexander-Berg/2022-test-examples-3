package ru.yandex.calendar.logic.layer;

import org.joda.time.Instant;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.logic.beans.generated.Layer;
import ru.yandex.calendar.logic.beans.generated.LayerFields;
import ru.yandex.calendar.logic.event.ActionInfo;
import ru.yandex.calendar.logic.user.SettingsRoutines;
import ru.yandex.calendar.test.auto.db.util.TestManager;
import ru.yandex.calendar.test.auto.db.util.TestUserInfo;
import ru.yandex.calendar.test.generic.AbstractConfTest;
import ru.yandex.inside.passport.PassportSid;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.misc.test.Assert;

public class LayerComparatorTest extends AbstractConfTest {
    @Autowired
    private TestManager testManager;
    @Autowired
    private LayerRoutines layerRoutines;
    @Autowired
    private LayerDao layerDao;
    @Autowired
    private SettingsRoutines settingsRoutines;


    @Test
    public void earliestFirst() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-14500").getUid();

        Layer earliestZzz = createAndSaveUserLayer(uid, new Instant(1000), Option.of("zzz"));
        Layer qqq = createAndSaveUserLayer(uid, new Instant(2000), Option.of("qqq"));
        Layer aaa = createAndSaveUserLayer(uid, new Instant(3000), Option.of("aaa"));

        sortAndAssert(Cf.list(earliestZzz, aaa, qqq), Cf.list(earliestZzz, qqq, aaa), uid);
    }

    @Test
    public void defaultFirst() {
        PassportUid uid = testManager.prepareUser("yandex-team-mm-14501").getUid();

        Layer earliestZzz = createAndSaveUserLayer(uid, new Instant(1000), Option.of("zzz"));
        Layer defaultQqq = createAndSaveUserLayer(uid, new Instant(2000), Option.of("qqq"));
        Layer aaa = createAndSaveUserLayer(uid, new Instant(3000), Option.of("aaa"));

        settingsRoutines.updateDefaultLayer(uid, defaultQqq.getId());

        sortAndAssert(Cf.list(defaultQqq, aaa, earliestZzz), Cf.list(earliestZzz, defaultQqq, aaa), uid);
    }

    @Test
    public void longListWithoutExplicitDefaultLayer() {
        TestUserInfo myYaTeamUser = testManager.prepareRandomYaTeamUser(14510);
        PassportUid myYaTeamUid = myYaTeamUser.getUid();
        Assert.A.equals('f', settingsRoutines.getSettingsByUid(myYaTeamUid).getUserName().get().charAt(0));

        TestUserInfo otherYaTeamUser = testManager.prepareRandomYaTeamUser(14511);
        PassportUid otherYaTeamUid = otherYaTeamUser.getUid();
        Assert.A.equals('f', settingsRoutines.getSettingsByUid(otherYaTeamUid).getUserName().get().charAt(0));

        layerRoutines.deleteLayer(myYaTeamUser.getUserInfo(), myYaTeamUser.getDefaultLayerId(), ActionInfo.webTest());
        layerRoutines.deleteLayer(otherYaTeamUser.getUserInfo(), otherYaTeamUser.getDefaultLayerId(), ActionInfo.webTest());

        Layer myEarliest = createAndSaveUserLayer(myYaTeamUid, new Instant(12000), Option.of("my earliest"));
        Layer myZzz      = createAndSaveUserLayer(myYaTeamUid, new Instant(13000), Option.of("zzz"));
        Layer myGgg      = createAndSaveUserLayer(myYaTeamUid, new Instant(14000), Option.of("ggg"));
        Layer myEee      = createAndSaveUserLayer(myYaTeamUid, new Instant(15000), Option.of("eee"));
        Layer myNullName = createAndSaveUserLayer(myYaTeamUid, new Instant(16000), Option.<String>empty());
        Layer myAaa      = createAndSaveUserLayer(myYaTeamUid, new Instant(17000), Option.of("aaa"));
        Layer myRusCap   = createAndSaveUserLayer(myYaTeamUid, new Instant(18000), Option.of("Русский с заглавной"));

        Layer myTV       = createAndSaveServiceLayer(myYaTeamUid, new Instant(20000), PassportSid.TV);
        Layer myAfisha   = createAndSaveServiceLayer(myYaTeamUid, new Instant(21000), PassportSid.AFISHA);

        Layer sharedEarliest = createAndSaveUserLayer(otherYaTeamUid, new Instant(11000), Option.of("earliest shared"));
        Layer sharedYyy      = createAndSaveUserLayer(otherYaTeamUid, new Instant(30000), Option.of("yyy shared"));
        Layer sharedBbb      = createAndSaveUserLayer(otherYaTeamUid, new Instant(31000), Option.of("bbb shared"));
        Layer sharedNullName = createAndSaveUserLayer(otherYaTeamUid, new Instant(32000), Option.<String>empty());

        ListF<Layer> sourceList = Cf.list(
            myEarliest, myZzz, myGgg, myEee, myNullName, myAaa, myRusCap,
            myTV, myAfisha,
            sharedEarliest, sharedYyy, sharedBbb, sharedNullName
        );

        ListF<Layer> expectedList = Cf.list(
            myEarliest, myAaa, myEee, myNullName, myGgg, myZzz, myRusCap,
            sharedBbb, sharedEarliest, sharedNullName, sharedYyy, // don't show other user's default layer
            myAfisha, myTV
        );

        sortAndAssert(expectedList, sourceList, myYaTeamUid);
    }



    private Layer createAndSaveUserLayer(PassportUid creatorUid, Instant creationTs, Option<String> nameO) {
        return createAndSaveLayer(LayerType.USER, PassportSid.CALENDAR, creatorUid, creationTs, nameO);
    }

    private Layer createAndSaveServiceLayer(PassportUid creatorUid, Instant creationTs, PassportSid sid) {
        return createAndSaveLayer(LayerType.SERVICE, sid, creatorUid, creationTs, Option.<String>empty());
    }

    private Layer createAndSaveLayer(
            LayerType type, PassportSid sid, PassportUid creatorUid,
            Instant creationTs, Option<String> nameO)
    {
        Layer layer = new Layer();
        layer.setType(type);
        layer.setSid(sid);
        layer.setCreatorUid(creatorUid);
        layer.setCreationTs(creationTs);
        layer.setName(nameO);
        layer.setId(layerDao.saveLayer(layer));
        return layer;
    }

    private void sortAndAssert(ListF<Layer> expectedList, ListF<Layer> sourceList, PassportUid forUid) {
        ListF<Layer> sortedList = sourceList.sorted(layerRoutines.layerComparatorForUser(Option.of(forUid)));

        // first, lightweight human-readable "name-only" check (covers most cases)
        Assert.A.equals(expectedList.map(LayerFields.NAME.getF()), sortedList.map(LayerFields.NAME.getF()));
        Assert.A.equals(expectedList, sortedList);
    }

}

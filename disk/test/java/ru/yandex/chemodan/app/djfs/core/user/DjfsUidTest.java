package ru.yandex.chemodan.app.djfs.core.user;

import org.junit.Test;

import ru.yandex.chemodan.app.djfs.core.ActionContext;
import ru.yandex.misc.test.Assert;

/**
 * @author eoshch
 */
public class DjfsUidTest {
    @Test(expected = InvalidDjfsUidException.class)
    public void exceptionOnNull() {
        DjfsUid.cons(null);
    }

    @Test(expected = InvalidDjfsUidException.class)
    public void exceptionOnEmpty() {
        DjfsUid.cons("");
    }

    @Test(expected = InvalidDjfsUidException.class)
    public void exceptionOnRandomString() {
        DjfsUid.cons("not_a_valid_uid");
    }

    @Test(expected = InvalidDjfsUidException.class)
    public void exceptionOnNegative() {
        DjfsUid.cons(-1234);
    }

    @Test(expected = InvalidDjfsUidException.class)
    public void exceptionOnNegativeAsString() {
        DjfsUid.cons("-1234");
    }

    @Test
    public void createCommon() {
        DjfsUid uid = DjfsUid.cons("31337");
        Assert.equals(31337L, uid.asLong());
        Assert.equals("31337", uid.asString());
    }

    @Test
    public void createShareProduction() {
        DjfsUid uid = DjfsUid.cons("share_production");
        Assert.equals(1L, uid.asLong());
        Assert.equals("share_production", uid.asString());
    }

    @Test
    public void shareProductionEquality() {
        DjfsUid uidFromString = DjfsUid.cons("share_production");
        DjfsUid uidFromLong = DjfsUid.cons(1);
        DjfsUid uidFromLongAsString = DjfsUid.cons("1");
        Assert.assertSame(DjfsUid.SHARE_PRODUCTION, uidFromString);
        Assert.assertSame(DjfsUid.SHARE_PRODUCTION, uidFromLong);
        Assert.assertSame(DjfsUid.SHARE_PRODUCTION, uidFromLongAsString);
    }

    @Test
    public void shareProductionIsValid() {
        Assert.isTrue(DjfsUid.isValid("share_production"));
    }

    @Test(expected = InvalidClientInputDjfsUidException.class)
    public void clientException() {
        DjfsUid.cons("not_a_valid_uid", ActionContext.CLIENT_INPUT);
    }
}

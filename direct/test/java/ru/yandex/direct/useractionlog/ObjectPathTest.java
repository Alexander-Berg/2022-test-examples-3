package ru.yandex.direct.useractionlog;

import javax.annotation.ParametersAreNonnullByDefault;

import org.junit.Assert;
import org.junit.Test;

import ru.yandex.direct.useractionlog.schema.ObjectPath;

@ParametersAreNonnullByDefault
public class ObjectPathTest {
    @Test
    public void testClientIdEquals() {
        Object original = new ObjectPath.ClientPath(new ClientId(123));
        Object same = new ObjectPath.ClientPath(new ClientId(123));
        Object otherValue = new ObjectPath.ClientPath(new ClientId(345));
        Object otherType = new ObjectPath.CampaignPath(new ClientId(345), new CampaignId(123));

        Assert.assertEquals(original, same);
        Assert.assertEquals(original.hashCode(), same.hashCode());

        Assert.assertNotEquals(original, otherValue);
        Assert.assertNotEquals(original, otherType);
    }

    @Test
    public void testCampaignIdEquals() {
        Object original = new ObjectPath.CampaignPath(new ClientId(123), new CampaignId(123));
        Object same = new ObjectPath.CampaignPath(new ClientId(123), new CampaignId(123));
        Object otherValue = new ObjectPath.CampaignPath(new ClientId(123), new CampaignId(345));
        Object otherType = new ObjectPath.ClientPath(new ClientId(123));

        Assert.assertEquals(original, same);
        Assert.assertEquals(original.hashCode(), same.hashCode());

        Assert.assertNotEquals(original, otherValue);
        Assert.assertNotEquals(original, otherType);
    }

    @Test
    public void testAdGroupIdEquals() {
        Object original = new ObjectPath.AdGroupPath(new ClientId(1), new CampaignId(100500), new AdGroupId(123));
        Object same = new ObjectPath.AdGroupPath(new ClientId(1), new CampaignId(100500), new AdGroupId(123));
        Object otherValue1 = new ObjectPath.AdGroupPath(new ClientId(1), new CampaignId(100500), new AdGroupId(345));
        Object otherValue2 = new ObjectPath.AdGroupPath(new ClientId(1), new CampaignId(100501), new AdGroupId(123));
        Object otherType = new ObjectPath.CampaignPath(new ClientId(1), new CampaignId(123));

        Assert.assertEquals(original, same);
        Assert.assertEquals(original.hashCode(), same.hashCode());

        Assert.assertNotEquals(original, otherValue1);
        Assert.assertNotEquals(original, otherValue2);
        Assert.assertNotEquals(original, otherType);
    }
}

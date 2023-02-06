package ru.yandex.market.tsum.core.agent;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Dmitry Andreev <a href="mailto:AndreevDm@yandex-team.ru"></a>
 * @date 10/03/2017
 */
public class PackageStatusTest {
    @Test
    public void testEquals() {
        PackageStatus packageStatus1 = new PackageStatus("abc", PackageType.SOX);
        PackageStatus packageStatus2 = new PackageStatus("abc", PackageType.SOX);
        packageStatus1.addVersionMismatch(new PackageStatus.VersionMismatch("host1", "prod", "v1", "v2"), 42);
        packageStatus1.addVersionMismatch(new PackageStatus.VersionMismatch("host2", "prod", "v11", "v22"), 42);
        packageStatus2.addVersionMismatch(new PackageStatus.VersionMismatch("host2", "prod", "v11", "v22"), 84);
        packageStatus2.addVersionMismatch(new PackageStatus.VersionMismatch("host1", "prod", "v1", "v2"), 84);

        Assert.assertTrue(packageStatus1.equals(packageStatus2));
    }
}
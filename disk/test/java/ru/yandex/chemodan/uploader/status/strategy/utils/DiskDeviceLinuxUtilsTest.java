package ru.yandex.chemodan.uploader.status.strategy.utils;

import org.junit.Test;

import ru.yandex.misc.test.Assert;

/**
 * @author nshmakov
 * @author akirakozov
 */
public class DiskDeviceLinuxUtilsTest {

    @Test
    public void getDeviceShortName() {
        Assert.equals("vda", DiskDeviceLinuxUtils.getDeviceShortName("vda"));
        Assert.equals("vda", DiskDeviceLinuxUtils.getDeviceShortName("vda1"));
        Assert.equals("md", DiskDeviceLinuxUtils.getDeviceShortName("md23"));
    }

}


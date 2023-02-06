package ru.yandex.market.partner.content.common.utils;

import org.junit.Assert;
import org.junit.Test;
import ru.yandex.market.mbo.export.MboParameters;

public class PickerImageUtilsTest {

    @Test
    public void convertAvatarUrl() {
        String avatarImage =
            "//avatars.mds.yandex.net/get-mpic/364668/c7c0e4df28911cbbb6f7ba4c244fc876/orig";

        MboParameters.PickerImage pickerImage = PickerImageUtils.convertAvatarUrl(avatarImage);

        Assert.assertEquals(pickerImage.getNameSpace(), "get-mpic");
        Assert.assertEquals(pickerImage.getGroupId(), "364668");
        Assert.assertEquals(pickerImage.getImageName(), "c7c0e4df28911cbbb6f7ba4c244fc876");
        Assert.assertEquals(pickerImage.getName(), "c7c0e4df28911cbbb6f7ba4c244fc876");
    }

    @Test(expected = IllegalArgumentException.class)
    public void throwsOnBadUrl() {
        String nonAvatarImage = "http://example.com/imp.jpg";
        PickerImageUtils.convertAvatarUrl(nonAvatarImage);
    }
}
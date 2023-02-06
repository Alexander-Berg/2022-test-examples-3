package ru.yandex.market.mbo.gwt.client.utils;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import ru.yandex.market.mbo.gwt.models.params.PickerImage;
import ru.yandex.market.mbo.gwt.utils.PickerImageUtils;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author s-ermakov
 */
@RunWith(Parameterized.class)
@SuppressWarnings("checkstyle:VisibilityModifier")
public class PickerImageConvertFromPictureTest {

    @Parameterized.Parameter(0)
    public String given;

    @Parameterized.Parameter(1)
    public PickerImage expected;

    @Parameterized.Parameter(2)
    public Class<? extends Exception> expectedException;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void test() {
        if (expectedException != null) {
            thrown.expect(expectedException);
        }

        PickerImage actual = PickerImageUtils.convert(given);

        assertPickers(expected, actual);
    }

    private static void assertPickers(PickerImage expected, PickerImage actual) {
        Assert.assertEquals(expected.getUrl(), actual.getUrl());
        Assert.assertEquals(expected.getGroupId(), actual.getGroupId());
        Assert.assertEquals(expected.getImageName(), actual.getImageName());
        Assert.assertEquals(expected.getNamespace(), actual.getNamespace());
    }

    private static Object[] createParams(String url, Class<? extends Exception> expectedException) {
        return new Object[]{url, null, expectedException};
    }

    private static Object[] createParams(String url, String namespace, String groupId, String imageName) {
        PickerImage pickerImage = new PickerImage(url, "test", namespace, groupId, imageName);
        return new Object[]{url, pickerImage, null};
    }

    @Parameterized.Parameters
    @SuppressWarnings("checkstyle:lineLength")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            createParams("//avatars.mdst.yandex.net/get-mpic/4138/img_id2458882964333285755.jpeg/orig",
                "get-mpic", "4138", "img_id2458882964333285755.jpeg"),
            createParams("http://avatars.mdst.yandex.net/get-mpic/4138/img_id2458882964333285755.jpeg/orig",
                "get-mpic", "4138", "img_id2458882964333285755.jpeg"),
            createParams("https://avatars.mdst.yandex.net/mbo/4138aaas/img_id2458882964333285755.jpeg/orig",
                "mbo", "4138aaas", "img_id2458882964333285755.jpeg"),


            createParams("//avatars.mds.yandex.net/get-mpic/466729/model_option-picker-1720223892-15277688--23eb4aa7c58204db7daf73714a46de9c/orig",
                "get-mpic", "466729", "model_option-picker-1720223892-15277688--23eb4aa7c58204db7daf73714a46de9c"),
            createParams("http://avatars.mds.yandex.net/get-mpic/466729/model_option-picker-1720223892-15277688--23eb4aa7c58204db7daf73714a46de9c/orig",
                "get-mpic", "466729", "model_option-picker-1720223892-15277688--23eb4aa7c58204db7daf73714a46de9c"),
            createParams("https://avatars.mds.yandex.net/get-mpic/466729/model_option-picker-1720223892-15277688--23eb4aa7c58204db7daf73714a46de9c/orig",
                "get-mpic", "466729", "model_option-picker-1720223892-15277688--23eb4aa7c58204db7daf73714a46de9c"),

            createParams("https://avatars.mdst.yandex.net/mbo/4138aaas/img_id2458882/964333285755.jpeg/orig", IllegalStateException.class),

            createParams("https://avatars.mdst.yandex.net/mbo4138aaasimg_id2458882964333285755.jpeg/orig", IllegalStateException.class)
        });
    }
}

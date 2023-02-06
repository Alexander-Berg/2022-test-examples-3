package ru.yandex.market.mbo.gwt.client.widgets.fileupload;

import org.junit.Assert;
import org.junit.runners.Parameterized;
import ru.yandex.market.mbo.gwt.client.widgets.image.UploadResponseUnmarshaller;
import ru.yandex.market.mbo.gwt.models.params.PickerImage;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author s-ermakov
 */
public class PickerImageUnmarshallerTest extends BaseUploadResponseUnmarshallerTest<PickerImage> {
    @Override
    protected UploadResponseUnmarshaller<PickerImage> getUnmarshaller() {
        return UploadResponseUnmarshaller.PICKER_IMAGE;
    }

    @Override
    protected void assertEquals(PickerImage expected, PickerImage actual) {
        if (expected == null && actual == null) {
            return;
        }

        Assert.assertEquals(expected.getName(), actual.getName());
        Assert.assertEquals(expected.getUrl(), actual.getUrl());
        Assert.assertEquals(expected.getGroupId(), actual.getGroupId());
        Assert.assertEquals(expected.getImageName(), actual.getImageName());
        Assert.assertEquals(expected.getNamespace(), actual.getNamespace());
    }

    private static Object[] createParams(String name, String url, String namespace, String groupId, String imageName) {
        PickerImage pickerImage = new PickerImage(url, name, namespace, groupId, imageName);
        return createParams(name + "\t" + url, pickerImage);
    }

    @Parameterized.Parameters
    @SuppressWarnings("checkstyle:lineLength")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            createParams("img_id2458882964333285755.jpeg", "//avatars.mdst.yandex.net/get-mpic/4138/img_id2458882964333285755.jpeg/orig",
                "get-mpic", "4138", "img_id2458882964333285755.jpeg"),
            createParams("img_id2458882964333285755.jpeg", "http://avatars.mdst.yandex.net/get-mpic/4138/img_id2458882964333285755.jpeg/orig",
                "get-mpic", "4138", "img_id2458882964333285755.jpeg"),
            createParams("test.jpeg", "https://avatars.mdst.yandex.net/mbo/4138aaas/img_id2458882964333285755.jpeg/orig",
                "mbo", "4138aaas", "img_id2458882964333285755.jpeg"),


            createParams("test.orig", "//avatars.mds.yandex.net/get-mpic/466729/model_option-picker-1720223892-15277688--23eb4aa7c58204db7daf73714a46de9c/orig",
                "get-mpic", "466729", "model_option-picker-1720223892-15277688--23eb4aa7c58204db7daf73714a46de9c"),
            createParams("test.orig", "http://avatars.mds.yandex.net/get-mpic/466729/model_option-picker-1720223892-15277688--23eb4aa7c58204db7daf73714a46de9c/orig",
                "get-mpic", "466729", "model_option-picker-1720223892-15277688--23eb4aa7c58204db7daf73714a46de9c"),
            createParams("test.orig", "https://avatars.mds.yandex.net/get-mpic/466729/model_option-picker-1720223892-15277688--23eb4aa7c58204db7daf73714a46de9c/orig",
                "get-mpic", "466729", "model_option-picker-1720223892-15277688--23eb4aa7c58204db7daf73714a46de9c"),
            createParams("test.orig", null)
        });
    }
}

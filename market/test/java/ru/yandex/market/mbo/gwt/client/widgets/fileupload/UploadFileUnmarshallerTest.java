package ru.yandex.market.mbo.gwt.client.widgets.fileupload;

import org.junit.Assert;
import org.junit.runners.Parameterized;
import ru.yandex.market.mbo.gwt.client.widgets.image.ImageFile;
import ru.yandex.market.mbo.gwt.client.widgets.image.UploadResponseUnmarshaller;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author s-ermakov
 */
public class UploadFileUnmarshallerTest extends BaseUploadResponseUnmarshallerTest<ImageFile> {
    @Override
    protected UploadResponseUnmarshaller<ImageFile> getUnmarshaller() {
        return UploadResponseUnmarshaller.FILE;
    }

    @Override
    protected void assertEquals(ImageFile expected, ImageFile actual) {
        if (expected == null && actual == null) {
            return;
        }

        Assert.assertEquals(expected.getName(), actual.getName());
        Assert.assertEquals(expected.getUrl(), actual.getUrl());
        Assert.assertEquals(expected.getGroupId(), actual.getGroupId());
        Assert.assertEquals(expected.getImageName(), actual.getImageName());
        Assert.assertEquals(expected.getNamespace(), actual.getNamespace());
    }

    private static Object[] createParams(String name, String url, String namespace, String groupId, String fileName) {
        ImageFile uploadFile = new ImageFile(name, url);
        uploadFile.setGroupId(groupId);
        uploadFile.setImageName(fileName);
        uploadFile.setNamespace(namespace);
        return createParams(name + "\t" + url, uploadFile);
    }

    @Parameterized.Parameters
    @SuppressWarnings("checkstyle:lineLength")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
            createParams("doc1.doc", "//some.resource.com/doc/667/doc1.doc/orig",
                "doc", "667", "doc1.doc"),
            createParams("doc.doc", "http://some.resource.com/docs/666/doc2.doc/orig",
                "docs", "666", "doc2.doc"),
            createParams("doc.doc", "https://some.resource.com/docs/668/doc1.doc/orig",
                "docs", "668", "doc1.doc"),
            createParams("img_id2458882964333285755.jpeg", "//avatars.mdst.yandex.net/get-mpic/4138/img_id2458882964333285755.jpeg/orig",
                "get-mpic", "4138", "img_id2458882964333285755.jpeg"),
            createParams("https://some.resource.com/dos/666/doc.doc/orig", null),
            createParams("doc.doc//some.resource.com/dos/666/doc.doc/orig", null),
            createParams("doc.doc\n//some.resource.com/dos/666/doc.doc/orig", null)
        });
    }
}

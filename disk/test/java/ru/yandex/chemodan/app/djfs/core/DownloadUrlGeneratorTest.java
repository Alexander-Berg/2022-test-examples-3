package ru.yandex.chemodan.app.djfs.core;

import java.net.URI;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResourcePath;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.FileDjfsResource;
import ru.yandex.chemodan.app.djfs.core.test.DjfsTestBase;
import ru.yandex.chemodan.app.djfs.core.user.DjfsUid;
import ru.yandex.misc.image.Dimension;
import ru.yandex.misc.test.Assert;


public class DownloadUrlGeneratorTest extends DjfsTestBase {
    private static String previewStid = "100000.yadisk:128280859.3094486800568916220";
    protected static DjfsUid UID = DjfsUid.cons(31337);
    protected static DjfsUid OWNER_UID = DjfsUid.cons(73313);
    protected static String filepath = "/disk/test1.jpg";
    protected static Option<Dimension> defaultDimension = Option.of(Dimension.valueOf(800, 600));

    @Autowired
    private DownloadUrlGenerator downloadUrlGenerator;

    @Test
    public void generateUserEternalPreviewUrl() {
        FileDjfsResource file = FileDjfsResource.random(DjfsResourcePath.cons(UID, filepath),
                x -> x.previewStid(previewStid).mimetype("image/jpeg"));
        URI url = downloadUrlGenerator.makeUserPreviewUrl(OWNER_UID, file::getUid, file.getPreviewStid().get(),
                file.getMimetype(), file.getPath().getName(), defaultDimension, true, true);

        Assert.assertContains(url.getPath(), "inf");
        Assert.assertContains(url.getQuery(), UID.asString());
    }

    @Test
    public void generatePublicEternalPreviewUrl() {
        FileDjfsResource file = FileDjfsResource.random(DjfsResourcePath.cons(UID, filepath),
                x -> x.previewStid(previewStid).mimetype("image/jpeg"));
        URI url = downloadUrlGenerator.makePublicPreviewUrl(() -> OWNER_UID, file.getPreviewStid().get(),
                file.getMimetype(), file.getPath().getName(), defaultDimension, true, true);

        Assert.assertContains(url.getPath(), "inf");
        Assert.assertFalse(url.getQuery().contains(UID.asString()));
    }

    @Test
    public void generatePublicTemporaryPreviewUrl() {
        FileDjfsResource file = FileDjfsResource.random(DjfsResourcePath.cons(UID, filepath),
                x -> x.previewStid(previewStid).mimetype("image/jpeg"));
        URI url = downloadUrlGenerator.makePublicPreviewUrl(() -> OWNER_UID, file.getPreviewStid().get(),
                file.getMimetype(), file.getPath().getName(), defaultDimension, true, false);

        Assert.assertFalse(url.getPath().contains("inf"));
    }
}

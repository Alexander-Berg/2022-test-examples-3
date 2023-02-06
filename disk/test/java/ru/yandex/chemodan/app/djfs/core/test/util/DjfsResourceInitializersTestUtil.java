package ru.yandex.chemodan.app.djfs.core.test.util;

import ru.yandex.bolts.function.Function1V;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsFileId;
import ru.yandex.chemodan.app.djfs.core.filesystem.model.DjfsResource;
import ru.yandex.misc.lang.StringUtils;

/**
 * @author eoshch
 */
public class DjfsResourceInitializersTestUtil {
    public final Function1V<DjfsResource.Builder> FILEID_FROM_PATH =
            (DjfsResource.Builder x) -> x.fileId(DjfsFileId.cons(StringUtils.leftPad(hexFromPath(x), 64, "0")));

    private static String hexFromPath(DjfsResource.Builder resource) {
        return resource.getPath().getPath().replace("/disk", "").replace("/", "");
    }
}

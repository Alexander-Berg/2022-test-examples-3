package ru.yandex.chemodan.uploader.test;

import ru.yandex.bolts.collection.Option;
import ru.yandex.chemodan.uploader.mulca.MulcaUploadInfo;
import ru.yandex.inside.mulca.MulcaId;

/**
 * @author nshmakov
 */
public class DomainUtils {

    public static MulcaId mid(int id) {
        return MulcaId.valueOf(String.valueOf(id), "");
    }

    public static MulcaUploadInfo uploadInfo(MulcaId fileMid) {
        return new MulcaUploadInfo(fileMid, Option.empty());
    }
}

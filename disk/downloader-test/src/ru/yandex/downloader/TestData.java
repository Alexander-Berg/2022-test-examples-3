package ru.yandex.downloader;

import ru.yandex.inside.mulca.MulcaId;
import ru.yandex.inside.passport.PassportUid;

/**
 * @author akirakozov
 */
public class TestData {
    public static final MulcaId IMAGE_PNG_STID =
            MulcaId.fromSerializedString("1000005.yadisk:173337688.3983296384203508725332872709265");
    public static final MulcaId SMALL_TXT_FILE_STID =
            MulcaId.fromSerializedString("1000005.1.42433480221430165204869206570");

    public static final PassportUid ZIP_TEST_UID = new PassportUid(5181427L);
    public static final String MPFS_HOST= "mpfs01f.dst.yandex.net";

    /**
     * Test passport user
     * login: test-dv
     * passwd: tester1
     */
    public static PassportUid TEST_UID = new PassportUid(4000475747L);
}

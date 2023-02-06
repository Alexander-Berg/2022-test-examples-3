package ru.yandex.direct.core.testing.data;

public class TestMdsConstants {

    public static final int TEST_MDS_GROUP = 1234;
    public static final String TEST_MDS_HOST = "storage-int.mdst.yandex.net";
    public static final String TEST_MDS_PATH = "get-direct-files";
    public static final String TEST_MDS_FILENAME = "567.pdf";

    public static final String TEST_HOST_URL = "http://" + TEST_MDS_HOST + "/";
    public static final String TEST_PATH_URL = TEST_HOST_URL + TEST_MDS_PATH + "/";
    public static final String TEST_FILE_URL = TEST_PATH_URL + TEST_MDS_GROUP + "/" + TEST_MDS_FILENAME;

    private TestMdsConstants() {
    }

}

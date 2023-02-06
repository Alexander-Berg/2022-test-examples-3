package ru.yandex.chemodan.app.docviewer;

import ru.yandex.inside.passport.PassportUidOrZero;

/**
 * @author akirakozov
 */
public class TestUser {
    /**
     * Yandex team user
     * login: akirakozov
     * passwd: ****** ;)
     */
    public static TestUser YA_TEAM_AKIRAKOZOV = new TestUser(
            PassportUidOrZero.fromUid(1120000000000744L),
            "akirakozov");

    /**
     * Production passport user
     * login: disk-docviewer-test-1
     * passwd: docviewer@ya.disk
     */
    public static TestUser PROD = new TestUser(
            PassportUidOrZero.fromUid(134192620),
            "disk-docviewer-test-1");

    /**
     * Test passport user
     * login: test-dv
     * passwd: tester1
     */
    public static TestUser TEST = new TestUser(
            PassportUidOrZero.fromUid(4000475747L),
            "test-dv");

    /**
     * Test passport user
     * login: test.dv2
     * passwd: tester1
     */
    public static TestUser TEST_WITH_NDA = new TestUser(
            PassportUidOrZero.fromUid(4004157675L),
            "test.dv2");



    public final PassportUidOrZero uid;
    public final String login;

    public TestUser(PassportUidOrZero uid, String login) {
        this.uid = uid;
        this.login = login;
    }

    // Files of test passport user (test-dv)
    public static final String DOCX_FILE_URI = "ya-disk:///disk/test_file.docx";
}

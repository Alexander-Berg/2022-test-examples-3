package ru.yandex.chemodan.util.test;

import ru.yandex.chemodan.mpfs.MpfsHid;
import ru.yandex.inside.passport.PassportUid;

/**
 * @author akirakozov
 */
public class TestUser {
    /**
     * Production test user
     * login:    kladun-user
     * password: tester
     */
    public static PassportUid uid = new PassportUid(201515217);

    /**
     * Test passport user
     * login: test-dv
     * passwd: tester1
     */
    public static PassportUid uid2 = new PassportUid(4000475747L);

    /**
     * File /disk/a.txt of user test-dv (uid2)
     */
    public static final MpfsHid TEST_HID = new MpfsHid("aec577379e76c1420d3358b72108f0fd");

    /**
     * Public folder /disk/zipfolderpublictest of user test-dv (uid2) for zip test
     */
    public static final String ZIP_PUBLIC_FOLDER_HASH = "7eDe+JUPR1ZaLWZCuLroX64Ml74WGWrJ4zV7rZFNASo=";
}

package ru.yandex.chemodan.util.test;

import ru.yandex.chemodan.util.oauth.OauthAccessToken;
import ru.yandex.chemodan.util.oauth.OauthClient;
import ru.yandex.chemodan.util.passport.PassportApp;
import ru.yandex.chemodan.util.passport.PassportUser;
import ru.yandex.misc.random.Random2;

/**
 * @author Dmitriy Amelin (lemeh)
 */
public final class TestAppsAndUsers {
    private TestAppsAndUsers() {}

    public static final PassportUser testingUser =
            PassportUser.atTesting(4003323001L, "dataapi", "passw0rd123");

    public static final PassportUser testingUser2 =
            PassportUser.atTesting(4003724959L, "dataapi2", "passw0rd123");

    public static final PassportUser qaUser =
            PassportUser.atProduction(417995041L, "dataapi", "AB1blPgS0t")
                    .withToken("datasync-tests", "bearer", "AQAAAAAY6hkhAANwDPC8C6JPjU1ohNT9Px-0nrk");



    // authorize client url:
    // https://oauth-test.yandex.ru/authorize?response_type=token&client_id=0795482a113042f5b6bfbe77219e83cd
    public static final PassportApp testingApp =
            PassportApp.atTesting("dataapi-tests",
                    "0795482a113042f5b6bfbe77219e83cd",
                    "9a4c3b15756c4bb5b9b36025ee5ade88");

    @SuppressWarnings("unused")
    public static final PassportApp qaApp =
            PassportApp.atProduction("datasync-tests",
                    "f969a148b6224e37b03712ca5a60721c",
                    "fedf3f62f28948f29789b07ef11aac2c");

    public static PassportUser randomTestingUser() {
        String login = "dataapi" + Random2.threadLocal().nextInt(10000);
        OauthAccessToken token
                = new OauthClient(testingApp).getAccessTokenByPassword(login, "passw0rd123");

        return PassportUser
                    .atTesting(token.uid.getUid(), login, "passw0rd123")
                    .withToken(testingApp.name, token.type, token.token);
    }
//
//    public static final ListF<PassportUser> testingUsers =
//            new ClassPathResourceInputStreamSource(TestAppsAndUsers.class, "test_users.txt").
//                    readLines()
//                    .map(line -> {
//                        String[] parts = line.split(",");
//                        return PassportUser.atTesting(
//                                Cf.Long.parse(StringUtils.trim(parts[0])),
//                                StringUtils.trim(parts[1]),
//                                "passw0rd123"
//                        );
//                    })
//                    .plus1(testingUser)
//                    .plus1(testingUser2);
}

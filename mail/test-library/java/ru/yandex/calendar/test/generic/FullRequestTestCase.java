package ru.yandex.calendar.test.generic;

import ru.yandex.bolts.collection.Option;
import ru.yandex.calendar.frontend.web.AuthInfo;

import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.calendar.logic.domain.PassportAuthDomains;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.PassportUidOrZero;
import ru.yandex.misc.env.Environment;

/**
 * A full-request test case that contains:
 * <ul>
 * <li>configuration</li>
 * <li>utc context</li>
 * <li>authentication</li>
 * <li>empty request data</li>
 * </ul>
 * @author ssytnik
 *
 */
public abstract class FullRequestTestCase extends AbstractUtcCtxConfTest {

    @SuppressWarnings("serial")
    protected static class UidLogin extends Tuple2<PassportUid, String> {
        public UidLogin(PassportUid uid, String login) {
            super(uid, login);
        }

        // XXX keep long to shorten initialization. Anyway, this is obsolete. // ssytnik@
        public static UidLogin ul(long uid, String login) {
            return new UidLogin(new PassportUid(uid), login);
        }

        public PassportUid getUid() { return get1(); }
        public String getLogin() { return get2(); }
    }

    // Available auth info (public; yandex-team)
    protected static final Tuple2<UidLogin, UidLogin>
        UI_SSYTNIK       = Tuple2.tuple(UidLogin.ul( 5181427L, "ss-i"         ), UidLogin.ul(1120000000000400L, "ssytnik"   )),
        UI_AKUDRIAV      = Tuple2.tuple(UidLogin.ul( 5453401L, "kudranna"     ), UidLogin.ul(1120000000000791L, "akudriav"  )),
        UI_LEONYA        = Tuple2.tuple(UidLogin.ul(22069275L, "new-direct-ui"), UidLogin.ul(1120000000000324L, "leonya"    )),
        UI_KRASULYA      = Tuple2.tuple(UidLogin.ul(14229568L, "Krassus"      ), UidLogin.ul(1120000000000401L, "krasulya"  )),
        UI_AKIRAKOZOV    = Tuple2.tuple(UidLogin.ul( 1260145L, "hristoforich" ), UidLogin.ul(1120000000000744L, "akirakozov")),
        UI_SANEK_TESTER  = Tuple2.tuple(UidLogin.ul(48797237L, "sanek-tester" ), UidLogin.ul(1120000000000744L, "akirakozov")),
        PASS_TEST_HACK   = Tuple2.tuple(UidLogin.ul(   18619L, "bb-test"      ), UidLogin.ul(           18619L, "bb-test-yt")),
        UI_SSYTNIK_B1MBO0_PDD = Tuple2.tuple(UidLogin.ul(1130000000068228L, "ssytnik"), UidLogin.ul(1130000000068228L, "ssytnik-yt"));
    // Currently used auth info
    protected static final Tuple2<UidLogin, UidLogin> defaultUserInfo =
        Environment.isDeveloperNotebook() ? PASS_TEST_HACK : UI_SSYTNIK
        //Environment.isDeveloperNotebook() ? UI_SSYTNIK_B1MBO0_PDD : UI_SSYTNIK
        //UI_SSYTNIK_B1MBO0_PDD
        ;

    protected UidLogin getUidLogin(Tuple2<UidLogin, UidLogin> userInfo) {
        PassportAuthDomains authDomains = PassportAuthDomains.YT;
        return (PassportAuthDomains.YT == authDomains ? userInfo.get2() : userInfo.get1());
    }

    protected UidLogin getUidLogin() {
        return getUidLogin(defaultUserInfo);
    }

    protected PassportUid getUid() {
        return getUidLogin().getUid();
    }

    protected String getLogin() {
        return getUidLogin().getLogin();
    }

    protected AuthInfo getAuthInfo() {
        return new AuthInfo(PassportUidOrZero.fromUid(getUid()), Option.empty(), Option.empty());
    }
}

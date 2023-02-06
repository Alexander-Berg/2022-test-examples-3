package ru.yandex.calendar.logic.user;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.SetF;
import ru.yandex.calendar.micro.yt.entity.YtUser;
import ru.yandex.calendar.util.idlent.YandexUser;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.login.PassportLogin;
import ru.yandex.misc.email.Email;

class TestUserRegistry {
    private volatile SetF<YandexUser> yandexUsers = Cf.set();
    private volatile SetF<PassportUid> yaMoneyUsers = Cf.set();
    private volatile SetF<YtUser> ytUsers = Cf.set();

    synchronized void register(YandexUser user) {
        yandexUsers = yandexUsers
                .filterNot(u -> u.getUid().sameAs(user.getUid())
                        || u.getLogin().equals(user.getLogin())
                        || u.getEmail().exists(user.getEmail()::isSome))
                .plus(user).unique();
    }

    synchronized void register(YtUser user) {
        ytUsers = ytUsers
                .filterNot(u -> u.getUid().getValue() == user.getUid().getValue()
                        || u.getLogin().equals(user.getLogin()))
                .plus(user).unique();
    }

    synchronized void makeYaMoney(PassportUid uid) {
        yaMoneyUsers = yaMoneyUsers.plus1(uid);
    }

    void clear() {
        yandexUsers = Cf.set();
    }

    Option<YandexUser> getUserByEmail(Email email) {
        return yandexUsers.find(YandexUser.getEmailF().andThenEquals(Option.of(email)));
    }

    Option<PassportUid> getUidByEmail(Email email) {
        return getUserByEmail(email).map(YandexUser.getUidF());
    }

    Option<String> getUserNameByEmail(Email email) {
        final Option<YandexUser> userO = getUserByEmail(email);
        if (userO.isPresent()) {
            return userO.get().getName();
        } else {
            return Option.empty();
        }
    }

    Option<YandexUser> getUserByUid(PassportUid uid) {
        return yandexUsers.find(YandexUser.getUidF().andThenEquals(uid));
    }

    Option<YtUser> getYtUserByUid(long uid) {
        return ytUsers.find(u -> u.getUid().getValue() == uid);
    }

    public Option<PassportUid> getUidByLogin(PassportLogin login) {
        return yandexUsers.find(YandexUser.getLoginF().andThenEquals(login))
                .map(YandexUser.getUidF());
    }

    public Option<PassportLogin> getLoginByUid(PassportUid uid) {
        return yandexUsers.find(YandexUser.getUidF().andThenEquals(uid)).map(YandexUser.getLoginF());
    }

    public boolean isYaMoney(PassportUid uid) {
        return yaMoneyUsers.containsTs(uid);
    }
}

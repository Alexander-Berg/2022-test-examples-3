package ru.yandex.market.pers.notify.mock;

import org.jetbrains.annotations.NotNull;
import ru.yandex.market.pers.notify.passport.PassportService;
import ru.yandex.market.pers.notify.passport.model.UserInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class MockedBlackBoxPassportService implements PassportService {
    private static final ThreadLocalRandom RND = ThreadLocalRandom.current();

    private final Map<String, UserInfoWithEmails> infoByEmail = new HashMap<>();
    private final Map<Long, UserInfoWithEmails> infoByUid = new HashMap<>();
    private final AtomicLong uidGenerator = new AtomicLong(0);

    @Override
    public UserInfo getUserInfo(long uid) {
        return getOrCreateInfoWithEmailsByUid(uid, null, generateEmail()).userInfo;
    }

    @Override
    public synchronized long findUid(String login) {
        UserInfoWithEmails info = infoByEmail.get(login);
        if (info == null) {
            List<String> emails = generateEmails(RND.nextInt(100) + 1);
            emails.set(0, login);
            long uid = uidGenerator.getAndIncrement();
            UserInfo userInfo = generateUserInfo(uid, null, login);
            info = new UserInfoWithEmails(userInfo, emails);
            infoByUid.put(uid, info);
            infoByEmail.put(login, info);
        }
        return info.userInfo.getId();
    }

    @Override
    public Map<String, String> getUserParams(long uid) {
        return Collections.emptyMap();
    }

    @Override
    public Collection<UserInfo> getUsers(Collection<Long> uids) {
        return uids.stream().map(this::getUserInfo).collect(Collectors.toList());
    }

    @Override
    public List<String> getEmails(long uid) {
        return getOrCreateInfoWithEmailsByUid(uid, null, generateEmail()).emails;
    }

    public synchronized void reset() {
        infoByEmail.clear();
        infoByUid.clear();
        uidGenerator.set(0);
    }

    public synchronized void doReturn(long uid, String email) {
        infoByEmail.remove(email);
        infoByUid.remove(uid);
        getOrCreateInfoWithEmailsByUid(uid, null, email);
    }

    public synchronized void doReturn(long uid, String email, String name) {
        infoByEmail.remove(email);
        infoByUid.remove(uid);
        getOrCreateInfoWithEmailsByUid(uid, name, email);
    }

    public synchronized void doReturn(long uid, String email, String name, String login) {
        infoByEmail.remove(email);
        infoByUid.remove(uid);
        getOrCreateInfoWithEmailsByUid(uid, name, login, email);
    }

    public synchronized void doReturn(long uid, List<String> emails) {
        if (emails == null || emails.isEmpty()) {
            returnEmptyForUid(uid);
            return;
        }

        infoByEmail.remove(emails.get(0));
        infoByUid.remove(uid);
        getOrCreateInfoWithEmailsByUid(uid, null, emails);
    }

    public synchronized void returnEmptyForUid(long uid) {
        infoByUid.remove(uid);
        infoByUid.put(uid, new UserInfoWithEmails(generateUserInfo(uid, null, null), Collections.emptyList()));
    }

    @NotNull
    private synchronized UserInfoWithEmails getOrCreateInfoWithEmailsByUid(long uid, String name, String email) {
        return getOrCreateInfoWithEmailsByUid(uid, name, null, email);
    }

    @NotNull
    private synchronized UserInfoWithEmails getOrCreateInfoWithEmailsByUid(long uid, String name, String login, String email) {
        final List<String> emails = generateEmails(RND.nextInt(100) + 2);
        emails.set(0, email);
        return getOrCreateInfoWithEmailsByUid(uid, name, login, emails);
    }

    @NotNull
    private synchronized UserInfoWithEmails getOrCreateInfoWithEmailsByUid(long uid, String name, String login, List<String> emails) {
        UserInfoWithEmails info = infoByUid.get(uid);
        if (info == null) {
            UserInfo userInfo = generateUserInfo(uid, name, emails.get(0), login);
            info = new UserInfoWithEmails(userInfo, emails);
            infoByUid.put(uid, info);
            infoByEmail.put(emails.get(0), info);
        }
        return info;
    }

    @NotNull
    private synchronized UserInfoWithEmails getOrCreateInfoWithEmailsByUid(long uid, String name, List<String> emails) {
        return getOrCreateInfoWithEmailsByUid(uid, name, null, emails);
    }


    private static UserInfo generateUserInfo(long uid, String name, String email) {
        return generateUserInfo(uid, name, email, null);
    }

    private static UserInfo generateUserInfo(long uid, String name, String email, String login) {
        return new UserInfo(uid, name == null ? UUID.randomUUID().toString() : name, email,
            login == null ? UUID.randomUUID().toString() : login, name == null ? "PassportFirstName" : name, name == null ? "PassportLastName" : "");
    }

    private static List<String> generateEmails(int count) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(generateEmail());
        }
        return result;
    }

    private static String generateEmail() {
        return UUID.randomUUID().toString() + "@" + UUID.randomUUID().toString() + ".ru";
    }


    private static class UserInfoWithEmails {
        final UserInfo userInfo;
        final List<String> emails;

        public UserInfoWithEmails(UserInfo userInfo, List<String> emails) {
            this.userInfo = userInfo;
            this.emails = emails;
        }
    }
}

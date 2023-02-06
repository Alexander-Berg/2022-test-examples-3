package ru.yandex.chemodan.app.telemost.mock.blackbox;

import lombok.Data;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxAliases;

@Data
public class UserData {

    public static UserData staff(String login, Option<String> displayName, Option<String> avatarId,
            MapF<Integer, String> attributes) {
        return new UserData(login, displayName, avatarId, Cf.list(BlackboxAliases.YANDEXOID), attributes);
    }

    public static UserData defaultUser(String login, Option<String> displayName, Option<String> avatarUrl,
            MapF<Integer, String> attributes) {
        return new UserData(login, displayName, avatarUrl, Cf.list(), attributes);
    }

    private final String login;

    private final Option<String> displayName;

    private final Option<String> avatarId;

    private final ListF<Integer> aliases;

    private final MapF<Integer, String> attributes;
}

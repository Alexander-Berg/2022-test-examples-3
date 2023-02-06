package ru.yandex.chemodan.app.telemost.mock.blackbox;

import java.util.function.Supplier;

import ru.yandex.bolts.collection.Cf;
import ru.yandex.bolts.collection.Either;
import ru.yandex.bolts.collection.ListF;
import ru.yandex.bolts.collection.MapF;
import ru.yandex.bolts.collection.Option;
import ru.yandex.bolts.collection.Tuple2;
import ru.yandex.bolts.internal.NotImplementedException;
import ru.yandex.inside.passport.PassportDomain;
import ru.yandex.inside.passport.PassportUid;
import ru.yandex.inside.passport.blackbox2.BlackboxQueryable;
import ru.yandex.inside.passport.blackbox2.BlackboxRequestExecutor;
import ru.yandex.inside.passport.blackbox2.protocol.BlackboxMethod;
import ru.yandex.inside.passport.blackbox2.protocol.request.BlackboxMethodParameter;
import ru.yandex.inside.passport.blackbox2.protocol.request.BlackboxRequest;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxAbstractResponse;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxAvatar;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxBulkResponse;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxCorrectResponse;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxDisplayName;
import ru.yandex.inside.passport.blackbox2.protocol.response.BlackboxResponseBuilder;

public class MockBlackboxRequestExecutor extends BlackboxRequestExecutor {

    private final Supplier<MapF<Long, UserData>> userDataProvider;

    public MockBlackboxRequestExecutor(Supplier<MapF<Long, UserData>> userDataProvider) {
        this.userDataProvider = userDataProvider;
    }

    public BlackboxQueryable asQueryable() {
        return new MockBlackboxQueryable(this);
    }

    @Override
    public Either<BlackboxAbstractResponse, BlackboxBulkResponse> execute(BlackboxRequest request) {
        if (request.getMethod() != BlackboxMethod.USER_INFO) {
            throw new NotImplementedException();
        }
        ListF<String> uids = request.getRequestParameters().find(tuple -> BlackboxMethodParameter.UID.toRequestParameterName().equals(tuple._1))
                .map(Tuple2::get2).map(value -> value.split(",")).map(Cf::x).getOrElse(Cf::list);
        if (uids.isEmpty()) {
            return Either.right(new BlackboxBulkResponse(Cf.map()));
        }
        ListF<Integer> attributes = request.getRequestParameters()
                .find(tuple -> BlackboxMethodParameter.ATTRIBUTES.toRequestParameterName().equals(tuple._1))
                .map(Tuple2::get2).map(value -> value.split(",")).map(Cf::x).getOrElse(Cf::list).map(Integer::parseInt);
        MapF<Long, UserData> usersData = userDataProvider.get();
        if (!request.getHeaders().find(header -> header._1.equals(MockBlackboxQueryable.IS_BULK_QUERY))
                .map(Tuple2::get2).map(Boolean::valueOf).getOrElse(Boolean.FALSE)) {
            Option<UserData> userData = usersData.getO(Long.parseLong(uids.first()));
            if (!userData.isPresent()) {
                return Either.left(new BlackboxResponseBuilder(BlackboxMethod.USER_INFO)
                        .setUidDomain(Option.empty())
                        .build());
            }
            return Either.left(buildResponseForUsersData(uids.first(), userData.get(), attributes));
        }
        return Either.right(new BlackboxBulkResponse(uids.map(Long::parseLong)
                .filter(uid -> usersData.containsKeyTs(uid))
                .map(PassportUid::cons)
                .toMapMappingToValue(uid -> buildResponseForUsersData(uid.toString(), usersData.getTs(uid.getUid()), attributes))));
    }

    private BlackboxCorrectResponse buildResponseForUsersData(String uid, UserData userData, ListF<Integer> attributes) {
        return new BlackboxResponseBuilder(BlackboxMethod.USER_INFO)
                .setStatus(-1)
                .setUidDomain(Option.of(new Tuple2<>(PassportUid.cons(Long.parseLong(uid)), PassportDomain.YANDEX_RU)))
                .setAliases(userData.getAliases().toMapMappingToValue(key -> userData.getLogin()))
                .setDisplayName(userData.getDisplayName()
                        .map(displayName -> new BlackboxDisplayName(
                                displayName,
                                Option.empty(),
                                userData.getAvatarId().map(avatarId -> new BlackboxAvatar(avatarId, false)),
                                Option.empty(), Option.of(displayName))))
                .setAttributes(userData.getAttributes().filterKeys(attributes::containsTs))
                .build();
    }
}

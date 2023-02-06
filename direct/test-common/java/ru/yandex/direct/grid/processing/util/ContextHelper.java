package ru.yandex.direct.grid.processing.util;

import java.time.Instant;

import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.data.TestUsers;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.grid.processing.context.container.GridGraphQLContext;
import ru.yandex.direct.grid.processing.model.client.GdClientInfo;
import ru.yandex.direct.regions.Region;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static ru.yandex.direct.grid.core.util.FetchedFieldsResolverCoreUtil.buildFetchedFieldsResolver;
import static ru.yandex.direct.grid.processing.util.CampaignTestDataUtils.defaultGdClientAutoOverdraftInfo;
import static ru.yandex.direct.utils.CommonUtils.ifNotNull;
import static ru.yandex.direct.utils.CommonUtils.isValidId;

public class ContextHelper {

    private static final int DEFAULT_SHARD = 1;

    public static GridGraphQLContext buildDefaultContext() {
        return buildContext(TestUsers.generateNewUser()
                .withUid(RandomNumberUtils.nextPositiveLong())
                .withChiefUid(RandomNumberUtils.nextPositiveLong())
                .withClientId(ClientId.fromLong(RandomNumberUtils.nextPositiveLong()))
        );
    }

    public static GridGraphQLContext buildDefaultContextWithSubjectUser(User subjectUser) {
        return buildContext(TestUsers.generateNewUser()
                        .withUid(RandomNumberUtils.nextPositiveLong())
                        .withChiefUid(RandomNumberUtils.nextPositiveLong())
                        .withClientId(ClientId.fromLong(RandomNumberUtils.nextPositiveLong())),
                subjectUser);
    }

    public static GridGraphQLContext buildContext(User operator) {
        return buildContext(operator, operator);
    }

    public static GridGraphQLContext buildContext(User operator, User subjectUser) {
        return new GridGraphQLContext(operator, subjectUser)
                .withQueriedClient(toGdClientInfo(subjectUser))
                .withInstant(Instant.now())
                .withFetchedFieldsReslover(buildFetchedFieldsResolver(true));
    }

    public static GdClientInfo toGdClientInfo(User user) {
        return new GdClientInfo()
                .withShard(DEFAULT_SHARD)
                .withId(ifNotNull(user.getClientId(), ClientId::asLong))
                .withChiefUserId(user.getChiefUid())
                .withManagerUserId(user.getManagerUserId())
                .withAgencyClientId(isValidId(user.getAgencyClientId()) ? user.getAgencyClientId() : null)
                .withAgencyUserId(user.getAgencyUserId())
                .withCountryRegionId(Region.RUSSIA_REGION_ID)
                .withNonResident(false)
                .withAutoOverdraftInfo(defaultGdClientAutoOverdraftInfo());
    }

}

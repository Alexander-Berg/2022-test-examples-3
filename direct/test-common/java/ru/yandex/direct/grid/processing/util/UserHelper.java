package ru.yandex.direct.grid.processing.util;

import java.math.BigDecimal;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.direct.core.entity.client.model.Client;
import ru.yandex.direct.core.entity.client.model.ClientNds;
import ru.yandex.direct.core.entity.user.model.User;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.currency.Percent;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.rbac.RbacRole;

@ParametersAreNonnullByDefault
public class UserHelper {

    private static final RbacRole DEFAULT_ROLE = RbacRole.CLIENT;

    public static User getUser(KeywordInfo keyword) {
        return getUser(keyword.getAdGroupInfo());
    }

    public static User getUser(AdGroupInfo adGroupInfo) {
        Client client = adGroupInfo.getClientInfo().getClient();
        return getUser(client);
    }

    public static User getUser(Client client) {
        return new User()
                .withUid(client.getChiefUid())
                .withRole(getRole(client.getRole()))
                .withClientId(ClientId.fromLong(client.getId()))
                .withIsReadonlyRep(false)
                .withStatusBlocked(false);
    }

    private static RbacRole getRole(RbacRole role) {
        //DEFAULT_ROLE нужно проставлять, чтобы проходили проверки на права - @PreAuthorizeWrite
        return role != RbacRole.EMPTY ? role : DEFAULT_ROLE;
    }

    public static ClientNds defaultClientNds(Long clientId) {
        return new ClientNds().withClientId(clientId)
                .withNds(Percent.fromPercent(BigDecimal.valueOf(20)));
    }
}

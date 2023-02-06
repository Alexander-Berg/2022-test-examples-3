package ru.yandex.direct.core.testing.steps;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import ru.yandex.direct.core.entity.freelancer.model.ClientAvatar;
import ru.yandex.direct.core.entity.freelancer.model.ClientAvatarsHost;
import ru.yandex.direct.core.entity.freelancer.repository.ClientAvatarRepository;
import ru.yandex.direct.core.testing.info.ClientAvatarInfo;
import ru.yandex.direct.dbutil.sharding.ShardHelper;

import static java.util.Collections.singletonList;
import static ru.yandex.direct.core.testing.data.TestAvatars.defaultAvatarExternalId;

public class AvatarSteps {
    private static final LocalDateTime YESTERDAY = LocalDate.now().minusDays(1).atTime(0, 0);
    private final FreelancerSteps freelancerSteps;
    private final ClientAvatarRepository clientAvatarRepository;
    private final ShardHelper shardHelper;

    public AvatarSteps(FreelancerSteps freelancerSteps,
                       ClientAvatarRepository clientAvatarRepository,
                       ShardHelper shardHelper) {
        this.freelancerSteps = freelancerSteps;
        this.clientAvatarRepository = clientAvatarRepository;
        this.shardHelper = shardHelper;
    }

    public static ClientAvatar defaultClientAvatar() {
        return new ClientAvatar()
                .withHost(ClientAvatarsHost.AVATARS_MDST_YANDEX_NET)
                .withCreateTime(YESTERDAY)
                .withIsDeleted(false);
    }

    public ClientAvatarInfo addDefaultClientAvatar() {
        return createClientAvatar((ClientAvatar) null);
    }

    @SuppressWarnings("WeakerAccess")
    public ClientAvatarInfo createClientAvatar(ClientAvatar clientAvatar) {
        return createClientAvatar(new ClientAvatarInfo().withClientAvatar(clientAvatar));
    }

    @SuppressWarnings("WeakerAccess")
    public ClientAvatarInfo createClientAvatar(ClientAvatarInfo clientAvatarInfo) {
        if (clientAvatarInfo.getFreelancerInfo() == null) {
            clientAvatarInfo.withFreelancerInfo(freelancerSteps.addDefaultFreelancer());
        }
        if (clientAvatarInfo.getClientAvatar() == null) {
            clientAvatarInfo.withClientAvatar(defaultClientAvatar());
        }
        long clientAvatarId = shardHelper.generateClientAvatarIds(1).get(0);
        clientAvatarInfo.getClientAvatar().withClientId(clientAvatarInfo.getFreelancerId())
                .withId(clientAvatarId)
                .withExternalId(defaultAvatarExternalId(clientAvatarId));

        Integer shard = clientAvatarInfo.getFreelancerInfo().getShard();
        ClientAvatar clientAvatar = clientAvatarInfo.getClientAvatar();
        clientAvatarRepository.add(shard, singletonList(clientAvatar));
        List<ClientAvatar> clientAvatars =
                clientAvatarRepository.get(shard, singletonList(clientAvatarInfo.getClientAvatar().getId()));
        clientAvatarInfo.withClientAvatar(clientAvatars.get(0));
        return clientAvatarInfo;
    }
}

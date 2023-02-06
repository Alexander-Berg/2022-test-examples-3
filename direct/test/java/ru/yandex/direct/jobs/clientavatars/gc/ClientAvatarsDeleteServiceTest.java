package ru.yandex.direct.jobs.clientavatars.gc;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.avatars.client.AvatarsClient;
import ru.yandex.direct.avatars.client.model.AvatarId;
import ru.yandex.direct.core.entity.freelancer.model.ClientAvatar;
import ru.yandex.direct.core.entity.freelancer.repository.ClientAvatarRepository;
import ru.yandex.direct.core.entity.freelancer.service.AvatarsClientPool;
import ru.yandex.direct.core.entity.freelancer.service.AvatarsConfigNameConverter;
import ru.yandex.direct.core.testing.info.ClientAvatarInfo;
import ru.yandex.direct.core.testing.steps.AvatarSteps;
import ru.yandex.direct.jobs.configuration.JobsTest;

import static com.google.common.base.Preconditions.checkState;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.testing.data.TestAvatars.defaultAvatarId;
import static ru.yandex.direct.core.testing.steps.AvatarSteps.defaultClientAvatar;

@JobsTest
@ExtendWith(SpringExtension.class)
class ClientAvatarsDeleteServiceTest {
    private static final int AVATARS_EXPIRATION_PERIOD = 24 * 60 * 60;

    @Autowired
    private AvatarSteps avatarSteps;
    @Qualifier("freelancersAvatarsClientPool")
    @Autowired
    private AvatarsClientPool avatarsClientPool;
    @Autowired
    private ClientAvatarRepository clientAvatarRepository;
    @Autowired
    private AvatarsConfigNameConverter avatarsConfigNameConverter;

    /**
     * Тестируем случай, когда аватарка не используется и клиент аватарницы смог её удалить.
     * Сервис должен удалить запись из базы.
     */
    @Test
    void doWork_avatarIsUnused_mdsDelete_thenServiceDelete() {
        AvatarsClientPool mockedClientPool = getAvatarsClientPoolMock(true);
        ClientAvatarsDeleteService clientAvatarsDeleteService = new ClientAvatarsDeleteService(
                clientAvatarRepository, mockedClientPool, avatarsConfigNameConverter);
        ClientAvatarInfo clientAvatarInfo = getClientAvatarInfo(true);
        int shard = clientAvatarInfo.getFreelancerInfo().getShard();
        ClientAvatar initialClientAvatar = clientAvatarInfo.getClientAvatar();

        ClientAvatar actualClientAvatar =
                clientAvatarRepository.get(shard, List.of(initialClientAvatar.getId()))
                .stream().findFirst().orElse(null);
        checkState(initialClientAvatar.equals(actualClientAvatar),
                "Expected client avatar: " + initialClientAvatar + ", but found: " + actualClientAvatar);

        clientAvatarsDeleteService.doWork(shard);
        verify(mockedClientPool.getDefaultClient()).delete(eq(defaultAvatarId(initialClientAvatar.getId())));
        List<ClientAvatar> clientAvatarsAfter =
                clientAvatarRepository.get(shard, List.of(initialClientAvatar.getId()));
        assertThat(clientAvatarsAfter).isEmpty();
    }

    /**
     * Тестируем случай, когда аватарка не используется, но клиент аватарницы не смог её удалить.
     * Сервис должен пометить её как удалённую, но не должен удалять запись из базы.
     */
    @Test
    void doWork_avatarIsUnused_mdsNotDelete_thenServiceNotDelete() {
        AvatarsClientPool mockedClientPool = getAvatarsClientPoolMock(false);
        ClientAvatarsDeleteService clientAvatarsDeleteService = new ClientAvatarsDeleteService(
                clientAvatarRepository, mockedClientPool, avatarsConfigNameConverter);
        ClientAvatarInfo clientAvatarInfo = getClientAvatarInfo(true);
        int shard = clientAvatarInfo.getFreelancerInfo().getShard();
        ClientAvatar initialClientAvatar = clientAvatarInfo.getClientAvatar();

        ClientAvatar actualClientAvatar =
                clientAvatarRepository.get(shard, List.of(initialClientAvatar.getId()))
                        .stream().findFirst().orElse(null);
        checkState(initialClientAvatar.equals(actualClientAvatar),
                "Expected client avatar: " + initialClientAvatar + ", but found: " + actualClientAvatar);

        clientAvatarsDeleteService.doWork(shard);
        verify(mockedClientPool.getDefaultClient()).delete(eq(defaultAvatarId(initialClientAvatar.getId())));
        List<ClientAvatar> clientAvatarsAfter =
                clientAvatarRepository.get(shard, List.of(initialClientAvatar.getId()));
        assertThat(clientAvatarsAfter).isEmpty();   // field IS_DELETED is true, so can't get this avatar from database

        List<ClientAvatar> unusedAvatarsAfter = clientAvatarRepository.getUnused(
                shard,
                avatarsConfigNameConverter.getHost(avatarsClientPool.getDefaultConfigName()),
                AVATARS_EXPIRATION_PERIOD);
        assertThat(unusedAvatarsAfter).contains(initialClientAvatar.withIsDeleted(true));
    }

    /**
     * Тестируем случай, когда аватарка используется.
     * В этом случае клиент аватарницы не должен был вызываться, а запись не должна быть удалена.
     */
    @Test
    void doWork_avatarIsUsed_thenNotDelete() {
        AvatarsClientPool mockedClientPool = getAvatarsClientPoolMock(true);
        ClientAvatarsDeleteService clientAvatarsDeleteService = new ClientAvatarsDeleteService(
                clientAvatarRepository, mockedClientPool, avatarsConfigNameConverter);
        ClientAvatarInfo clientAvatarInfo = getClientAvatarInfo(false);
        int shard = clientAvatarInfo.getFreelancerInfo().getShard();
        ClientAvatar initialClientAvatar = clientAvatarInfo.getClientAvatar();

        ClientAvatar actualClientAvatar =
                clientAvatarRepository.get(shard, List.of(initialClientAvatar.getId()))
                        .stream().findFirst().orElse(null);
        checkState(initialClientAvatar.equals(actualClientAvatar),
                "Expected client avatar: " + initialClientAvatar + ", but found: " + actualClientAvatar);

        clientAvatarsDeleteService.doWork(shard);
        verify(mockedClientPool.getDefaultClient(), never()).delete(any());
        List<ClientAvatar> clientAvatarsAfter =
                clientAvatarRepository.get(shard, List.of(initialClientAvatar.getId()));
        assertThat(clientAvatarsAfter.stream().findFirst().orElse(null))
                .isEqualTo(clientAvatarInfo.getClientAvatar());
    }

    private AvatarsClientPool getAvatarsClientPoolMock(boolean avatarsClientDeleteResult) {
        AvatarsClient avatarsClient = mock(AvatarsClient.class);
        when(avatarsClient.delete(any(AvatarId.class))).thenReturn(avatarsClientDeleteResult);
        AvatarsClientPool mockedClientPool = mock(AvatarsClientPool.class);
        when(mockedClientPool.getDefaultClient()).thenReturn(avatarsClient);
        when(mockedClientPool.getDefaultConfigName()).thenReturn(avatarsClientPool.getDefaultConfigName());
        return mockedClientPool;
    }

    private ClientAvatarInfo getClientAvatarInfo(boolean isUnused) {
        if (isUnused) {
            return avatarSteps.addDefaultClientAvatar();
        } else {
            return avatarSteps.createClientAvatar(
                    defaultClientAvatar().withCreateTime(LocalDateTime.now().minusMinutes(1)));
        }
    }
}

package ru.yandex.direct.core.entity.freelancer.service;

import java.util.List;
import java.util.UUID;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.avatars.client.AvatarsClient;
import ru.yandex.direct.avatars.client.exception.AvatarsClientCommonException;
import ru.yandex.direct.avatars.client.model.AvatarId;
import ru.yandex.direct.core.entity.freelancer.model.ClientAvatar;
import ru.yandex.direct.core.entity.freelancer.model.Freelancer;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerCard;
import ru.yandex.direct.core.entity.freelancer.model.FreelancersCardStatusModerate;
import ru.yandex.direct.core.entity.freelancer.repository.ClientAvatarRepository;
import ru.yandex.direct.core.entity.freelancer.repository.FreelancerCardRepository;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.info.FreelancerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.ClientsAvatarsHostConfigName;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.result.ResultState;
import ru.yandex.direct.validation.result.Defect;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.direct.core.entity.freelancer.service.validation.AvatarsDefects.unknownError;
import static ru.yandex.direct.core.entity.freelancer.service.validation.FreelancerDefects.mustHaveFreelancerCard;
import static ru.yandex.direct.core.testing.data.TestFreelancers.defaultFreelancer;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FreelancerAvatarsClientServiceTest {
    private static final String FAKE_NAMESPACE = "FAKE-NAMESPACE";
    private static final Integer FAKE_GROUP_ID = 1;
    private static final byte[] IMAGE_BODY = new byte[0];
    private static final String CONFIG_NAME = ClientsAvatarsHostConfigName.avatars_mdst_yandex_net.getLiteral();
    @Autowired
    public ShardHelper shardHelper;
    @Autowired
    public FreelancerCardRepository freelancerCardRepository;
    @Autowired
    public FreelancerCardService freelancerCardUpdateService;
    @Autowired
    public ClientAvatarRepository clientAvatarRepository;
    @Autowired
    public AvatarsConfigNameConverter avatarsConfigNameConverter;
    @Autowired
    Steps steps;
    private FreelancerInfo freelancerInfo;
    private AvatarsClient mockAvatarsClient;
    private FreelancerClientAvatarService freelancerClientAvatarService;
    private ClientId freelancerId;

    @Before
    public void setUp() throws Exception {
        freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
        mockAvatarsClient = getMockAvatarsClient();
        freelancerClientAvatarService = getClientAvatarService(mockAvatarsClient);
        freelancerId = freelancerInfo.getClientId();
    }

    private AvatarsClient getMockAvatarsClient() {
        AvatarsClient avatarsClient = mock(AvatarsClient.class);
        when(avatarsClient.upload(any())).then(l ->
        {
            String key = UUID.randomUUID().toString();
            return new AvatarId(FAKE_NAMESPACE, FAKE_GROUP_ID, key);
        });
        return avatarsClient;
    }

    private FreelancerClientAvatarService getClientAvatarService(AvatarsClient avatarsClient) {
        AvatarsClientPool avatarsClientPool = mock(AvatarsClientPool.class);
        when(avatarsClientPool.getDefaultClient()).thenReturn(avatarsClient);
        when(avatarsClientPool.getDefaultConfigName()).thenReturn(CONFIG_NAME);
        return new FreelancerClientAvatarService(
                shardHelper,
                freelancerCardRepository,
                freelancerCardUpdateService,
                clientAvatarRepository,
                avatarsClientPool,
                avatarsConfigNameConverter);
    }


    @Test
    public void updateAvatar_success() {
        freelancerClientAvatarService.updateAvatar(freelancerId, IMAGE_BODY);
        ClientAvatar firstClientAvatar = getClientAvatar(freelancerInfo);
        freelancerClientAvatarService.updateAvatar(freelancerId, IMAGE_BODY);
        ClientAvatar secondClientAvatar = getClientAvatar(freelancerInfo);
        assertThat(secondClientAvatar.getExternalId()).isNotEqualTo(firstClientAvatar.getExternalId());
    }

    @Test
    public void saveAvatar_success() {
        freelancerClientAvatarService.updateAvatar(freelancerId, IMAGE_BODY);
        ClientAvatar firstClientAvatar = getClientAvatar(freelancerInfo);

        freelancerClientAvatarService.saveAvatar(freelancerId.asLong(), IMAGE_BODY);
        ClientAvatar secondClientAvatar = getClientAvatar(freelancerInfo);

        assertThat(secondClientAvatar.getExternalId()).isEqualTo(firstClientAvatar.getExternalId());
    }

    @Test
    public void updateAvatar_retainOldAvatar() {
        freelancerClientAvatarService.updateAvatar(freelancerId, IMAGE_BODY);
        FreelancerCard card =
                freelancerCardUpdateService.getNewestFreelancerCards(singletonList(freelancerId.asLong())).get(0);
        // Модерируем карточку с обновлённой аватаркой
        FreelancerCard moderatedCard = card.withStatusModerate(FreelancersCardStatusModerate.ACCEPTED);
        freelancerCardUpdateService.applyModerationResult(moderatedCard);
        // На публичной карточке теперь точно есть аватарка
        ClientAvatar publicAvatar = getClientPublicAvatar(freelancerInfo);
        assumeThat(publicAvatar, notNullValue());

        freelancerClientAvatarService.updateAvatar(freelancerId, IMAGE_BODY);
        // Проверяем, что после обновления аватари не затёрлась аватарка на принятой карточке
        ClientAvatar publicAvatarAfterUpdate = getClientPublicAvatar(freelancerInfo);
        assertThat(publicAvatarAfterUpdate).isNotNull();
    }

    private ClientAvatar getClientAvatar(FreelancerInfo freelancerInfo) {
        Integer shard = freelancerInfo.getShard();
        ClientId freelancerId = freelancerInfo.getClientId();
        long freelancerIdAsLong = freelancerId.asLong();
        List<FreelancerCard> newestFreelancerCards = freelancerCardRepository
                .getNewestFreelancerCard(shard, singletonList(freelancerIdAsLong));
        checkState(!newestFreelancerCards.isEmpty(), "FreelancerCard wasn't found for freelancerId=%s", freelancerId);
        FreelancerCard freelancerCard = newestFreelancerCards.get(0);
        Long avatarId = freelancerCard.getAvatarId();
        ClientAvatar clientAvatar = clientAvatarRepository
                .get(shard, singletonList(avatarId))
                .get(0);
        checkState(clientAvatar.getClientId().equals(freelancerIdAsLong));
        return clientAvatar;
    }

    private ClientAvatar getClientPublicAvatar(FreelancerInfo freelancerInfo) {
        Integer shard = freelancerInfo.getShard();
        ClientId clientId = freelancerInfo.getClientId();

        long freelancerId = clientId.asLong();
        FreelancerCard publicCard =
                freelancerCardRepository.getAcceptedCardsByFreelancerIds(shard, singletonList(freelancerId))
                        .get(freelancerId);
        checkNotNull(publicCard, "No accepted card for freelancer %s", freelancerId);

        Long avatarId = publicCard.getAvatarId();
        List<ClientAvatar> foundAvatars = clientAvatarRepository.get(shard, singletonList(avatarId));
        if (foundAvatars.isEmpty()) {
            return null;
        }
        return foundAvatars.get(0);
    }

    @Test
    public void updateAvatar_ThrowInAvatarClient_BrokenResult() {
        RuntimeException innerException = new RuntimeException();
        AvatarsClientCommonException avatarsClientCommonException = new AvatarsClientCommonException(innerException);
        when(mockAvatarsClient.upload(any())).thenThrow(avatarsClientCommonException);

        Result<Long> result = freelancerClientAvatarService.updateAvatar(freelancerId, IMAGE_BODY);
        ResultState resultState = result.getState();
        Defect resultDefect = result.getErrors().get(0).getDefect();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(resultState).describedAs("resultState").isEqualTo(ResultState.BROKEN);
            softly.assertThat(resultDefect).describedAs("resultDefect").isEqualTo(unknownError());
        });
    }

    @Test
    public void updateAvatar_FreelancerHasNoCard_BrokenResult() {
        ClientInfo clientInfo = steps.clientSteps().createDefaultClient();
        ClientId clientId = clientInfo.getClientId();
        long freelancerId = clientId.asLong();
        Freelancer freelancerWithoutCard = defaultFreelancer(freelancerId)
                .withCard(null);
        FreelancerInfo fInfoNoCard = new FreelancerInfo()
                .withClientInfo(clientInfo)
                .withFreelancer(freelancerWithoutCard);
        steps.freelancerSteps().createFreelancer(fInfoNoCard);
        Result<Long> result = freelancerClientAvatarService.updateAvatar(clientId, IMAGE_BODY);
        ResultState resultState = result.getState();
        Defect resultDefect = result.getErrors().get(0).getDefect();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(resultState).describedAs("resultState").isEqualTo(ResultState.BROKEN);
            softly.assertThat(resultDefect).describedAs("resultDefect").isEqualTo(mustHaveFreelancerCard());
        });
    }

}

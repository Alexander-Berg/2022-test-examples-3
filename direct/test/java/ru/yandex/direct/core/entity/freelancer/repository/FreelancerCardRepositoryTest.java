package ru.yandex.direct.core.entity.freelancer.repository;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.freelancer.model.Freelancer;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerCard;
import ru.yandex.direct.core.entity.freelancer.model.FreelancersCardStatusModerate;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestFreelancers;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;


@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FreelancerCardRepositoryTest {

    public static final String SECOND_CARD_BRIEF = "SecondCardDescription";
    @Autowired
    Steps steps;
    @Autowired
    FreelancerRepository freelancerRepository;
    @Autowired
    FreelancerCardRepository freelancerCardRepository;

    private ClientInfo clientInfo;

    @Before
    public void setUp() throws Exception {
        clientInfo = steps.clientSteps().createDefaultClient();
    }

    @Test
    public void get_success_whenTwoCardsAcceptedAndThirdInDraftStatus() {
        Long freelancerId = clientInfo.getClientId().asLong();
        Freelancer freelancer =
                TestFreelancers.defaultFreelancer(freelancerId);
        checkState(freelancer.getCard().getStatusModerate().equals(FreelancersCardStatusModerate.ACCEPTED));
        int shard = clientInfo.getShard();
        freelancerRepository.addFreelancers(shard, singletonList(freelancer));
        FreelancerCard secondCard =
                TestFreelancers.defaultFreelancerCard(freelancerId)
                        .withBriefInfo(SECOND_CARD_BRIEF);
        checkState(secondCard.getStatusModerate().equals(FreelancersCardStatusModerate.ACCEPTED));
        freelancerCardRepository.addFreelancerCards(shard, singletonList(secondCard));
        FreelancerCard thirdCard =
                TestFreelancers.defaultFreelancerCard(freelancerId)
                        .withStatusModerate(FreelancersCardStatusModerate.DRAFT);
        freelancerCardRepository.addFreelancerCards(shard, singletonList(thirdCard));
        Map<Long, FreelancerCard> acceptedCardsByFreelancerIds =
                freelancerCardRepository.getAcceptedCardsByFreelancerIds(shard, singleton(freelancerId));
        FreelancerCard freelancerCard = acceptedCardsByFreelancerIds.get(freelancerId);
        String briefInfo = freelancerCard.getBriefInfo();
        assertThat(briefInfo).isEqualTo(SECOND_CARD_BRIEF);
    }

}

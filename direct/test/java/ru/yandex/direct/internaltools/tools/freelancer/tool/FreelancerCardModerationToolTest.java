package ru.yandex.direct.internaltools.tools.freelancer.tool;

import java.util.EnumSet;
import java.util.List;

import com.google.common.collect.Iterables;
import org.assertj.core.api.Assertions;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.freelancer.model.FreelancerCard;
import ru.yandex.direct.core.entity.freelancer.model.FreelancersCardDeclineReason;
import ru.yandex.direct.core.entity.freelancer.model.FreelancersCardStatusModerate;
import ru.yandex.direct.core.entity.freelancer.service.FreelancerCardService;
import ru.yandex.direct.core.testing.info.FreelancerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.internaltools.configuration.InternalToolsTest;
import ru.yandex.direct.internaltools.tools.freelancer.model.CardModerationParameters;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;


@InternalToolsTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FreelancerCardModerationToolTest {
    @Autowired
    Steps steps;
    @Autowired
    ShardHelper shardHelper;
    @Autowired
    FreelancerCardService freelancerCardService;

    @Autowired
    FreelancerCardModerationTool testedTool;

    private FreelancerInfo freelancerInfo;
    private FreelancerCard cardToModerate;

    @Before
    public void setUp() {
        freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
        freelancerCardService.addChangedFreelancerCard(freelancerInfo.getClientId(),
                new FreelancerCard().withBriefInfo("brand new brief"));

        cardToModerate = getNewestFreelancerCard();
        assumeThat(cardToModerate, Matchers.notNullValue());
        //noinspection ConstantConditions
        assumeThat(cardToModerate.getStatusModerate(), equalTo(FreelancersCardStatusModerate.DRAFT));
    }

    private FreelancerCard getNewestFreelancerCard() {
        List<FreelancerCard> newestFreelancerCard =
                freelancerCardService.getNewestFreelancerCards(singletonList(freelancerInfo.getFreelancerId()));
        return Iterables.getFirst(newestFreelancerCard, null);
    }

    @Test
    public void moderateCard_declined() {
        CardModerationParameters params = new CardModerationParameters();
        params.setCardId(cardToModerate.getId());
        params.setFreelancerId(cardToModerate.getFreelancerId());
        params.setModerationResult(FreelancersCardStatusModerate.DECLINED.name());
        params.setDeclineReason(EnumSet.allOf(FreelancersCardDeclineReason.class));

        Assertions.assertThatCode(() -> testedTool.getMassData(params))
                .doesNotThrowAnyException();

        FreelancerCard card = getNewestFreelancerCard();
        assertSoftly(softly -> {
            softly.assertThat(card.getStatusModerate())
                    .describedAs("statusModerate")
                    .isEqualTo(FreelancersCardStatusModerate.DECLINED);
            softly.assertThat(card.getDeclineReason())
                    .describedAs("declineReason")
                    .containsExactlyInAnyOrder(FreelancersCardDeclineReason.values());
        });
    }

    @Test
    public void moderateCard_accepted() {
        CardModerationParameters params = new CardModerationParameters();
        params.setCardId(cardToModerate.getId());
        params.setFreelancerId(cardToModerate.getFreelancerId());
        params.setModerationResult(FreelancersCardStatusModerate.ACCEPTED.name());

        Assertions.assertThatCode(() -> testedTool.getMassData(params))
                .doesNotThrowAnyException();

        FreelancerCard card = getNewestFreelancerCard();
        assertSoftly(softly -> {
            softly.assertThat(card.getStatusModerate())
                    .describedAs("statusModerate")
                    .isEqualTo(FreelancersCardStatusModerate.ACCEPTED);
            softly.assertThat(card.getDeclineReason())
                    .describedAs("declineReason")
                    .isEmpty();
        });
    }

}

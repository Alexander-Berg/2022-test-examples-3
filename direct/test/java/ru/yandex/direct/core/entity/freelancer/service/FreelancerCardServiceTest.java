package ru.yandex.direct.core.entity.freelancer.service;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.freelancer.model.FreelancerCard;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerCardModeration;
import ru.yandex.direct.core.entity.freelancer.model.FreelancerContacts;
import ru.yandex.direct.core.entity.freelancer.model.FreelancersCardDeclineReason;
import ru.yandex.direct.core.entity.freelancer.model.FreelancersCardStatusModerate;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.FreelancerInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;
import ru.yandex.direct.dbutil.sharding.ShardHelper;
import ru.yandex.direct.result.Result;
import ru.yandex.direct.validation.defect.CommonDefects;

import static com.google.common.base.Preconditions.checkState;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.freelancer.model.FreelancersCardStatusModerate.ACCEPTED;
import static ru.yandex.direct.core.entity.freelancer.model.FreelancersCardStatusModerate.DECLINED;
import static ru.yandex.direct.core.entity.freelancer.model.FreelancersCardStatusModerate.DRAFT;
import static ru.yandex.direct.core.testing.data.TestFreelancers.defaultFreelancerCard;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.test.utils.assertj.Conditions.matchedBy;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasDefectWithDefinition;
import static ru.yandex.direct.testing.matchers.validation.Matchers.hasNoErrors;
import static ru.yandex.direct.testing.matchers.validation.Matchers.validationError;
import static ru.yandex.direct.validation.result.PathHelper.field;
import static ru.yandex.direct.validation.result.PathHelper.path;


@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)

public class FreelancerCardServiceTest {

    private static final String SITE_URL_WITHOUT_PROTOCOL = "ya.ru";
    private static final String WRONG_EMAIL = "wrong_email";
    private static final String ADDED_PROTOCOL = "http://";
    @Autowired
    Steps steps;

    @Autowired
    FreelancerCardService freelancerCardService;

    @Autowired
    ShardHelper shardHelper;

    private FreelancerCard getNewFreelancerCard(Long freelancerId) {
        return new FreelancerCard()
                .withFreelancerId(freelancerId)
                .withAvatarId(888L)
                .withBriefInfo("new_brief")
                .withContacts(new FreelancerContacts()
                        .withEmail("new_email@ya.ru")
                        .withSiteUrl("http://news.yandex.ua")
                        .withPhone("+7 495 123-45-67"));
    }

    @Test
    public void addChangedFreelancerCard_success_newModerationData() {
        //Подготавливаем исходные данные
        FreelancerInfo freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
        ClientId clientId = freelancerInfo.getClientId();
        Long freelancerId = freelancerInfo.getFreelancer().getId();

        //Ожидаемый результат
        FreelancerCard expectedFreelancerCard = getNewFreelancerCard(freelancerId)
                .withStatusModerate(DRAFT);

        //Сохраняем новую карточку
        FreelancerCard newFreelancerCard = getNewFreelancerCard(freelancerId);
        Result<Long> result = freelancerCardService.addChangedFreelancerCard(clientId, newFreelancerCard);
        Long newFreelancerCardId = result.getResult();

        //Сверяем ожидания и реальность
        FreelancerCard readFreelancerCard = freelancerCardService.getFreelancerCard(clientId, newFreelancerCardId);
        assertThat(readFreelancerCard)
                .is(matchedBy(beanDiffer(expectedFreelancerCard).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addChangedFreelancerCard_success_sameModerationData() {
        //Подготавливаем исходные данные
        FreelancerInfo freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
        ClientId clientId = freelancerInfo.getClientId();
        Long freelancerId = freelancerInfo.getFreelancer().getId();

        //Ожидаемый результат
        FreelancerCard expectedFreelancerCard = defaultFreelancerCard(freelancerId)
                .withStatusModerate(ACCEPTED);

        //Сохраняем новую карточку
        FreelancerCard newFreelancerCard = defaultFreelancerCard(freelancerId);
        Result<Long> result = freelancerCardService.addChangedFreelancerCard(clientId, newFreelancerCard);
        Long newFreelancerCardId = result.getResult();

        //Сверяем ожидания и реальность
        FreelancerCard readFreelancerCard = freelancerCardService.getFreelancerCard(clientId, newFreelancerCardId);
        assertThat(readFreelancerCard)
                .is(matchedBy(beanDiffer(expectedFreelancerCard).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addChangedFreelancerCard_SiteUrlWithoutProtocol_Fixed() {
        FreelancerInfo freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
        ClientId clientId = freelancerInfo.getClientId();
        Long freelancerId = freelancerInfo.getFreelancer().getId();
        FreelancerCard newFreelancerCard = getNewFreelancerCard(freelancerId);
        newFreelancerCard.getContacts().setSiteUrl(SITE_URL_WITHOUT_PROTOCOL);
        Result<Long> result = freelancerCardService.addChangedFreelancerCard(clientId, newFreelancerCard);
        Long newFreelancerCardId = result.getResult();
        FreelancerCard readFreelancerCard = freelancerCardService.getFreelancerCard(clientId, newFreelancerCardId);
        FreelancerCard expectedFreelancerCard = getNewFreelancerCard(freelancerId);
        FreelancerContacts expectedContacts = expectedFreelancerCard.getContacts();
        String expectedUrl = ADDED_PROTOCOL + SITE_URL_WITHOUT_PROTOCOL;
        expectedContacts.withSiteUrl(expectedUrl);
        assertThat(readFreelancerCard)
                .is(matchedBy(beanDiffer(expectedFreelancerCard).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void addChangedFreelancerCard_fail_onInvalidEmailValidation() {
        FreelancerInfo freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
        ClientId clientId = freelancerInfo.getClientId();
        Long freelancerId = freelancerInfo.getFreelancer().getId();
        FreelancerCard newFreelancerCard = getNewFreelancerCard(freelancerId);
        newFreelancerCard.getContacts().withEmail(WRONG_EMAIL);
        Result<Long> result = freelancerCardService.addChangedFreelancerCard(clientId, newFreelancerCard);
        assertThat(result.getValidationResult())
                .is(matchedBy(hasDefectWithDefinition(validationError(
                        path(field(FreelancerCard.CONTACTS), field(FreelancerContacts.EMAIL)),
                        CommonDefects.invalidValue()
                ))));
    }

    @Test
    public void getNewestFreelancerCards_success() {
        FreelancerInfo freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
        Long freelancerId = freelancerInfo.getFreelancer().getId();
        String newBrif = "getNewestFreelancerCards_success test";
        FreelancerCard newFreelancerCard = getNewFreelancerCard(freelancerId)
                .withBriefInfo(newBrif);
        freelancerCardService.addChangedFreelancerCard(freelancerInfo.getClientId(), newFreelancerCard);
        List<FreelancerCard> newestFreelancerCards =
                freelancerCardService.getNewestFreelancerCards(singletonList(freelancerId));
        checkState(!newestFreelancerCards.isEmpty(), "Не найдена карта фрилансера");
        FreelancerCard readFreelancerCard = newestFreelancerCards.get(0);
        FreelancerCard expectedFreelancerCard = getNewFreelancerCard(freelancerId)
                .withBriefInfo(newBrif)
                .withStatusModerate(FreelancersCardStatusModerate.DRAFT);
        assertThat(readFreelancerCard)
                .is(matchedBy(beanDiffer(expectedFreelancerCard).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void applyModerationResult_accept_success() {
        FreelancerInfo freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
        ClientId clientId = freelancerInfo.getClientId();
        Long freelancerId = clientId.asLong();
        List<FreelancerCard> freelancerCards =
                freelancerCardService.getNewestFreelancerCards(singletonList(freelancerId));
        checkState(!freelancerCards.isEmpty(), "Не найдена карта фрилансера");
        FreelancerCard srcFreelancerCard = freelancerCards.get(0);
        checkState(!srcFreelancerCard.getIsArchived(),
                "Ожидается, что дефолтная карточки фрилансера создаётся не архивной");
        Long firstFreelancerCardId = srcFreelancerCard.getId();
        FreelancerCard newFreelancerCard = getNewFreelancerCard(freelancerId);
        Result<Long> addResult =
                freelancerCardService.addChangedFreelancerCard(clientId, newFreelancerCard);
        Long secondFreelancerCardId = addResult.getResult();
        FreelancerCardModeration moderationResult = new FreelancerCard()
                .withId(secondFreelancerCardId)
                .withFreelancerId(freelancerId)
                .withStatusModerate(ACCEPTED)
                .withDeclineReason(emptySet());
        freelancerCardService.applyModerationResult(moderationResult);
        FreelancerCard firstFreelancerCard = freelancerCardService.getFreelancerCard(clientId, firstFreelancerCardId);
        FreelancerCard firstExpected = defaultFreelancerCard(freelancerId)
                .withIsArchived(true);
        FreelancerCard secondFreelancerCard = freelancerCardService.getFreelancerCard(clientId, secondFreelancerCardId);
        FreelancerCard secondExpected = getNewFreelancerCard(freelancerId)
                .withStatusModerate(ACCEPTED)
                .withIsArchived(false);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(firstFreelancerCard)
                    .is(matchedBy(beanDiffer(firstExpected).useCompareStrategy(onlyExpectedFields())));
            softly.assertThat(secondFreelancerCard)
                    .is(matchedBy(beanDiffer(secondExpected).useCompareStrategy(onlyExpectedFields())));
        });
    }

    @Test
    public void applyModerationResult_decline_success() {
        FreelancerInfo freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
        ClientId clientId = freelancerInfo.getClientId();
        Long freelancerId = clientId.asLong();
        List<FreelancerCard> freelancerCards =
                freelancerCardService.getNewestFreelancerCards(singletonList(freelancerId));
        checkState(!freelancerCards.isEmpty(), "Не найдена карта фрилансера");
        FreelancerCard srcFreelancerCard = freelancerCards.get(0);
        checkState(!srcFreelancerCard.getIsArchived(),
                "Ожидается, что дефолтная карточки фрилансера создаётся не архивной");
        Long firstFreelancerCardId = srcFreelancerCard.getId();
        FreelancerCard newFreelancerCard = getNewFreelancerCard(freelancerId);
        Result<Long> addResult =
                freelancerCardService.addChangedFreelancerCard(clientId, newFreelancerCard);
        Long secondFreelancerCardId = addResult.getResult();
        Set<FreelancersCardDeclineReason> declineReasons = new HashSet<>();
        declineReasons.add(FreelancersCardDeclineReason.BAD_IMAGE);
        declineReasons.add(FreelancersCardDeclineReason.BAD_DESCRIPTION);
        declineReasons.add(FreelancersCardDeclineReason.BAD_HREF);
        FreelancerCardModeration moderationResult = new FreelancerCard()
                .withId(secondFreelancerCardId)
                .withFreelancerId(freelancerId)
                .withStatusModerate(DECLINED)
                .withDeclineReason(declineReasons);
        freelancerCardService.applyModerationResult(moderationResult);
        FreelancerCard firstFreelancerCard = freelancerCardService.getFreelancerCard(clientId, firstFreelancerCardId);
        FreelancerCard firstExpected = defaultFreelancerCard(freelancerId)
                .withIsArchived(false);
        FreelancerCard secondFreelancerCard = freelancerCardService.getFreelancerCard(clientId, secondFreelancerCardId);
        FreelancerCard secondExpected = getNewFreelancerCard(freelancerId)
                .withStatusModerate(DECLINED)
                .withIsArchived(false)
                .withDeclineReason(declineReasons);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(firstFreelancerCard)
                    .is(matchedBy(beanDiffer(firstExpected).useCompareStrategy(onlyExpectedFields())));
            softly.assertThat(secondFreelancerCard)
                    .is(matchedBy(beanDiffer(secondExpected).useCompareStrategy(onlyExpectedFields())));
        });
    }

    @Test
    public void moderateCard_correctState_whenDeclineAndThenAccept() {
        FreelancerInfo freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
        ClientId clientId = freelancerInfo.getClientId();
        Long freelancerId = clientId.asLong();

        FreelancerCard newFreelancerCard = getNewFreelancerCard(freelancerId);
        Result<Long> addResult = freelancerCardService.addChangedFreelancerCard(clientId, newFreelancerCard);
        Long secondFreelancerCardId = addResult.getResult();

        // Decline
        FreelancerCardModeration moderationResultByAccessor = new FreelancerCard()
                .withId(secondFreelancerCardId)
                .withFreelancerId(freelancerId)
                .withStatusModerate(DECLINED)
                .withDeclineReason(EnumSet.allOf(FreelancersCardDeclineReason.class));
        Result<Long> firstResult = freelancerCardService.applyModerationResult(moderationResultByAccessor);
        assumeThat(firstResult.getValidationResult(), hasNoErrors());

        // Accept
        FreelancerCardModeration moderationResultByManager = new FreelancerCard()
                .withId(secondFreelancerCardId)
                .withFreelancerId(freelancerId)
                .withStatusModerate(ACCEPTED)
                .withDeclineReason(emptySet());
        Result<Long> secondResult = freelancerCardService.applyModerationResult(moderationResultByManager);
        assumeThat(secondResult.getValidationResult(), hasNoErrors());

        FreelancerCard actualCard = freelancerCardService.getFreelancerCard(clientId, secondFreelancerCardId);
        FreelancerCard expectedCard = getNewFreelancerCard(freelancerId)
                .withStatusModerate(ACCEPTED)
                .withIsArchived(false)
                .withDeclineReason(emptySet());
        assertThat(actualCard).is(matchedBy(beanDiffer(expectedCard).useCompareStrategy(onlyExpectedFields())));
    }

    @Test
    public void moderateCard_correctState_whenAcceptThenDecline() {
        FreelancerInfo freelancerInfo = steps.freelancerSteps().addDefaultFreelancer();
        ClientId clientId = freelancerInfo.getClientId();
        Long freelancerId = clientId.asLong();

        FreelancerCard secondFreelancerCard = getNewFreelancerCard(freelancerId);
        Result<Long> secondAddResult =
                freelancerCardService.addChangedFreelancerCard(clientId, secondFreelancerCard);
        Long secondFreelancerCardId = secondAddResult.getResult();

        FreelancerCardModeration secondModerationResultByAccessor = new FreelancerCard()
                .withId(secondFreelancerCardId)
                .withFreelancerId(freelancerId)
                .withStatusModerate(ACCEPTED)
                .withDeclineReason(emptySet());
        freelancerCardService.applyModerationResult(secondModerationResultByAccessor);

        FreelancerCardModeration secondModerationResultByManager = new FreelancerCard()
                .withId(secondFreelancerCardId)
                .withFreelancerId(freelancerId)
                .withStatusModerate(DECLINED)
                .withDeclineReason(EnumSet.allOf(FreelancersCardDeclineReason.class));
        freelancerCardService.applyModerationResult(secondModerationResultByManager);

        FreelancerCard expectedCard = defaultFreelancerCard(freelancerId).withIsArchived(false);
        Long firstFreelancerCardId = freelancerInfo.getFreelancer().getCard().getId();
        FreelancerCard actualCard = freelancerCardService.getFreelancerCard(clientId, firstFreelancerCardId);
        assertThat(actualCard).is(matchedBy(beanDiffer(expectedCard).useCompareStrategy(onlyExpectedFields())));
    }
}

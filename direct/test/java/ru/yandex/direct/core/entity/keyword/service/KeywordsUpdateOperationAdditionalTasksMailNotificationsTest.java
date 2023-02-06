package ru.yandex.direct.core.entity.keyword.service;

import java.math.BigDecimal;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.entity.keyword.container.UpdatedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.mailnotification.model.EventType;
import ru.yandex.direct.core.entity.mailnotification.model.MailNotificationEvent;
import ru.yandex.direct.core.entity.mailnotification.model.ObjectType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.repository.TestMailNotificationEventRepository;
import ru.yandex.direct.model.ModelChanges;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertThat;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyExpectedFields;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isNotUpdated;
import static ru.yandex.direct.core.entity.keyword.service.UpdateKeywordMatchers.isUpdated;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsUpdateOperationAdditionalTasksMailNotificationsTest extends KeywordsUpdateOperationBaseTest {

    @Autowired
    private TestMailNotificationEventRepository testEventRepository;

    @Before
    @Override
    public void before() {
        super.before();
        operatorClientInfo = steps.clientSteps().createDefaultClientWithRole(RbacRole.MANAGER);
        clientInfo = steps.clientSteps().createDefaultClientUnderManager(operatorClientInfo);
    }

    @Test
    public void execute_NoChange_EventNotAdded() {
        createOneActiveAdGroup(operatorClientInfo);
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, PHRASE_1).getId();

        ModelChanges<Keyword> changesKeyword = keywordModelChanges(keywordIdToUpdate, PHRASE_1);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeyword));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, PHRASE_1)));

        List<MailNotificationEvent> actualEvents =
                testEventRepository.getEventsByOwnerUids(clientInfo.getShard(), singletonList(clientInfo.getUid()));
        assertThat("Нотификации должны отстуствовать", actualEvents, empty());
    }

    @Test
    public void execute_OperatorIsClient_EventNotAdded() {
        operatorClientInfo = clientInfo = steps.clientSteps().createDefaultClient();
        createOneActiveAdGroup();
        KeywordInfo keywordInfoToUpdate = createKeyword(adGroupInfo1, PHRASE_1);

        ModelChanges<Keyword> changesKeyword = keywordModelChanges(keywordInfoToUpdate.getId(), PHRASE_2);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeyword));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordInfoToUpdate.getId(), PHRASE_2)));

        List<MailNotificationEvent> actualEvents =
                testEventRepository.getEventsByOwnerUids(clientInfo.getShard(), singletonList(clientInfo.getUid()));
        assertThat("Нотификации должны отстуствовать", actualEvents, empty());
    }

    @Test
    public void execute_ChangePhrase_EventAddedCorrectly() {
        createOneActiveAdGroup(operatorClientInfo);
        KeywordInfo keywordInfoToUpdate = createKeyword(adGroupInfo1, PHRASE_1);

        ModelChanges<Keyword> changesKeyword = keywordModelChanges(keywordInfoToUpdate.getId(), PHRASE_2);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeyword));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordInfoToUpdate.getId(), PHRASE_2)));

        assertMailNotificationEventsPresent(notification(keywordInfoToUpdate, EventType.PH_CHANGE));
    }

    @Test
    public void execute_ChangePhraseAndThenItConvertsToOldValue_EventNotAdded() {
        String oldPhrase = "ремонт volvo";
        String newPhrase = "ремонт  volvo"; // убирается лишний пробел
        createOneActiveAdGroup(operatorClientInfo);
        Long keywordIdToUpdate = createKeyword(adGroupInfo1, oldPhrase).getId();

        ModelChanges<Keyword> changesKeyword = keywordModelChanges(keywordIdToUpdate, newPhrase);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeyword));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordIdToUpdate, oldPhrase)));

        List<MailNotificationEvent> actualEvents =
                testEventRepository.getEventsByOwnerUids(clientInfo.getShard(), singletonList(clientInfo.getUid()));
        assertThat("Нотификации должны отстуствовать", actualEvents, empty());
    }

    @Test
    public void execute_ChangePrice_EventAddedCorrectly() {
        createOneActiveAdGroup(operatorClientInfo);
        KeywordInfo keywordInfoToUpdate = createKeyword(adGroupInfo1, PHRASE_1);

        ModelChanges<Keyword> changesKeyword = keywordModelChanges(keywordInfoToUpdate.getId(), PHRASE_1, 10L);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeyword));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordInfoToUpdate.getId(), PHRASE_1)));

        assertMailNotificationEventsPresent(notification(keywordInfoToUpdate, EventType.PH_PRICE));
    }

    @Test
    public void execute_ChangePriceContext_EventAddedCorrectly() {
        createOneActiveAdGroup(operatorClientInfo);
        KeywordInfo keywordInfoToUpdate = createKeyword(adGroupInfo1, PHRASE_1);

        ModelChanges<Keyword> changesKeyword = new ModelChanges<>(keywordInfoToUpdate.getId(), Keyword.class)
                .process(BigDecimal.valueOf(10L), Keyword.PRICE_CONTEXT);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeyword));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordInfoToUpdate.getId(), PHRASE_1)));

        assertMailNotificationEventsPresent(notification(keywordInfoToUpdate, EventType.PH_PRICE_CTX));
    }

    @Test
    public void execute_ChangePhraseAndPriceAndPriceContext_EventsAddedCorrectly() {
        createOneActiveAdGroup(operatorClientInfo);
        KeywordInfo keywordInfoToUpdate = createKeyword(adGroupInfo1, PHRASE_1);

        ModelChanges<Keyword> changesKeyword = new ModelChanges<>(keywordInfoToUpdate.getId(), Keyword.class)
                .process(PHRASE_2, Keyword.PHRASE)
                .process(BigDecimal.valueOf(10L), Keyword.PRICE)
                .process(BigDecimal.valueOf(10L), Keyword.PRICE_CONTEXT);
        MassResult<UpdatedKeywordInfo> result = executePartial(singletonList(changesKeyword));
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordInfoToUpdate.getId(), PHRASE_2)));

        assertMailNotificationEventsPresent(asList(notification(keywordInfoToUpdate, EventType.PH_CHANGE),
                notification(keywordInfoToUpdate, EventType.PH_PRICE),
                notification(keywordInfoToUpdate, EventType.PH_PRICE_CTX)));
    }

    @Test
    public void execute_OneAdGroupWithTwoKeywords_EventsAddedCorrectly() {
        createOneActiveAdGroup(operatorClientInfo);
        KeywordInfo keywordInfoToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1);
        KeywordInfo keywordInfoToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2);

        String firstPhrase = "слон";
        List<ModelChanges<Keyword>> changesKeywords =
                asList(keywordModelChanges(keywordInfoToUpdate1.getId(), firstPhrase),
                        keywordModelChanges(keywordInfoToUpdate2.getId(), PHRASE_3));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordInfoToUpdate1.getId(), firstPhrase),
                isUpdated(keywordInfoToUpdate2.getId(), PHRASE_3)));

        assertMailNotificationEventsPresent(asList(notification(keywordInfoToUpdate1, EventType.PH_CHANGE),
                notification(keywordInfoToUpdate2, EventType.PH_CHANGE)));
    }

    @Test
    public void execute_TwoAdGroupsInOneCampaign_EventsAddedCorrectly() {
        createTwoActiveAdGroups(operatorClientInfo);
        KeywordInfo keywordInfoToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1);
        KeywordInfo keywordInfoToUpdate2 = createKeyword(adGroupInfo2, PHRASE_2);

        String firstPhrase = "слон";
        List<ModelChanges<Keyword>> changesKeywords =
                asList(keywordModelChanges(keywordInfoToUpdate1.getId(), firstPhrase),
                        keywordModelChanges(keywordInfoToUpdate2.getId(), PHRASE_3));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordInfoToUpdate1.getId(), firstPhrase),
                isUpdated(keywordInfoToUpdate2.getId(), PHRASE_3)));

        assertMailNotificationEventsPresent(asList(notification(keywordInfoToUpdate1, EventType.PH_CHANGE),
                notification(keywordInfoToUpdate2, EventType.PH_CHANGE)));
    }

    @Test
    public void execute_AdGroupContainsInvalidKeyword_EventForInvalidKeywordIsNotUpdated() {
        createOneActiveAdGroup(operatorClientInfo);
        KeywordInfo keywordInfoToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1);
        KeywordInfo keywordInfoToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2);

        List<ModelChanges<Keyword>> changesKeywords =
                asList(keywordModelChanges(keywordInfoToUpdate1.getId(), PHRASE_3),
                        keywordModelChanges(keywordInfoToUpdate2.getId(), INVALID_PHRASE_1));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isUpdated(keywordInfoToUpdate1.getId(), PHRASE_3), null));

        assertMailNotificationEventsPresent(notification(keywordInfoToUpdate1, EventType.PH_CHANGE));
    }

    @Test
    public void execute_AdGroupContainsDuplicatedKeyword_EventForDuplicatedKeywordIsNotUpdated() {
        String phrase = "слон";
        createOneActiveAdGroup(operatorClientInfo);
        Long existingKeywordId = createKeyword(adGroupInfo1, phrase).getId();
        KeywordInfo keywordInfoToUpdate1 = createKeyword(adGroupInfo1, PHRASE_1);
        KeywordInfo keywordInfoToUpdate2 = createKeyword(adGroupInfo1, PHRASE_2);

        List<ModelChanges<Keyword>> changesKeywords = asList(keywordModelChanges(keywordInfoToUpdate1.getId(), phrase),
                keywordModelChanges(keywordInfoToUpdate2.getId(), PHRASE_3));
        MassResult<UpdatedKeywordInfo> result = executePartial(changesKeywords);
        assumeThat(result, isSuccessfulWithMatchers(isNotUpdated(existingKeywordId, phrase),
                isUpdated(keywordInfoToUpdate2.getId(), PHRASE_3)));

        assertMailNotificationEventsPresent(notification(keywordInfoToUpdate2, EventType.PH_CHANGE));
    }

    private void assertMailNotificationEventsPresent(MailNotificationEvent expectedEvent) {
        assertMailNotificationEventsPresent(singletonList(expectedEvent));
    }

    private void assertMailNotificationEventsPresent(List<MailNotificationEvent> expectedEvents) {

        List<MailNotificationEvent> actualEvents =
                testEventRepository.getEventsByOwnerUids(clientInfo.getShard(), singletonList(clientInfo.getUid()));
        assertThat(String.format("список добавленных почтовых нотификаций не соответствует ожидаемому.\n "
                        + "Ожидаемые нотификации: %s\n\n"
                        + "Реальные нотификации: %s", expectedEvents, actualEvents),
                actualEvents, containsInAnyOrder(
                        mapList(expectedEvents, ev -> beanDiffer(ev).useCompareStrategy(onlyExpectedFields()))));
    }

    private MailNotificationEvent notification(KeywordInfo keywordInfo, EventType eventType) {
        Long clientUid = clientInfo.getUid();
        Long operatorUid = operatorClientInfo.getUid();

        return new MailNotificationEvent()
                .withObjectType(ObjectType.PHRASE)
                .withEventType(eventType)
                .withOperatorUid(operatorUid)
                .withOwnerUid(clientUid)
                .withCampaignId(keywordInfo.getCampaignId())
                .withObjectId(keywordInfo.getAdGroupId());
    }
}

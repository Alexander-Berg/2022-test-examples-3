package ru.yandex.direct.core.entity.keyword.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.direct.core.entity.keyword.container.AddedKeywordInfo;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.mailnotification.model.EventType;
import ru.yandex.direct.core.entity.mailnotification.model.MailNotificationEvent;
import ru.yandex.direct.core.entity.mailnotification.model.ObjectType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.repository.TestMailNotificationEventRepository;
import ru.yandex.direct.rbac.RbacRole;
import ru.yandex.direct.result.MassResult;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isAdded;
import static ru.yandex.direct.core.entity.keyword.service.AddKeywordMatchers.isNotAdded;
import static ru.yandex.direct.core.testing.data.TestKeywords.defaultKeyword;
import static ru.yandex.direct.test.utils.TestUtils.assumeThat;
import static ru.yandex.direct.testing.matchers.result.MassResultMatcher.isSuccessfulWithMatchers;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@CoreTest
@RunWith(SpringRunner.class)
public class KeywordsAddOperationMailNotificationsTest extends KeywordsAddOperationBaseTest {

    public static final ObjectMapper MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Autowired
    private TestMailNotificationEventRepository testEventRepository;

    private ClientInfo manager;

    @Before
    public void before() {
        super.before();
        manager = steps.clientSteps().createDefaultClientWithRole(RbacRole.MANAGER);
        clientInfo = steps.clientSteps().createDefaultClientUnderManager(manager);
        operatorClientInfo = clientInfo;
    }

    @Test
    public void execute_OneAdGroupWithOneKeyword_EventAddedCorrectly() {
        createOneActiveAdGroup(manager);

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords, manager.getUid());
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));

        assertMailNotificationEventsPresent(adGroupWithKeywords(adGroupInfo1, keywords));
    }

    @Test
    public void execute_OneAdGroupWithTwoKeywords_EventsAddedCorrectly() {
        createOneActiveAdGroup(manager);

        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, PHRASE_1), clientKeyword(adGroupInfo1, PHRASE_2));
        MassResult<AddedKeywordInfo> result = executePartial(keywords, manager.getUid());
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), isAdded(PHRASE_2)));

        assertMailNotificationEventsPresent(adGroupWithKeywords(adGroupInfo1, keywords));
    }

    @Test
    public void execute_TwoAdGroupsInOneCampaign_EventsAddedCorrectly() {
        createTwoActiveAdGroups(manager);

        Keyword adGroup1Keyword = clientKeyword(adGroupInfo1, PHRASE_1);
        Keyword adGroup2Keyword = clientKeyword(adGroupInfo2, PHRASE_2);
        List<Keyword> keywords = asList(adGroup1Keyword, adGroup2Keyword);
        MassResult<AddedKeywordInfo> result = executePartial(keywords, manager.getUid());
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), isAdded(PHRASE_2)));

        assertMailNotificationEventsPresent(asList(
                adGroupWithKeywords(adGroupInfo1, singleton(adGroup1Keyword)),
                adGroupWithKeywords(adGroupInfo2, singleton(adGroup2Keyword))));
    }

    @Test
    public void execute_AdGroupContainsInvalidKeyword_EventForInvalidKeywordIsNotAdded() {
        createOneActiveAdGroup(manager);

        Keyword validKeyword = clientKeyword(adGroupInfo1, PHRASE_1);
        List<Keyword> keywords = asList(validKeyword, clientKeyword(adGroupInfo1, INVALID_PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords, manager.getUid());
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1), null));

        assertMailNotificationEventsPresent(adGroupWithKeywords(adGroupInfo1, singleton(validKeyword)));
    }

    @Test
    public void execute_AdGroupContainsDuplicatedKeyword_EventForDuplicatedKeywordIsNotAdded() {
        createOneActiveAdGroup(manager);

        keywordSteps.createKeyword(adGroupInfo1, defaultKeyword().withPhrase(PHRASE_1));

        Keyword goodKeyword = clientKeyword(adGroupInfo1, PHRASE_2);
        List<Keyword> keywords = asList(clientKeyword(adGroupInfo1, PHRASE_1), goodKeyword);
        MassResult<AddedKeywordInfo> result = executePartial(keywords, manager.getUid());
        assumeThat(result, isSuccessfulWithMatchers(isNotAdded(PHRASE_1), isAdded(PHRASE_2)));

        assertMailNotificationEventsPresent(adGroupWithKeywords(adGroupInfo1, singleton(goodKeyword)));
    }

    @Test
    public void execute_ClientAddsValidKeyword_EventIsNotAdded() {
        clientInfo = steps.clientSteps().createDefaultClient();
        operatorClientInfo = clientInfo;
        createOneActiveAdGroup();

        List<Keyword> keywords = singletonList(clientKeyword(adGroupInfo1, PHRASE_1));
        MassResult<AddedKeywordInfo> result = executePartial(keywords);
        assumeThat(result, isSuccessfulWithMatchers(isAdded(PHRASE_1)));

        assertMailNotificationEventsDontPresent();
    }

    private void assertMailNotificationEventsPresent(AdGroupWithKeywords adGroupsWithKeywords) {
        assertMailNotificationEventsPresent(singleton(adGroupsWithKeywords));
    }

    private void assertMailNotificationEventsPresent(Collection<AdGroupWithKeywords> adGroupsWithKeywords) {
        List<MailNotificationEvent> expectedEvents = computeExpectedMailNotificationEvents(adGroupsWithKeywords);
        List<MailNotificationEvent> actualEvents =
                testEventRepository.getEventsByOwnerUids(clientInfo.getShard(), singletonList(clientInfo.getUid()));
        assertThat(String.format("список добавленных почтовых нотификаций не соответствует ожидаемому.\n "
                        + "Ожидаемые нотификации: %s\n\n"
                        + "Реальные нотификации: %s", toJson(expectedEvents), toJson(actualEvents)),
                actualEvents, containsInAnyOrder(mapList(expectedEvents, BeanDifferMatcher::beanDiffer)));
    }

    private void assertMailNotificationEventsDontPresent() {
        List<MailNotificationEvent> actualEvents =
                testEventRepository.getEventsByOwnerUids(clientInfo.getShard(), singletonList(clientInfo.getUid()));
        assertThat("Почтовые нотификации не должны быть добавлены", actualEvents, emptyIterable());
    }

    private String toJson(Object o) {
        try {
            return MAPPER.writeValueAsString(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<MailNotificationEvent> computeExpectedMailNotificationEvents(
            Collection<AdGroupWithKeywords> adGroupsWithKeywords) {
        Long clientUid = clientInfo.getUid();

        List<MailNotificationEvent> expectedEvents = new ArrayList<>();
        adGroupsWithKeywords.forEach(adGroupWithKeywords -> {
            Long campaignId = adGroupWithKeywords.adGroupInfo.getCampaignId();
            Long adGroupId = adGroupWithKeywords.adGroupInfo.getAdGroupId();
            String adGroupName = adGroupWithKeywords.adGroupInfo.getAdGroup().getName();

            adGroupWithKeywords.keywords.forEach(keyword -> {
                String jsonData = String.format(
                        "{\"pid\":%d,\"group_name\":\"%s\",\"old_text\":\"\",\"new_text\":\"%s\"}",
                        adGroupId, adGroupName, keyword.getPhrase());
                MailNotificationEvent expectedEvent = new MailNotificationEvent()
                        .withObjectType(ObjectType.ADGROUP)
                        .withEventType(EventType.ADGR_WORD)
                        .withOperatorUid(manager.getUid())
                        .withOwnerUid(clientUid)
                        .withCampaignId(campaignId)
                        .withObjectId(adGroupId)
                        .withJsonData(jsonData);
                expectedEvents.add(expectedEvent);
            });
        });
        return expectedEvents;
    }

    private static class AdGroupWithKeywords {
        AdGroupInfo adGroupInfo;
        Collection<Keyword> keywords;

        public AdGroupWithKeywords(AdGroupInfo adGroupInfo,
                                   Collection<Keyword> keywords) {
            this.adGroupInfo = adGroupInfo;
            this.keywords = keywords;
        }
    }

    static AdGroupWithKeywords adGroupWithKeywords(AdGroupInfo adGroupInfo, Collection<Keyword> keywords) {
        return new AdGroupWithKeywords(adGroupInfo, keywords);
    }
}

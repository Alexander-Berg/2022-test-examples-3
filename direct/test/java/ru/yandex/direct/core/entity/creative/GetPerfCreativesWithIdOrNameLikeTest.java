package ru.yandex.direct.core.entity.creative;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher;
import ru.yandex.direct.core.entity.creative.model.Creative;
import ru.yandex.direct.core.entity.creative.model.CreativeBusinessType;
import ru.yandex.direct.core.entity.creative.service.CreativeService;
import ru.yandex.direct.core.entity.feed.model.BusinessType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.data.TestCreatives;
import ru.yandex.direct.core.testing.info.ClientInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbutil.model.ClientId;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;
import static ru.yandex.direct.utils.FunctionalUtils.mapList;

@SuppressWarnings("unused")
@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class GetPerfCreativesWithIdOrNameLikeTest {
    @Autowired
    private CreativeService creativeService;
    @Autowired
    private Steps steps;

    private ClientId clientId;
    private ClientInfo clientInfo;

    @Before
    public void before() {
        clientInfo = steps.userSteps().createDefaultUser().getClientInfo();
        clientId = clientInfo.getClientId();

        // создание креативов происходит в тестах а не в before,
        // чтобы креативы из соседних тестов не мешали друг другу
    }

    @Test
    public void getCreativesWithBusinessType_whenNameLike() {
        Creative creative1 = createCreative("aaa_aaa", null, null);
        Creative creative2 = createCreative("aaa_ccc", null, null);

        getCreativesAndCheckResult("a_a", singletonList(creative1));
    }

    @Test
    public void getCreativesWithBusinessType_whenGroupNameLike() {
        Creative creative1 = createCreative("aaa", "bbbb", null);
        Creative creative2 = createCreative("aaa", "zzzz", null);

        getCreativesAndCheckResult("bbb", singletonList(creative1));
    }

    @Test
    public void getCreativesWithBusinessType_whenCreativeIdLike() {
        Creative creative1 = createCreative("aaa", null, null);
        Creative creative2 = createCreative("aaa", null, null);

        getCreativesAndCheckResult(creative2.getId().toString(), singletonList(creative2));
    }

    @Test
    public void getCreativesWithBusinessType_whenCreativeGroupIdLike() {
        Long creativeGroupId = getCreativeGroupId();
        Creative creative1 = createCreative("aaa", null, creativeGroupId);
        Creative creative2 = createCreative("aaa", null, 5678L);

        String likeString = creativeGroupId.toString().substring(1);

        getCreativesAndCheckResult(likeString, singletonList(creative1));
    }

    @Test
    public void getCreativesWithBusinessType_whenLeadingZeroInSearchCriteria() {
        Long creativeGroupId = getCreativeGroupId();
        Creative creative1 = createCreative("aaa", null, creativeGroupId);
        Creative creative2 = createCreative("aaa", null, 5678L);

        String likeString = "0" + creativeGroupId.toString();

        getCreativesAndCheckResult(likeString, emptyList());
    }

    /**
     * Генерирует уникальный {@code creativeGroupId} длины не менее 5 символов
     */
    private Long getCreativeGroupId() {
        Long nextCreativeGroupId = steps.creativeSteps().getNextCreativeGroupId(clientInfo.getShard());
        return Long.valueOf(StringUtils.leftPad(nextCreativeGroupId.toString(), 5, "1234"));
    }

    @Test
    public void getCreativesWithBusinessType_whenTwoCreativesMatch() {
        Creative creative1 = createCreative("aaa", null, null);
        Creative creative2 = createCreative("aaa", null, null);
        Creative creative3 = createCreative("ууу", null, null);

        getCreativesAndCheckResult("aaa", asList(creative1, creative2));
    }

    @Test
    public void getCreativesWithBusinessType_whenNoneMatch() {
        Creative creative1 = createCreative("aaa", null, null);
        Creative creative2 = createCreative("aaa", null, null);

        getCreativesAndCheckResult("aaabbb", emptyList());
    }

    @Test
    public void getCreativesWithBusinessType_whenNull() {
        Creative creative1 = createCreative("aaa", null, null);
        Creative creative2 = createCreative("aaa", null, null);

        getCreativesAndCheckResult(null, asList(creative1, creative2));
    }

    @Test
    public void getCreativesWithBusinessType_whenEmptyString() {
        Creative creative1 = createCreative("aaa", null, null);
        Creative creative2 = createCreative("aaa", null, null);

        getCreativesAndCheckResult("", asList(creative1, creative2));
    }

    private Creative createCreative(String name, String groupName, Long creativeGroupId) {
        Creative creative = TestCreatives.defaultPerformanceCreative(clientId, null)
                .withBusinessType(CreativeBusinessType.RETAIL)
                .withName(name)
                .withGroupName(groupName)
                .withCreativeGroupId(creativeGroupId);

        return steps.creativeSteps().createCreative(creative, clientInfo).getCreative();
    }

    /**
     * Выполняет поиск креативов по {@code idOrNameLike} и проверяет, что ответ в точности соответствует {@code expectedCreatives}
     */
    private void getCreativesAndCheckResult(String idOrNameLike, List<Creative> expectedCreatives) {
        List<Creative> actualCreatives = creativeService.getCreativesWithBusinessType(
                clientId, BusinessType.RETAIL, idOrNameLike);

        assertThat(actualCreatives, containsInAnyOrder(mapList(expectedCreatives, BeanDifferMatcher::beanDiffer)));
    }
}

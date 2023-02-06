package ru.yandex.direct.core.entity.moderationreason.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.CompareStrategy;
import ru.yandex.direct.core.entity.keyword.model.Keyword;
import ru.yandex.direct.core.entity.moderationreason.model.ModerationReasonObjectType;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.core.testing.info.KeywordInfo;
import ru.yandex.direct.core.testing.repository.TestModerationReasonsRepository;
import ru.yandex.direct.core.testing.steps.KeywordSteps;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static ru.yandex.autotests.irt.testutils.beandiffer2.BeanDifferMatcher.beanDiffer;
import static ru.yandex.autotests.irt.testutils.beandiffer2.beanfield.BeanFieldPath.newPath;
import static ru.yandex.autotests.irt.testutils.beandiffer2.comparestrategy.defaultcomparestrategy.DefaultCompareStrategies.onlyFields;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class ModerationKeywordServiceTest {
    @Autowired
    private ModerationKeywordService moderationKeywordService;
    @Autowired
    private KeywordSteps keywordSteps;

    @Autowired
    private TestModerationReasonsRepository testModerationReasonsRepository;

    private KeywordInfo keywordInfo;

    private static final CompareStrategy COMPARE_STRATEGY = onlyFields(newPath("phrase"), newPath("id"));

    private static String listReasonToDb(List<Long> listReasonIds) {
        StringBuilder builder = new StringBuilder("-\n  id: 13\n  list:\n");
        for (Long listReasonId : listReasonIds) {
            builder.append("    -\n");
            builder.append("      id: '").append(listReasonId).append("'\n");
            builder.append("      phrase: услуги -электрик\n");
        }
        return builder.toString();
    }


    public void insertListReason(int shardId, Map<ModerationReasonObjectType,
            List<Long>> moderationReasonsByObjectTypeAndId, Long reason) {
        testModerationReasonsRepository.insertReasons(shardId, moderationReasonsByObjectTypeAndId,
                listReasonToDb(Collections.singletonList(reason)));
    }

    @Before
    public void setUp() throws Exception {
        keywordInfo = keywordSteps.createDefaultKeyword();
        insertListReason(
                keywordInfo.getShard(),
                ImmutableMap.of(ModerationReasonObjectType.PHRASES, singletonList(keywordInfo.getAdGroupId())),
                keywordInfo.getKeyword().getId()
        );
    }

    @Test
    public void getPhrasesByGroupIds_ExistingGroup_ReturnCorrect() {
        Map<Long, List<Keyword>> keywords =
                moderationKeywordService.getKeywordsByGroupIds(singletonList(keywordInfo.getAdGroupId()));
        assertThat(keywords.keySet(), hasSize(1));
        assertThat(keywords.get(keywordInfo.getAdGroupId()),
                contains(beanDiffer(keywordInfo.getKeyword()).useCompareStrategy(COMPARE_STRATEGY)));
    }


    @Test
    public void getPhrasesByGroupIds_NonExistingGroup_ReturnEmpty() {
        Map<Long, List<Keyword>> keywords =
                moderationKeywordService.getKeywordsByGroupIds(singletonList(100500L));
        assertThat(keywords.keySet(), hasSize(0));
    }

    @Test
    public void getPhrasesByGroupIds_EmptyListGroups_ReturnEmpty() {
        Map<Long, List<Keyword>> keywords =
                moderationKeywordService.getKeywordsByGroupIds(emptyList());
        assertThat(keywords.keySet(), hasSize(0));
    }
}

package ru.yandex.direct.core.entity.targettag.repository;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.targettags.model.TargetTag;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.direct.dbschema.ppcdict.tables.TargetTags.TARGET_TAGS;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class TargetTagRepositoryTest {
    @Autowired
    private DslContextProvider dslContextProvider;

    private TargetTagRepository targetTagRepository;

    private static final TargetTag TARGET_TAG_1 = new TargetTag()
            .withId(1L)
            .withName("tag1")
            .withDescription("descr1");

    private static final TargetTag TARGET_TAG_2 = new TargetTag()
            .withId(2L)
            .withName("tag2")
            .withDescription("descr2");

    private static final TargetTag TARGET_TAG_3 = new TargetTag()
            .withId(3L)
            .withName("tag3")
            .withDescription("descr3");

    @Before
    public void before() throws Exception {
        MockitoAnnotations.initMocks(this);
        targetTagRepository = new TargetTagRepository(dslContextProvider);

        // очищаем полностью таблицу
        dslContextProvider.ppcdict().deleteFrom(TARGET_TAGS).execute();

        List<TargetTag> testTargetTags = List.of(TARGET_TAG_1, TARGET_TAG_2);
        targetTagRepository.updateTargetTags(testTargetTags);
    }

    /**
     * Тест: если отправить в таблицу TARGET_TAGS существующие в ней объекты с измененными значениями ->
     * они все сохранятся в БД с новыми значениями, если новые -> то добавятся
     */
    @Test
    public void updateTargetTags() {
        TargetTag changedTag = new TargetTag()
                .withId(2L)
                .withName("tag22")
                .withDescription("descr22");

        List<TargetTag> targetTagsToUpdate = List.of(changedTag, TARGET_TAG_3);

        List<TargetTag> expectedTargetTags = List.of(TARGET_TAG_1, changedTag, TARGET_TAG_3);

        targetTagRepository.updateTargetTags(targetTagsToUpdate);
        List<TargetTag> targetTags = targetTagRepository.getAllTargetTags();

        assertThat(targetTags).isEqualTo(expectedTargetTags);
    }

    /**
     * Тест: если отправить в таблицу TARGET_TAGS существующие в ней объекты с измененными значениями ->
     * они все сохранятся в БД с новыми значениями, если новые -> то добавятся
     */
    @Test
    public void findTargetTagsByNamePrefix() {
        List<TargetTag> targetTags = targetTagRepository.findTargetTagsByNamePrefix("tag", 10);
        List<TargetTag> expectedTargetTags = List.of(TARGET_TAG_1, TARGET_TAG_2);
        assertThat(targetTags).isEqualTo(expectedTargetTags);

        targetTags = targetTagRepository.findTargetTagsByNamePrefix("tag1", 10);
        expectedTargetTags = List.of(TARGET_TAG_1);
        assertThat(targetTags).isEqualTo(expectedTargetTags);
    }
}

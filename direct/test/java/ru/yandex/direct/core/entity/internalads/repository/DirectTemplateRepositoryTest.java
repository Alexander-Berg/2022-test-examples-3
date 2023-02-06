package ru.yandex.direct.core.entity.internalads.repository;

import java.util.List;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import ru.yandex.direct.core.entity.internalads.model.DirectTemplate;
import ru.yandex.direct.core.entity.internalads.model.DirectTemplateState;
import ru.yandex.direct.core.testing.configuration.CoreTest;
import ru.yandex.direct.test.utils.RandomNumberUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@CoreTest
@RunWith(SpringJUnit4ClassRunner.class)
public class DirectTemplateRepositoryTest {
    private static final String FORMAT_NAME_NEW = "new template";
    private static final String FORMAT_NAME_OLD = "old template";
    private static final Long TEMPLATE_ID_OLD = RandomNumberUtils.nextPositiveLong(1_000_000L);
    private static final DirectTemplate DIRECT_TEMPLATE_NEW = new DirectTemplate()
            .withFormatName(FORMAT_NAME_NEW)
            .withState(DirectTemplateState.UNIFIED);
    private static final DirectTemplate DIRECT_TEMPLATE_OLD = new DirectTemplate()
            .withFormatName(FORMAT_NAME_OLD)
            .withState(DirectTemplateState.UNIFIED)
            .withDirectTemplateId(TEMPLATE_ID_OLD);

    @Autowired
    DirectTemplateRepository directTemplateRepository;

    @Before
    public void before() {
        for (var directTemplateId : directTemplateRepository.getAll().keySet()) {
            directTemplateRepository.delete(directTemplateId);
        }
        assertThat(directTemplateRepository.getAll().keySet(), hasSize(0));
    }

    @Test
    public void insertAndGet_compareData() {
        var id = directTemplateRepository.add(DIRECT_TEMPLATE_NEW);
        var template = directTemplateRepository.get(List.of(id)).get(id);
        assertNotNull(template);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(template.getFormatName()).isEqualTo(FORMAT_NAME_NEW);
            softly.assertThat(template.getState()).isEqualTo(DirectTemplateState.UNIFIED);
        });
    }

    @Test
    public void insertWithIdAndGet_compareData() {
        directTemplateRepository.addOldTemplate(DIRECT_TEMPLATE_OLD);
        var template = directTemplateRepository.get(List.of(TEMPLATE_ID_OLD)).get(TEMPLATE_ID_OLD);
        assertNotNull(template);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(template.getDirectTemplateId()).isEqualTo(TEMPLATE_ID_OLD);
            softly.assertThat(template.getFormatName()).isEqualTo(FORMAT_NAME_OLD);
            softly.assertThat(template.getState()).isEqualTo(DirectTemplateState.UNIFIED);
        });
    }

    @Test
    public void setState_checkState() {
        var id = directTemplateRepository.add(DIRECT_TEMPLATE_NEW);
        directTemplateRepository.setState(id, DirectTemplateState.DEFAULT);
        var template = directTemplateRepository.get(List.of(id)).get(id);
        assertNotNull(template);
        assertEquals(DirectTemplateState.DEFAULT, template.getState());
    }

    @Test
    public void setFormatName_checkFormatName() {
        var id = directTemplateRepository.add(DIRECT_TEMPLATE_NEW);
        directTemplateRepository.setFormatName(id, "another name");
        var template = directTemplateRepository.get(List.of(id)).get(id);
        assertNotNull(template);
        assertEquals("another name", template.getFormatName());
    }

    @Test
    public void testGetAll() {
        directTemplateRepository.add(DIRECT_TEMPLATE_NEW);
        directTemplateRepository.addOldTemplate(DIRECT_TEMPLATE_OLD);
        assertThat(directTemplateRepository.getAll().keySet(), hasSize(2));
    }
}

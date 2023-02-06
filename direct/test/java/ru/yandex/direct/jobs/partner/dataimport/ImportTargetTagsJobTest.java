package ru.yandex.direct.jobs.partner.dataimport;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.direct.core.entity.targettag.repository.TargetTagRepository;
import ru.yandex.direct.core.entity.targettags.model.TargetTag;
import ru.yandex.direct.jobs.configuration.JobsTest;
import ru.yandex.direct.ytwrapper.client.YtProvider;
import ru.yandex.direct.ytwrapper.model.YtCluster;
import ru.yandex.direct.ytwrapper.model.YtOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@JobsTest
@ExtendWith(SpringExtension.class)
class ImportTargetTagsJobTest {
    @Autowired
    private YtCluster ytCluster;

    @Autowired
    private TargetTagRepository targetTagRepository;

    @Mock
    private YtProvider ytProvider;

    @Mock
    private YtOperator ytOperator;

    private ImportTargetTagsJob importTargetTagsJob;

    @BeforeEach
    void before() {
        MockitoAnnotations.initMocks(this);
        importTargetTagsJob = new ImportTargetTagsJob(ytProvider, ytCluster, targetTagRepository);
        when(ytProvider.getOperator(any())).thenReturn(ytOperator);
        when(ytOperator.exists(any())).thenReturn(true);

        targetTagRepository.updateTargetTags(List.of(
                new TargetTag()
                        .withId(1L)
                        .withName("tag1")
                        .withDescription("descr1"),
                new TargetTag()
                        .withId(2L)
                        .withName("tag2")
                        .withDescription("descr2")));
    }

    @Test
    void insertNewTargetTagsAndUpdateExisting() {
        when(ytOperator.readTableAndMap(any(), any(), any())).thenReturn(List.of(
                new TargetTag()
                        .withId(2L)
                        .withName("tag22")
                        .withDescription("descr22"),
                new TargetTag()
                        .withId(3L)
                        .withName("tag3")
                        .withDescription("descr3")));

        importTargetTagsJob.execute();

        List<TargetTag> expectedTargetTags = List.of(
                new TargetTag()
                        .withId(1L)
                        .withName("tag1")
                        .withDescription("descr1"),
                new TargetTag()
                        .withId(2L)
                        .withName("tag22")
                        .withDescription("descr22"),
                new TargetTag()
                        .withId(3L)
                        .withName("tag3")
                        .withDescription("descr3"));

        List<TargetTag> targetTags = targetTagRepository.getAllTargetTags();

        assertThat(targetTags).isEqualTo(expectedTargetTags);
    }
}

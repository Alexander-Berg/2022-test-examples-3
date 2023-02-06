package ru.yandex.direct.jobs.segment;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.common.db.PpcPropertiesSupport;
import ru.yandex.direct.core.entity.userssegments.repository.UsersSegmentRepository;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.jobs.segment.common.meta.SegmentMetaFetchingStrategyFactory;
import ru.yandex.direct.jobs.segment.common.meta.SegmentMetaUpdatingStrategyFactory;
import ru.yandex.direct.jobs.segment.common.preprocessor.SegmentSourceDataPreprocessorFactory;
import ru.yandex.direct.jobs.segment.common.target.SegmentTargetUpdatingStrategyFactory;
import ru.yandex.direct.solomon.SolomonPushClient;

public class SegmentJobsTestBase {

    public static final LocalDate TODAY = LocalDate.now();
    public static final LocalDate FINISH_LOG_DATE = TODAY.minusDays(1);

    public static final Long AUDIENCE_ID = 948239L;

    @Autowired
    public Steps steps;

    @Autowired
    public UsersSegmentRepository usersSegmentRepository;

    @Autowired
    public SegmentMetaFetchingStrategyFactory metaFetchingStrategyFactory;

    @Autowired
    public SegmentSourceDataPreprocessorFactory preprocessorFactory;

    @Autowired
    public SegmentTargetUpdatingStrategyFactory targetUpdatingStrategyFactory;

    @Autowired
    public SegmentMetaUpdatingStrategyFactory metaUpdatingStrategyFactory;

    @Autowired
    public PpcPropertiesSupport ppcPropertiesSupport;

    @Autowired
    public SolomonPushClient solomonPushClient;
}

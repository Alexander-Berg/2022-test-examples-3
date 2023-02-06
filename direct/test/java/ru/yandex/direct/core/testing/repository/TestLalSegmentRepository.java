package ru.yandex.direct.core.testing.repository;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import ru.yandex.direct.core.entity.metrika.repository.LalSegmentRepository;
import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.GoalType;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingCondition;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingConditionBase;
import ru.yandex.direct.core.entity.retargeting.model.Rule;
import ru.yandex.direct.dbschema.ppcdict.tables.records.LalSegmentsRecord;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.jooqmapper.JooqMapperWithSupplier;
import ru.yandex.direct.jooqmapperhelper.InsertHelper;

import static ru.yandex.direct.dbschema.ppcdict.tables.LalSegments.LAL_SEGMENTS;

public class TestLalSegmentRepository {
    private final DslContextProvider dslContextProvider;
    private final JooqMapperWithSupplier<Goal> jooqMapper;

    public TestLalSegmentRepository(DslContextProvider dslContextProvider, LalSegmentRepository lalSegmentRepository) {
        this.dslContextProvider = dslContextProvider;
        this.jooqMapper = lalSegmentRepository.getLalSegmentMapper();
    }

    public void addLalSegmentsFromRetargetingConditions(List<RetargetingCondition> retargetingConditions) {
        List<Goal> lalSegments = retargetingConditions.stream()
                .filter(Objects::nonNull)
                .map(RetargetingConditionBase::getRules)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .map(Rule::getGoals)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .filter(goal -> goal.getType() == GoalType.LAL_SEGMENT)
                .distinct()
                .collect(Collectors.toList());

        addAll(lalSegments);
    }

    public void addAll(List<Goal> lalSegments) {
        if (lalSegments.isEmpty()) {
            return;
        }
        InsertHelper<LalSegmentsRecord> insertHelper =
                new InsertHelper<>(dslContextProvider.ppcdict(), LAL_SEGMENTS);
        insertHelper.addAll(jooqMapper, lalSegments);

        insertHelper
                .onDuplicateKeyIgnore()
                .execute();
    }
}

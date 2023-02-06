package ru.yandex.direct.core.testing.steps;

import java.util.Collection;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import one.util.streamex.StreamEx;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.direct.core.entity.retargeting.model.Goal;
import ru.yandex.direct.core.entity.retargeting.model.MetrikaCounterGoalType;
import ru.yandex.direct.core.entity.retargeting.model.RawMetrikaSegmentPreset;
import ru.yandex.direct.core.entity.retargeting.model.RetargetingConditionGoal;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingGoalsPpcDictRepository;
import ru.yandex.direct.core.entity.retargeting.repository.RetargetingGoalsRepository;
import ru.yandex.direct.core.testing.info.RetargetingInfo;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;

import static ru.yandex.direct.dbschema.ppcdict.tables.MetrikaSegmentPresets.METRIKA_SEGMENT_PRESETS;

public class RetargetingGoalsSteps {
    @Autowired
    private RetargetingGoalsRepository retargetingGoalsRepository;
    @Autowired
    private RetargetingGoalsPpcDictRepository retargetingGoalsPpcDictRepository;
    @Autowired
    private DslContextProvider dslContextProvider;

    public void addGoal(RetargetingInfo retargetingInfo, RetargetingConditionGoal goal) {
        Multimap<Long, RetargetingConditionGoal> goals = HashMultimap.create();
        goals.put(retargetingInfo.getRetargetingId(), goal);
        retargetingGoalsRepository.add(retargetingInfo.getShard(), goals);
    }

    public void createMetrikaGoalsInPpcDict(Collection<Goal> goals) {
        Set<Long> goalIds = StreamEx.of(goals)
                .map(Goal::getId)
                .toSet();

        Set<Goal> goalsToAdd = StreamEx.of(Set.copyOf(goals))
                .map(goal -> (Goal) goal.withMetrikaCounterGoalType(MetrikaCounterGoalType.URL))
                .toSet();

        retargetingGoalsPpcDictRepository.deleteMetrikaGoalsFromPpcDict(goalIds);
        retargetingGoalsPpcDictRepository.addMetrikaGoalsToPpcDict(goalsToAdd);
    }

    public void createMetrikaSegmentPreset(RawMetrikaSegmentPreset preset) {
        dslContextProvider.ppcdict()
                .insertInto(METRIKA_SEGMENT_PRESETS)
                .set(METRIKA_SEGMENT_PRESETS.PRESET_ID, preset.getPresetId().longValue())
                .set(METRIKA_SEGMENT_PRESETS.NAME, preset.getName())
                .set(METRIKA_SEGMENT_PRESETS.EXPRESSION, preset.getExpression())
                .set(METRIKA_SEGMENT_PRESETS.TANKER_NAME_KEY, preset.getTankerNameKey())
                .execute();
    }

    public void clearMetrikaSegmentPresets() {
        dslContextProvider.ppcdict().truncate(METRIKA_SEGMENT_PRESETS).execute();
    }
}

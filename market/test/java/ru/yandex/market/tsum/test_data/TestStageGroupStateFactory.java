package ru.yandex.market.tsum.test_data;

import java.util.Arrays;
import java.util.List;

import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StageGroupState;
import ru.yandex.market.tsum.pipe.engine.runtime.state.model.StoredStage;

/**
 * @author Anton Sukhonosenko <a href="mailto:algebraic@yandex-team.ru"></a>
 * @date 19.02.18
 */
public class TestStageGroupStateFactory {
    private TestStageGroupStateFactory() {
    }

    public static StageGroupState create(String id) {
        return new StageGroupState(id, 1L);
    }

    private static List<StoredStage> getStages() {
        return Arrays.asList(
            new StoredStage("testing", null, true),
            new StoredStage("stable", null, true)
        );
    }

}

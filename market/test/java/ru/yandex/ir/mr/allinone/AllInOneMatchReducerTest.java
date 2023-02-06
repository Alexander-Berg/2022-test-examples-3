package ru.yandex.ir.mr.allinone;

import org.junit.jupiter.api.Test;

import ru.yandex.ir.mr.FormulasData;
import ru.yandex.ir.mr.allinone.config.MatchStepsProperties;
import ru.yandex.ir.predictors.full.pickers.ElementsPickerStrategy;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.ir.predictors.full.MatcherEngine.MatchMode.CANDIDATES_ONLY;

class AllInOneMatchReducerTest {

    @Test
    void extractFeatureNames() {
        String[] featureNames = AllInOneMatchReducer.extractFeatureNames(
                new MatchStepsProperties(
                        CANDIDATES_ONLY,
                        FormulasData.empty(),
                        FormulasData.empty(),
                        ElementsPickerStrategy.EMPTY
                )
        );
        assertThat(featureNames).isEmpty();
    }
}

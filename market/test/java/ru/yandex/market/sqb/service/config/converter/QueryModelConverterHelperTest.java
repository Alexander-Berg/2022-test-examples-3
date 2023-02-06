package ru.yandex.market.sqb.service.config.converter;

import java.util.List;

import com.google.common.collect.ImmutableList;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.market.sqb.exception.SqbValidationException;
import ru.yandex.market.sqb.model.conf.AliasModel;

/**
 * @author fbokovikov
 */
class QueryModelConverterHelperTest {

    private static final AliasModel MODEL_1 = new AliasModel("TAB", "chr(9)");
    private static final AliasModel MODEL_2 = new AliasModel("QUOTE_MARK", "chr(148)");
    private static final AliasModel MODEL_3 = new AliasModel("DATAFEED", "df");
    private static final AliasModel MODEL_1_DUPL = new AliasModel("TAB", "chr(10)");

    @Test
    void okNonOverrideModels() {
        List<AliasModel> baseModels = ImmutableList.of(
                MODEL_1,
                MODEL_2
        );
        List<AliasModel> additionalModels = ImmutableList.of(
                MODEL_3
        );
        List<AliasModel> aliasModels = QueryModelConverterHelper.joinModels(
                baseModels,
                additionalModels,
                false
        );
        MatcherAssert.assertThat(
                aliasModels,
                Matchers.containsInAnyOrder(MODEL_1, MODEL_2, MODEL_3)
        );
    }

    @Test
    void duplicateNonOverrideModels() {
        List<AliasModel> baseModels = ImmutableList.of(
                MODEL_1,
                MODEL_2
        );
        List<AliasModel> additionalModels = ImmutableList.of(
                MODEL_3,
                MODEL_1
        );

        Assertions.assertThrows(SqbValidationException.class, () ->
                QueryModelConverterHelper.joinModels(
                        baseModels,
                        additionalModels,
                        false
                )
        );
    }

    @Test
    void overrideModels() {
        List<AliasModel> baseModels = ImmutableList.of(
                MODEL_1,
                MODEL_2
        );
        List<AliasModel> additionalModels = ImmutableList.of(
                MODEL_3
        );
        List<AliasModel> aliasModels = QueryModelConverterHelper.joinModels(
                baseModels,
                additionalModels,
                true
        );
        MatcherAssert.assertThat(
                aliasModels,
                Matchers.containsInAnyOrder(MODEL_1, MODEL_2, MODEL_3)
        );
    }

    @Test
    void overrideModelsDuplicate() {
        List<AliasModel> baseModels = ImmutableList.of(
                MODEL_1,
                MODEL_2
        );
        List<AliasModel> additionalModels = ImmutableList.of(
                MODEL_3,
                MODEL_1_DUPL,
                MODEL_2
        );
        List<AliasModel> aliasModels = QueryModelConverterHelper.joinModels(
                baseModels,
                additionalModels,
                true
        );
        MatcherAssert.assertThat(
                aliasModels,
                Matchers.containsInAnyOrder(MODEL_1, MODEL_2, MODEL_3)
        );
    }

}

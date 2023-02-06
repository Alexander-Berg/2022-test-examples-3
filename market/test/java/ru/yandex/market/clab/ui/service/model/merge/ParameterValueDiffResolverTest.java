package ru.yandex.market.clab.ui.service.model.merge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ru.yandex.market.clab.common.cache.CachedParamInfo;
import ru.yandex.market.clab.common.cache.CategoryInfoCache;
import ru.yandex.market.clab.common.cache.ValueType;
import ru.yandex.market.clab.common.mbo.ProtoUtils;
import ru.yandex.market.clab.common.merge.Change;
import ru.yandex.market.mbo.http.ModelStorage.ParameterValue;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static ru.yandex.market.clab.ui.service.model.merge.ChangeAssertion.adding;
import static ru.yandex.market.clab.ui.service.model.merge.ChangeAssertion.assertSingle;
import static ru.yandex.market.clab.ui.service.model.merge.ChangeAssertion.removing;
import static ru.yandex.market.clab.ui.service.model.merge.ChangeAssertion.updating;
import static ru.yandex.market.clab.ui.service.model.merge.ParameterValueAssert.parameterValue;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @since 12.04.2019
 */
@RunWith(MockitoJUnitRunner.class)
public class ParameterValueDiffResolverTest {

    private static final long CATEGORY_ID = 13;
    private static final long MODEL_ID = 14;
    private static final long PARAM_ID_1 = 15;
    private static final long PARAM_ID_2 = 16;
    private static final int OPTION_HORSE_ID = 17;
    private static final int OPTION_TIGER_ID = 18;
    private static final int OPTION_SNAKE_ID = 19;

    private static final boolean SINGLE_VALUE = false;
    private static final boolean MULTI_VALUE = true;

    @Mock
    private CategoryInfoCache categoryInfoCache;

    private ParameterValueDiffResolver resolver;

    @Before
    public void setUp() {
        resolver = new ParameterValueDiffResolver(categoryInfoCache, CATEGORY_ID, MODEL_ID);
    }

    @Test
    public void booleanChange() {
        when(categoryInfoCache.getParamInfo(CATEGORY_ID, PARAM_ID_1))
            .thenReturn(paramInfo(PARAM_ID_1, ValueType.BOOLEAN, SINGLE_VALUE));

        resolver.apply(
            singletonList(booleanValue(PARAM_ID_1, false)),
            singletonList(booleanValue(PARAM_ID_1, false)),
            singletonList(booleanValue(PARAM_ID_1, true)),
            PARAM_ID_1
        );

        List<Change<ParameterValue>> changes = resolver.getChanges().asList();
        assertSingle(updating(), changes).matches(c -> {
            assertThat(c.getBefore()).extracting(ParameterValue::getBoolValue).isEqualTo(false);
            assertThat(c.getAfter()).extracting(ParameterValue::getBoolValue).isEqualTo(true);
            return true;
        });
        assertThat(resolver.getValues(), parameterValue())
            .extracting(ParameterValue::getBoolValue)
            .containsExactly(true);
    }

    @Test
    public void evenNotMultivalueStringTreatAsMultivalue() {
        when(categoryInfoCache.getParamInfo(CATEGORY_ID, PARAM_ID_1))
            .thenReturn(paramInfo(PARAM_ID_1, ValueType.STRING, SINGLE_VALUE));

        resolver.apply(
            singletonList(stringValue(PARAM_ID_1, "green", "red")),
            singletonList(stringValue(PARAM_ID_1, "green", "red")),
            singletonList(stringValue(PARAM_ID_1, "green", "white")),
            PARAM_ID_1
        );

        List<Change<ParameterValue>> changes = new ArrayList<>(resolver.getChanges().asList());

        assertThat(changes).hasSize(2);

        assertSingle(removing(), changes).withBefore(parameterValue())
            .extractingStrings()
            .containsExactly("red");

        assertSingle(adding(), changes).withAfter(parameterValue())
            .extractingStrings()
            .containsExactly("white");

        assertThat(resolver.getValues(), parameterValue())
            .hasSize(1)
            .first()
            .extractingStrings()
            .containsExactly("green", "white");
    }

    @Test
    public void singleValueEnumUsesUpdate() {
        when(categoryInfoCache.getParamInfo(CATEGORY_ID, PARAM_ID_1))
            .thenReturn(paramInfo(PARAM_ID_1, ValueType.ENUM, SINGLE_VALUE));

        resolver.apply(
            singletonList(option(PARAM_ID_1, OPTION_HORSE_ID)),
            singletonList(option(PARAM_ID_1, OPTION_HORSE_ID)),
            singletonList(option(PARAM_ID_1, OPTION_TIGER_ID)),
            PARAM_ID_1
        );

        List<Change<ParameterValue>> changes = new ArrayList<>(resolver.getChanges().asList());

        assertThat(changes).hasSize(1);

        assertSingle(updating(), changes).matches(c -> {
            assertThat(c.getBefore()).extracting(ParameterValue::getOptionId).isEqualTo(OPTION_HORSE_ID);
            assertThat(c.getAfter()).extracting(ParameterValue::getOptionId).isEqualTo(OPTION_TIGER_ID);
            return true;
        });

        assertThat(resolver.getValues())
            .extracting(ParameterValue::getOptionId)
            .containsExactly(OPTION_TIGER_ID);
    }


    @Test
    public void multivalueValueEnumUsesRemoveAdd() {
        when(categoryInfoCache.getParamInfo(CATEGORY_ID, PARAM_ID_1))
            .thenReturn(paramInfo(PARAM_ID_1, ValueType.ENUM, MULTI_VALUE));

        resolver.apply(
            singletonList(option(PARAM_ID_1, OPTION_HORSE_ID)),
            singletonList(option(PARAM_ID_1, OPTION_HORSE_ID)),
            singletonList(option(PARAM_ID_1, OPTION_TIGER_ID)),
            PARAM_ID_1
        );

        List<Change<ParameterValue>> changes = new ArrayList<>(resolver.getChanges().asList());

        assertThat(changes).hasSize(2);

        assertSingle(removing(), changes).withBefore()
            .extracting(ParameterValue::getOptionId)
            .isEqualTo(OPTION_HORSE_ID);

        assertSingle(adding(), changes).withAfter()
            .extracting(ParameterValue::getOptionId)
            .isEqualTo(OPTION_TIGER_ID);

        assertThat(resolver.getValues())
            .extracting(ParameterValue::getOptionId)
            .containsExactly(OPTION_TIGER_ID);
    }

    @Test
    public void singlevalueFallbackIsFoundMoreThanOneValue() {
        when(categoryInfoCache.getParamInfo(CATEGORY_ID, PARAM_ID_1))
            .thenReturn(paramInfo(PARAM_ID_1, ValueType.ENUM, SINGLE_VALUE));

        resolver.apply(
            asList(option(PARAM_ID_1, OPTION_HORSE_ID), option(PARAM_ID_1, OPTION_SNAKE_ID)),
            asList(option(PARAM_ID_1, OPTION_HORSE_ID), option(PARAM_ID_1, OPTION_SNAKE_ID)),
            asList(option(PARAM_ID_1, OPTION_HORSE_ID), option(PARAM_ID_1, OPTION_TIGER_ID)),
            PARAM_ID_1
        );

        List<Change<ParameterValue>> changes = new ArrayList<>(resolver.getChanges().asList());

        assertThat(changes).hasSize(2);

        assertSingle(removing(), changes).withBefore()
            .extracting(ParameterValue::getOptionId)
            .isEqualTo(OPTION_SNAKE_ID);

        assertSingle(adding(), changes).withAfter()
            .extracting(ParameterValue::getOptionId)
            .isEqualTo(OPTION_TIGER_ID);

        assertThat(resolver.getValues())
            .extracting(ParameterValue::getOptionId)
            .containsExactly(OPTION_HORSE_ID, OPTION_TIGER_ID);
    }


    @Test
    public void rearrangingDoesntRegisterButAccepts() {
        when(categoryInfoCache.getParamInfo(CATEGORY_ID, PARAM_ID_1))
            .thenReturn(paramInfo(PARAM_ID_1, ValueType.ENUM, SINGLE_VALUE));

        resolver.apply(
            asList(option(PARAM_ID_1, OPTION_HORSE_ID), option(PARAM_ID_1, OPTION_SNAKE_ID)),
            asList(option(PARAM_ID_1, OPTION_HORSE_ID), option(PARAM_ID_1, OPTION_SNAKE_ID)),
            asList(option(PARAM_ID_1, OPTION_SNAKE_ID), option(PARAM_ID_1, OPTION_HORSE_ID)),
            PARAM_ID_1
        );

        assertThat(resolver.getChanges()).isEmpty();

        assertThat(resolver.getValues())
            .extracting(ParameterValue::getOptionId)
            .containsExactly(OPTION_SNAKE_ID, OPTION_HORSE_ID);
    }

    @Test
    public void conflictWithDifferentValues() {
        when(categoryInfoCache.getParamInfo(CATEGORY_ID, PARAM_ID_1))
            .thenReturn(paramInfo(PARAM_ID_1, ValueType.ENUM, SINGLE_VALUE));

        resolver.apply(
            asList(option(PARAM_ID_1, OPTION_HORSE_ID), option(PARAM_ID_1, OPTION_SNAKE_ID)),
            Collections.emptyList(),
            asList(option(PARAM_ID_1, OPTION_SNAKE_ID), option(PARAM_ID_1, OPTION_TIGER_ID)),
            PARAM_ID_1
        );

        assertThat(resolver.getChanges()).isEmpty();

        assertThat(resolver.getValues())
            .extracting(ParameterValue::getOptionId)
            .containsExactly(OPTION_HORSE_ID, OPTION_SNAKE_ID);
        assertThat(resolver.getWarnings()).isNotEmpty();
    }

    @Test
    public void conflictWithSameValues() {
        resolver.apply(
            asList(option(PARAM_ID_1, OPTION_HORSE_ID), option(PARAM_ID_1, OPTION_SNAKE_ID)),
            Collections.emptyList(),
            asList(option(PARAM_ID_1, OPTION_SNAKE_ID), option(PARAM_ID_1, OPTION_HORSE_ID)),
            PARAM_ID_1
        );

        assertThat(resolver.getChanges()).isEmpty();

        assertThat(resolver.getValues())
            .extracting(ParameterValue::getOptionId)
            .containsExactly(OPTION_HORSE_ID, OPTION_SNAKE_ID);
        assertThat(resolver.getWarnings()).isEmpty();
    }

    public ParameterValue booleanValue(long paramId, boolean value) {
        return ParameterValue.newBuilder()
            .setParamId(paramId)
            .setBoolValue(value)
            .setOptionId(((int) paramId * 100 + (value ? 1 : 0)))
            .build();
    }

    public ParameterValue option(long paramId, int optionId) {
        return ParameterValue.newBuilder()
            .setParamId(paramId)
            .setOptionId(optionId)
            .build();
    }

    public ParameterValue stringValue(long paramId, String... values) {
        ParameterValue.Builder builder = ParameterValue.newBuilder()
            .setParamId(paramId);

        Stream.of(values)
            .map(ProtoUtils::defaultLocalizedString)
            .forEach(builder::addStrValue);

        return builder
            .build();
    }

    public CachedParamInfo paramInfo(long paramId, ValueType type, boolean multivalue) {
        CachedParamInfo info = new CachedParamInfo();
        info.setId(paramId);
        info.setType(type);
        info.setMultivalue(multivalue);
        return info;
    }
}

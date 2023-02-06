package ru.yandex.market.robot.db.raw_model;

import io.qameta.allure.Issue;
import io.qameta.allure.junit4.Tag;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.yandex.market.robot.auto_generation.RemoveAfterFormalizationPatternProcessor;
import ru.yandex.market.robot.auto_generation.RemovePatternProcessor;
import ru.yandex.market.robot.auto_generation.ReplacePatternProcessor;
import ru.yandex.market.robot.auto_generation.SelectPatternProcessor;
import ru.yandex.market.robot.auto_generation.TitleProcessorFactory;
import ru.yandex.market.robot.shared.clusterizer.title_utils.TitleProcessor;
import ru.yandex.market.robot.shared.clusterizer.title_utils.TitleProcessorType;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * @author jkt on 19.12.17.
 */
@Tag("Component")
@Issue("MARKETIR-4136")
@RunWith(JUnitParamsRunner.class)
public class TitleProcessorFactoryTest {

    private static final String SETTINGS = "Replacement|Pattern";
    private static final String REPLACEMENT = "replacement";

    @Test
    @Parameters(source = TestCases.TypeClasses.class)
    public void shouldReturnCorrectClass(TitleProcessorType type, Class<?> expectedClass) {
        TitleProcessor titleProcessor = getTitleProcessor(type, SETTINGS);

        assertSoftly(soft -> {
            soft.assertThat(titleProcessor.getType()).isEqualTo(type);
            soft.assertThat(titleProcessor).isInstanceOf(expectedClass);
        });
    }

    @Test
    public void whenTypeReplaceShouldSplitSettings() {
        String replacement = "Replacement";
        String settings = "Regex|" + replacement;

        TitleProcessor titleProcessor = getTitleProcessor(TitleProcessorType.REPLACE, settings);

        assertSoftly(soft -> {
            soft.assertThat(titleProcessor).hasFieldOrPropertyWithValue(REPLACEMENT, replacement);
        });
    }

    @Test
    @Parameters(source = TestCases.InvalidSettings.class)
    public void whenTypeReplaceAndInvalidSettingsShouldThrowException(String settings) {

        assertThatThrownBy(() -> getTitleProcessor(TitleProcessorType.REPLACE, settings))
            .isInstanceOf(IllegalArgumentException.class);

    }

    @Test
    @Parameters(source = TestCases.InvalidSettings.class)
    public void whenTypeNotReplaceAndInvalidSettingsShouldNotThrowException(String settings) {
        assertSoftly(soft -> {
            soft.assertThat(catchThrowable(() -> getTitleProcessor(TitleProcessorType.REMOVE, settings))).isNull();
            soft.assertThat(catchThrowable(() -> getTitleProcessor(TitleProcessorType.SELECT, settings))).isNull();
        });

    }

    private TitleProcessor getTitleProcessor(TitleProcessorType type, String settings) {
        return TitleProcessorFactory.getTitleProcessor(type.ordinal(), settings);
    }

    public static class TestCases {

        public static class TypeClasses {

            public static Object[] provide() {
                return new Object[]{
                    new Object[]{TitleProcessorType.REMOVE, RemovePatternProcessor.class},
                    new Object[]{TitleProcessorType.REPLACE, ReplacePatternProcessor.class},
                    new Object[]{TitleProcessorType.SELECT, SelectPatternProcessor.class},
                    new Object[]{TitleProcessorType.REMOVE_AFTER_FORMALIZATION,
                        RemoveAfterFormalizationPatternProcessor.class},
                };
            }
        }

        public static class InvalidSettings {

            public static Object[] provide() {
                return new Object[]{
                    new Object[]{"to|much|separators"},
                    new Object[]{"does_not_contain_separator"},
                };
            }
        }
    }
}

package ru.yandex.market.mbo.db.params;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.market.mbo.common.model.Language;
import ru.yandex.market.mbo.gwt.exceptions.dto.OptionParametersDuplicationDto;
import ru.yandex.market.mbo.gwt.models.gurulight.ParameterValuesChanges;
import ru.yandex.market.mbo.gwt.models.linkedvalues.InitializedValueLink;
import ru.yandex.market.mbo.gwt.models.linkedvalues.LinkDirection;
import ru.yandex.market.mbo.gwt.models.linkedvalues.ValueLinkType;
import ru.yandex.market.mbo.gwt.models.params.Option;
import ru.yandex.market.mbo.gwt.models.params.OptionBuilder;
import ru.yandex.market.mbo.gwt.models.visual.Word;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Alexander Kramarev (pochemuto@yandex-team.ru)
 * @date 11.07.2018
 */
@SuppressWarnings("checkstyle:magicnumber")
public class OptionDuplicationRecoverServiceTest {

    private OptionDuplicationRecoverService recoverService;

    private Recover recover;

    @Before
    public void before() {
        recoverService = new OptionDuplicationRecoverService();
        recover = Recover.reuseOptions();
    }

    @Test
    public void recoverOnEmpty() {
        ParameterValuesChanges emptyChanges = new ParameterValuesChanges();
        OptionParametersDuplicationDto emptyDuplications = new OptionParametersDuplicationDto();
        assertThat(recoverService.tryRecover(emptyChanges, emptyDuplications, recover)).isTrue();
    }

    @Test
    public void noConflicts() {
        ParameterValuesChanges valuesChanges = new ParameterValuesChanges();
        valuesChanges.valueUpdated(option("red"));
        valuesChanges.valueUpdated(option("blue"));
        assertThat(recoverService.tryRecover(valuesChanges, new OptionParametersDuplicationDto(), recover))
            .isTrue();
    }

    @Test
    public void cannotRecoverMultipleWordOption() {
        ParameterValuesChanges valuesChanges = new ParameterValuesChanges();
        Option newOption = option("red");
        newOption.addName(new Word(Language.TURKEY.getId(), "reddd"));
        valuesChanges.valueUpdated(newOption);
        OptionParametersDuplicationDto duplications = duplications().nonGroup("red", newOption, option(1, "red")).get();
        assertThat(recoverService.tryRecover(valuesChanges, duplications, recover)).isFalse();
    }

    @Test
    public void replace() {
        ParameterValuesChanges valuesChanges = new ParameterValuesChanges();
        Option addRed = option("red");
        Option existingRed = option(14, "red");
        Option addBlue = option("blue");
        Option addBlueSecond = option("голубая");
        Option existingBlue = option(31, "blue");
        InitializedValueLink link = link(addRed, addBlue);
        valuesChanges.valueUpdated(addRed);
        valuesChanges.valueUpdated(addBlue);
        valuesChanges.valueUpdated(addBlueSecond);
        valuesChanges.linkAdded(link);


        OptionParametersDuplicationDto duplications = duplications()
            .nonGroup("red", addRed, existingRed)
            .nonGroup("blue", addBlue, addBlueSecond, existingBlue)
            .get();


        assertThat(recoverService.tryRecover(valuesChanges, duplications, recover)).isTrue();
        assertThat(valuesChanges.getAdded()).isEmpty();
        assertThat(recover.getReusings()).hasSize(3)
            .containsEntry(addRed, existingRed)
            .containsEntry(addBlue, existingBlue)
            .containsEntry(addBlueSecond, existingBlue);
        assertThat(valuesChanges.getAddedLinks()).hasSize(1);
        assertThat(valuesChanges.getAddedLinks().iterator().next()).isSameAs(link);
        assertThat(link.getSourceOption()).isSameAs(existingRed);
        assertThat(link.getTargetOption()).isSameAs(existingBlue);
    }

    @Test
    public void cannotRecoverWithoutExisting() {
        ParameterValuesChanges valuesChanges = new ParameterValuesChanges();
        Option addRed = option("red");
        Option addRedSecond = option("red");
        valuesChanges.valueUpdated(addRed);
        valuesChanges.valueUpdated(addRedSecond);

        OptionParametersDuplicationDto duplications = duplications()
            .nonGroup("blue", addRed, addRedSecond)
            .get();

        assertThat(recoverService.tryRecover(valuesChanges, duplications, recover)).isFalse();
        assertThat(recover.getReusings()).isEmpty();
    }

    @Test
    public void cannotRecoverWithMultipleOptions() {
        ParameterValuesChanges valuesChanges = new ParameterValuesChanges();
        Option addRed = option("red");
        Option existingRed1 = option(14, "red");
        Option existingRed2 = option(31, "red");
        valuesChanges.valueUpdated(addRed);

        OptionParametersDuplicationDto duplications = duplications()
            .nonGroup("blue", addRed, existingRed1, existingRed2)
            .get();

        assertThat(recoverService.tryRecover(valuesChanges, duplications, recover)).isFalse();
        assertThat(recover.getReusings()).isEmpty();
    }

    @Nonnull
    protected InitializedValueLink link(Option source, Option target) {
        return new InitializedValueLink(source, target, LinkDirection.BIDIRECTIONAL, ValueLinkType.GENERAL);
    }

    private static Duplications duplications() {
        return new Duplications();
    }

    private static class Duplications {
        private OptionParametersDuplicationDto duplication = new OptionParametersDuplicationDto();

        public Duplications nonGroup(String word, Option... options) {
            duplication.getNonGroupedDuplications().put(word, new HashSet<>(Arrays.asList(options)));
            return this;
        }

        public OptionParametersDuplicationDto get() {
            return duplication;
        }
    }

    private static Option option(int id, String... words) {
        OptionBuilder builder = OptionBuilder.newBuilder(id);
        for (String word : words) {
            builder.addName(word);
        }
        return builder.build();
    }

    private static Option option(String... words) {
        return option(0, words);
    }
}

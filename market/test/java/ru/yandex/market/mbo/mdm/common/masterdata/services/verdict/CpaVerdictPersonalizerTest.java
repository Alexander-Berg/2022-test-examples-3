package ru.yandex.market.mbo.mdm.common.masterdata.services.verdict;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Ignore;
import org.junit.Test;

import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.masterdata.parsing.SskuMasterDataFields;
import ru.yandex.market.mboc.common.utils.ErrorInfo;

public class CpaVerdictPersonalizerTest {

    private final CpaVerdictPersonalizer personalizer = new CpaVerdictPersonalizer();

    @Test
    public void shouldConvertCpaErrorWithErrorLevel() {
        // given
        var cpaError = MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.BOX_DIMENSIONS);

        // when
        var results = personalizer
            .personalize(new CommonSskuErrorInfos(List.of(cpaError), List.of()));

        // then
        Assertions.assertThat(results.getErrorInfos()).allMatch(it -> it.getLevel() == ErrorInfo.Level.ERROR);
        Assertions.assertThat(results.getErrorInfos()).hasSize(1);
    }

    @Test
    public void shouldConvertCpaErrorWithMixedLevels() {
        // given
        var cpaError = MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.BOX_DIMENSIONS);
        var anotherError = MbocErrors.get().excelInvalidGuid("GUID", "123");
        var allErrors = List.of(cpaError, anotherError);

        // when
        var results = personalizer
            .personalize(new CommonSskuErrorInfos(allErrors, List.of()));

        // then
        Assertions.assertThat(results.getErrorInfos()).anyMatch(it -> it.getLevel() == ErrorInfo.Level.ERROR);
        Assertions.assertThat(results.getErrorInfos()).anyMatch(it -> it.getLevel() == ErrorInfo.Level.WARNING);
        Assertions.assertThat(results.getErrorInfos()).hasSize(2);
    }

    @Test
    @Ignore("Тест не имеет смысла, пока нет специфичных для 1Р ошибок (ранее таковой была ошибка по СГ)")
    public void shouldConvertShelfLifeDifferentlyFor1pAndNot() {
        // given
        var shelfLifeError = MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.SHELF_LIFE);
        var allErrors = List.of(shelfLifeError);
        var onePPersonalizer = new CpaVerdictPersonalizer(true);

        // when
        var results = personalizer
            .personalize(new CommonSskuErrorInfos(allErrors, List.of()));
        var onePResults = onePPersonalizer
            .personalize(new CommonSskuErrorInfos(allErrors, List.of()));

        // then
        Assertions.assertThat(results.getErrorInfos()).anyMatch(it -> it.getLevel() == ErrorInfo.Level.WARNING);

        // and
        Assertions.assertThat(onePResults.getErrorInfos()).anyMatch(it -> it.getLevel() == ErrorInfo.Level.ERROR);
    }
}

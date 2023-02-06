package ru.yandex.market.mbo.mdm.common.masterdata.services.verdict;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.masterdata.parsing.SskuMasterDataFields;
import ru.yandex.market.mboc.common.utils.ErrorInfo;

public class DbsVerdictPersonalizerTest {

    private final DbsVerdictPersonalizer personalizer = new DbsVerdictPersonalizer();

    @Test
    public void shouldConvertGoldVghErrorWithWarningLevel() {
        // given
        var warning = MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.BOX_DIMENSIONS);

        // when
        var results = personalizer
            .personalize(new CommonSskuErrorInfos(List.of(warning), List.of()));

        // then
        Assertions.assertThat(results.getErrorInfos()).allMatch(it -> it.getLevel() == ErrorInfo.Level.WARNING);
        Assertions.assertThat(results.getErrorInfos()).hasSize(1);
    }

    @Test
    public void shouldConvertSilverVghErrorWithWarningLevel() {
        // given
        var warning = MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.BOX_DIMENSIONS);

        // when
        var results = personalizer
            .personalize(new CommonSskuErrorInfos(List.of(), List.of(warning)));

        // then
        Assertions.assertThat(results.getErrorInfos()).allMatch(it -> it.getLevel() == ErrorInfo.Level.WARNING);
        Assertions.assertThat(results.getErrorInfos()).hasSize(1);
    }

    @Test
    public void shouldConvertServiceAndBaseVghErrorsWithWarningLevel() {
        // given
        var warning = MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.BOX_DIMENSIONS);

        // when
        var results = personalizer
            .personalize(new CommonSskuErrorInfos(List.of(warning), List.of(warning)));

        // then
        Assertions.assertThat(results.getErrorInfos()).allMatch(it -> it.getLevel() == ErrorInfo.Level.WARNING);
        Assertions.assertThat(results.getErrorInfos()).hasSize(2);
    }
}

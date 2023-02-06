package ru.yandex.market.mbo.mdm.common.masterdata.services.verdict;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.masterdata.parsing.SskuMasterDataFields;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MdmErrorInfoMergerTest {

    private final MdmErrorInfoClarifier mdmErrorInfoClarifier = mock(MdmErrorInfoClarifier.class);
    private final MdmErrorInfoMerger merger = new MdmErrorInfoMerger(mdmErrorInfoClarifier);

    @Test
    public void shouldPropagateTheSameGoldAndSilverErrorsIfNoOverridesAndIntersections() {
        // given
        var goldenErrors = List.of(MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.BOX_WIDTH));
        var silverErrors = List.of(MbocErrors.get()
            .excelValueIsRequired(SskuMasterDataFields.QUANTUM_OF_SUPPLY));
        // no overrides
        when(mdmErrorInfoClarifier.clarifyErrors(goldenErrors, silverErrors)).thenReturn(goldenErrors);

        // when
        var result = merger.merge(new CommonSskuErrorInfos(goldenErrors, silverErrors));

        // then
        Assertions.assertThat(result.getGoldenErrors()).containsAll(goldenErrors);
        Assertions.assertThat(result.getSilverErrors()).containsAll(silverErrors);
    }

    @Test
    public void shouldFilterErrorFromSilverWhenSilverOverrideGolden() {
        // given
        var goldenErrors = List.of(MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.BOX_WIDTH));
        var silverErrors = List.of(MbocErrors.get()
            .excelValueIsRequired(SskuMasterDataFields.QUANTUM_OF_SUPPLY));
        when(mdmErrorInfoClarifier.clarifyErrors(goldenErrors, silverErrors)).thenReturn(silverErrors);

        // when
        var result = merger.merge(new CommonSskuErrorInfos(goldenErrors, silverErrors));

        // then
        Assertions.assertThat(result.getGoldenErrors()).containsAll(silverErrors);
        Assertions.assertThat(result.getSilverErrors()).isEmpty();
    }

    @Test
    public void shouldFilterErrorFromSilverWhenSilverContainsTheSameErrorAsGolden() {
        // given
        var goldenErrors = List.of(MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.BOX_WIDTH));
        var silverErrors = List.of(MbocErrors.get()
            .excelValueIsRequired(SskuMasterDataFields.BOX_WIDTH));
        // no overrides
        when(mdmErrorInfoClarifier.clarifyErrors(goldenErrors, silverErrors)).thenReturn(goldenErrors);

        // when
        var result = merger.merge(new CommonSskuErrorInfos(goldenErrors, silverErrors));

        // then
        Assertions.assertThat(result.getGoldenErrors()).containsAll(goldenErrors);
        Assertions.assertThat(result.getSilverErrors()).isEmpty();
    }


}

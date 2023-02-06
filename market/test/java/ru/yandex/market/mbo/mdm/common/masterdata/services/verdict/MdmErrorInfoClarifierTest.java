package ru.yandex.market.mbo.mdm.common.masterdata.services.verdict;

import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.market.mbo.storage.StorageKeyValueService;
import ru.yandex.market.mboc.common.MbocErrors;
import ru.yandex.market.mboc.common.masterdata.parsing.SskuMasterDataFields;
import ru.yandex.market.mboc.common.utils.MdmProperties;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static ru.yandex.market.mboc.common.masterdata.parsing.SskuMasterDataFields.BOX_DIMENSIONS;

public class MdmErrorInfoClarifierTest {

    private final StorageKeyValueService keyValueService = mock(StorageKeyValueService.class);
    private final MdmErrorInfoClarifier mdmErrorInfoClarifier = new MdmErrorInfoClarifier(keyValueService);

    @Before
    public void setUp() {
        given(keyValueService.getCachedBool(eq(MdmProperties.MERGE_SSKU_GOLDEN_VERDICTS_ENABLED_KEY), any()))
            .willReturn(true);
    }

    @Test
    public void moreDetailedErrorFromOverrideWithSameHeaderShouldWin() {
        // given
        var initialError = MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.BOX_WIDTH);
        var detailedError =
            MbocErrors.get().excelValueMustBeInRange(SskuMasterDataFields.BOX_WIDTH, "100", "10", "20");

        // when
        var mergeResult = mdmErrorInfoClarifier.clarifyErrors(List.of(initialError), List.of(detailedError));

        // then
        Assertions.assertThat(mergeResult).containsExactly(detailedError);
    }

    @Test
    public void lessDetailedErrorFromOverrideWithSameHeaderShouldLose() {
        // given
        var initialError = MbocErrors.get()
            .excelValueMustBeInRange(SskuMasterDataFields.BOX_WIDTH, "100", "10", "20");
        var lessDetailedError = MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.BOX_WIDTH);


        // when
        var mergeResult = mdmErrorInfoClarifier
            .clarifyErrors(List.of(initialError), List.of(lessDetailedError));

        // then
        Assertions.assertThat(mergeResult).containsExactly(initialError);
    }

    @Test
    public void moreThanOneDetailsAboutDimensionsShouldWinIncompleteDimensions() {
        // given
        var initialError = MbocErrors.get().excelValueIsRequired(BOX_DIMENSIONS);
        var widthDetails =
            MbocErrors.get().excelValueMustBeInRange(SskuMasterDataFields.BOX_WIDTH, "100", "10", "20");

        var lengthDetails = MbocErrors.get()
            .excelValueMustBeInRange(SskuMasterDataFields.BOX_LENGTH, "200", "10", "20");

        // when
        var mergeResult =
            mdmErrorInfoClarifier.clarifyErrors(List.of(initialError), List.of(widthDetails, lengthDetails));

        // then
        Assertions.assertThat(mergeResult).containsExactly(widthDetails, lengthDetails);
    }

    @Test
    public void shouldReturnBaseIfNoOverrides() {
        // given
        var initialError = MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.BOX_WIDTH);

        // when
        var mergeResult = mdmErrorInfoClarifier.clarifyErrors(List.of(initialError), List.of());

        // then
        Assertions.assertThat(mergeResult).containsExactly(initialError);
    }

    @Test
    public void shouldReturnBaseAndOverrideInMixCase() {
        // given
        var initialError = MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.BOX_WIDTH);
        var detailedError = MbocErrors.get()
            .excelValueMustBeInRange(SskuMasterDataFields.BOX_WIDTH, "100", "10", "20");
        var anotherInitial = MbocErrors.get()
            .excelValueMustBeInRange(SskuMasterDataFields.BOX_LENGTH, "100", "10", "20");

        // when
        var mergeResult = mdmErrorInfoClarifier
            .clarifyErrors(List.of(initialError, anotherInitial), List.of(detailedError));

        // then
        Assertions.assertThat(mergeResult).containsExactly(detailedError, anotherInitial);
    }

    @Test
    public void vghShouldBeClarifiedIfAtLeastOneDetailPresentInOverride() {
        // given
        var initialWeight = MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.WEIGHT_GROSS);
        var initialBoxDimensions = MbocErrors.get().excelValueIsRequired(BOX_DIMENSIONS);
        var detailedError =
            MbocErrors.get().excelValueMustBeInRange(SskuMasterDataFields.BOX_WIDTH, "100", "10", "20");

        // when
        var mergeResult = mdmErrorInfoClarifier
            .clarifyErrors(List.of(initialWeight, initialBoxDimensions), List.of(detailedError));

        // then
        Assertions.assertThat(mergeResult).containsExactly(detailedError);
    }


    @Test
    public void vghShouldNotLoseEmptyWeightIfAnotherPresentInOverride() {
        // given
        var initialWeight = MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.WEIGHT_GROSS);
        var initialBoxDimensions = MbocErrors.get().excelValueIsRequired(BOX_DIMENSIONS);
        var detailedWidth =
            MbocErrors.get().excelValueMustBeInRange(SskuMasterDataFields.BOX_WIDTH, "100", "10", "20");
        var detailedWeight = MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.WEIGHT_GROSS);


        // when
        var mergeResult = mdmErrorInfoClarifier
            .clarifyErrors(List.of(initialWeight, initialBoxDimensions), List.of(detailedWidth, detailedWeight));

        // then
        Assertions.assertThat(mergeResult).containsExactlyInAnyOrder(detailedWeight, detailedWidth);
    }

    @Test
    public void vghShouldNotBeClarifiedIfNoDetailsPresentInOverride() {
        // given
        var initialWeight = MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.WEIGHT_GROSS);
        var initialBoxDimensions = MbocErrors.get().excelValueIsRequired(BOX_DIMENSIONS);

        // when
        var mergeResult = mdmErrorInfoClarifier
            .clarifyErrors(List.of(initialWeight, initialBoxDimensions), List.of());

        // then
        Assertions.assertThat(mergeResult).containsExactly(initialWeight, initialBoxDimensions);
    }

    @Test
    public void vghShouldRemoveVghPartErrorIfNotPresentInOverride() {
        // given
        var initialWeight = MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.WEIGHT_GROSS);
        var initialBoxDimensions = MbocErrors.get().excelValueIsRequired(BOX_DIMENSIONS);
        var detailedBoxDimensions =
            MbocErrors.get().excelValueIsRequired(SskuMasterDataFields.BOX_DIMENSIONS);

        // when
        var mergeResult = mdmErrorInfoClarifier
            .clarifyErrors(List.of(initialWeight, initialBoxDimensions), List.of(detailedBoxDimensions));

        // then
        Assertions.assertThat(mergeResult).containsExactly(initialBoxDimensions);
    }

}

package ru.yandex.direct.oneshot.oneshots.bsexport;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import ru.yandex.direct.core.testing.info.AdGroupBidModifierInfo;
import ru.yandex.direct.core.testing.info.AdGroupInfo;
import ru.yandex.direct.core.testing.steps.Steps;
import ru.yandex.direct.dbschema.ppc.enums.HierarchicalMultipliersType;
import ru.yandex.direct.dbutil.wrapper.DslContextProvider;
import ru.yandex.direct.ess.logicobjects.bsexport.multipliers.BsExportMultipliersObject;
import ru.yandex.direct.ess.logicobjects.bsexport.multipliers.MultiplierType;
import ru.yandex.direct.ess.logicobjects.bsexport.multipliers.UpsertInfo;
import ru.yandex.direct.logicprocessor.processors.bsexport.multipliers.BsExportMultipliersService;
import ru.yandex.direct.oneshot.configuration.OneshotTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@OneshotTest
@RunWith(SpringRunner.class)
public class BsExportMultipliersOneshotTest {

    @Autowired
    private Steps steps;

    @Autowired
    private DslContextProvider dslContextProvider;

    private BsExportMultipliersOneshot oneshot;

    private BsExportMultipliersService bsExportMultipliersService;

    private Integer shard;
    private Long bidModifierId;

    @Before
    public void before() {
        bsExportMultipliersService = mock(BsExportMultipliersService.class);
        oneshot = new BsExportMultipliersOneshot(dslContextProvider, bsExportMultipliersService);

        AdGroupInfo activeTextAdGroup = steps.adGroupSteps().createActiveTextAdGroup();
        shard = activeTextAdGroup.getShard();
        AdGroupBidModifierInfo bidModifierInfo =
                steps.bidModifierSteps().createDefaultAdGroupBidModifierWeather(activeTextAdGroup);
        bidModifierId = bidModifierInfo.getBidModifierId();
        steps.bidModifierSteps().createDefaultAdGroupBidModifierDemographics(activeTextAdGroup);
    }

    @Test
    public void validate_Success() {
        var inputData = new BsExportMultipliersOneshot.InputData(
                List.of(HierarchicalMultipliersType.weather_multiplier.getLiteral()));
        var vr = oneshot.validate(inputData);
        assertThat(vr.flattenErrors()).isEmpty();
    }

    @Test
    public void validate_EmptyTypes() {
        var inputData = new BsExportMultipliersOneshot.InputData(List.of());
        var vr = oneshot.validate(inputData);
        assertThat(vr.flattenErrors()).isNotEmpty();
    }

    @Test
    public void validate_UnknownType() {
        var inputData = new BsExportMultipliersOneshot.InputData(List.of("xxx_bad_type_xxx"));
        var vr = oneshot.validate(inputData);
        assertThat(vr.flattenErrors()).isNotEmpty();
    }

    @Test
    public void execute_Initial() {
        var inputData = new BsExportMultipliersOneshot.InputData(
                List.of(HierarchicalMultipliersType.weather_multiplier.getLiteral()));
        BsExportMultipliersOneshot.State state = oneshot.execute(inputData, null, shard);
        assertThat(state).isNotNull();
        assertThat(state.lastHierarchicalMultiplierId).isEqualTo(0);
        verify(bsExportMultipliersService, never()).updateMultipliers(anyInt(), anyList());
    }

    @Test
    public void execute() {
        var inputData = new BsExportMultipliersOneshot.InputData(
                List.of(HierarchicalMultipliersType.weather_multiplier.getLiteral()));
        BsExportMultipliersOneshot.State state = oneshot.execute(
                inputData, new BsExportMultipliersOneshot.State(bidModifierId - 1L), shard);
        assertThat(state).isNotNull();
        assertThat(state.lastHierarchicalMultiplierId).isEqualTo(bidModifierId);
        verify(bsExportMultipliersService).updateMultipliers(shard, List.of(
                BsExportMultipliersObject.upsert(new UpsertInfo(MultiplierType.WEATHER, bidModifierId), 0L, "", "")));
    }

    @Test
    public void execute_Last() {
        var inputData = new BsExportMultipliersOneshot.InputData(
                List.of(HierarchicalMultipliersType.weather_multiplier.getLiteral()));
        BsExportMultipliersOneshot.State state = oneshot.execute(
                inputData, new BsExportMultipliersOneshot.State(bidModifierId), shard);
        assertThat(state).isNull();
        verify(bsExportMultipliersService, never()).updateMultipliers(anyInt(), anyList());
    }
}

package ru.yandex.market.mbo.mdm.common.masterdata.validator.block;

import java.math.BigDecimal;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.MasterDataSourceType;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.ItemBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.TestBlockCreationUtil;
import ru.yandex.market.mbo.mdm.common.masterdata.model.golden.blocks.sskumd.MdmParamValueBlock;
import ru.yandex.market.mbo.mdm.common.masterdata.services.param.KnownMdmParams;

public class RequiredExpiryDateValidationPreconditionTest {

    private final RequiredExpiryDateValidationPrecondition precondition =
        new RequiredExpiryDateValidationPrecondition();

    @Test
    public void whenExpiryDateBlockHasTrueConditionPass() {
        // given
        var expirDataBlock = createExpirDateBlock(true);
        var otherBlock = createOtherBlock(134);
        var validationData = new ItemBlockValidationData(List.of(expirDataBlock, otherBlock));

        // when
        var result = precondition.apply(validationData);

        // then
        Assertions.assertThat(result).contains(true);
    }

    @Test
    public void whenExpiryDateBlockHasFalseConditionNotPass() {
        // given
        var expirDataBlock = createExpirDateBlock(false);
        var otherBlock = createOtherBlock(134);
        var validationData = new ItemBlockValidationData(List.of(expirDataBlock, otherBlock));

        // when
        var result = precondition.apply(validationData);

        // then
        Assertions.assertThat(result).contains(false);
    }

    @Test
    public void whenNoExpiryDateBlockConditionReturnNoResult() {
        // given
        var otherBlock = createOtherBlock(134);
        var validationData = new ItemBlockValidationData(List.of(otherBlock));

        // when
        var result = precondition.apply(validationData);

        // then
        Assertions.assertThat(result).isEmpty();
    }


    @Test
    public void whenNoBlocksConditionReturnNoResult() {
        // given

        var validationData = new ItemBlockValidationData(List.of());

        // when
        var result = precondition.apply(validationData);

        // then
        Assertions.assertThat(result).isEmpty();
    }

    private MdmParamValueBlock<Boolean> createExpirDateBlock(boolean boolValue) {
        return TestBlockCreationUtil.createBoolParamValueBlock(
            ItemBlock.BlockType.EXPIR_DATE, KnownMdmParams.EXPIR_DATE, boolValue, MasterDataSourceType.AUTO,
            "1234", "login");
    }

    private MdmParamValueBlock<BigDecimal> createOtherBlock(int value) {
        return TestBlockCreationUtil.createNumericMdmParamValueBlock(
            KnownMdmParams.MIN_SHIPMENT,
            BigDecimal.valueOf(value)
        );
    }
}

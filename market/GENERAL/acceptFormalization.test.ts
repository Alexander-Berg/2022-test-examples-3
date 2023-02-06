import { acceptFormalization, getModelsWithFormalization } from './acceptFormalization';
import { shopModel, vendorFormalizationValue, manualValues, ruleHypotheses } from 'src/test/data';
import { VENDOR_PARAMETER_ID, NAME_PARAMETER_ID } from 'src/constants';
import { ShopModelView, ValueSource } from 'src/java/definitions';

const modelWithFormalization = {
  ...shopModel,
  marketValues: { [VENDOR_PARAMETER_ID]: [vendorFormalizationValue] },
};
const modelWithoutValues = { ...shopModel, marketValues: {} };
const modelWithManualValues = { ...shopModel, marketValues: { [VENDOR_PARAMETER_ID]: manualValues } };
const modelWithRuleHypotheses: ShopModelView = {
  ...shopModel,
  marketValues: { [VENDOR_PARAMETER_ID]: [ruleHypotheses] },
};

describe('shopModelsUtils', () => {
  test('getModelsWithFormalization with parameter', () => {
    expect(getModelsWithFormalization([modelWithFormalization], VENDOR_PARAMETER_ID)).toHaveLength(1);
    expect(getModelsWithFormalization([modelWithFormalization], NAME_PARAMETER_ID)).toHaveLength(0);
  });

  test('Подтверждение формализации', () => {
    const { updatedModels, rulesToConfirm } = acceptFormalization(
      [modelWithFormalization, modelWithoutValues, modelWithManualValues],
      VENDOR_PARAMETER_ID
    );

    // обновиться должны только товары с маркетными значениями среди которых есть формализация, остальные товары игноряться
    expect(updatedModels).toHaveLength(1);
    expect(rulesToConfirm.size).toBe(0);

    // ValueSource меняется с FORMALIZATION на MANUAL
    expect(updatedModels[0].marketValues[VENDOR_PARAMETER_ID][0].valueSource).toBe(ValueSource.MANUAL);
  });

  test('Подтверждение гипотез правил', () => {
    const { updatedModels, rulesToConfirm } = acceptFormalization(
      [modelWithRuleHypotheses, modelWithoutValues, modelWithManualValues],
      VENDOR_PARAMETER_ID
    );

    expect(updatedModels).toHaveLength(1);
    expect(updatedModels[0].marketValues[VENDOR_PARAMETER_ID][0].valueSource).toBe(ValueSource.RULE);
    // у одного из товаров есть гипотезы правил, их нужно подтверждать отдельно, поэтому ожидаем такой результат
    expect(rulesToConfirm.size).toBe(1);
    expect(rulesToConfirm.has(ruleHypotheses.ruleId)).toBeTruthy();
  });
});

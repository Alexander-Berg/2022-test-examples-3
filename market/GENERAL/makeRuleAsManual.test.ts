import { makeRuleAsManual } from './makeRuleAsManual';
import { shopModel, vendorFormalizationValue, manualValues } from 'src/test/data';
import { VENDOR_PARAMETER_ID } from 'src/constants';
import { ValueSource } from 'src/java/definitions';

const modelWithFormalization = {
  ...shopModel,
  marketValues: { [VENDOR_PARAMETER_ID]: [vendorFormalizationValue] },
};
const modelWithoutValues = { ...shopModel, marketValues: {} };
const modelWithManualValues = { ...shopModel, marketValues: { [VENDOR_PARAMETER_ID]: manualValues } };

describe('shopModelsUtils', () => {
  test('makeRuleAsManual меняем массово правила на ручные значения', () => {
    const updatedModels = makeRuleAsManual([shopModel], VENDOR_PARAMETER_ID);

    // не должно быть правил для этого параметра
    expect(
      updatedModels[0].marketValues[VENDOR_PARAMETER_ID].filter(el => el.valueSource === ValueSource.RULE)
    ).toHaveLength(0);

    // значение должно быть ручным
    expect(updatedModels[0].marketValues[VENDOR_PARAMETER_ID][0].valueSource).toBe(ValueSource.MANUAL);

    expect(updatedModels).toHaveLength(1);
  });

  test('Меняем массово правила на ручные значения, некоторые товары должны пропускаться', () => {
    expect(
      makeRuleAsManual(
        [shopModel, modelWithoutValues, modelWithFormalization, modelWithManualValues],
        VENDOR_PARAMETER_ID
      )
    ).toHaveLength(1);
  });
});

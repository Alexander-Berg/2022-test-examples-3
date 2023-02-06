import { updateMarketValues } from './updateMarketValues';
import { shopModel, vendorFormalizationValue } from 'src/test/data';
import { VENDOR_PARAMETER_ID } from 'src/constants';

const modelWithFormalization = {
  ...shopModel,
  marketValues: { [VENDOR_PARAMETER_ID]: [vendorFormalizationValue] },
};

describe('shopModelsUtils', () => {
  test('updateMarketValues change value', () => {
    const vendorName = 'Philips';
    const newMarketValues = [{ optionId: 1, hypothesis: vendorName }];
    const updatedModels = updateMarketValues([modelWithFormalization], VENDOR_PARAMETER_ID, newMarketValues);
    const updatedMarketValue = updatedModels[0].marketValues[VENDOR_PARAMETER_ID];

    expect(updatedMarketValue.length).toEqual(1);
    expect(updatedMarketValue[0].value.hypothesis).toEqual(vendorName);
  });

  test('updateMarketValues clear value', () => {
    const updatedModels = updateMarketValues([modelWithFormalization], VENDOR_PARAMETER_ID, []);
    const updatedMarketValue = updatedModels[0].marketValues[VENDOR_PARAMETER_ID];

    expect(updatedMarketValue).toBe(undefined);
  });
});

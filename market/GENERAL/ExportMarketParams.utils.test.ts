import { getMultiValues, getParamValues, getMarketParamsFields } from './ExportMarketParams.utils';
import { rules, shopModel, parameter, formalizationValues, vendorParameter, categoryData } from 'src/test/data';

describe('utils', () => {
  test('getMultiValues', () => {
    const values = getMultiValues(rules, 'hypothesis');
    expect(values).toBe(rules[0].value.hypothesis);
  });

  test('getParamValues', () => {
    const values = getParamValues(shopModel, vendorParameter);
    expect(values).toBe(rules[0].value.hypothesis);
  });

  test('getParamValues формализованые значения не должны проходить', () => {
    const marketParams = {
      ...shopModel,
      marketValues: {
        [parameter.id]: formalizationValues,
      },
    };
    const values = getParamValues(marketParams, parameter);
    expect(values).toBeFalsy();
  });

  test('getMarketParamsFields', () => {
    const fields = getMarketParamsFields(categoryData);
    expect(fields.map(el => el.label)).toEqual(categoryData.parameters.map(el => el.name));
  });
});

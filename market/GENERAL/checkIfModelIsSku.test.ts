import { checkIfModelIsSku } from './checkIfModelIsSku';
import { IS_SKU_PARAM_ID } from 'src/utils/constants';

describe('checkIfModelIsSku', () => {
  it('works with invalid data', () => {
    expect(checkIfModelIsSku()).toEqual(false);
    expect(checkIfModelIsSku({})).toEqual(false);
  });
  it('works with valid data', () => {
    expect(checkIfModelIsSku({ parameter_values: [] })).toEqual(false);
    expect(checkIfModelIsSku({ parameter_values: [{ param_id: IS_SKU_PARAM_ID, bool_value: false }] })).toEqual(false);
    expect(checkIfModelIsSku({ parameter_values: [{ param_id: IS_SKU_PARAM_ID, bool_value: true }] })).toEqual(true);
  });
});

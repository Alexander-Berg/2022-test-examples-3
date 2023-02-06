import { IS_SKU_PARAM_ID } from 'src/pages/ModelEditorCluster/constants';
import { checkIfModelIsSku } from 'src/pages/ModelEditorCluster/utils/model/checkIfModelIsSku';

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

import { getNameParameterValue, MODEL_NAME_PARAM_ID } from './getNameParameterValue';

describe('getNameParameterValue', () => {
  it('works', () => {
    expect(getNameParameterValue({})).toBeUndefined();
    expect(getNameParameterValue({ parameter_values: [] })).toBeUndefined();
    expect(getNameParameterValue({ parameter_values: [{ param_id: MODEL_NAME_PARAM_ID, str_value: [] }] })).toEqual([]);

    const TEST_STRING_VALUE = [{ isoCode: 'ru', value: 'test' }];
    expect(
      getNameParameterValue({
        parameter_values: [{ param_id: MODEL_NAME_PARAM_ID, str_value: TEST_STRING_VALUE }],
      })
    ).toEqual(TEST_STRING_VALUE);
  });
});

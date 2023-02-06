import { getPickerOptionIds } from './getPickerOptionIds';

describe('getPickerOptionIds', () => {
  it('works', () => {
    expect(getPickerOptionIds([], 123)).toEqual([]);
    expect(
      getPickerOptionIds(
        [
          { parameter_values: [{ param_id: 123, option_id: 123 }] },
          { parameter_values: [{ param_id: 123, option_id: 123 }] },
          { parameter_values: [{ param_id: 123, option_id: 234 }] },
        ],
        123
      )
    ).toEqual([123, 234]);
  });
});

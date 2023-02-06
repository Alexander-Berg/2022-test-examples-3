import { getModelPickerOptionIds } from './getModelPickerOptionIds';

describe('getModelPickerOptionIds', () => {
  it('works', () => {
    expect(getModelPickerOptionIds({})).toEqual([]);
    expect(getModelPickerOptionIds({ parameter_value_links: [{ option_id: 123 }, { option_id: 312 }] })).toEqual([
      123,
      312,
    ]);
  });
});

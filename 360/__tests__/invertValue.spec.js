import invertValue from 'utils/invertValue';

describe('invertValue', () => {
  test('должен работать', () => {
    expect(invertValue(true)).toBe(false);
    expect(invertValue(false)).toBe(true);
  });
});

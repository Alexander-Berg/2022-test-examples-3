import { countName } from './localization';

describe('utils/localization.ts', () => {
  const names = ['офферов', 'оффер', 'оффера'];
  it.each([
    [0, names[0]],
    [1, names[1]],
    [4, names[2]],
    [5, names[0]],
    [11, names[0]],
    [21, names[1]],
    [22, names[2]],
    [29, names[0]],
    [101, names[1]],
    [103, names[2]],
    [111, names[0]],
  ])('countName(%i) = %p', (count, expected) => {
    expect(countName(count, names)).toBe(expected);
  });
});

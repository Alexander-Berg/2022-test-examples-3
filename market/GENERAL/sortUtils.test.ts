import { numberSort, stringSort } from './sortUtils';

describe('sortUtils', () => {
  it.each([
    [
      [0, -1, 1],
      [-1, 0, 1],
    ],
    [
      [1, 1, 1],
      [1, 1, 1],
    ],
    [
      [-1, -2, -3],
      [-3, -2, -1],
    ],
    [
      [0, undefined, 6],
      [0, 6, undefined],
    ],
    [
      [undefined, undefined, undefined, -1],
      [-1, undefined, undefined, undefined],
    ],
  ])('%s number sorted as %s', (a: number[], expected: number[]) => {
    expect(a.sort(numberSort)).toEqual(expected);
  });

  it.each([
    [
      ['10', '1', '1'],
      ['1', '1', '10'],
    ],
    [
      ['qwe', '', 'rty'],
      ['', 'qwe', 'rty'],
    ],
    [
      [undefined, '', undefined, 'qwe'],
      ['', 'qwe', undefined, undefined],
    ],
  ])('%s string sorted as %s', (a: string[], expected: string[]) => {
    expect(a.sort(stringSort)).toEqual(expected);
  });
});

import { ContainsOperator } from './ContainsOperator';
import { StartsWithOperator } from './StartsWithOperator';
import { EqualsOperator } from './EqualsOperator';
import { BetweenOperator } from './BetweenOperator';
import { NumberBetweenOperator } from './NumberBetweenOperator';
import { NotEqualsOperator } from './NotEqualsOperator';
import { IncludesAnyOperator } from './IncludesAnyOperator';
import { NotContainsOperator } from './NotContainsOperator';

describe('complex filters operators', () => {
  it.each([
    [['TeStiK'], 'test', true],
    [['TeStiK'], 'testo', false],
    [['TeStiK'], 123, false],
    [[123456], 123, true],
    [[123], undefined, true],
    [[undefined], 123, false],
  ])('%s contains %s = %d', (a: string[], b: string, expected: boolean) => {
    const operator = new ContainsOperator();
    const result = operator.apply(a, { value: b });
    expect(result).toBe(expected);
  });

  it.each([
    [['TeStiK'], 'test', false],
    [['TeStiK'], 'testo', true],
    [['TeStiK'], 123, true],
    [[123456], 123, false],
    [[123], undefined, true],
    [[undefined], 123, true],
  ])('%s does not contains %s = %d', (a: string[], b: string, expected: boolean) => {
    const operator = new NotContainsOperator();
    const result = operator.apply(a, { value: b });
    expect(result).toBe(expected);
  });

  it.each([
    [['TeStiK'], 'test', true],
    [['TeStiK'], 'est', false],
    [['TeStiK', 'estonia'], 'est', true],
    [['TeStiK'], 123, false],
    [[123456], 123, true],
  ])('%s starts with %s = %d', (a: string[], b: string, expected: boolean) => {
    const operator = new StartsWithOperator();
    const result = operator.apply(a, { value: b });
    expect(result).toBe(expected);
  });

  it.each([
    [['TeStiK'], 'test', false],
    [['TeStiK'], 'test,testik', true],
    [['TeStiK'], 'testik', true],
    [['123'], 123, true],
    [[123], 123, true],
    [[123], '123,456', true],
    [[123], '12,123', true],
    [[123], 456, false],
    [[123], undefined, true],
  ])('%s equals %s = %d', (a: string[], b: string, expected: boolean) => {
    const operator = new EqualsOperator();
    const result = operator.apply(a, { value: b });
    expect(result).toBe(expected);
  });

  it.each([
    [['TeStiK'], 'test', true],
    [['TeStiK'], 'test,testik', false],
    [['TeStiK'], 'testik', false],
    [['123'], 123, false],
    [[123], 123, false],
    [[123], '123,456', false],
    [[123], '12,123', false],
    [[123], 456, true],
  ])('%s does not equal %s = %d', (a: string[], b: string, expected: boolean) => {
    const operator = new NotEqualsOperator();
    const result = operator.apply(a, { value: b });
    expect(result).toBe(expected);
  });

  it.each([
    [[new Date(2000, 0, 2)], new Date(2000, 0, 1), new Date(2000, 0, 2, 1), true],
    [[new Date(2000, 0, 2, 12)], new Date(2000, 0, 1), new Date(2000, 0, 1, 23, 59, 59, 999), false],
    [[new Date(2000, 0, 2)], new Date(2000, 0, 1), undefined, true],
    [[new Date(2000, 0, 1)], new Date(2000, 0, 2, 12), undefined, false],
    [[new Date(2000, 0, 2)], undefined, new Date(2000, 0, 1), false],
    [[new Date(2000, 0, 2)], new Date(2000, 0, 1), undefined, true],
    [[undefined], new Date(2000, 0, 1), undefined, false],
  ])('%s between [%s, %s] = %d', (a: Date[], b: Date, c: Date, expected: boolean) => {
    const operator = new BetweenOperator();
    const result = operator.apply(
      a.map(d => d?.getTime()),
      { from: b, to: c }
    );
    expect(result).toBe(expected);
  });

  it.each([
    [[123], -123, 456, true],
    [[123], 123, 456, true],
    [[123], 124, 456, false],
    [[123], 1, undefined, true],
    [[123], undefined, 123, true],
    [[undefined], undefined, 123, false],
  ])(
    'number %i between [%i %i] = %d',
    (a: (number | undefined)[], b: number | undefined, c: number | undefined, expected: boolean) => {
      const operator = new NumberBetweenOperator();
      const result = operator.apply(a as number[], { from: b, to: c });
      expect(result).toBe(expected);
    }
  );

  it.each([
    [[123], [123], true],
    [[123], [456], false],
    [[], [456], false],
    [[1], [1, 2], true],
    [[1], [], true],
    [[1], undefined, true],
  ])('%s includes [%s] = %d', (a: number[], b: number[], expected: boolean) => {
    const operator = new IncludesAnyOperator<number>();
    const result = operator.apply(a, { value: b });
    expect(result).toBe(expected);
  });
});

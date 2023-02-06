import { stringContainsSubstring } from './string';

describe('string utils', () => {
  it.each([
    ['Testik', 'test', false, true],
    ['Testik', 'test', true, false],
    [undefined, 'test', false, false],
    ['Testik', undefined, false, false],
    [undefined, undefined, false, false],
    ['est', 'Testovich', false, false],
    ['', '', false, false],
    ['', '', true, false],
  ])(
    'stringContainsSubstring(%s, %s, %s) equals %s',
    (source: string | undefined, search: string | undefined, caseSensitive: boolean, result: boolean) => {
      expect(stringContainsSubstring(source, search, caseSensitive)).toStrictEqual(result);
    }
  );
});

import { getParams, getOrigin, getPreviousPath, buildUrl } from '../params';

describe('getParams', () => {
  test('should return empty object when string has no params', () => {
    const input1 = 'http://example.com';
    const input2 = 'http://example.com?';

    expect(getParams(input1)).toEqual({});
    expect(getParams(input2)).toEqual({});
  });

  test('should return object of parameters when has parameters in string', () => {
    const input = 'https://example.com?key=value&otherKey=otherValue';

    expect(getParams(input)).toEqual({ key: 'value', otherKey: 'otherValue' });
  });
});

describe('getOrigin', () => {
  test("should return url's origin on valid input", () => {
    const input1 = 'http://example.com/captcha?params';
    const input2 = 'https://example.com?params=1';

    expect(getOrigin(input1)).toBe('http://example.com');
    expect(getOrigin(input2)).toBe('https://example.com');
  });

  test('shoud return empty string on invalid input', () => {
    const input1 = 'htt://example.com/';
    const input2 = 'example.com';
    const input3 = 'example';

    expect(getOrigin(input1)).toBe('');
    expect(getOrigin(input2)).toBe('');
    expect(getOrigin(input3)).toBe('');
  });
});

describe('buildUrl', () => {
  test('should build url', () => {
    const input1 = 'http://origin.com';
    const input2 = 'http://origin.com/';
    const inputObj = {
      q1: 'v1',
      q2: 'v2',
    };

    expect(buildUrl(input1, inputObj)).toBe('http://origin.com?q1=v1&q2=v2');
    expect(buildUrl(input2, inputObj)).toBe('http://origin.com/?q1=v1&q2=v2');
  });

  test('should remove undefined values', () => {
    const input = 'http://origin.com';
    const inputObj = {
      q1: 'v1',
      q2: 'v2',
      q3: undefined,
      q4: undefined,
    } as unknown as Record<string, string>;

    expect(buildUrl(input, inputObj)).toBe('http://origin.com?q1=v1&q2=v2');
  });
});

describe('getPrevoiusPath', () => {
  test('should return prevoius path of the URL', () => {
    const url = 'http://example.com/path/to/file';

    expect(getPreviousPath(url)).toBe('http://example.com/path/to');
  });
});

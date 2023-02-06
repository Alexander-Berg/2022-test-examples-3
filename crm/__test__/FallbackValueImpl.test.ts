import { FallbackValueImpl } from '../FallbackValueImpl';

describe('FallbackValueImpl', () => {
  test('true value', () => {
    const value = new FallbackValueImpl(true);

    expect(value.value).toBe(true);
    expect(value.toString()).toBe('on');
    expect(value.toJSON()).toBe(true);
  });

  test('false value', () => {
    const value = new FallbackValueImpl(false);

    expect(value.value).toBe(false);
    expect(value.toString()).toBe('off');
    expect(value.toJSON()).toBe(false);
  });

  test('"on" value', () => {
    const value = new FallbackValueImpl('on');

    expect(value.value).toBe(true);
    expect(value.toString()).toBe('on');
    expect(value.toJSON()).toBe(true);
  });

  test('"off" value', () => {
    const value = new FallbackValueImpl('off');

    expect(value.value).toBe(false);
    expect(value.toString()).toBe('off');
    expect(value.toJSON()).toBe(false);
  });
});

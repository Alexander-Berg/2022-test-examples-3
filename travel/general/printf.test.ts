import { printf } from './printf';

test('should format different inputs', () => {
    expect(printf('foo', [])).toBe('foo');
    expect(printf('foo', ['bar'])).toBe('foo');
    expect(printf('foo %s', ['bar'])).toBe('foo bar');
    // Это корректно?
    expect(printf('foo %d', ['bar'])).toBe('foo NaN');
    expect(printf('foo %d %s %d', ['6', 'of', 10])).toBe('foo 6 of 10');
});

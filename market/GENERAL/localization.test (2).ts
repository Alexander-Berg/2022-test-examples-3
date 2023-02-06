import { countName } from 'src/utils/localization';

describe('localization', () => {
  it('provides correct names for russian multiples', () => {
    const names = ['яблок', 'яблоко', 'яблока']; // 0 яблок, 1 яблоко, 2 яблока
    expect(countName(0, names)).toBe('яблок');
    expect(countName(1, names)).toBe('яблоко');
    expect(countName(2, names)).toBe('яблока');
    expect(countName(3, names)).toBe('яблока');
    expect(countName(4, names)).toBe('яблока');
    expect(countName(5, names)).toBe('яблок');
    expect(countName(9, names)).toBe('яблок');
    expect(countName(10, names)).toBe('яблок');
    expect(countName(11, names)).toBe('яблок');
    expect(countName(19, names)).toBe('яблок');
    expect(countName(20, names)).toBe('яблок');
    expect(countName(21, names)).toBe('яблоко');
    expect(countName(22, names)).toBe('яблока');
    expect(countName(23, names)).toBe('яблока');
    expect(countName(25, names)).toBe('яблок');
  });
});

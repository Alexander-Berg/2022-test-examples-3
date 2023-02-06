import { BaseDataSource } from './BaseDataSource';

describe('BaseDataSource', () => {
  it.each([
    ['(qwe\rty\testik \r\t\ff \\zzz"}}', '(qwe\rty\testik \r\t\ff \\\\zzz"}}'],
    [
      '"324\\\\333\\\\1622","":"Игрушки\\\\Куклы и аксессуары\\\\Куклы модельные"}',
      '"324\\\\333\\\\1622","":"Игрушки\\\\Куклы и аксессуары\\\\Куклы модельные"}',
    ],
    [
      '"324\\333\\1622","":"Игрушки\\Куклы и аксессуары\\Куклы модельные"}',
      '"324\\\\333\\\\1622","":"Игрушки\\\\Куклы и аксессуары\\\\Куклы модельные"}',
    ],
    ['Testik', 'Testik'],
    [
      '\\xFE\\xFF\\xFF\\xFF\\u0001\\u0000\\u0000\\u0000\\u0001\\u0000\\u0000\\u0000',
      '\\\\xFE\\\\xFF\\\\xFF\\\\xFF\\\\u0001\\\\u0000\\\\u0000\\\\u0000\\\\u0001\\\\u0000\\\\u0000\\\\u0000',
    ],
    ['QWE\\', 'QWE\\\\'],
  ])('prepareJsonString(%s)', (text: string, expected: string) => {
    expect(BaseDataSource.prepareJsonString(text)).toBe(expected);
  });
});

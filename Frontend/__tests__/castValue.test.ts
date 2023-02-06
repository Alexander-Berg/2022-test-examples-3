import { castValue, EFlagType } from 'neo/lib/experiments/castValue';

const UNEXPECTED_ITS_VALUES = [
  true,
  false,
  ['__arrayItem__'],
  { prop: '__value__' },
];

const EMPTY_VALUES = [
  undefined,
  null,
  '',
  ' ',
  '  ',
];

describe('castValue', () => {
  it('не приводит пустое значение к строке', () => {
    EMPTY_VALUES.forEach((value) => {
      expect(castValue(value, EFlagType.STRING)).toBe(undefined);
    });
  });

  it('не приводит неожиданные значения из ITS к строке', () => {
    UNEXPECTED_ITS_VALUES.forEach((value) => {
      expect(castValue(value, EFlagType.STRING)).toBe(undefined);
    });
  });

  it('приводит к строке', () => {
    expect(castValue('__str__', EFlagType.STRING)).toBe('__str__');
    expect(castValue('123', EFlagType.STRING)).toBe('123');
    expect(castValue(123, EFlagType.STRING)).toBe('123');
    expect(castValue(1.2, EFlagType.STRING)).toBe('1.2');
    expect(castValue(0, EFlagType.STRING)).toBe('0');
    expect(castValue(-1.2, EFlagType.STRING)).toBe('-1.2');
  });

  it('не приводит пустое значение к числу', () => {
    EMPTY_VALUES.forEach((value) => {
      expect(castValue(value, EFlagType.NUMBER)).toBe(undefined);
    });
  });

  it('не приводит неожиданные значения из ITS к числу', () => {
    UNEXPECTED_ITS_VALUES.forEach((value) => {
      expect(castValue(value, EFlagType.NUMBER)).toBe(undefined);
    });
  });

  it('приводит к числу', () => {
    expect(castValue('123', EFlagType.NUMBER)).toBe(123);
    expect(castValue('1.2', EFlagType.NUMBER)).toBe(1.2);
    expect(castValue('0', EFlagType.NUMBER)).toBe(0);
    expect(castValue('-1.2', EFlagType.NUMBER)).toBe(-1.2);
    expect(castValue(123, EFlagType.NUMBER)).toBe(123);
    expect(castValue(1.2, EFlagType.NUMBER)).toBe(1.2);
    expect(castValue(0, EFlagType.NUMBER)).toBe(0);
    expect(castValue(-1.2, EFlagType.NUMBER)).toBe(-1.2);
  });

  it('не приводит некорректные строки к числу', () => {
    expect(castValue('abc', EFlagType.NUMBER)).toBe(undefined);
    expect(castValue('_1', EFlagType.NUMBER)).toBe(undefined);
    expect(castValue('1_', EFlagType.NUMBER)).toBe(undefined);
    expect(castValue('1,2', EFlagType.NUMBER)).toBe(undefined);
  });

  it('приводит пустое значение к false', () => {
    EMPTY_VALUES.forEach((value) => {
      expect(castValue(value, EFlagType.BOOL)).toBe(false);
    });
  });

  it('приводит значение 0 к false', () => {
    expect(castValue(0, EFlagType.BOOL)).toBe(false);
    expect(castValue('0', EFlagType.BOOL)).toBe(false);
  });

  it('приводит false из ITS к false', () => {
    expect(castValue(false, EFlagType.BOOL)).toBe(false);
  });

  it('приводит непустые значения к true', () => {
    expect(castValue('1', EFlagType.BOOL)).toBe(true);
    expect(castValue('a', EFlagType.BOOL)).toBe(true);
    expect(castValue('false', EFlagType.BOOL)).toBe(true);
    expect(castValue(1, EFlagType.BOOL)).toBe(true);
    expect(castValue(true, EFlagType.BOOL)).toBe(true);
    expect(castValue([], EFlagType.BOOL)).toBe(true);
    expect(castValue({}, EFlagType.BOOL)).toBe(true);
  });

  it('приводит неожиданные значения из ITS к пустому списку строк', () => {
    UNEXPECTED_ITS_VALUES.forEach((value) => {
      expect(castValue(value, EFlagType.STRING_ARRAY)).toEqual([]);
    });
  });

  it('не приводит неожиданные значения из ITS к пустому списку строк', () => {
    UNEXPECTED_ITS_VALUES.forEach((value) => {
      expect(castValue(value, EFlagType.STRING_OPTIONAL_ARRAY)).toEqual(undefined);
    });
  });

  it('приводит пустые значения к пустому списку строк', () => {
    EMPTY_VALUES.forEach((value) => {
      expect(castValue(value, EFlagType.STRING_ARRAY)).toEqual([]);
    });
  });

  it('не приводит пустые значения к пустому списку строк', () => {
    EMPTY_VALUES.forEach((value) => {
      expect(castValue(value, EFlagType.STRING_OPTIONAL_ARRAY)).toEqual(undefined);
    });
  });

  it('приводит одиночное значение к списку строк', () => {
    expect(castValue('__one__', EFlagType.STRING_ARRAY)).toEqual(['__one__']);
    expect(castValue(123, EFlagType.STRING_ARRAY)).toEqual(['123']);
    expect(castValue('__one__', EFlagType.STRING_OPTIONAL_ARRAY)).toEqual(['__one__']);
    expect(castValue(123, EFlagType.STRING_OPTIONAL_ARRAY)).toEqual(['123']);
  });

  it('приводит к списку строк', () => {
    expect(castValue('__one__,__two__', EFlagType.STRING_ARRAY)).toEqual(['__one__', '__two__']);
    expect(castValue('__one__,__two__', EFlagType.STRING_OPTIONAL_ARRAY))
      .toEqual(['__one__', '__two__']);
  });

  it('игнорирует пустые строки в списке строк', () => {
    expect(castValue('__one__,,__two__', EFlagType.STRING_ARRAY)).toEqual(['__one__', '__two__']);
    expect(castValue('__one__,,__two__', EFlagType.STRING_OPTIONAL_ARRAY))
      .toEqual(['__one__', '__two__']);
  });

  it('тримит пробелы в списке строк', () => {
    expect(castValue(' __one__ , , __two__ ', EFlagType.STRING_ARRAY))
      .toEqual(['__one__', '__two__']);
    expect(castValue(' __one__ , , __two__ ', EFlagType.STRING_OPTIONAL_ARRAY))
      .toEqual(['__one__', '__two__']);
  });

  it('приводит неожиданные значения из ITS к пустому списку чисел', () => {
    UNEXPECTED_ITS_VALUES.forEach((value) => {
      expect(castValue(value, EFlagType.NUMBER_ARRAY)).toEqual([]);
    });
  });

  it('не приводит неожиданные значения из ITS к пустому списку чисел', () => {
    UNEXPECTED_ITS_VALUES.forEach((value) => {
      expect(castValue(value, EFlagType.NUMBER_OPTIONAL_ARRAY)).toEqual(undefined);
    });
  });

  it('приводит пустые значения к пустому списку чисел', () => {
    EMPTY_VALUES.forEach((value) => {
      expect(castValue(value, EFlagType.NUMBER_ARRAY)).toEqual([]);
    });
  });

  it('не приводит пустые значения к пустому списку чисел', () => {
    EMPTY_VALUES.forEach((value) => {
      expect(castValue(value, EFlagType.NUMBER_OPTIONAL_ARRAY)).toEqual(undefined);
    });
  });

  it('приводит одиночное значение к списку чисел', () => {
    expect(castValue('123', EFlagType.NUMBER_ARRAY)).toEqual([123]);
    expect(castValue(123, EFlagType.NUMBER_ARRAY)).toEqual([123]);
    expect(castValue('123', EFlagType.NUMBER_OPTIONAL_ARRAY)).toEqual([123]);
    expect(castValue(123, EFlagType.NUMBER_OPTIONAL_ARRAY)).toEqual([123]);
  });

  it('приводит к списку чисел', () => {
    expect(castValue('123,456', EFlagType.NUMBER_ARRAY)).toEqual([123, 456]);
    expect(castValue('123,456', EFlagType.NUMBER_OPTIONAL_ARRAY)).toEqual([123, 456]);
  });

  it('игнорирует нечисловые значения в списке чисел', () => {
    expect(castValue('123,abc,_1,1_,4,5,6', EFlagType.NUMBER_ARRAY)).toEqual([123, 4, 5, 6]);
    expect(castValue('123,abc,_1,1_,4,5,6', EFlagType.NUMBER_OPTIONAL_ARRAY))
      .toEqual([123, 4, 5, 6]);
  });

  it('игнорирует пустые строки в списке чисел', () => {
    expect(castValue('1,,3', EFlagType.NUMBER_ARRAY)).toEqual([1, 3]);
    expect(castValue('1,,3', EFlagType.NUMBER_OPTIONAL_ARRAY)).toEqual([1, 3]);
  });

  it('игнорирует пробелы в списке чисел', () => {
    expect(castValue(' 1 , , 3 ', EFlagType.NUMBER_ARRAY)).toEqual([1, 3]);
    expect(castValue(' 1 , , 3 ', EFlagType.NUMBER_OPTIONAL_ARRAY)).toEqual([1, 3]);
  });

  it('не фильтрует значение "0" в списке чисел', () => {
    expect(castValue('1,0,3', EFlagType.NUMBER_ARRAY)).toEqual([1, 0, 3]);
    expect(castValue('1,0,3', EFlagType.NUMBER_OPTIONAL_ARRAY)).toEqual([1, 0, 3]);
  });
});

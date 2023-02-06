import { createCompareFn, sort } from '@/utils/sort';

describe('Тестирование утилиты сортировки', () => {
  it('Функция сортировки не должен мутировать исходный массив', () => {
    const unsorted = [1, 3, 2];

    expect(sort(unsorted)).not.toBe(unsorted);
  });

  it('Функция сортировки должен сортировать по возрастанию по-умолчанию', () => {
    expect(sort([1, 3, 2])).toEqual([1, 2, 3]);
    expect(sort(['1', '3', '2'])).toEqual(['1', '2', '3']);
    expect(
      sort([
        '2012-12-21T02:41:33Z',
        '2012-01-21T02:41:32Z',
        '2012-12-01T02:41:33Z',
        '2012-12-01T02:41:34Z',
      ]),
    ).toEqual([
      '2012-01-21T02:41:32Z',
      '2012-12-01T02:41:33Z',
      '2012-12-01T02:41:34Z',
      '2012-12-21T02:41:33Z',
    ]);
  });

  it('Функция сортировки может принимать порядок сортировки вторым параметром', () => {
    expect(sort([1, 3, 2], 'ASC')).toEqual([1, 2, 3]);
    expect(sort([1, 3, 2], 'DESC')).toEqual([3, 2, 1]);
  });

  it('Функция сортировки может принимать функцию-компаратор вторым параметром', () => {
    const absComparator = (a: number, b: number) => Math.abs(a) > Math.abs(b);

    expect(sort([1, -3, 2], 'ASC')).toEqual([-3, 1, 2]);
    expect(sort([1, -3, 2], absComparator)).toEqual([1, 2, -3]);
  });

  it('Функция сортировки может принимать функцию-трансформатор/модификатор/экстрактор третьим параметром', () => {
    class Nested {
      static resolve(struct: Nested) {
        return struct.nested;
      }
      nested: number;
      constructor(n: number) {
        this.nested = n;
      }
    }

    expect(sort([1, -3, 2], 'ASC')).toEqual([-3, 1, 2]);
    expect(sort([1, -3, 2], 'ASC', Math.abs)).toEqual([1, 2, -3]);
    expect(sort(['1', '-3', '2'], 'ASC', Number)).toEqual(['-3', '1', '2']);
    expect(
      sort(
        [new Nested(1), new Nested(-3), new Nested(2)],
        'ASC',
        Nested.resolve,
      ),
    ).toEqual([new Nested(-3), new Nested(1), new Nested(2)]);
  });
});

describe('Тестирование утилиты для создания функции сравнения', () => {
  it('Функция сравнения возвращает верные значения при передаче порядка сортировки', () => {
    const ascComparator = createCompareFn('ASC');
    const descComparator = createCompareFn('DESC');

    expect(ascComparator(2, 1)).toBe(1);
    expect(ascComparator(1, 2)).toBe(-1);
    expect(ascComparator(1, 1)).toBe(0);
    expect(descComparator(1, 2)).toBe(1);
    expect(descComparator(2, 1)).toBe(-1);
    expect(descComparator(2, 2)).toBe(0);
  });

  it('Функция сравнения возвращает верные значения при передаче более комплексного компаратора', () => {
    const absAscComparator = createCompareFn(
      (a: number, b: number) => Math.abs(a) > Math.abs(b),
    );

    expect(absAscComparator(2, 1)).toBe(1);
    expect(absAscComparator(1, 2)).toBe(-1);
    expect(absAscComparator(1, 1)).toBe(0);
    expect(absAscComparator(1, -2)).toBe(-1);
    expect(absAscComparator(-1, 2)).toBe(-1);
    expect(absAscComparator(-2, 1)).toBe(1);
    expect(absAscComparator(2, -1)).toBe(1);
  });

  it('Функция сравнения возвращает верные значения при передаче функции-трансформатора/модификатора/экстрактора', () => {
    const absAscComparator = createCompareFn('ASC', Math.abs);

    expect(absAscComparator(2, 1)).toBe(1);
    expect(absAscComparator(1, 2)).toBe(-1);
    expect(absAscComparator(1, 1)).toBe(0);
    expect(absAscComparator(-1, 1)).toBe(0);
    expect(absAscComparator(1, -1)).toBe(0);
    expect(absAscComparator(1, -2)).toBe(-1);
    expect(absAscComparator(-1, 2)).toBe(-1);
  });
});

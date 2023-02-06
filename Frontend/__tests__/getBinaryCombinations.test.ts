import { getBinaryCombinations } from '../getBinaryCombinations';

interface ITest {
  name: string;
  expected: Record<string, number>[];
  keys: TKey[];
  emptyValue: number;
}

const tests: ITest[] = [
  { name: 'пустое множество', keys: [], expected: [], emptyValue: 0 },
  { name: 'одно значение', keys: ['a'], expected: [{ a: 0 }, { a: 4 }], emptyValue: 0 },
  {
    name: 'два значения',
    keys: ['a', 'b'],
    expected: [
      { a: 2, b: 2 },
      { a: 4, b: 2 },
      { a: 2, b: 5 },
      { a: 4, b: 5 },
    ],
    emptyValue: 2,
  },
  {
    name: 'три значения',
    keys: ['a', 'b', 'c'],
    expected: [
      { a: 3, b: 3, c: 3 },
      { a: 4, b: 3, c: 3 },
      { a: 3, b: 5, c: 3 },
      { a: 4, b: 5, c: 3 },
      { a: 3, b: 3, c: 6 },
      { a: 4, b: 3, c: 6 },
      { a: 3, b: 5, c: 6 },
      { a: 4, b: 5, c: 6 },
    ],
    emptyValue: 3,
  },
];

type TKey = 'a' | 'b' | 'c'

function getSlotValue(key: TKey) {
  const values = { a: 4, b: 5, c: 6 };

  return values[key];
}

describe('binaryPlacement', () => {
  tests.forEach((test) => {
    it(test.name, () => {
      expect(getBinaryCombinations<TKey, number, number>(test.keys, getSlotValue, test.emptyValue))
        .toEqual(test.expected);
    });
  });
});

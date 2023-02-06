import filterArray from '../filterArray';

const data = [
  { name: 'test' },
  { name: 'second' },
];

describe('filterArray', () => {
  test('filter test', () => {
    const result = filterArray(data, 'test');
    expect(result.length).toBe(1);
    expect(result[0].name).toBe(data[0].name);
  });

  test('filter test', () => {
    const result = filterArray(data, 'test1');
    expect(result.length).toBe(0);
  });
});

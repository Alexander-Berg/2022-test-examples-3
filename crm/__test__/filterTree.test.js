import { filterMap } from '../filter-tree';

const listIn = {
  root: {
    items: [1, 2],
  },
  1: {
    id: 1,
    name: 'Abc',
    items: [3],
  },
  2: {
    id: 2,
    name: 'Test',
  },
  3: {
    id: 3,
    name: 'TEST',
  },
};

const listOut = {
  1: { id: 1, name: 'Abc', items: [3] },
  2: { id: 2, name: 'Test', searchRange: [1, 4] },
  3: { id: 3, name: 'TEST', searchRange: [1, 4] },
  root: { items: [1, 2] },
};

describe('filter tree map', () => {
  test('filter', () => {
    const result = filterMap(listIn, 'EST');
    expect(result).toEqual(listOut);
  });
});

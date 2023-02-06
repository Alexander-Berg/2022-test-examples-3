import { SortOrder } from 'src/rest/definitions';
import sort from './sort';

const dataUnsorted = [
  {
    id: 1,
  },
  {
    id: 3,
  },
  {
    id: 2,
  },
  {
    id: 2,
  },
];

const dataAsc = [
  {
    id: 1,
  },
  {
    id: 2,
  },
  {
    id: 2,
  },
  {
    id: 3,
  },
];

const dataDesc = [
  {
    id: 3,
  },
  {
    id: 2,
  },
  {
    id: 2,
  },
  {
    id: 1,
  },
];

describe('sort', () => {
  it('should sorted by asc', () => {
    expect(sort(dataUnsorted, 'id', SortOrder.ASC)).toEqual(dataAsc);
  });

  it('should sorted by desc', () => {
    expect(sort(dataUnsorted, 'id', SortOrder.DESC)).toEqual(dataDesc);
  });

  it('should sort by nested key', () => {
    const nested = dataUnsorted.map(item => ({ item }));

    expect(sort(nested, 'item.id', SortOrder.ASC)).toEqual(dataAsc.map(item => ({ item })));
    expect(sort(nested, 'item.id', SortOrder.DESC)).toEqual(dataDesc.map(item => ({ item })));
  });
});

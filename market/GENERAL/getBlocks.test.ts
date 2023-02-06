import { getBlocks } from './getBlocks';

describe('getBlocks', () => {
  it('works', () => {
    expect(getBlocks([])).toEqual([]);
    expect(getBlocks([123, 234, 345, 456, 567, 678])).toEqual([
      { name: '', parameterIds: [123] },
      { name: '', parameterIds: [234] },
      { name: '', parameterIds: [345] },
      { name: '', parameterIds: [456] },
      { name: '', parameterIds: [567] },
      { name: '', parameterIds: [678] },
    ]);
    expect(getBlocks([123, 234, 345, 456])).toEqual([
      { name: '', parameterIds: [123] },
      { name: '', parameterIds: [234] },
      { name: '', parameterIds: [345] },
      { name: '', parameterIds: [456] },
    ]);
    expect(getBlocks([123, 234, 345, 456, 567, 678], 2)).toEqual([
      { name: '', parameterIds: [123, 234, 345] },
      { name: '', parameterIds: [456, 567, 678] },
    ]);
  });
});

import { deduplicate } from 'src/utils/deduplicate';

describe('deduplicate.ts', () => {
  it('works empty', () => {
    expect(deduplicate([], [])).toEqual([]);
  });
  it('works', () => {
    expect(deduplicate(['id'], [{ id: 1 }, { id: 2 }, { id: 1 }])).toEqual([{ id: 1 }, { id: 2 }]);
  });
});

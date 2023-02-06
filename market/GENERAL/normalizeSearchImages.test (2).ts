import { normalizeSearchImages } from './normalizeSearchImages';

describe('normalizeSearchImages', () => {
  it('works', () => {
    expect(normalizeSearchImages()).toEqual([]);
    expect(normalizeSearchImages(['', 'test1'])).toEqual([
      {
        source: 'test1',
        type: 'REMOTE',
        url: 'test1',
      },
    ]);
  });
});

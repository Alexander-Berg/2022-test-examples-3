import { normalizeSearchImages } from './normalizeSearchImages';

describe('normalizeSearchImages', () => {
  it('works', () => {
    expect(normalizeSearchImages()).toEqual([]);
    expect(normalizeSearchImages([{}, { original_url: 'test1' }])).toEqual([
      {
        markOfferUrl: false,
        source: 'test1',
        type: 'REMOTE',
        url: 'test1',
      },
    ]);
    expect(normalizeSearchImages([{}, { original_url: 'test1' }], true)).toEqual([
      {
        markOfferUrl: true,
        source: 'test1',
        type: 'REMOTE',
        url: 'test1',
      },
    ]);
  });
});

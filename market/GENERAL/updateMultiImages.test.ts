import { ImageType } from '@yandex-market/mbo-parameter-editor';

import { updateMultiImages } from './updateMultiImages';

describe('updateMultiImages', () => {
  it('works with empty data', () => {
    expect(updateMultiImages(undefined, {})).toEqual(undefined);
    expect(updateMultiImages([], {})).toEqual([]);
  });
  it('works with data', () => {
    expect(
      updateMultiImages(
        [
          { url: 'test', type: ImageType.REMOTE },
          { url: 'test4', type: ImageType.REMOTE },
        ],
        {
          test: { picture: { url: 'return' } },
          test2: { picture: { url: 'return2' } },
          test3: { picture: { url: 'return3' } },
        }
      )
    ).toEqual([
      {
        picture: {
          url: 'return',
        },
        type: 'REMOTE',
        url: 'return',
        validationMessage: undefined,
      },

      {
        type: 'REMOTE',
        url: 'test4',
      },
    ]);
  });
});

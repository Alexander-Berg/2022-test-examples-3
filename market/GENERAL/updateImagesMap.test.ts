import { ImageType } from '@yandex-market/mbo-parameter-editor';

import { updateImagesMap } from './updateImagesMap';

describe('updateImagesMap', () => {
  it('works empty', () => {
    expect(updateImagesMap({}, {}, [])).toEqual({});
  });
  it('works without changes', () => {
    expect(
      updateImagesMap(
        {
          testOfferId1: [{ url: 'test', type: ImageType.REMOTE }],
          testOfferId2: [{ url: 'test2', type: ImageType.REMOTE }],
        },
        {},
        []
      )
    ).toEqual({
      testOfferId1: [{ type: 'REMOTE', url: 'test' }],
      testOfferId2: [{ type: 'REMOTE', url: 'test2' }],
    });
  });
  it('works with changes', () => {
    expect(
      updateImagesMap(
        {
          testOfferId1: [{ url: 'test', type: ImageType.REMOTE }],
          testOfferId2: [{ url: 'test2', type: ImageType.REMOTE }],
          testOfferId3: [{ url: 'test4', type: ImageType.REMOTE }],
        },
        {
          test: { picture: { url: 'return' } },
          test2: { picture: { url: 'return2' } },
          test3: { picture: { url: 'return3' } },
        },
        ['test', 'test2']
      )
    ).toEqual({
      testOfferId1: [
        {
          picture: {
            url: 'return',
          },
          type: 'REMOTE',
          url: 'return',
          validationMessage: undefined,
        },
      ],
      testOfferId2: [
        {
          picture: {
            url: 'return2',
          },
          type: 'REMOTE',
          url: 'return2',
          validationMessage: undefined,
        },
      ],
      testOfferId3: [
        {
          type: 'REMOTE',
          url: 'test4',
        },
      ],
    });
  });
});

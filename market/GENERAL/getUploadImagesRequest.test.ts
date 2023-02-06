import { ImageType } from '@yandex-market/mbo-parameter-editor';

import { getUploadImagesRequest } from './getUploadImagesRequest';

describe('getUploadImagesRequest', () => {
  it('works with empty array', () => {
    expect(getUploadImagesRequest([])).toEqual({ local_image: [], remote_url: [] });
  });
  it('works with remote urls', () => {
    expect(
      getUploadImagesRequest([
        {
          type: ImageType.REMOTE,
          url: 'testUrl',
        },
      ])
    ).toEqual({ local_image: [], remote_url: ['testUrl'] });
  });
  it('works with local images', () => {
    expect(
      getUploadImagesRequest([
        {
          type: ImageType.LOCAL,
          url: 'testUrl',
          source: 'testSource',
        },
      ])
    ).toEqual({
      local_image: [
        {
          content_base64: 'testUrl',
          content_type: 'image/png',
          original_url: 'testSource',
        },
      ],
      remote_url: [],
    });
  });
});

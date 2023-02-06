import { ImageType } from '@yandex-market/mbo-parameter-editor';

import { putImage } from 'src/utils/putImage';

const IMAGE_1 = { type: ImageType.PICTURE, url: 'test_url_1' };
const IMAGE_2 = { type: ImageType.PICTURE, url: 'test_url_2' };
const IMAGE_3 = { type: ImageType.PICTURE, url: 'test_url_3' };

describe('putImage', () => {
  it('work for empty array', () => {
    expect(putImage([], IMAGE_1)).toEqual([IMAGE_1]);
  });
  it('work for filled array', () => {
    expect(putImage([IMAGE_1, IMAGE_2], IMAGE_3)).toEqual([IMAGE_1, IMAGE_2, IMAGE_3]);
  });
  it('work for filled array non zero index', () => {
    expect(putImage([IMAGE_1, IMAGE_3], IMAGE_2, 1)).toEqual([IMAGE_1, IMAGE_2, IMAGE_3]);
  });
});

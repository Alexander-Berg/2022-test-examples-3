import { ModificationSource } from '@yandex-market/market-proto-dts/Market/Mbo/Models';
import { ImageType } from '@yandex-market/mbo-parameter-editor';

import { updateImages } from './updateImages';

describe('updateImages', () => {
  it('works with empty data', () => {
    expect(updateImages([], 'testUrl')).toEqual([]);
  });
  it('works with not changed images', () => {
    expect(updateImages([{ url: 'testOther', type: ImageType.REMOTE }], 'testUrl')).toEqual([
      {
        type: 'REMOTE',
        url: 'testOther',
      },
    ]);
  });
  it('works with changed images', () => {
    expect(
      updateImages([{ url: 'testUrl', type: ImageType.REMOTE }], 'testUrl', {
        picture: { url: 'testUrl2', url_source: 'testSource', value_source: ModificationSource.AUTO },
      })
    ).toEqual([
      {
        url: 'testUrl2',
        type: 'REMOTE',
        picture: { parameterValues: {}, url: 'testUrl2', urlSource: 'testSource', valueSource: 'AUTO' },
      },
    ]);
  });
});

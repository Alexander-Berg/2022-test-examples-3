import { partialWrapper } from '@yandex-market/mbo-test-utils';
import { ImageType, NormalisedModel, NormalizedImage } from '@yandex-market/mbo-parameter-editor';
import { ValueType } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';

import { cutImageFromSource } from 'src/utils/cutImageFromSource';
import { ImageObject, ItemTypes } from 'src/types';

const MODEL_1 = partialWrapper<NormalisedModel>({});
const IMAGE_1: NormalizedImage = { type: ImageType.PICTURE, url: 'test_url_1' };
const PICKER_1: NormalizedImage = {
  type: ImageType.PICTURE,
  url: 'test_url_2',
  link: { parameterId: 321, type: ValueType.ENUM, optionId: 123 },
};

describe('cutImageFromSource', () => {
  it('works empty', () => {
    expect(cutImageFromSource(partialWrapper<ImageObject>({}), [], MODEL_1, [])).toEqual({});
  });
  it('works for cart image', () => {
    expect(
      cutImageFromSource(
        partialWrapper<ImageObject>({ type: ItemTypes.IMAGE_CART, src: 'test_url_1' }),
        [IMAGE_1],
        MODEL_1,
        []
      )
    ).toEqual({ picture: IMAGE_1 });
  });
  it('works for model image', () => {
    expect(
      cutImageFromSource(
        partialWrapper<ImageObject>({ type: ItemTypes.IMAGE_MODEL, src: 'test_url_1' }),
        [],
        partialWrapper<NormalisedModel>({ normalizedPictures: [IMAGE_1] }),
        []
      )
    ).toEqual({ picture: IMAGE_1, updatedModel: { normalizedPictures: [] } });
  });
  it('works for model picker image', () => {
    expect(
      cutImageFromSource(
        partialWrapper<ImageObject>({
          type: ItemTypes.IMAGE_PICKER,
          src: 'test_url_1',
          meta: { optionId: 123, parameterId: 321 },
        }),
        [],
        partialWrapper<NormalisedModel>({ normalizedPickers: [PICKER_1] }),
        []
      )
    ).toEqual({ picture: PICKER_1, updatedModel: { normalizedPickers: [] } });
  });
  it('works for sku image', () => {
    const sku1 = partialWrapper<NormalisedModel>({
      id: 123,
      normalizedPictures: [IMAGE_1],
    });

    const sku2 = partialWrapper<NormalisedModel>({
      id: 234,
      normalizedPictures: [{ type: ImageType.PICTURE, url: 'test_url_2' }],
    });

    expect(
      cutImageFromSource(
        partialWrapper<ImageObject>({ type: ItemTypes.IMAGE_SKU, src: 'test_url_1', meta: { skuId: 123 } }),
        [],
        MODEL_1,
        [sku1, sku2]
      )
    ).toEqual({ picture: IMAGE_1, updatedSkus: [{ ...sku1, normalizedPictures: [] }, sku2] });
  });
});

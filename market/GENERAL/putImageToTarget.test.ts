import { partialWrapper } from '@yandex-market/mbo-test-utils';
import { ImageType, NormalisedModel, NormalizedImage } from '@yandex-market/mbo-parameter-editor';

import { putImageToTarget } from 'src/utils/putImageToTarget';
import { ItemTypes } from 'src/types';

const MODEL_1 = partialWrapper<NormalisedModel>({});
const IMAGE_1: NormalizedImage = { type: ImageType.PICTURE, url: 'test_url_1' };

describe('putImageToTarget', () => {
  it('works empty', () => {
    expect(putImageToTarget(undefined as any, MODEL_1, [], IMAGE_1)).toEqual({});
  });
  it('works for cart', () => {
    expect(putImageToTarget(ItemTypes.IMAGE_CART, MODEL_1, [], IMAGE_1)).toEqual({ cartImage: IMAGE_1 });
  });
  describe('works for sku', () => {
    const sku1 = partialWrapper<NormalisedModel>({
      id: 123,
      normalizedPictures: [],
    });

    const sku2 = partialWrapper<NormalisedModel>({
      id: 234,
      normalizedPictures: [],
    });

    it('put image', () => {
      expect(putImageToTarget(ItemTypes.IMAGE_SKU, MODEL_1, [sku1, sku2], IMAGE_1, { skuId: 123 })).toEqual({
        updatedSkus: [{ ...sku1, normalizedPictures: [IMAGE_1] }, sku2],
      });
    });
    it('put picker', () => {
      expect(
        putImageToTarget(
          ItemTypes.IMAGE_SKU,
          MODEL_1,
          [sku1, sku2],
          { ...IMAGE_1, picture: { xslName: 'XL-Picture' } },
          { skuId: 123 }
        )
      ).toEqual({
        updatedSkus: [{ ...sku1, normalizedPictures: [{ ...IMAGE_1, picture: {} }] }, sku2],
      });
    });
    it('put image over existing one', () => {
      expect(
        putImageToTarget(ItemTypes.IMAGE_SKU, MODEL_1, [{ ...sku1, normalizedPictures: [IMAGE_1] }, sku2], IMAGE_1, {
          skuId: 123,
        })
      ).toEqual({});
    });
  });
  it('works for model', () => {
    expect(putImageToTarget(ItemTypes.IMAGE_MODEL, MODEL_1, [], IMAGE_1)).toEqual({
      updatedModel: { normalizedPictures: [{ ...IMAGE_1, picture: { xslName: 'XL-Picture' } }] },
    });

    expect(
      putImageToTarget(
        ItemTypes.IMAGE_MODEL,
        partialWrapper<NormalisedModel>({ normalizedPictures: [IMAGE_1] }),
        [],
        { type: ImageType.PICTURE, url: 'test_url_2' }
      )
    ).toEqual({
      updatedModel: {
        normalizedPictures: [
          { ...IMAGE_1, picture: { xslName: 'XL-Picture' } },
          { type: ImageType.PICTURE, url: 'test_url_2', picture: { xslName: 'XL-Picture_2' } },
        ],
      },
    });

    expect(
      putImageToTarget(
        ItemTypes.IMAGE_MODEL,
        partialWrapper<NormalisedModel>({ normalizedPictures: [IMAGE_1] }),
        [],
        IMAGE_1
      )
    ).toEqual({});
  });
  it('works for model picker', () => {
    expect(putImageToTarget(ItemTypes.IMAGE_PICKER, MODEL_1, [], IMAGE_1, { parameterId: 123, optionId: 321 })).toEqual(
      {
        updatedModel: {
          normalizedPickers: [{ ...IMAGE_1, source: IMAGE_1.url, link: { parameterId: 123, optionId: 321 } }],
        },
      }
    );
  });
});

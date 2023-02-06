import { NormalisedModel, NormalizedImage } from '@yandex-market/mbo-parameter-editor';

import { removeImagesFromModels } from 'src/tasks/common-logs/store/images/epics/helpers/removeImagesFromModels';

const getNormalisedModel = (model: Partial<NormalisedModel>) => model as NormalisedModel;
const getNormalisedImage = (image: Partial<NormalizedImage>) => image as NormalizedImage;

describe('removeImagesFromModels', () => {
  it('ignores models without given images', () => {
    const models = removeImagesFromModels([getNormalisedModel({}), getNormalisedModel({})], ['test1', 'test2']);

    expect(models).toHaveLength(0);
  });

  it('removes pickers', () => {
    const models = removeImagesFromModels(
      [
        getNormalisedModel({
          normalizedPickers: [
            getNormalisedImage({ url: 'test1' }),
            getNormalisedImage({ url: 'test2' }),
            getNormalisedImage({ url: 'test3' }),
          ],
        }),
        getNormalisedModel({}),
      ],
      ['test1', 'test2']
    );

    expect(models).toHaveLength(1);
    expect(models[0].normalizedPickers).toHaveLength(1);
    expect(models[0]?.normalizedPickers?.[0].url).toEqual('test3');
  });

  it('removes pictures', () => {
    const models = removeImagesFromModels(
      [
        getNormalisedModel({
          normalizedPictures: [
            getNormalisedImage({ url: 'test1' }),
            getNormalisedImage({ url: 'test2' }),
            getNormalisedImage({ url: 'test4' }),
          ],
        }),
        getNormalisedModel({}),
      ],
      ['test1', 'test2']
    );

    expect(models).toHaveLength(1);
    expect(models[0].normalizedPictures).toHaveLength(1);
    expect(models[0]?.normalizedPictures?.[0].url).toEqual('test4');
  });
});

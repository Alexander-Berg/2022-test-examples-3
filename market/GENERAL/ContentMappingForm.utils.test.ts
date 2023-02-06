import {
  prepareMarketParams,
  prepareMappings,
  deleteMappings,
  getLocalMappings,
  isValidShopValues,
} from './ContentMappingForm.utils';
import { MAIN_PICTURE_ID, PICTURE_ID } from 'src/constants';
import { simpleMapping } from 'src/test/data/mapping';
import { ParamMappingType } from 'src/java/definitions';

describe('ContentMappingForm.utils', () => {
  test('prepareMarketParams', () => {
    // обычные параметры так и должны оставаться
    expect(prepareMarketParams([{ parameterId: 1 }])).toEqual([{ parameterId: 1 }]);
    // если в интерфейсе выбрали ппараметр "фото" "доп фото" то при сохранении маппинга marketParams должен быть пустым массивом, потому что mappingType === PICTURE
    expect(prepareMarketParams([{ parameterId: MAIN_PICTURE_ID }])).toEqual([]);
  });

  test('prepareMappings', () => {
    // маппинги запрещенные на редактирование должны отсекаться
    expect(prepareMappings([{ ...simpleMapping, editable: false }])).toEqual([]);
    // маппинги без shopParams тоже не должны проходить
    expect(prepareMappings([{ ...simpleMapping, editable: false }])).toEqual([]);
  });

  test('getLocalMappings', () => {
    // с бека маппинги на картинки приходят без marketParams, нужно проставить эти параметры что бы они отобразились в форме
    expect(
      getLocalMappings([{ ...simpleMapping, mappingType: ParamMappingType.PICTURE, marketParams: [] }])[0].marketParams
    ).toEqual([{ parameterId: PICTURE_ID }]);

    expect(
      getLocalMappings([{ ...simpleMapping, mappingType: ParamMappingType.FIRST_PICTURE, marketParams: [] }])[0]
        .marketParams
    ).toEqual([{ parameterId: MAIN_PICTURE_ID }]);
  });

  test('isValidPossibleValues invalid', () => {
    // если выбран маркетный ппараметр картинки, а в товарах значения не похожи на ссылки, то маппинг невалидный
    const shopValues = ['123', 'rer'];
    expect(
      isValidShopValues({ ...simpleMapping, marketParams: [{ parameterId: MAIN_PICTURE_ID }] }, shopValues)
    ).toBeFalsy();
    expect(
      isValidShopValues({ ...simpleMapping, marketParams: [{ parameterId: PICTURE_ID }] }, shopValues)
    ).toBeFalsy();
  });

  test('isValidPossibleValues valid', () => {
    const shopValues = ['https://img.best-kitchen.ru/images/products/1/7031/77601655/3.jpg'];
    expect(
      isValidShopValues({ ...simpleMapping, marketParams: [{ parameterId: MAIN_PICTURE_ID }] }, shopValues)
    ).toBeTruthy();
    expect(
      isValidShopValues({ ...simpleMapping, marketParams: [{ parameterId: PICTURE_ID }] }, shopValues)
    ).toBeTruthy();
  });

  test('deleteMappings', async () => {
    const onDelete = jest.fn();
    await deleteMappings(
      [
        { ...simpleMapping, id: 1 },
        { ...simpleMapping, id: 2 },
      ],
      onDelete
    );
    expect(onDelete).toBeCalledTimes(2);
  });
});

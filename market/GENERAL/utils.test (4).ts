import { getMappingType, needFormalizeMapping } from './utils';
import { simpleMapping, categoryData, stringParameter, vendorParameter, numericParameter } from 'src/test/data';
import { ParamMappingType } from 'src/java/definitions';

describe('mappings utils', () => {
  test('getMappingType string parameter', () => {
    const mappingType = getMappingType(
      { ...simpleMapping, marketParams: [{ parameterId: stringParameter.hid }] },
      categoryData
    );
    // параметр маркета строковой то обязательно должен быть DIRECT мапппиг !
    expect(mappingType).toEqual('DIRECT');
  });

  test('getMappingType enum parameter', () => {
    const mappingType = getMappingType(
      { ...simpleMapping, marketParams: [{ parameterId: vendorParameter.hid }] },
      categoryData
    );
    expect(mappingType).toEqual('MAPPING');
  });

  test('getMappingType numeric parameter', () => {
    const mappingType = getMappingType(
      { ...simpleMapping, marketParams: [{ parameterId: numericParameter.hid }] },
      categoryData
    );
    expect(mappingType).toEqual('MAPPING');
  });

  test('needFormalizeMapping у маппингов с кол-ом marketParams больше 1 не нужно запрашивать формализацию', () => {
    const moreMarketParams = {
      ...simpleMapping,
      id: 0,
      marketParams: [{ parameterId: vendorParameter.hid }, { parameterId: vendorParameter.hid }],
    };

    expect(needFormalizeMapping([moreMarketParams], categoryData)).toBeFalsy();
  });

  test('needFormalizeMapping у маппингов с картинками не нужно запрашивать формализацию', () => {
    const pictureMappings = {
      ...simpleMapping,
      mappingType: ParamMappingType.PICTURE,
      marketParams: [],
    };

    expect(needFormalizeMapping([pictureMappings], categoryData)).toBeFalsy();

    const firstPictureMapping = {
      ...simpleMapping,
      mappingType: ParamMappingType.FIRST_PICTURE,
      marketParams: [],
    };

    expect(needFormalizeMapping([firstPictureMapping], categoryData)).toBeFalsy();
  });
});

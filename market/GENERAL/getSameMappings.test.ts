import { simpleMapping } from 'src/test/data';
import { getSameMappings } from './getSameMappings';
import { ParamMappingType } from 'src/java/definitions';

describe('getSameMappings', () => {
  test('find same mappings', () => {
    // не должен чекать переданый маппинг
    expect(getSameMappings(simpleMapping, [simpleMapping])).toHaveLength(0);

    expect(getSameMappings(simpleMapping, [simpleMapping, { ...simpleMapping, id: 5 }])).toHaveLength(1);
  });

  test('find same picture mappings', () => {
    const pic = { ...simpleMapping, mappingType: ParamMappingType.PICTURE };
    // не должен чекать переданый маппинг
    expect(getSameMappings(pic, [pic, { ...pic, id: 6, shopParams: [{ name: 'other' }] }])).toHaveLength(0);
  });
});

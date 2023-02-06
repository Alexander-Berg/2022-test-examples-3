import { ValueLinkRestrictionType } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';

import {
  getCategoryId,
  getCategoryName,
  getCategoryParameterIds,
  getCategoryParameterValueLinks,
} from 'src/entities/category/getters';
import { Category } from 'src/entities/category/types';

const category: Category = {
  guruId: 0,
  uniqueName: '',
  id: 1,
  name: 'Название категории',
  parameterIds: [1, 2, 3, 4, 5],
  parameterValueLinks: [
    {
      linkedParamId: 1,
      linkedValue: [
        {
          linkedOptionIds: [1, 2, 3],
          optionId: 4,
        },
      ],
      parameterId: 6,
      type: ValueLinkRestrictionType.BIDIRECTIONAL,
    },
  ],
};

describe('src/entities/category', () => {
  describe('getters', () => {
    test('getCategoryId должен вернуть id категории', () => {
      expect(getCategoryId(category)).toEqual(category.id);
    });

    test('getCategoryName должен вернуть название категории', () => {
      expect(getCategoryName(category)).toEqual(category.name);
    });

    test('getCategoryParameterIds должен вернуть список id параметров категории', () => {
      expect(getCategoryParameterIds(category)).toEqual(category.parameterIds);
    });

    test('getCategoryParameterValueLinks должен вернуть список parameterValueLinks категории', () => {
      expect(getCategoryParameterValueLinks(category)).toEqual(category.parameterValueLinks);
    });
  });
});

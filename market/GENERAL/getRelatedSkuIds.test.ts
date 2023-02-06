import { RelationType } from '@yandex-market/market-proto-dts/Market/Mbo/Models';
import { partialWrapper } from '@yandex-market/mbo-test-utils';

import { NormalisedModel } from '../types';
import { getRelatedSkuIds } from './getRelatedSkuIds';

describe('getRelatedSkuIds', () => {
  it('works empty', () => {
    expect(getRelatedSkuIds()).toEqual([]);
    expect(getRelatedSkuIds(partialWrapper<NormalisedModel>({}))).toEqual([]);
  });
  it('works', () => {
    expect(
      getRelatedSkuIds(
        partialWrapper<NormalisedModel>({
          relations: [{ type: RelationType.SKU_MODEL, id: 123, categoryId: 1 }],
        })
      )
    ).toEqual([123]);
  });
});

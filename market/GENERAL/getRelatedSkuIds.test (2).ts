import { RelationType } from '@yandex-market/market-proto-dts/Market/Mbo/Models';
import { NormalisedModel } from '@yandex-market/mbo-parameter-editor';

import { partialWrapper } from 'src/test/utils/partialWrapper';
import { getRelatedSkuIds } from './getRelatedSkuIds';

describe('getRelatedSkuIds', () => {
  it('works empty', () => {
    expect(getRelatedSkuIds()).toEqual([]);
    expect(getRelatedSkuIds(partialWrapper({}))).toEqual([]);
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

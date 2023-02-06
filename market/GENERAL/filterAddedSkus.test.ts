import { Relation } from '@yandex-market/market-proto-dts/Market/Mbo/Models';

import { filterAddedSkus } from './filterAddedSkus';
import { NormalisedModel } from 'src/entities';

const model1 = { relations: [{ id: 123 } as Relation, { id: 234 } as Relation] } as NormalisedModel;
const model2 = { relations: [{ id: 123 } as Relation, { id: 345 } as Relation] } as NormalisedModel;

describe('filterAddedSkus', () => {
  it('works with empty', () => {
    expect(filterAddedSkus()).toEqual([]);
    expect(filterAddedSkus({ relations: [] as Relation[] } as NormalisedModel)).toEqual([]);
  });
  it('works with data', () => {
    expect(filterAddedSkus(model1)).toEqual([{ id: 123 }, { id: 234 }]);
    expect(filterAddedSkus(model1, model1)).toEqual([]);
    expect(filterAddedSkus(model1, model2)).toEqual([{ id: 234 }]);
  });
});

import { RelationType } from '@yandex-market/market-proto-dts/Market/Mbo/Models';

import { getParentRelative } from './getParentRelative';

describe('getParentRelative', () => {
  it('works empty', () => {
    expect(getParentRelative()).toBeUndefined();
    expect(getParentRelative({})).toBeUndefined();
    expect(getParentRelative({ relations: [] })).toBeUndefined();
  });
  it('works', () => {
    const relation = { type: RelationType.SKU_PARENT_MODEL, id: 1 };
    expect(getParentRelative({ relations: [relation] })).toEqual(relation);
  });
});

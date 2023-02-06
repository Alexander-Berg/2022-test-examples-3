import { Relation } from '@yandex-market/market-proto-dts/Market/Mbo/Models';

import { getServerRemovedSkuIds } from './getServerRemovedSkuIds';
import { ParamValuesChangedBy } from './types';

const skuRelation1 = {
  id: 1,
} as Relation;

const skuRelation2 = {
  id: 2,
} as Relation;

const theirChanges = {
  conflicts: {
    deletedInTheir: [skuRelation1],
  },
  updates: {
    deleted: [skuRelation2],
    changedBy: ParamValuesChangedBy.Their,
  },
};

describe('getServerRemovedSkuIds', () => {
  it('with no changes', () => {
    expect(getServerRemovedSkuIds()).toEqual([]);
  });
  it('with their changes', () => {
    expect(getServerRemovedSkuIds(theirChanges)).toEqual([2, 1]);
  });
});

import { ValueType } from '@yandex-market/market-proto-dts/Market/Mbo/Parameters';

import { getModelsChanges } from './getModelsChanges';
import { ProtoModel } from 'src/entities';

describe('getModelsChanges', () => {
  it('works with empty data', () => {
    const model: ProtoModel = { id: 1, category_id: 1, vendor_id: 1, created_date: 1, modified_ts: 1 };

    expect(
      getModelsChanges({
        categoryData: {
          options: {},
          parameters: {
            1: {
              unit: 'test1',
              title: 'test1',
              max_value: 0,
              min_value: 0,
              type: ValueType.STRING,
              description: 'test1',
              id: 1,
              name: 'test1',
              multivalue: false,
            },
            2: {
              unit: 'test2',
              title: 'test1',
              max_value: 0,
              min_value: 0,
              type: ValueType.STRING,
              description: 'test1',
              id: 2,
              name: 'test1',
              multivalue: false,
            },
            3: {
              unit: 'test1',
              title: 'test1',
              max_value: 0,
              min_value: 0,
              type: ValueType.STRING,
              description: 'test1',
              id: 3,
              name: 'test1',
              multivalue: false,
            },
            4: {
              unit: 'test1',
              title: 'test1',
              max_value: 0,
              min_value: 0,
              type: ValueType.STRING,
              description: 'test1',
              id: 4,
              name: 'test1',
              multivalue: false,
            },
          },
        } as any,
        protoModels: [model],
        requestModels: [model],
        storageModels: [model],
        updatedModelsIsSku: [],
      })
    ).toEqual({
      '1': {
        hasConflicts: false,
        hasUpdates: false,
        newSkuIds: [],
        resultMessage: undefined,
        serverRemovedSkuIds: [],
        updatedProtoModel: {
          id: 1,
          vendor_id: 1,
          modified_ts: 1,
          category_id: 1,
          created_date: 1,
          deleted: false,
          parameter_value_links: [],
          parameter_values: undefined,
          pictures: [],
        },
      },
    });
  });
});

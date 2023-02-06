import {
  ModelType,
  OperationStatus as Market_Mbo_Models_OperationStatus,
  OperationStatusType,
  RelationType,
} from '@yandex-market/market-proto-dts/Market/Mbo/Models';
import { CategoryData, NormalisedModel } from '@yandex-market/mbo-parameter-editor';

import { getModelUpdateErrors } from 'src/tasks/common-logs/store/models/epics/helpers/getModelUpdateErrors';
import { RootState } from 'src/tasks/common-logs/store/root/reducer';

const model = { id: 123, currentType: ModelType.GURU, title: 'testModelTitle' } as NormalisedModel;
const sku = {
  id: 234,
  currentType: ModelType.SKU,
  title: 'testSkuTitle',
  relations: [{ type: RelationType.SKU_PARENT_MODEL, id: 123 }],
} as NormalisedModel;

const state = {
  data: { categoryId: 123 },
  aliasMaker: { categoryData: { category: { parameterIds: [] }, sizeMeasureInfos: [] } as unknown as CategoryData },
  models: { normalisedModels: { [model.id]: model, [sku.id]: sku } },
} as RootState;

describe('getModelUpdateErrors', () => {
  it('works with empty data', () => {
    expect(getModelUpdateErrors({}, state)).toEqual({ result: { mbo_status: undefined } });
    expect(getModelUpdateErrors({ result: {} }, state)).toEqual({ result: { mbo_status: undefined } });
    expect(getModelUpdateErrors({ result: { mbo_status: [] } }, state)).toEqual({ result: { mbo_status: [] } });
  });
  it('works without model', () => {
    expect(
      getModelUpdateErrors(
        {
          result: {
            mbo_status: [
              {
                model_id: 321,
                status: OperationStatusType.VALIDATION_ERROR,
                localized_message: [{ value: 'test' }],
              } as Market_Mbo_Models_OperationStatus,
            ],
          },
        },
        state
      )
    ).toEqual({
      result: {
        mbo_status: [
          {
            localized_message: [
              {
                value: 'test',
              },
            ],
            model_id: 321,
            status: 'VALIDATION_ERROR',
          },
        ],
      },
    });
  });
  it('works with invalid model', () => {
    expect(
      getModelUpdateErrors(
        {
          result: {
            mbo_status: [
              {
                model_id: 123,
                status: OperationStatusType.VALIDATION_ERROR,
                localized_message: [{ value: 'test' }],
              } as Market_Mbo_Models_OperationStatus,
            ],
          },
        },
        state
      )
    ).toEqual({
      result: {
        mbo_status: [
          {
            localized_message: [
              {
                value: 'test: ',
              },
            ],
            model_id: 123,
            status: 'VALIDATION_ERROR',
          },
        ],
      },
    });
  });
  it('works with invalid sku', () => {
    expect(
      getModelUpdateErrors(
        {
          result: {
            mbo_status: [
              {
                model_id: 234,
                status: OperationStatusType.VALIDATION_ERROR,
                localized_message: [{ value: 'test' }],
              } as Market_Mbo_Models_OperationStatus,
            ],
          },
        },
        state
      )
    ).toEqual({
      result: {
        mbo_status: [
          {
            localized_message: [
              {
                value: 'test: ',
              },
            ],
            model_id: 234,
            status: 'VALIDATION_ERROR',
          },
        ],
      },
    });
  });
});

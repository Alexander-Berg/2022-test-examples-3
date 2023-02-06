import { Relation } from '@yandex-market/market-proto-dts/Market/Mbo/Models';

import { getRelationsChanges } from './getRelationsChanges';
import { NormalisedModel } from 'src/entities';

const model1 = { relations: [{ id: 123 } as Relation, { id: 234 } as Relation] } as NormalisedModel;
const model2 = { relations: [{ id: 123 } as Relation] } as NormalisedModel;
const model3 = { relations: [{ id: 123 } as Relation, { id: 345 } as Relation] } as NormalisedModel;

describe('getAddedSkus', () => {
  it('with empty editor model', () => {
    expect(
      getRelationsChanges({ protoModel: model1, editorModel: { relations: [] } as any, storageModel: model3 }, [])
    ).toEqual({
      updates: {
        added: [
          {
            id: 345,
          },
        ],
        deleted: [
          {
            id: 234,
          },
        ],
        changedBy: 1,
      },
    });
  });
  it('with equal editor model', () => {
    expect(getRelationsChanges({ protoModel: model1, editorModel: model1, storageModel: model1 }, [])).toEqual(
      undefined
    );
  });
  it('with editor model', () => {
    expect(getRelationsChanges({ protoModel: model2, editorModel: model2, storageModel: model1 }, [])).toEqual({
      updates: {
        added: [
          {
            id: 234,
          },
        ],
        deleted: [],
        changedBy: 1,
      },
    });
  });
  it('with editor model reverse', () => {
    expect(getRelationsChanges({ protoModel: model2, editorModel: model1, storageModel: model2 }, [])).toEqual({
      updates: {
        added: [
          {
            id: 234,
          },
        ],
        deleted: [],
        changedBy: 0,
      },
    });
  });
  it('with editor model and conflict', () => {
    expect(getRelationsChanges({ protoModel: model2, editorModel: model3, storageModel: model1 }, [])).toEqual({
      conflicts: {
        addedInOur: [
          {
            id: 345,
          },
        ],
        addedInTheir: [
          {
            id: 234,
          },
        ],
        deletedInOur: [],
        deletedInTheir: [],
      },
    });
  });
});

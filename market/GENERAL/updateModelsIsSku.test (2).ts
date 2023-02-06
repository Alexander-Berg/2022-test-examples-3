import { RelationType } from '@yandex-market/market-proto-dts/Market/Mbo/Models';
import { ProtoModel } from '@yandex-market/mbo-parameter-editor/es/entities/protoModel/types';

import { IS_SKU_PARAM_ID } from 'src/pages/ModelEditorCluster/constants';
import { updateModelsIsSku } from './updateModelsIsSku';

const model: ProtoModel = { id: 1 };
const model2: ProtoModel = {
  id: 2,
  parameter_values: [{ param_id: IS_SKU_PARAM_ID, bool_value: true }, { param_id: 123 }],
};
const model3: ProtoModel = { id: 3, relations: [{ type: RelationType.SKU_PARENT_MODEL, id: 2 }] };

describe('updateModelsIsSku', () => {
  it('works with empty', () => {
    expect(updateModelsIsSku([], [])).toEqual({
      updatedModels: [],
      updatedModelsId: [],
    });
  });
  it('works with models without updates', () => {
    expect(updateModelsIsSku([model], [model, model2])).toEqual({
      updatedModels: [model],
      updatedModelsId: [],
    });
    expect(updateModelsIsSku([model2], [model2, model2])).toEqual({
      updatedModels: [model2],
      updatedModelsId: [],
    });
  });
  it('works with models and updates', () => {
    expect(updateModelsIsSku([model2], [model3])).toEqual({
      updatedModels: [
        {
          ...model2,
          parameter_values: [
            {
              bool_value: false,
              option_id: 15354458,
              param_id: 15354452,
            },
            { param_id: 123 },
          ],
        },
      ],
      updatedModelsId: [2],
    });
  });
});

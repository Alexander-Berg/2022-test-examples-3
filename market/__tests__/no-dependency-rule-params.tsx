import { ModificationSource } from '@yandex-market/market-proto-dts/Market/Mbo/Models';
import { getNormalizedParameter } from '@yandex-market/mbo-parameter-editor/es/entities/parameter/normalize';
import { ProtoModel } from '@yandex-market/mbo-parameter-editor/es/entities/protoModel/types';

import { RU_ISO_CODE } from 'src/shared/constants';
import { testCategoryProto } from 'src/shared/test-data/test-categories';
import { generateParamValue, testModelProto } from 'src/shared/test-data/test-models';
import { testParameterProto } from 'src/shared/test-data/test-parameters';
import { ModerationTaskType } from 'src/tasks/mapping-moderation/helpers/input-output';
import initializeModerationDataRequests from 'src/tasks/mapping-moderation/helpers/test/initializeModerationDataRequests';
import { setupTestApplication } from 'src/tasks/mapping-moderation/helpers/test/setupTestApplication';

describe('ModerationApp', () => {
  it('Should not display DEPENDENCY_RULE values', async () => {
    const size = testParameterProto();
    const sizeUi = getNormalizedParameter(size).parameter;
    const category = { ...testCategoryProto({ parameters: [size] }) };
    const sku: ProtoModel = {
      ...testModelProto({ category }),
      parameter_values: [
        {
          ...generateParamValue(sizeUi),
          value_source: ModificationSource.OPERATOR_FILLED,
          str_value: [{ value: 'Operator filled', isoCode: RU_ISO_CODE }],
        },
        {
          ...generateParamValue(sizeUi),
          value_source: ModificationSource.DEPENDENCY_RULE,
          str_value: [{ value: 'Dependency rule', isoCode: RU_ISO_CODE }],
        },
      ],
    };

    const { aliasMaker, app } = setupTestApplication({
      offers: [{ offer_id: '101' } as any],
      task_type: ModerationTaskType.MAPPING_MODERATION,
    });

    await initializeModerationDataRequests(aliasMaker, category, [sku], app, [
      { offer_id: '101', supplier_mapping_info: { sku_id: sku.id } },
    ]);

    expect(app.findWhere(w => w.text() === 'Operator filled').length).toBeTruthy();
    expect(app.findWhere(w => w.text() === 'Dependency rule').length).toBeFalsy();
  });
});

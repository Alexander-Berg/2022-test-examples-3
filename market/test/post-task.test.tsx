import { OperationStatus } from '@yandex-market/market-proto-dts/Market/AliasMaker';
import { Model, ModelType } from '@yandex-market/market-proto-dts/Market/Mbo/Models';

import { InputData } from 'src/shared/common-logs/helpers/types';
import { testModelProto } from 'src/shared/test-data/test-models';
import postTask from 'src/tasks/common-logs/task-suits/test-post-task';
import { initCommonLogsApp } from 'src/tasks/common-logs/test/utils/initCommonLogsApp';

describe('Common Logs Post Task', () => {
  it('should submit successfully', async () => {
    const { aliasMaker, submitHolder } = initCommonLogsApp({ initialData: postTask as unknown as any });

    const models: Model[] = (postTask as unknown as InputData).output!.task_result!.map(r =>
      testModelProto({
        id: r.market_sku_id,
        modelType: ModelType.SKU,
        vendorId: 100,
        // this flag is required for getModelsExported check
        published: true,
      })
    );

    const promise: Promise<any> = submitHolder.submit();

    aliasMaker.getModelsExported.next().resolve({
      model: models,
      result: { status: OperationStatus.SUCCESS },
    });

    const result = await promise;

    expect(result).toEqual({
      task_result: [
        {
          offer_id: '4485678',
          req_id: '211287',
          offer_mapping_status: 'MAPPED',
          market_sku_id: 1000,
          content_comment: [],
        },
        {
          offer_id: '4485677',
          req_id: '211289',
          offer_mapping_status: 'MAPPED',
          market_sku_id: 1001,
          content_comment: [],
        },
        {
          offer_id: '4485680',
          req_id: '211288',
          offer_mapping_status: 'MAPPED',
          market_sku_id: 1002,
          content_comment: [],
        },
      ],
      contractorWorkerId: 321321,
      req_id: 123123,
    });
  });
});

import { OperationStatus } from '@yandex-market/market-proto-dts/Market/AliasMaker';
import { Model, ModelType, RelationType } from '@yandex-market/market-proto-dts/Market/Mbo/Models';
import { ProtoModel } from '@yandex-market/mbo-parameter-editor/es/entities/protoModel/types';

import { testModelProto } from 'src/shared/test-data/test-models';
import { testVendor } from 'src/shared/test-data/test-vendor';
import simpleTask from 'src/tasks/common-logs/task-suits/test-simple-task';
import { initCommonLogsApp } from 'src/tasks/common-logs/test/utils/initCommonLogsApp';
import { selectModel } from 'src/tasks/common-logs/test/utils/selectModel';
import { selectSku } from 'src/tasks/common-logs/test/utils/selectSku';
import { selectVendor } from 'src/tasks/common-logs/test/utils/selectVendor';
import { switchOnOffer } from 'src/tasks/common-logs/test/utils/switchOnOffer';

let initResult: any;
const models: Model[] = [];

describe('all models exists and is SKU', () => {
  it('should process successfully', async () => {
    initResult = initCommonLogsApp({ initialData: simpleTask });

    const { app, aliasMaker } = initResult;

    let vendorId = 100;
    let modelId = 1000;

    const offersIds = simpleTask.logs.map(o => o.offer_id);
    for (const offerId of offersIds) {
      switchOnOffer(app, offerId);

      const vendor = testVendor({ vendor_id: vendorId++, name: 'Some Vendor' });
      const model: Model = testModelProto({
        id: modelId++,
        modelType: ModelType.GURU,
        vendorId: vendor.vendor_id,
        published: true, // this flag needed for getModelsExported check
        isSku: true,
      });

      models.push(model);

      selectVendor(app, aliasMaker, vendor);
      selectModel(app, aliasMaker, model);
    }
  });
  it('should submit successfully', async () => {
    const { aliasMaker, submitHolder } = initResult;

    const promise: Promise<any> = submitHolder.submit();

    aliasMaker.getModelsExported.next().resolve({
      model: models,
      result: { status: OperationStatus.SUCCESS },
    });

    const result: any = await promise;

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
    });
  });
});

describe('all models exists and has existing SKU', () => {
  it('should process successfully', async () => {
    initResult = initCommonLogsApp({ initialData: simpleTask });
    const { app, aliasMaker } = initResult;

    let vendorId = 100;
    let modelId = 1000;
    let skuId = 2000;

    const offersIds = simpleTask.logs.map(o => o.offer_id);
    offersIds.forEach(offerId => {
      switchOnOffer(app, offerId);
      const currentModelId = modelId++;
      const currentSkuId = skuId++;

      const vendor = testVendor({ vendor_id: vendorId++, name: 'Some Vendor' });
      const model: ProtoModel = testModelProto({
        id: currentModelId,
        vendorId: vendor.vendor_id,
        relations: [
          {
            categoryId: 0,
            id: currentSkuId,
            type: RelationType.SKU_MODEL,
          },
        ],
      });
      const sku: ProtoModel = testModelProto({
        id: currentSkuId,
        vendorId: vendor.vendor_id,
        relations: [
          {
            categoryId: 0,
            id: currentModelId,
            type: RelationType.SKU_PARENT_MODEL,
          },
        ],
        modelType: ModelType.SKU,
      });

      models.push(model, sku);

      selectVendor(app, aliasMaker, vendor);
      selectModel(app, aliasMaker, model, [sku]);
      selectSku(app, aliasMaker, sku);
    });
  });
  it('should submit successfully', async () => {
    const { aliasMaker, submitHolder } = initResult;

    const promise: Promise<any> = submitHolder.submit();

    aliasMaker.getModelsExported.next().resolve({
      model: models.map(m => ({ ...m, published_on_market: true })),
      result: { status: OperationStatus.SUCCESS },
    });

    const result: any = await promise;

    expect(result).toEqual({
      task_result: [
        {
          offer_id: '4485678',
          req_id: '211287',
          offer_mapping_status: 'MAPPED',
          market_sku_id: 2000,
          content_comment: [],
        },
        {
          offer_id: '4485677',
          req_id: '211289',
          offer_mapping_status: 'MAPPED',
          market_sku_id: 2001,
          content_comment: [],
        },
        {
          offer_id: '4485680',
          req_id: '211288',
          offer_mapping_status: 'MAPPED',
          market_sku_id: 2002,
          content_comment: [],
        },
      ],
    });
  });
});

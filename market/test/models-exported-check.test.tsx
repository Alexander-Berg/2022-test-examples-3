import { OperationStatus } from '@yandex-market/market-proto-dts/Market/AliasMaker';
import { Model, ModelType } from '@yandex-market/market-proto-dts/Market/Mbo/Models';
import { ToastContainer } from 'react-toastify';

import { testModelProto } from 'src/shared/test-data/test-models';
import { testVendor } from 'src/shared/test-data/test-vendor';
import { wait } from 'src/shared/utils/testing/utils';
import { TASK_NOT_COMPLETED_MESSAGE } from 'src/tasks/common-logs/store/submit/constants';
import simpleTask from 'src/tasks/common-logs/task-suits/test-simple-task';
import { initCommonLogsApp, selectModel, selectVendor, switchOnOffer } from './utils';

let initResult;
const models: Model[] = [];

const getResult = async <T extends any>(promise: Promise<T>): Promise<{ result?: T | undefined; error?: any }> =>
  new Promise<{ result?: T | undefined; error?: any }>(resolve => {
    promise.then(result => resolve({ result })).catch(error => resolve({ error }));
  });

describe.skip('all models exists and is SKU', () => {
  it('fail if has not exported models', async () => {
    initResult = initCommonLogsApp({ initialData: simpleTask });

    const { app, aliasMaker, submitHolder } = initResult;
    let vendorId = 100;
    let modelId = 1000;

    const offersIds = simpleTask.logs.map(o => o.offer_id);
    offersIds.forEach(offerId => {
      switchOnOffer(app, offerId);

      const vendor = testVendor({ vendor_id: vendorId++, name: 'Some Vendor' });
      const model: Model = testModelProto({
        id: modelId++,
        modelType: ModelType.GURU,
        vendorId: vendor.vendor_id,
        published: false, // this flag needed for getModelsExported check
        isSku: true,
      });
      models.push(model);

      selectVendor(app, aliasMaker, vendor);
      selectModel(app, aliasMaker, model);
    });

    const promise: Promise<any> = getResult(submitHolder.submit());

    aliasMaker.getModelsExported.next().resolve({
      model: models.slice(0, 2),
      result: { status: OperationStatus.SUCCESS },
    });

    await wait(1);

    const result = await promise;

    aliasMaker.sendStatistics.next().resolve({});

    expect(aliasMaker.activeRequests()).toHaveLength(0);
    expect(result).toEqual({ error: TASK_NOT_COMPLETED_MESSAGE });

    await wait(1);
    app.update();

    const messagesViewerHtml = app.find(ToastContainer).html();

    expect(messagesViewerHtml).toIncludeRepeated('не опубликована на Синем Маркете.', 2);
    expect(messagesViewerHtml).toIncludeRepeated('не сохранена в МБО', 1);
  });
});

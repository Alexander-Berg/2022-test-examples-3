import { MockedApiObject } from '@yandex-market/mbo-test-utils';

import { collectFetchedModels } from './fetchModels';
import { setupApi } from '../../../test/api/setupApi';
import { resolveLoadModelsRequest } from '../../../test/api/resolves';
import { shopModel } from '../../../test/data/shopModel';
import { Api } from '../../../java/Api';

const api: MockedApiObject<Api> = setupApi();

describe('fetchModels', () => {
  test('without max size', async () => {
    const request = collectFetchedModels({}, api);
    resolveLoadModelsRequest(api, new Array(9));

    await request;
    expect(api.shopModelController.loadAllModelsByShopIdV3.activeRequests()).toHaveLength(0);
  });

  test('slice on two request', async () => {
    // задаем максимальный размер страници с товарами
    const request = collectFetchedModels({}, api, 10);
    const models = [...new Array(10)].map(() => shopModel);

    // резолвим первую страницу
    resolveLoadModelsRequest(api, models);
    await new Promise(res => setTimeout(res, 100));
    // резолвим не полную страницу, больше запросов не должно уходить, так как товаров вроде больше нет
    resolveLoadModelsRequest(api, models.slice(0, 4));

    await request;
    // проверяем что больше запросов не было
    expect(api.shopModelController.loadAllModelsByShopIdV3.activeRequests()).toHaveLength(0);
  });
});

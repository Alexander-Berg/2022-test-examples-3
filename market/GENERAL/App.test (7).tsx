import { MockedApiObject } from '@yandex-market/mbo-test-utils';

import { setupTestApp } from './test/setupApp';
import {
  resolveLoadShopsRequest,
  resolveLoadCategoryTreeRequest,
  resolveUserInfoRequest,
  resolveLoadStatisticsRequest,
  resolveLoadMappingRequest,
} from './test/api/resolves';
import { shops, userInfo, categoryInfo, categoryStat } from './test/data';
import { Api } from './java/Api';
import { UpdateStatus } from './java/definitions';
import { ACTIVATE_SHOP_TEXT } from './pages/LandPage/components';
import { waitFor } from '@testing-library/react';

const resolveDefaultData = (api: MockedApiObject<Api>) => {
  resolveUserInfoRequest(api, userInfo);
  resolveLoadShopsRequest(api, shops);
  resolveLoadCategoryTreeRequest(api, [categoryInfo]);
  resolveLoadStatisticsRequest(api, [categoryStat]);
  resolveLoadMappingRequest(api, { mappings: [], rules: [] });
};

describe('App', () => {
  test('load all data on root page', async () => {
    const { api } = setupTestApp(`?shopId=${shops[0].id}`);

    resolveDefaultData(api);
    resolveLoadStatisticsRequest(api, [categoryStat]);
    await waitFor(() => expect(api.allActiveRequests).toEqual({}));
  });

  test('go to landing when shop is disabled', async () => {
    const { api, app } = setupTestApp(`?shopId=${shops[0].id}`);

    await waitFor(jest.fn());
    resolveLoadShopsRequest(api, [{ ...shops[0], updateStatus: UpdateStatus.DISABLED }]);

    await app.findByText(ACTIVATE_SHOP_TEXT);
  });

  test('go to landing when shop is locked', async () => {
    const { api, app } = setupTestApp(`?shopId=${shops[0].id}`);

    await waitFor(jest.fn());
    resolveLoadShopsRequest(api, [{ ...shops[0], locked: true }]);

    await app.findByText('Все действия в выбранном магазине временно недоступны');
  });
});

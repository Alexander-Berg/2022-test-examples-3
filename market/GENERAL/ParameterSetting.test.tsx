import userEvent from '@testing-library/user-event';
import { waitFor, RenderResult } from '@testing-library/react';
import { MockedApiObject } from '@yandex-market/mbo-test-utils';

import { PAGE_TITLE } from './ParameterSetting';
import { setupTestApp } from 'src/test/setupApp';
import {
  resolveDefaultData,
  resolveLoadMappingRequest,
  resolveLoadModelsRequest,
  resolveLoadCategoryDataRequest,
} from 'src/test/api/resolves';
import { simpleMapping, rule, shopModel, categoryData } from 'src/test/data';
import { Api } from 'src/java/Api';
import { CAN_EXPORT_TEXT } from './components/ExportStateToggler/ExportStateToggler';

const getCheckbox = (app: RenderResult) => {
  return app.container.querySelector('.react-grid-checkbox-container');
};

const resolveSelectedCategory = (api: MockedApiObject<Api>) => {
  resolveLoadModelsRequest(api, [shopModel]);
  resolveLoadCategoryDataRequest(api, categoryData);
};

const defaultResolves = async (api: MockedApiObject<Api>) => {
  resolveDefaultData(api);
  resolveLoadMappingRequest(api, { mappings: [simpleMapping], rules: [rule] });
  resolveSelectedCategory(api);

  await waitFor(() => expect(api.allActiveRequests).toEqual({}));
};
describe('ParameterSetting', () => {
  test('render', () => {
    const { app } = setupTestApp('/parameters');
    app.getAllByText(PAGE_TITLE);
  });

  test('select category', async () => {
    const { api, app } = setupTestApp('/parameters?shopId=1');

    resolveDefaultData(api);
    resolveLoadMappingRequest(api, { mappings: [simpleMapping], rules: [rule] });
    await waitFor(() => expect(api.allActiveRequests).toEqual({}));

    app.getByText(/Все товары/);

    userEvent.click(app.getAllByText(/Выбрать/)[0]);

    resolveLoadModelsRequest(api, [shopModel]);
    resolveLoadCategoryDataRequest(api, categoryData);

    await waitFor(() => expect(api.allActiveRequests).toEqual({}));
  });

  test('open with selected category', async () => {
    const { api, app } = setupTestApp(`/parameters?shopId=1&marketCategoryId=${categoryData.hid}`);

    await defaultResolves(api);
    // проверяем что товар для выбранной категории отображается
    await app.findByText(new RegExp(shopModel.shopSku));
  });

  test('select models', async () => {
    const { api, app } = setupTestApp(`/parameters?shopId=1&marketCategoryId=${categoryData.hid}`);

    await defaultResolves(api);

    const checkbox = getCheckbox(app);
    expect(getCheckbox(app)).toBeTruthy();
    userEvent.click(checkbox!);

    // проверяем что товар выбрался
    app.getAllByText(/Выбран 1 товар/);
  });

  test('setup with default filters', async () => {
    const { api, app } = setupTestApp(`/parameters?shopId=1&marketCategoryId=${categoryData.hid}`);

    await defaultResolves(api);

    await app.findByText(CAN_EXPORT_TEXT);
  });
});

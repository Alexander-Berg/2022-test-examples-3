import React from 'react';
import { act, render, RenderResult } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { setupTestProvider } from 'test/setupApp';
import { testMdmParamMetadata, testMdmSsku } from 'test/data/mdmSskuParameterEditor';
import { App } from 'src/App';
import { MockedApiObject } from '@yandex-market/mbo-test-utils';
import Api from '../../Api';
import { Store } from 'redux';
import { RootState } from '../../store/root/reducer';

const testFindResults = {
  items: [testMdmSsku],
  totalCount: 1,
};

describe('SskuListPage', () => {
  let app: RenderResult;
  let api: MockedApiObject<Api>;
  let store: Store<RootState>;

  beforeEach(() => {
    const testProvider = setupTestProvider(`/mdm/ssku`);
    const { Provider } = testProvider;
    api = testProvider.api;
    store = testProvider.store;

    app = render(
      <Provider>
        <App />
      </Provider>
    );
  });

  it('renders', () => {
    expect(app.getAllByText('Shop sku')).toHaveLength(1);
  });

  it('Get/store metadata/sskus correctly', () => {
    api.mdmSskuUiController.metadata.next().resolve(testMdmParamMetadata);
    api.mdmSskuUiController.find.next().resolve(testFindResults);

    expect(api.mdmSskuUiController.metadata).toBeCalledTimes(1);
    expect(store.getState().mdm.ssku.metadata).toMatchObject(testMdmParamMetadata);

    expect(api.mdmSskuUiController.find).toBeCalledTimes(1);

    const { findResults } = store.getState().mdm.ssku;
    expect(findResults).toHaveProperty('items');
    expect(findResults).toHaveProperty('totalCount');
  });

  it('export using file', async () => {
    const checkbox = app.getByText('Из файла');
    userEvent.click(checkbox);

    const attach = app.container.querySelector('input[name="mdm-exportToExcelByFile"]') as HTMLInputElement;
    userEvent.upload(attach, new File([], 'ssss', { type: 'xls' }));

    const exportSsku = app.getByText('Экспортировать ssku');
    userEvent.click(exportSsku);

    expect(api.mdmSskuUiController.exportToExcelByFile).toBeCalled();

    // expect(app.find(ExportSSKU).find(CircularProgress)).toHaveLength(1);
    await act(async () => {
      api.mdmSskuUiController.exportToExcelByFile.next().reject('Нет уж');
    });
    // expect(app.find(ExportSSKU).find(CircularProgress)).toHaveLength(0);
  });
});

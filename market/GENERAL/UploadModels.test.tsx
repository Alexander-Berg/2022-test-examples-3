import React from 'react';
import { MockedApiObject } from '@yandex-market/mbo-test-utils';
import { act, fireEvent, RenderResult, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import UploadModels from './UploadModels';
import { setupWithReatom } from 'src/test/withReatom';
import { processingConfigAtom } from './store/processingConfig.atom';
import { errorProcessingAtom } from './store/errorProcessing.atom';
import { importResult, processingConfigInfo } from 'src/test/data';
import { resolveCheckUploadRequest, resolveStartImportRequest } from 'src/test/api/resolves';
import { Api } from 'src/java/Api';
import { importResultAtom, setImportResultAction } from './store/importResult.atom';
import { setShopIdAction, shopIdAtom } from 'src/store/shop';
import { hasMissedModels } from './utils';

const TASK_ID = 123;

const getFile = () => {
  const fileContents = '';
  return new Blob([fileContents], { type: 'text/plain' });
};

const getFilesInput = (container: HTMLElement) => {
  const inputs = container.querySelectorAll('input');
  expect(inputs).toHaveLength(2);
  return inputs[0];
};

const loadFile = (fileInput: HTMLInputElement, api: MockedApiObject<Api>) => {
  const file = getFile();

  fireEvent.change(fileInput!, { target: { value: undefined, files: [file] } });

  const requests = api.fileProcessingController.startImport.activeRequests();
  expect(requests).toBeTruthy();
  expect(requests[0]).toBeTruthy();

  return requests[0];
};

const selectSku = (app: RenderResult) => {
  userEvent.type(app.getByRole('textbox'), 'SKU');
  fireEvent.keyDown(app.getAllByText(new RegExp('SKU', 'i'))[0], { key: 'Enter', code: 'Enter' });
};

const continueImport = (app: any) => {
  fireEvent.click(app.getByText('Сохранить'));
};

const defaultAtoms = { processingConfigAtom, errorProcessingAtom, shopIdAtom };

const defaultActions = [setShopIdAction(1)];

describe('UploadModels', () => {
  window.confirm = jest.fn(() => true);
  window.alert = jest.fn();

  test('set no partial', () => {
    const { app } = setupWithReatom(<UploadModels />, defaultAtoms, defaultActions);

    const loadTypeChecker = app.getByText('Перезаписать характеристики товаров');
    fireEvent.click(loadTypeChecker);

    const fileInput = getFilesInput(app.container);
    const file = getFile();
    fireEvent.change(fileInput!, { target: { value: undefined, files: [file] } });
  });

  test('ignored sku alert', () => {
    const { app, api } = setupWithReatom(<UploadModels />, { ...defaultAtoms, importResultAtom }, [
      ...defaultActions,
      setImportResultAction(importResult),
    ]);

    app.getByText('Некоторые товары из файла не были импортированы');
    app.getByText('578396999');

    const attach = app.getByText('Загрузить другой файл').parentElement?.getElementsByTagName('input')[0];
    expect(attach).toBeTruthy();
    const file = getFile();
    act(() => {
      fireEvent.change(attach!, { target: { value: undefined, files: [file] } });
    });
    expect(api.fileProcessingController.activeRequests()).toHaveLength(1);
  });

  test('partial load by default', async () => {
    const { app, api } = setupWithReatom(<UploadModels />, defaultAtoms, defaultActions);

    const fileInput = getFilesInput(app.container);

    // по умолчанию должна быть частичная загрузка
    expect(loadFile(fileInput, api)).toBeTruthy();

    resolveStartImportRequest(api, TASK_ID);

    await app.findByText('Загрузка файла...');

    resolveCheckUploadRequest(api, { result: { processingConfigInfo } });

    await waitFor(() => expect(api.allActiveRequests).toEqual({}));

    app.getByText('Ваш SKU *');

    selectSku(app);

    continueImport(app);

    await waitFor(() => expect(api.fileProcessingController.activeRequests()).toEqual([]));
  });

  it('check for missed models', () => {
    expect(
      hasMissedModels({
        duplicateSkus: [],
        failedModelCount: 1,
        notModifiedCount: 2,
        skusNotFoundCount: 22,
      })
    ).toBe(true);

    expect(
      hasMissedModels({
        totalRows: 2,
        modelsCount: 2,
      })
    ).toBe(false);

    expect(
      hasMissedModels({
        totalRows: 1,
        modelsCount: 0,
      })
    ).toBe(false);
  });
});

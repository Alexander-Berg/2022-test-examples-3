import React from 'react';
import userEvent from '@testing-library/user-event';
import { act, waitFor } from '@testing-library/react';

import { ExporterPanel } from './ExporterPanel';
import { setupWithReatom } from 'src/test/withReatom';
import { shopModel } from 'src/test/data';
import { ShopModelView, ShopModelProcessingStatus } from 'src/java/definitions';

describe('ExporterPanel', () => {
  const callYm = jest.fn();
  window.ym = callYm;

  test('render', async () => {
    const resetSelection = jest.fn();
    const updateModels = jest.fn((models: ShopModelView[]) => {
      // поменялся ли статус после отправки
      expect(models[0].shopModelProcessingStatus).toEqual(ShopModelProcessingStatus.PROCESSING);
    });

    const { app, api } = setupWithReatom(
      <ExporterPanel selection={[shopModel]} resetSelection={resetSelection} updateModels={updateModels} />
    );

    app.getByText(/1 товар/);

    userEvent.click(app.getByText('Загрузить в кабинет', { exact: false }));

    await waitFor(() => expect((api.allActiveRequests as any).exportController).toBeTruthy());

    act(() => {
      api.exportController.addModelsToExportQueue
        .next()
        .resolve([{ addedToExport: '', shopId: 1, shopModelId: shopModel.id }]);
    });

    await waitFor(() => expect(api.exportController.activeRequests()).toHaveLength(0));
    expect(resetSelection).toHaveBeenCalledTimes(1);
  });
});

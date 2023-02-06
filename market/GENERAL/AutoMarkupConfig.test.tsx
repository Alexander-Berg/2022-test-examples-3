import React from 'react';
import { fireEvent, render, waitFor } from '@testing-library/react';

import { AutoMarkupConfig } from './AutoMarkupConfig';
import { AutoMarkupConfigModel } from './auto-markup-config.model';
import { setupApi } from 'test/setupApi';

describe('AutoMarkupConfig', () => {
  it('renders', async () => {
    const api = setupApi();
    const app = render(<AutoMarkupConfig model={new AutoMarkupConfigModel(api)} />);
    api.mdmAutoMarkupController.findQueries.next().resolve([
      {
        id: 0,
        deleted: false,
        enabled: true,
        modifiedAt: '1234-12-12',
        name: 'Query_testik_name',
        type: 'MSKU_PARAM_YQL',
        yqlQueryId: 'query_id',
        rowsLimit: 1000,
        userLogin: 'testik',
      } as any,
    ]);

    const nameInput = await app.findByDisplayValue('Query_testik_name');
    fireEvent.change(nameInput, { target: { value: 'Testovich' } });
    const saveButton = await app.findByTitle('Сохранить изменения');
    fireEvent.click(saveButton);
    const saveRequest = api.mdmAutoMarkupController.updateQuery.next();
    expect(saveRequest.args()[0]).toEqual({
      enabled: true,
      id: 0,
      name: 'Testovich',
      rowsLimit: 1000,
      type: 'MSKU_PARAM_YQL',
      yqlQueryId: 'query_id',
    });
  });

  it('process saving', async () => {
    const api = setupApi();
    const app = render(<AutoMarkupConfig model={new AutoMarkupConfigModel(api)} />);
    api.mdmAutoMarkupController.findQueries.next().resolve([]);

    const addRuleButton = await app.findByTitle('Добавить правило');
    fireEvent.click(addRuleButton);
    const nameInput = (await app.findAllByText('Имя запроса'))[0].parentElement?.getElementsByTagName('input').item(0);
    const idInput = (await app.findAllByText('Id запроса'))[0].parentElement?.getElementsByTagName('input').item(0);
    const typeInput = (await app.findAllByText('Тип'))[0].parentElement?.getElementsByTagName('input').item(0);

    fireEvent.change(nameInput!, { target: { value: 'qwerty' } });
    fireEvent.change(idInput!, { target: { value: 'qwertievna' } });
    fireEvent.change(typeInput!, { target: { value: 'MSKU_PARAM_YQL' } });

    let saveButton = await app.findByTitle('Сохранить изменения');
    fireEvent.click(saveButton);
    expect(api.mdmAutoMarkupController.addQuery.activeRequests()).toHaveLength(1);
    api.mdmAutoMarkupController.addQuery.next().resolve({
      id: 0,
      deleted: false,
      enabled: true,
      modifiedAt: '1234-12-12',
      name: 'qwerty',
      type: 'MSKU_PARAM_YQL',
      yqlQueryId: 'qwertievna',
      rowsLimit: 1000,
      userLogin: 'testik',
    } as any);

    waitFor(() => expect(app.queryAllByTitle('Сохранить изменения')).toBeFalsy());

    const nameInput2 = (await app.findAllByText('Имя запроса'))[0].parentElement?.getElementsByTagName('input').item(0);
    fireEvent.change(nameInput2!, { target: { value: 'qwerty2' } });
    saveButton = await app.findByTitle('Сохранить изменения');
    fireEvent.click(saveButton);
    api.mdmAutoMarkupController.updateQuery.next().reject('Нет уж');
    expect(saveButton).toBeInTheDocument();
  });
});

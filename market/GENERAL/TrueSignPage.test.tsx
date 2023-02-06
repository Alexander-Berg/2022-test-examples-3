import React from 'react';
import { act, render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { setupTestProvider } from 'test/setupApp';
import { TrueSignPage } from './TrueSignPage';
import { TaskState } from '../../java/definitions';

describe('<TrueSignPage />', () => {
  it('load and render', async () => {
    const { Provider, api } = setupTestProvider();
    const app = render(
      <Provider>
        <TrueSignPage />
      </Provider>
    );

    const successOrders = [123456, 987654];
    const failedOrders = [1987654];
    userEvent.type(app.getByRole('textbox'), '1234, asdfgh, ');
    userEvent.click(app.getByRole('button'));
    expect(api.manualTaskController.removeCisRequirements).toBeCalledWith({ checkOnly: true, mskuIds: [1234] });

    await act(async () => {
      api.manualTaskController.removeCisRequirements
        .next()
        .resolve({ successfulOrderIds: successOrders, failedOrderIds: [], taskState: TaskState.SUCCESSFUL });
    });

    app.getByText(successOrders.join(', '));
    userEvent.click(app.getByText('Снять ЧЗ с заказов'));
    expect(api.manualTaskController.removeCisRequirements).toBeCalledWith({ checkOnly: false, mskuIds: [1234] });

    await act(async () => {
      api.manualTaskController.removeCisRequirements
        .next()
        .resolve({ successfulOrderIds: successOrders, failedOrderIds: failedOrders, taskState: TaskState.SEMI_FAILED });
    });

    expect(app.getByText(successOrders.join(', '))).toBeInTheDocument();
    expect(app.getByText(failedOrders.join(', '))).toBeInTheDocument();
  });
});

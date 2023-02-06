import React from 'react';
import { fireEvent, render } from '@testing-library/react';
import { createStore } from 'redux';
import { Provider } from 'react-redux';

import { rootReducer } from 'src/store/root/reducer';
import { OperationConfirmationDialog } from '.';

describe('OperationConfirmationDialog', () => {
  let component: ReturnType<typeof render> = null;
  beforeEach(() => {
    const store = createStore(rootReducer, {
      shopSkuAvailability: {
        operationConfirmation: {
          action: {
            type: 1,
          },
          message: 'Сообщение',
          show: true,
        },
      },
    });

    component = render(
      <Provider store={store}>
        <OperationConfirmationDialog />
      </Provider>
    );
  });
  it('main flow', () => {
    expect(component.queryByText('Сообщение')).toBeInTheDocument();
    fireEvent.click(component.getByRole('button', { name: 'Подтвердить' }));
    expect(component.queryByText('Сообщение')).not.toBeInTheDocument();
  });

  it('discard flow', () => {
    expect(component.queryByText('Сообщение')).toBeInTheDocument();
    fireEvent.click(component.getByRole('button', { name: 'Отменить' }));
    expect(component.queryByText('Сообщение')).not.toBeInTheDocument();
  });
});

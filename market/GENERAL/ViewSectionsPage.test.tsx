import React from 'react';
import { act, render } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { ViewSectionsPage } from './ViewSectionsPage';
import { DoctorEntities, DoctorViewType } from 'src/entities/doctor';
import { setupTestProvider } from 'src/test/utils';

export const response: DoctorEntities = {
  sections: [
    {
      type: DoctorViewType.LINKS,
      expanded: true,
    },
  ],
  links: [
    {
      url: 'https://doctor.market.yandex-team.ru',
      text: 'Ссылка на доктор',
    },
  ],
};

describe('<ViewSectionsPage />', () => {
  test('render', () => {
    jest.useFakeTimers();
    const { Provider } = setupTestProvider();
    const app = render(
      <Provider>
        <ViewSectionsPage />
      </Provider>
    );
    const input = app.getByRole('textbox');
    // в инпуте есть дебаунс
    userEvent.clear(input);
    act(() => {
      jest.runOnlyPendingTimers();
    });
    userEvent.type(input, JSON.stringify(response));
    act(() => {
      jest.runOnlyPendingTimers();
    });
    app.getAllByText('Ссылка на доктор', { exact: false });
  });
});

import { setupWithReatom } from 'src/test/withReatom';
import { UsersDialogActivator } from './UsersDialogActivator';
import React from 'react';
import userEvent from '@testing-library/user-event';
import { waitFor } from '@testing-library/react';

describe('UsersDialogActivator', () => {
  test('activate', async () => {
    const { app } = setupWithReatom(<UsersDialogActivator />);

    // название самой кнопки
    userEvent.click(app.getByText('Пользователи'));

    await waitFor(() => {
      //  + заголовок в модалке
      app.getAllByText('Пользователи');
    });
  });
});

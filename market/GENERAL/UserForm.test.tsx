import React from 'react';
import userEvent from '@testing-library/user-event';
import { waitFor, fireEvent, RenderResult, act } from '@testing-library/react';

import { setupWithReatom } from 'src/test/withReatom';
import { UserForm, DUPLICATE_USER_ERROR } from './UserForm';
import { userListAtom, setUserListAction } from 'src/store/userList.atom';
import { Role } from 'src/java/definitions';

const user = { role: Role.ADMIN, shopIds: [], id: 1 };
const atoms = { userListAtom };
const dispatches = [setUserListAction([user])];

const typeSelect = (app: RenderResult, inputs: HTMLElement, value: string) => {
  userEvent.type(inputs, Role.ADMIN);
  fireEvent.keyDown(app.getAllByText(new RegExp(value, 'i'))[0], { key: 'Enter', code: 'Enter' });
  userEvent.type(inputs, Role.ADMIN);
};

describe('ShopForm', () => {
  test('add user', async () => {
    const { app, api } = setupWithReatom(<UserForm />, atoms, dispatches);

    const userIdInput = app.getByRole('spinbutton');
    userEvent.type(userIdInput, '222');

    const selectors = app.getAllByRole('textbox');

    typeSelect(app, selectors[0], Role.ADMIN);

    userEvent.click(app.getByText(/Добавить/i));

    act(() => {
      api.userController.insertUserPermissions.next().resolve(user);
    });

    await waitFor(() => expect(api.userController.insertUserPermissions.activeRequests()).toHaveLength(0));
    // после добавления пользователя должен быть запрос на обновления списка пользователей
    expect(api.userController.getUsers.activeRequests()).toHaveLength(1);
  });

  test('invalid user', async () => {
    const { app, api } = setupWithReatom(<UserForm />, atoms, dispatches);

    const userIdInput = app.getByRole('spinbutton');
    userEvent.type(userIdInput, '1');

    app.getByText(DUPLICATE_USER_ERROR);

    userEvent.click(app.getByText(/Добавить/i));
    expect(api.userController.activeRequests()).toHaveLength(0);
  });
});

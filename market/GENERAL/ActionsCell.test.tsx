import userEvent from '@testing-library/user-event';
import React from 'react';

import { ActionsCell, DELETE_ACTION_TEXT } from './ActionsCell';
import { Role } from 'src/java/definitions';
import { setupWithReatom } from 'src/test/withReatom';
import { mockConfirm } from 'src/test/utils';

const user = { role: Role.ADMIN, shopIds: [], id: 1 };

describe('ActionsCell', () => {
  test('confirm delete user', () => {
    window.confirm = mockConfirm(true);

    const { app, api } = setupWithReatom(<ActionsCell row={user} />);

    userEvent.click(app.getByTitle(DELETE_ACTION_TEXT));
    api.userController.deleteUserPermissions.next().resolve();
  });

  test('cancel delete user', () => {
    window.confirm = mockConfirm(false);

    const { app, api } = setupWithReatom(<ActionsCell row={user} />);

    userEvent.click(app.getByTitle(DELETE_ACTION_TEXT));
    expect(api.userController.activeRequests().length).toBe(0);
  });
});

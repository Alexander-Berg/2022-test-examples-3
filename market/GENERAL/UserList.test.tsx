import React from 'react';

import { setupWithReatom } from 'src/test/withReatom';
import { userListAtom, setUserListAction } from 'src/store/userList.atom';
import { Role } from 'src/java/definitions';
import { UserList } from './UserList';
import { waitFor } from '@testing-library/react';

const user = { role: Role.ADMIN, shopIds: [], id: 1 };
const atoms = { userListAtom };
const dispatches = [setUserListAction([user])];

describe('UserList', () => {
  test('render', async () => {
    const { app, api } = setupWithReatom(<UserList />, atoms, dispatches);

    api.userController.getUsers.next().resolve([user]);

    await waitFor(() => {
      app.getByText(user.role);
      app.getByText(`${user.id}`);
    });
  });
});

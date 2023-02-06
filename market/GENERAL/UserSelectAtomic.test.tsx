import React from 'react';
import { createStore } from '@reatom/core';
import { Provider } from '@reatom/react';
import { render } from '@testing-library/react';

import { ManagersActions, ManagersAtom, ManagersOptionsAtom } from 'src/store/atoms';
import { UserSelectAtomic } from '.';
import { MboUser } from 'src/java/definitions';

const TEST_USERS: MboUser[] = [
  {
    uid: 0,
    fullname: 'Testik Testovich',
    email: 'test@test.test',
    login: 'test',
    pureLogin: 'test',
    staffEmail: 'test@test.test',
    staffLogin: 'test',
    yandexEmail: 'test@test.test',
  },
];

describe('<UserSelectAtomic />', () => {
  it('renders without errors', () => {
    const store = createStore(ManagersAtom);
    store.dispatch(ManagersActions.set(TEST_USERS));
    render(
      <Provider value={store}>
        <UserSelectAtomic atomSelector={ManagersOptionsAtom} onSelect={() => 1} />
      </Provider>
    );
  });
});

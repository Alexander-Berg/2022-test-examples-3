import React from 'react';

import { setupWithReatom } from 'src/test/withReatom';
import { Header } from './Header';
import { currentUserAtom, initialUser, setCurrentUserAction } from 'src/store/user.atom';
import { Role } from 'src/java/definitions';
import { TestingRouter } from 'src/test/setupApp';

describe('<Header />', () => {
  test('Show user and shop editor for ADMIN', () => {
    const { app } = setupWithReatom(
      <TestingRouter route="/categories?canChangeCategory=1">
        <Header />
      </TestingRouter>,
      { currentUserAtom },
      [setCurrentUserAction({ ...initialUser, role: Role.SUPERADMIN })]
    );

    app.getByText('Магазины');
    app.getByText('Пользователи');
  });

  test(`Don't show user and shop editor for users`, () => {
    const { app } = setupWithReatom(
      <TestingRouter route="/categories?canChangeCategory=1">
        <Header />
      </TestingRouter>,
      { currentUserAtom },
      [setCurrentUserAction({ ...initialUser, role: Role.OPERATOR })]
    );

    expect(app.queryByText('Магазины')).toBeFalsy();
    expect(app.queryByText('Пользователи')).toBeFalsy();
  });
});

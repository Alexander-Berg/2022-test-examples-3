import React from 'react';
import { act, render } from '@testing-library/react';

import { Header } from './Header';
import { setupTestProvider } from 'src/test/utils';
import { ROUTES } from 'src/routes/routes';

describe('<Header />', () => {
  it('renders without errors', async () => {
    const { Provider } = setupTestProvider();
    const app = render(
      <Provider initialLocation={{ pathname: ROUTES.OFFER_DIAGNOSTIC_PAGE }}>
        <Header />
      </Provider>
    );

    const offersTab = await app.findByText('Офферы');
    expect(offersTab).toHaveClass('Header-Menu-Item-Link_active');
  });

  it('renders avatar', async () => {
    const { Provider, api } = setupTestProvider();
    const app = render(
      <Provider initialLocation={{ pathname: ROUTES.OFFER_DIAGNOSTIC_PAGE }}>
        <Header />
      </Provider>
    );
    const login = 'testikovich';
    await act(async () => {
      api.authController.me.next().resolve({ login, roles: [] });
    });
    expect(app.getByAltText(login).getAttribute('src')).toMatch(new RegExp(login));
  });
});

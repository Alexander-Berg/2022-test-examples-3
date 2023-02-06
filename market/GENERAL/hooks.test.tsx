import React, { useCallback, useEffect } from 'react';
import userEvent from '@testing-library/user-event';
import { render, screen } from '@testing-library/react';

import { useMakeOffersGreatAgain } from './useMakeOffersGreatAgain';
import { setupTestProvider } from '../../test/setupApp';
import { useMappingsImport } from './useMappingsImport';
import { useMboMskuSync } from './useMboMskuSync';
import { useUiTheme } from './useUiTheme';

describe('hooks', () => {
  it('useMakeOffersGreatAgain', async () => {
    const { api } = renderComponent(useMakeOffersGreatAgain, { searchString: '123_456' });

    expect(api.mdmSskuUiController.eoxSync.activeRequests()).toHaveLength(1);
    api.mdmSskuUiController.eoxSync.next().reject({ status: '500', statusText: 'Не вышло' });
    await screen.findByText('Loading:false');
  });

  it('useMappingsImport', async () => {
    const { api } = renderComponent(useMappingsImport, '1234567');

    expect(api.mdmMskuUiController.mbocSync.activeRequests()).toHaveLength(1);
    api.mdmMskuUiController.mbocSync.next().reject({ status: '500', statusText: 'Не вышло' });
    await screen.findByText('Loading:false');
  });

  it('useMboMskuSync', async () => {
    const { api } = renderComponent(useMboMskuSync, '1234567');

    expect(api.mdmMskuUiController.mboSync.activeRequests()).toHaveLength(1);
    api.mdmMskuUiController.mboSync.next().reject({ status: '500', statusText: 'Не вышло' });
    await screen.findByText('Loading:false');
  });

  it('useUiTheme', async () => {
    const { Provider } = setupTestProvider();
    const Comp = () => {
      const { isDarkTheme, setDarkTheme } = useUiTheme();
      const toggleTheme = useCallback(() => setDarkTheme(!isDarkTheme), [isDarkTheme, setDarkTheme]);

      return <div onClick={toggleTheme}>{isDarkTheme ? 'тёмная' : 'светлая'}</div>;
    };
    render(
      <Provider>
        <Comp />
      </Provider>
    );

    const caption = screen.getByText('светлая');

    userEvent.click(caption);

    expect(screen.getByText('тёмная')).toBeTruthy();
  });
});

function renderComponent(hook: (v: any) => { load: () => void; isLoading: boolean }, arg: any) {
  const { api, Provider } = setupTestProvider();
  const Comp = () => {
    const { load, isLoading } = hook(arg);
    useEffect(() => {
      load();
    }, [load]);

    return <div>Loading:{String(isLoading)}</div>;
  };
  render(
    <Provider>
      <Comp />
    </Provider>
  );

  return { api };
}

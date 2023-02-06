import React from 'react';
import { render, waitFor } from '@testing-library/react';

import { App } from './App';
import { setupTestProvider } from 'src/test/utils';
import { ROUTES } from './routes/routes';
import { act } from 'react-dom/test-utils';

describe('<App />', () => {
  it('render without errors', () => {
    const { Provider } = setupTestProvider();
    const { container } = render(
      <Provider>
        <App />
      </Provider>
    );
    expect(container.firstChild).toBeInTheDocument();
  });

  it('open with offer with euid', async () => {
    const { Provider, api } = setupTestProvider();

    const offerIdWithParams = 'offerId=867?euid=1233';

    render(
      <Provider
        initialLocation={{
          pathname: ROUTES.OFFER_DIAGNOSTIC_PAGE,
          search: `?businessId=723377&mainTab=TROUBLES&${offerIdWithParams}`,
        }}
      >
        <App />
      </Provider>
    );

    await waitFor(() => {
      expect(api.sessionController.activeRequests()).toHaveLength(1);
    });

    act(() => {
      api.sessionController.startSession.next().resolve({ sessionId: '123', started: '2022-01-10T13:27:49.2557' });
    });

    await waitFor(() => {
      expect(api.serviceProxyController.activeRequests()).toHaveLength(1);
    });

    // смотрим что при запросе у shopSku отрезалось ?euid=1233
    expect(api.serviceProxyController.proxyGet).toHaveBeenCalledWith(
      'mdm',
      'mdm-api/doctor/ssku/offer-info/offer?shopSku=867&businessId=723377'
    );
  });
});

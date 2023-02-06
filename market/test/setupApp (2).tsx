import React, { FC } from 'react';
import { context as ReatomContext } from '@reatom/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryParamsProvider } from '@yandex-market/react-typesafe-query';
import { render } from '@testing-library/react';
// eslint-disable-next-line import/no-extraneous-dependencies
import { createBrowserHistory } from 'history';

import { AppContent, DataWorkFlow } from '../AppContent';
import { setupApi } from './api/setupApi';
import { initReatomStore } from '../store/reatom/reatomStore';
import { ApiContext } from 'src/java/Api';

const history = createBrowserHistory({});

export const TestingRouter: FC<{ route: string }> = ({ route, children }) => {
  history.replace(route);
  return (
    <QueryParamsProvider history={history}>
      <MemoryRouter initialEntries={[route]}>{children}</MemoryRouter>
    </QueryParamsProvider>
  );
};

export const setupTestApp = (route: string) => {
  const api = setupApi();
  const reatomStore = initReatomStore(api);

  const app = render(
    <TestingRouter route={route}>
      <ReatomContext.Provider value={reatomStore}>
        <ApiContext.Provider value={api}>
          <AppContent />
        </ApiContext.Provider>
      </ReatomContext.Provider>
    </TestingRouter>
  );
  return { app, api, reatomStore };
};

export const setupTestComponent = (component: React.ReactNode, route: string) => {
  const api = setupApi();
  const reatomStore = initReatomStore(api);

  const app = render(
    <TestingRouter route={route}>
      <ReatomContext.Provider value={reatomStore}>
        <ApiContext.Provider value={api}>
          <DataWorkFlow />
          {component}
        </ApiContext.Provider>
      </ReatomContext.Provider>
    </TestingRouter>
  );
  return { app, api, reatomStore };
};

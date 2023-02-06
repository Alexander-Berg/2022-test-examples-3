import React, { FC } from 'react';
import { context as ReatomContext } from '@reatom/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryParamsProvider } from '@yandex-market/react-typesafe-query';
import { render } from '@testing-library/react';
import { createBrowserHistory } from 'history';

import { App } from '../App';
import { setupTestStore } from './setupStores';
import { ApiContext } from 'src/context/ApiContext';
import { api } from 'src/singletones/api/test';

const history = createBrowserHistory();

export const setupTestApp = (route: string) => {
  const { reatomStore } = setupTestStore(api);

  const app = render(
    <MemoryRouter initialEntries={[route]}>
      <QueryParamsProvider history={history}>
        <ReatomContext.Provider value={reatomStore}>
          <ApiContext.Provider value={api}>
            <App />
          </ApiContext.Provider>
        </ReatomContext.Provider>
      </QueryParamsProvider>
    </MemoryRouter>
  );

  return { app, api, reatomStore };
};

export const setupWithStore = (component: React.ReactNode) => {
  const { reatomStore } = setupTestStore(api);

  const app = render(
    <ReatomContext.Provider value={reatomStore}>
      <ApiContext.Provider value={api}>{component}</ApiContext.Provider>
    </ReatomContext.Provider>
  );

  return { app, api, reatomStore };
};

export const TestingRoute: FC = ({ children }) => {
  return <QueryParamsProvider history={history}>{children}</QueryParamsProvider>;
};

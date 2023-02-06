import React, { FC } from 'react';
import { context as ReatomContext } from '@reatom/react';
import { RenderOptions } from '@testing-library/react';
import { QueryParamsProvider } from '@yandex-market/react-typesafe-query';
import { Router } from 'react-router-dom';
import { createBrowserHistory } from 'history';

import { ApiContext } from 'src/context/ApiContext';
import { api } from 'src/singletones/api/test';
import { setupTestStore } from 'src/test/setupStores';

const history = createBrowserHistory();

type ProviderOptions = Omit<RenderOptions, 'queries'> & { route: string };

export const setupTestProvider = (options?: ProviderOptions) => {
  const { reatomStore } = setupTestStore(api);

  if (options?.route) {
    history.push(options.route);
  }

  const TestProvider: FC = ({ children }) => {
    return (
      <Router history={history}>
        <QueryParamsProvider history={history}>
          <ReatomContext.Provider value={reatomStore}>
            <ApiContext.Provider value={api}>{children}</ApiContext.Provider>
          </ReatomContext.Provider>
        </QueryParamsProvider>
      </Router>
    );
  };

  return TestProvider;
};

export type TestProviderType = ReturnType<typeof setupTestProvider>;

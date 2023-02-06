import React, { FC } from 'react';
import { Provider } from 'react-redux';
import { MemoryRouter } from 'react-router-dom';
import { Middleware } from 'redux';
import { QueryParamsProvider } from '@yandex-market/react-typesafe-query';

import { setupApi } from './setupApi';
import { setupTestStore } from './setupTestStore';
import { RootState } from 'src/store/root/reducer';
import { ApiContext } from 'src/context/ApiContext';
import { CurrentUserContext } from '../src/context/CurrentUserContext';
import { ConfigContext } from '../src/context/ConfigContext';
import { useLoadConfig } from '../src/hooks/useLoadConfig';
import { useLoadCurrentUser } from '../src/hooks/useLoadCurrentUser';
import { RootModel, RootModelContext } from '../src/models/root.model';
import { createMemoryHistory } from 'history';
import { parseOptions, stringifyOptions } from '../src/queryParams';
import { ThemeContextProvider } from '../src/context/ThemeContext';

export const setupTestProvider = (route: string = '', ...extraMiddleware: Middleware[]) => {
  const api = setupApi();
  const { store } = setupTestStore<RootState>(api, ...extraMiddleware);
  const history = createMemoryHistory({ initialEntries: [route] });
  const rootModel = new RootModel(api, jest.fn());

  const TestProvider: FC = ({ children }) => {
    const config = useLoadConfig(api);
    const currentUser = useLoadCurrentUser(api);

    return (
      <MemoryRouter initialEntries={[route]}>
        <Provider store={store}>
          <ApiContext.Provider value={api}>
            <RootModelContext.Provider value={rootModel}>
              <ConfigContext.Provider value={config}>
                <CurrentUserContext.Provider value={currentUser}>
                  <QueryParamsProvider
                    history={history}
                    parseOptions={parseOptions}
                    stringifyOptions={stringifyOptions}
                  >
                    <ThemeContextProvider>
                    {children}
                    </ThemeContextProvider>
                  </QueryParamsProvider>
                </CurrentUserContext.Provider>
              </ConfigContext.Provider>
            </RootModelContext.Provider>
          </ApiContext.Provider>
        </Provider>
      </MemoryRouter>
    );
  };

  return {
    api,
    Provider: TestProvider,
    store,
    rootModel,
  };
};

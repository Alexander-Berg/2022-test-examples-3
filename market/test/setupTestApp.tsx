/**
 * autoUpdateApp automatically calls app.update() on promise resolve/reject. Like redux does.
 * Good thing is that tests rerender partial updates after each request so we catch bugs inside.
 */
import { Provider as ReatomProvider } from '@reatom/react';
import React from 'react';
import { Provider } from 'react-redux';
import { Router } from 'react-router-dom';
import { registerLocale, setDefaultLocale } from 'react-datepicker';
import ru from 'date-fns/locale/ru';
import { QueryParamsProvider } from '@yandex-market/react-typesafe-query';
import { render } from '@testing-library/react';

import { App } from 'src/app/App';
import { configureStore as configureReatomStore } from 'src/models/store';
import { parseOptions, stringifyOptions } from 'src/utils/queryParamsOptions';
import { ApiContext } from 'src/context/ApiContext';
import { setupTestStore, ITestStoreOptions } from './setupTestStore';

registerLocale('ru', ru);
setDefaultLocale('ru');

interface ITestAppOptions {
  route: string;
}

export const setupTestApp = ({ route, ...storeOptions }: ITestAppOptions & ITestStoreOptions) => {
  const { store, api, actions, history } = setupTestStore(storeOptions);

  const reatomStore = configureReatomStore({ dependencies: { api, history } });

  history.push(route);
  const app = render(
    <Router history={history}>
      <ApiContext.Provider value={api}>
        <ReatomProvider value={reatomStore}>
          <QueryParamsProvider history={history} parseOptions={parseOptions} stringifyOptions={stringifyOptions}>
            <Provider store={store}>
              <App />
            </Provider>
          </QueryParamsProvider>
        </ReatomProvider>
      </ApiContext.Provider>
    </Router>
  );

  return { store, api, app, actions, history, reatomStore };
};

export type TestApp = ReturnType<typeof setupTestApp>;

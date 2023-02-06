import React, { FC } from 'react';
import { Provider } from 'react-redux';
import { Router } from 'react-router';
import { createMemoryHistory } from 'history';
import { QueryParamsProvider } from '@yandex-market/react-typesafe-query';

import { configureStore } from '@/store/configureStore';

export const history = createMemoryHistory();

const store = configureStore({ history });

export const Wrapper: FC = ({ children }) => {
  return (
    <Provider store={store}>
      <Router history={history}>
        <QueryParamsProvider history={history}>{children}</QueryParamsProvider>
      </Router>
    </Provider>
  );
};

export function getTestProvider() {
  return { store, history, Provider: Wrapper };
}

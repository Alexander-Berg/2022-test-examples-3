/**
 * autoUpdateApp automatically calls app.update() on promise resolve/reject. Like redux does.
 * Good thing is that tests rerender partial updates after each request so we catch bugs inside.
 */
import React, { FC } from 'react';
import { DndProvider } from 'react-dnd';
import { HTML5Backend } from 'react-dnd-html5-backend';
import { Provider } from 'react-redux';
import { Router } from 'react-router-dom';
import { registerLocale, setDefaultLocale } from 'react-datepicker';
import ru from 'date-fns/locale/ru';
import { QueryParamsProvider } from '@yandex-market/react-typesafe-query';
import { Store, createStore } from '@reatom/core';
import { Provider as ReatomProvider } from '@reatom/react';
import { render, RenderOptions } from '@testing-library/react';
import { MemoryHistory } from 'history';

import { parseOptions, stringifyOptions } from 'src/utils/queryParamsOptions';
import { ApiContext } from 'src/context/ApiContext';
import { configureStore } from 'src/models/store';
import { setupTestStore } from './setupTestStore';

registerLocale('ru', ru);
setDefaultLocale('ru');

export const TestingRouter: FC<{ history: MemoryHistory<any>; route?: string }> = ({ history, route, children }) => {
  if (route) {
    history.push(route);
  }
  return (
    <Router history={history}>
      <QueryParamsProvider history={history} parseOptions={parseOptions} stringifyOptions={stringifyOptions}>
        {children}
      </QueryParamsProvider>
    </Router>
  );
};

type ProviderOptions = Omit<RenderOptions, 'queries'> & { route: string };

export const setupTestProvider = (options?: ProviderOptions) => {
  const { store, api, history } = setupTestStore();

  const reatomStore = configureStore({ dependencies: { api, history } });

  const TestProvider: FC = ({ children }) => {
    return (
      <TestingRouter history={history} route={options?.route}>
        <DndProvider backend={HTML5Backend}>
          <ReatomProvider value={reatomStore}>
            <ApiContext.Provider value={api}>
              <QueryParamsProvider history={history} parseOptions={parseOptions} stringifyOptions={stringifyOptions}>
                <Provider store={store}>{children}</Provider>
              </QueryParamsProvider>
            </ApiContext.Provider>
          </ReatomProvider>
        </DndProvider>
      </TestingRouter>
    );
  };

  return TestProvider;
};

export type TestProviderType = ReturnType<typeof setupTestProvider>;

export function renderWithProvider(ui: React.ReactElement, options?: ProviderOptions) {
  return render(ui, {
    wrapper: setupTestProvider(options),
    ...options,
  });
}

export function renderWithReatomStore(
  ui: React.ReactElement,
  options: Omit<RenderOptions, 'queries'> & { store?: Store } = {}
) {
  const { store = createStore(), ...opts } = options;

  return {
    ...render(ui, {
      wrapper: ({ children }) => {
        return <ReatomProvider value={store}>{children}</ReatomProvider>;
      },
      ...opts,
    }),
    store,
  };
}

export function renderTableCell(ui: React.ReactElement, options?: Omit<RenderOptions, 'queries'>) {
  return render(ui, {
    wrapper: props => (
      <table>
        <tbody>
          <tr>{props.children}</tr>
        </tbody>
      </table>
    ),
    ...options,
  });
}

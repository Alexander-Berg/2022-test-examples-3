import { EnzymePropSelector, mount, ReactWrapper } from 'enzyme';
import React from 'react';
import { Provider } from 'react-redux';
import { AnyAction, Middleware, Store } from 'redux';
import { QueryParamsProvider } from '@yandex-market/react-typesafe-query';
import { MockedApiObject } from '@yandex-market/mbo-test-utils';
import { createStore as createReatomStore } from '@reatom/core';
import { Provider as ReatomProvider } from '@reatom/react';
import { Router } from 'react-router';
import { createMemoryHistory } from 'history';

import Api from 'src/Api';
import { App } from '../App';
import { setupApi, setupTestStore } from './setup';
import { RootState } from 'src/store/root/reducer';
import { parseOptions, stringifyOptions } from 'src/utils/query-params-options';
import { ApiContext } from 'src/context';

/**
 * autoUpdateApp automatically call app.update() on promise resolve/reject. Like redux does.
 *
 * On my laptop simple page with filter & datatable with single row takes:
 * - 2.3 ms on average to .update()
 * - 4.8 ms 95 percentile
 * - 2 ms median
 * so for page with say 10 requests we get +23ms per test, which is probably fine for our current test count.
 *
 * Good thing is that test rerenders partial updates after each request so we catch bugs inside.
 * And tests are simpler. Just magically works.
 */
export const setupTestApp = (route: string, autoUpdateApp = true, ...extraMiddleware: Middleware[]) => {
  let app: ReactWrapper;
  const api = setupApi(
    autoUpdateApp ? { afterReject: () => app.update(), afterResolve: () => app.update() } : undefined
  );
  const { store, actions } = setupTestStore(api, ...extraMiddleware);
  const reatomStore = createReatomStore();
  const history = createMemoryHistory({ initialEntries: [route] });

  app = mount(
    <Router history={history}>
      <QueryParamsProvider parseOptions={parseOptions} stringifyOptions={stringifyOptions}>
        <ReatomProvider value={reatomStore}>
          <Provider store={store}>
            <ApiContext.Provider value={api}>
              <App />
            </ApiContext.Provider>
          </Provider>
        </ReatomProvider>
      </QueryParamsProvider>
    </Router>
  );

  return { store, api, app, actions, history };
};

export class TestCmApp {
  public store: Store<RootState>;
  public api: MockedApiObject<Api>;
  public app: ReactWrapper<{}, {}, React.Component>;
  public actions: AnyAction[];

  constructor(route: string, autoUpdateApp = true, ...extraMiddleware: Middleware[]) {
    const { store, actions, api, app } = setupTestApp(route, autoUpdateApp, ...extraMiddleware);
    this.store = store;
    this.api = api;
    this.app = app;
    this.actions = actions;
  }

  public expectSingle(component: EnzymePropSelector) {
    expect(this.app.find(component)).toHaveLength(1);
  }

  public expectHasText(component: EnzymePropSelector, text: string) {
    expect(this.app.find(component).html()).toContain(text);
  }

  public find = (component: EnzymePropSelector) => this.app.find(component);

  public unmount() {
    this.app.unmount();
  }

  public update() {
    this.app.update();
  }
}

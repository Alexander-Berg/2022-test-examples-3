/**
 * autoUpdateApp automatically calls app.update() on promise resolve/reject. Like redux does.
 * Good thing is that tests rerender partial updates after each request so we catch bugs inside.
 */
import { Middleware } from 'redux';
import { createMemoryHistory } from 'history';
import { MockedApiObject } from '@yandex-market/mbo-test-utils';

import { createActionRecordingMiddleware } from 'src/test/setup';
import { configureStore } from 'src/store/configureStore';
import { RootState } from 'src/store/root/types';
import { api } from 'src/singletons/apiSingleton';
import { RestService } from 'src/services/RestService';

const history = createMemoryHistory();

export interface ITestStoreOptions {
  initialState?: RootState;
  middlewares?: Middleware[];
}

export const setupTestStore = ({ initialState, middlewares = [] }: ITestStoreOptions = {}) => {
  const { actions, middleware } = createActionRecordingMiddleware();

  const dependencies = {
    api: api as MockedApiObject<RestService>,
    history,
  };

  const store = configureStore({
    initialState,
    middlewares: [...middlewares, middleware],
    dependencies,
  });

  return { store, api: api as MockedApiObject<RestService>, actions, history };
};

export type TestStore = ReturnType<typeof setupTestStore>;

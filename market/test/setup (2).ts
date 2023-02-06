import { applyMiddleware, combineReducers, createStore, Middleware, Store } from 'redux';
import { combineEpics, createEpicMiddleware } from 'redux-observable';
import { AnyAction } from 'typescript-fsa';
import { createApiObjectMock, MockedApiObject, MockPromiseExtension } from '@yandex-market/mbo-test-utils';
import { createMemoryHistory } from 'history';

import Api from '../Api';
import { FrontendConfig } from 'src/java/definitions';
import { createAppStore } from 'src/store/createStore';
import { EpicDependencies, EpicType } from 'src/store/types';
import { RootState } from 'src/store/root/reducer';

export const setupApi = (extension?: MockPromiseExtension) => {
  let configPromise: Promise<FrontendConfig>;
  const api = createApiObjectMock<Api>(
    {
      getConfig(): Promise<FrontendConfig> {
        if (!configPromise) {
          configPromise = api.configController.getConfig();
        }

        return configPromise;
      },
      trackingToken: 'test-tracking-token',
    },
    extension
  );

  return api;
};

export const createActionRecordingMiddleware = () => {
  const actions: AnyAction[] = [];
  const middleware: Middleware = _ => next => action => {
    actions.push(action);
    return next(action);
  };
  return { actions, middleware };
};

export const setupTestStore = (api: MockedApiObject<Api & {}>, ...extraMiddleware: Middleware[]) => {
  const { actions, middleware } = createActionRecordingMiddleware();
  const history = createMemoryHistory();
  const store = createAppStore({ api, history }, middleware, ...extraMiddleware);

  return { store, api, actions, history };
};

export const setupStoreWithEpics = (...epics: EpicType[]) => {
  const actionsStore: AnyAction[] = [];

  // NOTE: Difference is that this doesn't call next(). Why?
  const mockStoreMiddleware: Middleware<AnyAction, AnyAction> = () => () => (action: AnyAction) => {
    actionsStore.push(action);
  };

  const history = createMemoryHistory();
  const api = setupApi();

  const epicMiddleware = createEpicMiddleware<AnyAction, AnyAction, RootState, EpicDependencies>({
    dependencies: { api, history },
  });

  const middlewareEnhancer = applyMiddleware(epicMiddleware, mockStoreMiddleware);
  const store: Store<{}> = createStore(combineReducers([]), middlewareEnhancer);

  epicMiddleware.run(combineEpics(...epics));

  return { store, api, history, getActions: () => actionsStore };
};

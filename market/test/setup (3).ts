import { applyMiddleware, combineReducers, createStore, Dispatch, Middleware, MiddlewareAPI, Store } from 'redux';
import { combineEpics, createEpicMiddleware } from 'redux-observable';
import { createMemoryHistory } from 'history';
import { AnyAction } from 'typescript-fsa';
import { createApiObjectMock, MockPromiseExtension } from '@yandex-market/mbo-test-utils';

import { EpicDependencies } from 'src/models/types';
import { RootState } from 'src/store/root/types';
import { RestService } from 'src/services/RestService';
import { Config } from 'src/java/definitions';
import { EpicType } from 'src/utils/types';

const history = createMemoryHistory();

export const setupApi = (extension?: MockPromiseExtension) => {
  let configPromise: Promise<Config>;
  const api = createApiObjectMock<RestService>(
    {
      getConfig(): Promise<Config> {
        if (!configPromise) {
          configPromise = api.configController.getConfig();
        }

        return configPromise;
      },
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

export const createActionSubscriberMiddleware = (onAction: (action: AnyAction) => AnyAction) => {
  const middleware: Middleware = _ => next => action => {
    return next(onAction(action));
  };
  return middleware;
};

export const createActionLoggingMiddleware = (transform: (action: AnyAction) => string = JSON.stringify) =>
  createActionSubscriberMiddleware((action: AnyAction) => {
    // eslint-disable-next-line no-console
    console.log(transform(action));
    return action;
  });

export const setupStoreWithEpics = (...epics: EpicType[]) => {
  const actionsStore: AnyAction[] = [];

  // NOTE: Difference is that this doesn't call next(). Why?
  const mockStoreMiddleware: Middleware<AnyAction, AnyAction> = (_apiParam: MiddlewareAPI) => (
    _next: Dispatch<AnyAction>
  ) => (action: AnyAction) => {
    actionsStore.push(action);
  };

  const api = setupApi();

  const epicMiddleware = createEpicMiddleware<AnyAction, AnyAction, RootState, EpicDependencies>({
    dependencies: {
      api,
      history,
    },
  });

  const middlewareEnhancer = applyMiddleware(epicMiddleware, mockStoreMiddleware);
  const store: Store<any> = createStore(combineReducers([]), middlewareEnhancer);

  epicMiddleware.run(combineEpics(...epics));

  return { store, api, getActions: () => actionsStore };
};

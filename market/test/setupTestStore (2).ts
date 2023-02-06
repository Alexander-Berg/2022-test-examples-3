import { Middleware } from 'redux';
import { AnyAction } from 'typescript-fsa';
import { MockedApiObject } from '@yandex-market/mbo-test-utils';
import { createMemoryHistory } from 'history';
import { createAppStore } from '../src/store/createStore';
import Api from '../src/Api';

export const createActionRecordingMiddleware = () => {
  const actions: AnyAction[] = [];
  const middleware: Middleware = _ => next => action => {
    actions.push(action);
    return next(action);
  };
  return { actions, middleware };
};

export const setupTestStore = <S>(api: MockedApiObject<Api & {}>, ...extraMiddleware: Middleware[]) => {
  const { actions, middleware } = createActionRecordingMiddleware();
  const history = createMemoryHistory();
  const store = createAppStore({ api, history }, middleware, ...extraMiddleware);

  return { store, api, actions, history };
};

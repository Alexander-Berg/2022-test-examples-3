import { createStore } from '@reatom/core';

import { declareLoadingStateAtom } from './declareLoadingStateAtom';
import { declareAsyncActions } from './declareAsyncActions';
import { __resetActionTypes } from './declareAction';

describe('reatom-helpers/declareLoadingStateAtom', () => {
  afterEach(() => {
    __resetActionTypes();
  });

  it('should set state to true after dispatch start action', () => {
    const asyncActionCreator = declareAsyncActions('TEST')();
    const atom = declareLoadingStateAtom(asyncActionCreator);
    const store = createStore(atom);

    store.dispatch(asyncActionCreator.start());

    expect(store.getState(atom)).toBe(true);
  });

  it.each(['done', 'fail', 'cancel'] as const)('should set state to false after dispatch %s action', property => {
    const asyncActionCreator = declareAsyncActions('TEST')();
    const atom = declareLoadingStateAtom(asyncActionCreator);
    const store = createStore(atom);

    expect(store.getState(atom)).toBe(false);

    store.dispatch(asyncActionCreator.start());
    store.dispatch(asyncActionCreator[property]());

    expect(store.getState(atom)).toBe(false);
  });
});

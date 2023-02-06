/* eslint-disable no-console */
import { Reducer, AnyAction, createStore } from '@reduxjs/toolkit';
import combineReducers from './combineReducers';

describe('combineReducers', () => {
  it('returns a composite reducer that maps the state keys to given reducers', () => {
    const reducer = combineReducers({
      counter: (state: number = 0, action) => (action.type === 'increment' ? state + 1 : state),
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      stack: (state: any[] = [], action) =>
        action.type === 'push' ? [...state, action.value] : state,
    });

    const s1 = reducer(undefined, { type: 'increment' });
    expect(s1).toEqual({ counter: 1, stack: [] });
    const s2 = reducer(s1, { type: 'push', value: 'a' });
    expect(s2).toEqual({ counter: 1, stack: ['a'] });
  });

  it('ignores all props which are not a function', () => {
    // we double-cast because these conditions can only happen in a javascript setting
    const reducer = combineReducers({
      fake: (true as unknown) as Reducer<unknown>,
      broken: ('string' as unknown) as Reducer<unknown>,
      another: ({ nested: 'object' } as unknown) as Reducer<unknown>,
      stack: (state = []) => state,
    });

    expect(Object.keys(reducer(undefined, { type: 'push' }))).toEqual(['stack']);
  });

  it('throws an error if a reducer returns undefined handling an action', () => {
    const reducer = combineReducers({
      counter(state: number = 0, action) {
        switch (action && action.type) {
          case 'increment':
            return state + 1;
          case 'decrement':
            return state - 1;
          case 'whatever':
          case null:
          case undefined:
            return undefined;
          default:
            return state;
        }
      },
    });

    expect(() => reducer({ counter: 0 }, { type: 'whatever' })).toThrow(/"whatever".*"counter"/);
    expect(() => reducer({ counter: 0 }, (null as unknown) as AnyAction)).toThrow(
      /"counter".*an action/,
    );
    expect(() => reducer({ counter: 0 }, ({} as unknown) as AnyAction)).toThrow(
      /"counter".*an action/,
    );
  });

  it('catches error thrown in reducer when initializing and re-throw', () => {
    const reducer = combineReducers({
      throwingReducer() {
        throw new Error('Error thrown in reducer');
      },
    });
    expect(() => reducer(undefined, (undefined as unknown) as AnyAction)).toThrow(
      /Error thrown in reducer/,
    );
  });

  it('allows a symbol to be used as an action type', () => {
    const increment = Symbol('INCREMENT');

    const reducer = combineReducers({
      counter(state: number = 0, action) {
        switch (action.type) {
          case increment:
            return state + 1;
          default:
            return state;
        }
      },
    });

    expect(reducer({ counter: 0 }, { type: increment }).counter).toEqual(1);
  });

  it('maintains referential equality if the reducers it is combining do', () => {
    const reducer = combineReducers({
      child1(state = {}) {
        return state;
      },
      child2(state = {}) {
        return state;
      },
      child3(state = {}) {
        return state;
      },
    });

    const initialState = reducer(undefined, { type: '@@INIT' });
    expect(reducer(initialState, { type: 'FOO' })).toBe(initialState);
  });

  it('does not have referential equality if one of the reducers changes something', () => {
    const reducer = combineReducers({
      child1(state = {}) {
        return state;
      },
      child2(state: { count: number } = { count: 0 }, action) {
        switch (action.type) {
          case 'increment':
            return { count: state.count + 1 };
          default:
            return state;
        }
      },
      child3(state = {}) {
        return state;
      },
    });

    const initialState = reducer(undefined, { type: '@@INIT' });
    expect(reducer(initialState, { type: 'increment' })).not.toBe(initialState);
  });

  describe('With Replace Reducers', function() {
    const foo = (state = {}) => state;
    const bar = (state = {}) => state;
    const ACTION = { type: 'ACTION' };

    it('should return an updated state when additional reducers are passed to combineReducers', function() {
      const originalCompositeReducer = combineReducers({ foo });
      const store = createStore(originalCompositeReducer);

      store.dispatch(ACTION);

      const initialState = store.getState();

      store.replaceReducer(combineReducers({ foo, bar }));
      store.dispatch(ACTION);

      const nextState = store.getState();
      expect(nextState).not.toBe(initialState);
    });

    it('should return an updated state when reducers passed to combineReducers are changed', function() {
      const baz = (state = {}) => state;

      const originalCompositeReducer = combineReducers({ foo, bar });
      const store = createStore(originalCompositeReducer);

      store.dispatch(ACTION);

      const initialState = store.getState();

      store.replaceReducer(
        (combineReducers({ baz, bar }) as unknown) as typeof originalCompositeReducer,
      );
      store.dispatch(ACTION);

      const nextState = store.getState();
      expect(nextState).not.toBe(initialState);
    });

    it('should return the same state when reducers passed to combineReducers not changed', function() {
      const originalCompositeReducer = combineReducers({ foo, bar });
      const store = createStore(originalCompositeReducer);

      store.dispatch(ACTION);

      const initialState = store.getState();

      store.replaceReducer(combineReducers({ foo, bar }));
      store.dispatch(ACTION);

      const nextState = store.getState();
      expect(nextState).toBe(initialState);
    });

    it('should return an updated state when one of more reducers passed to the combineReducers are removed', function() {
      const originalCompositeReducer = combineReducers({ foo, bar });
      const store = createStore(originalCompositeReducer);

      store.dispatch(ACTION);

      const initialState = store.getState();

      store.replaceReducer(combineReducers({ bar }) as typeof originalCompositeReducer);

      const nextState = store.getState();
      expect(nextState).not.toBe(initialState);
    });
  });
});

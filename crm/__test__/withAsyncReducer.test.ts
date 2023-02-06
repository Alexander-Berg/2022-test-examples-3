import { ReducersMapObject, Reducer, configureStore } from '@reduxjs/toolkit';
import combineReducers from 'utils/reducer/combineReducers';
import { withAsyncReducer } from '../withAsyncReducer';

jest.useFakeTimers();

describe('withAsyncReducer', () => {
  const createReducer = jest.fn((reducers: ReducersMapObject) => combineReducers(reducers));

  const createEnhancedStore = () => {
    const store = configureStore({ reducer: () => ({}) });

    return withAsyncReducer(store, { createReducer });
  };

  test('should support add reducer', () => {
    const enhancedStore = createEnhancedStore();

    const reducer: Reducer = () => 'test';

    enhancedStore.injectReducer('name', reducer);
    enhancedStore.dispatch({ type: 'action' });

    expect(enhancedStore.getState()).toEqual({ name: 'test' });
  });

  it('should remove reducer with delay', () => {
    const enhancedStore = createEnhancedStore();

    const reducer: Reducer = () => 'test';

    enhancedStore.injectReducer('name', reducer);
    enhancedStore.dispatch({ type: 'action' });

    expect(enhancedStore.getState()).toEqual({ name: 'test' });

    enhancedStore.removeReducer('name');

    expect(enhancedStore.getState()).toEqual({ name: 'test' });

    jest.advanceTimersByTime(1000);

    expect(enhancedStore.getState()).toEqual({});
  });

  it('should not remove reducer if it add again', () => {
    const enhancedStore = createEnhancedStore();

    const reducer: Reducer = () => 'test';

    enhancedStore.injectReducer('name', reducer);
    enhancedStore.dispatch({ type: 'action' });

    enhancedStore.removeReducer('name');

    jest.advanceTimersByTime(900);

    enhancedStore.injectReducer('name', reducer);

    jest.advanceTimersByTime(2000);

    expect(enhancedStore.getState()).toEqual({ name: 'test' });
  });
});

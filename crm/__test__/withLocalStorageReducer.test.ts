import { withLocalStorageReducer } from '../withLocalStorageReducer';
import { AppStore } from '../types';

const store = {
  injectReducer: jest.fn(),
};

describe('withLocalStorageReducer', () => {
  beforeEach(() => {
    store.injectReducer.mockClear();
  });

  test('default localStorageReducers', () => {
    const enhancedStore = withLocalStorageReducer((store as unknown) as AppStore);

    expect(enhancedStore.localStorageReducers).toBeInstanceOf(Object);
    expect(Object.keys(enhancedStore.localStorageReducers)).toHaveLength(0);
    expect(enhancedStore.localStorageReducer({ state: 'state' }, { type: 'action' })).toEqual({
      state: 'state',
    });
  });

  test('default localStorageReducer', () => {
    const enhancedStore = withLocalStorageReducer((store as unknown) as AppStore);

    expect(enhancedStore.localStorageReducer({ state: 'state' }, { type: 'action' })).toEqual({
      state: 'state',
    });
  });

  test('injectReducerWithLocalStorage', () => {
    const enhancedStore = withLocalStorageReducer((store as unknown) as AppStore);

    const reducer = () => {};

    enhancedStore.injectReducerWithLocalStorage('name', reducer, false);

    expect(enhancedStore.localStorageReducers.name).toBe(reducer);
    expect(store.injectReducer).toBeCalledTimes(1);
    expect(store.injectReducer).toBeCalledWith('name', reducer, false);

    const newReducer = () => {};
    enhancedStore.injectReducerWithLocalStorage('name', newReducer, false);

    expect(enhancedStore.localStorageReducers.name).toBe(reducer);
    expect(store.injectReducer).toBeCalledTimes(2);
    expect(store.injectReducer).toBeCalledWith('name', reducer, false);
  });
});

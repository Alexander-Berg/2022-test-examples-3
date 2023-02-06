import { Store } from '@reatom/core';

import { configureStore } from 'src/models/store';
import { AntiMappingsSelectionActions, AntiMappingsSelectionAtom } from './AntiMappingsSelectionAtom';
import { setupTestStore } from 'src/test/setupTestStore';

describe('AntiMappingsSelectionAtom::', () => {
  let store: Store;

  beforeEach(() => {
    const { api, history } = setupTestStore();
    store = configureStore({ dependencies: { api, history } });
  });

  it('inits', () => {
    expect(store.getState(AntiMappingsSelectionAtom)).toEqual({});
  });

  it('set works', () => {
    store.subscribe(AntiMappingsSelectionAtom, jest.fn());
    store.dispatch(AntiMappingsSelectionActions.set({ 123: true, 234: true }));
    expect(store.getState(AntiMappingsSelectionAtom)).toEqual({ 123: true, 234: true });
  });
});

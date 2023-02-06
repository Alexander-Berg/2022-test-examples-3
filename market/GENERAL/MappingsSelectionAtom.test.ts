import { Store } from '@reatom/core';

import { configureStore } from 'src/models/store';
import { MappingsSelectionActions, MappingsSelectionAtom } from './MappingsSelectionAtom';
import { setupTestStore } from 'src/test/setupTestStore';

describe('MappingsSelectionAtom::', () => {
  let store: Store;

  beforeEach(() => {
    const { api, history } = setupTestStore();
    store = configureStore({ dependencies: { api, history } });
  });

  it('inits', () => {
    expect(store.getState(MappingsSelectionAtom)).toEqual({});
  });

  it('set works', () => {
    store.subscribe(MappingsSelectionAtom, jest.fn());
    store.dispatch(MappingsSelectionActions.set({ 123: true, 234: true }));
    expect(store.getState(MappingsSelectionAtom)).toEqual({ 123: true, 234: true });
  });

  it('switchAll works', () => {
    store.subscribe(MappingsSelectionAtom, jest.fn());
    store.dispatch(MappingsSelectionActions.set({ 123: true, 234: true }));
    store.dispatch(MappingsSelectionActions.switchAll([234, 345]));
    expect(store.getState(MappingsSelectionAtom)).toEqual({ 123: true, 234: false, 345: false });
    store.dispatch(MappingsSelectionActions.switchAll([234, 345]));
    expect(store.getState(MappingsSelectionAtom)).toEqual({ 123: true, 234: true, 345: true });
  });
});

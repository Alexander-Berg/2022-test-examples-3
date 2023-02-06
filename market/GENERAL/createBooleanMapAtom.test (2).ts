import { createStore } from '@reatom/core';

import { createBooleanMapAtom } from 'src/utils';

describe('createBooleanMapAtom', () => {
  it('inits', () => {
    const store = createStore();

    const { MapAtom, TrulyIdsAtom } = createBooleanMapAtom('TEST');

    store.subscribe(MapAtom, jest.fn());

    expect(store.getState(MapAtom)).toEqual({});
    expect(store.getState(TrulyIdsAtom)).toEqual([]);
  });
  it('set works', () => {
    const store = createStore();

    const { MapAtom, TrulyIdsAtom, set } = createBooleanMapAtom('TEST');

    store.subscribe(MapAtom, jest.fn());

    store.dispatch(set({ 123: true, 234: false }));

    expect(store.getState(MapAtom)).toEqual({ 123: true, 234: false });
    expect(store.getState(TrulyIdsAtom)).toEqual([123]);
  });
  it('setSome works', () => {
    const store = createStore();

    const { MapAtom, TrulyIdsAtom, set, setSome } = createBooleanMapAtom('TEST');

    store.subscribe(MapAtom, jest.fn());

    store.dispatch(set({ 123: true, 234: false }));
    store.dispatch(setSome({ 234: true }));

    expect(store.getState(MapAtom)).toEqual({ 123: true, 234: true });
    expect(store.getState(TrulyIdsAtom)).toEqual([123, 234]);
  });
});

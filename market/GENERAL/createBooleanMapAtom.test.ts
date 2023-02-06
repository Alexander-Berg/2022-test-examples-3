import { createStore } from '@reatom/core';

import { createBooleanMapAtom } from './createBooleanMapAtom';

describe('createBooleanMapAtom', () => {
  it('inits', () => {
    const store = createStore();

    const { Atom, TrulyIdsAtom } = createBooleanMapAtom<number>('TEST');

    store.subscribe(Atom, jest.fn());

    expect(store.getState(Atom)).toEqual({});
    expect(store.getState(TrulyIdsAtom)).toEqual([]);
  });
  it('set works', () => {
    const store = createStore();

    const { Atom, TrulyIdsAtom, SetAction } = createBooleanMapAtom<number>('TEST', true);

    store.subscribe(Atom, jest.fn());

    store.dispatch(SetAction({ 123: true, 234: false }));

    expect(store.getState(Atom)).toEqual({ 123: true, 234: false });
    expect(store.getState(TrulyIdsAtom)).toEqual([123]);
  });
  it('setSome works', () => {
    const store = createStore();

    const { Atom, TrulyIdsAtom, SetAction, SetSomeAction } = createBooleanMapAtom<number>('TEST', true);

    store.subscribe(Atom, jest.fn());

    store.dispatch(SetAction({ 123: true, 234: false }));
    store.dispatch(SetSomeAction({ 234: true }));

    expect(store.getState(Atom)).toEqual({ 123: true, 234: true });
    expect(store.getState(TrulyIdsAtom)).toEqual([123, 234]);
  });
});

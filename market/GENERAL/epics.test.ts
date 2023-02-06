import { Subject } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';
import { ActionsObservable, StateObservable } from 'redux-observable';
import { createStore, getTree, TreeId, Store } from '@reatom/core';
import * as R from 'ramda';

import { contentTypesAtom } from '../content-types';
import { createReferenceType, EntityType } from '../common';
import { createEntryType, createContentType, createContentTypeField } from './utils';
import { entriesInfoAtom, entriesAtom } from './atoms';
import { entriesEpics } from './epics';
import {
  createEntryAction,
  updateEntriesAction,
  changeEntryFieldValueAction,
  removeEntryAction,
  setEntriesAction,
} from './actions';

function createObservableState(store: Store) {
  const state$ = new StateObservable(new Subject<typeof store.getState>(), store.getState);

  return state$;
}

function setupSimpleState(initialState: Record<TreeId, unknown> = {}) {
  const entry1 = createEntryType('Foo');
  const entry2 = createEntryType('Foo', entry1.id);
  entry1.fields.testField = createReferenceType(entry2.id, EntityType.Entry);
  const entries = R.indexBy(R.prop('id'), [entry1, entry2]);
  const contentTypes = R.indexBy(R.prop('id'), [
    createContentType('Foo', {
      fields: [createContentTypeField('testField')],
    }),
  ]);
  const store = createStore(entriesInfoAtom, {
    [getTree(contentTypesAtom).id]: contentTypes,
    [getTree(entriesAtom).id]: entries,
    ...initialState,
  });

  const state$ = createObservableState(store);

  return { store, state$, entries, entry1, entry2 };
}

describe('models/entries/epics', () => {
  describe('createEntryEpic', () => {
    it('must call the updateEntriesAction after calling the createEntryAction', () => {
      const store = createStore();
      const state$ = createObservableState(store);
      const entry = createEntryType('Foo');
      const testScheduler = new TestScheduler((actual, expected) => {
        expect(actual).toEqual(expected);
      });

      testScheduler.run(({ hot, expectObservable }) => {
        const values = {
          a: createEntryAction(entry),
          b: updateEntriesAction(R.indexBy(R.prop('id'), [entry])),
        };
        const action$ = ActionsObservable.from(hot('--a', values));

        const output$ = entriesEpics(action$, state$, {});

        expectObservable(output$).toBe('--b', values);
      });
    });
  });

  describe('changeEntryFieldValueEpic', () => {
    it('must call the updateEntriesAction after calling the changeEntryFieldValueAction', () => {
      const { state$, entry1 } = setupSimpleState();

      const testScheduler = new TestScheduler((actual, expected) => {
        expect(actual).toEqual(expected);
      });

      testScheduler.run(({ hot, expectObservable }) => {
        const values = {
          a: changeEntryFieldValueAction({ entryId: entry1.id, fieldName: 'testField', value: 'bar' }),
          b: updateEntriesAction(R.indexBy(R.prop('id'), [R.set(R.lensPath(['fields', 'testField']), 'bar', entry1)])),
        };
        const action$ = ActionsObservable.from(hot('--a', values));

        const output$ = entriesEpics(action$, state$, {});

        expectObservable(output$).toBe('--b', values);
      });
    });
  });

  describe('removeEntryEpic', () => {
    it('should remove only one entry with setEntries action', () => {
      const { state$, entry1, entry2 } = setupSimpleState();

      const testScheduler = new TestScheduler((actual, expected) => {
        expect(actual).toEqual(expected);
      });

      testScheduler.run(({ hot, expectObservable }) => {
        const values = {
          a: removeEntryAction(entry2.id),
          b: setEntriesAction(R.indexBy(R.prop('id'), [R.over(R.lensProp('fields'), R.omit(['testField']), entry1)])),
        };
        const action$ = ActionsObservable.from(hot('--a', values));

        const output$ = entriesEpics(action$, state$, {});

        expectObservable(output$).toBe('--b', values);
      });
    });

    it('should remove children entries with setEntries action', () => {
      const { state$, entry1 } = setupSimpleState();

      const testScheduler = new TestScheduler((actual, expected) => {
        expect(actual).toEqual(expected);
      });

      testScheduler.run(({ hot, expectObservable }) => {
        const values = {
          a: removeEntryAction(entry1.id),
          b: setEntriesAction({}),
        };
        const action$ = ActionsObservable.from(hot('--a', values));

        const output$ = entriesEpics(action$, state$, {});

        expectObservable(output$).toBe('--b', values);
      });
    });
  });
});

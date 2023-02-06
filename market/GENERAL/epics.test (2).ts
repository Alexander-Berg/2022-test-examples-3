import { Subject } from 'rxjs';
import { TestScheduler } from 'rxjs/testing';
import { ActionsObservable, StateObservable } from 'redux-observable';
import { createStore, getTree, TreeId } from '@reatom/core';
import * as R from 'ramda';

import {
  setEntriesAction,
  entriesInfoAtom,
  createEntryType,
  createContentType,
  entriesAtom,
  updateEntriesAction,
} from '../entries';
import { contentTypesAtom } from '../content-types';
import { validationsEpics } from './epics';
import { validateEntriesByIdAction, setValidationErrorsAction, updateValidationErrorsAction } from './actions';

function setupSimpleState(initialState: Record<TreeId, unknown> = {}) {
  const entry1 = createEntryType('Foo');
  const entry2 = createEntryType('Foo');
  const entries = R.indexBy(R.prop('id'), [entry1, entry2]);
  const contentTypes = R.indexBy(R.prop('id'), [createContentType('Foo')]);
  const store = createStore(entriesInfoAtom, {
    [getTree(contentTypesAtom).id]: contentTypes,
    [getTree(entriesAtom).id]: entries,
    ...initialState,
  });

  const state$ = new StateObservable(new Subject<typeof store.getState>(), store.getState);

  return { store, state$, entries, entry1, entry2 };
}

describe('models/validations/epics', () => {
  describe('validateAllEpic', () => {
    it('must call the validateEntriesByIdAction after calling the setEntriesAction', () => {
      const { state$, entries, entry1, entry2 } = setupSimpleState();
      const testScheduler = new TestScheduler((actual, expected) => {
        expect(actual).toEqual(expected);
      });

      testScheduler.run(({ hot, expectObservable }) => {
        const values = {
          a: setEntriesAction(entries),
          b: validateEntriesByIdAction({ ids: [entry1.id, entry2.id] }),
        };
        const action$ = ActionsObservable.from(hot('--a', values));

        const output$ = validationsEpics(action$, state$, {});

        expectObservable(output$).toBe('--b', values);
      });
    });
  });

  describe('validateChangedEpic', () => {
    it('must call the validateEntriesByIdAction after calling the updateEntriesAction', () => {
      const { state$, entry1 } = setupSimpleState();
      const testScheduler = new TestScheduler((actual, expected) => {
        expect(actual).toEqual(expected);
      });

      testScheduler.run(({ hot, expectObservable }) => {
        const values = {
          a: updateEntriesAction(R.indexBy(R.prop('id'), [entry1])),
          b: validateEntriesByIdAction({ ids: [entry1.id] }),
        };
        const action$ = ActionsObservable.from(hot('--a', values));

        const output$ = validationsEpics(action$, state$, {});

        expectObservable(output$).toBe('--b', values);
      });
    });
  });

  describe('validateEntriesByIdEpic', () => {
    it('must call the debounced setValidationErrorsAction after calling the validateEntriesByIdAction', () => {
      const { state$, entry1 } = setupSimpleState();
      const testScheduler = new TestScheduler((actual, expected) => {
        expect(actual).toEqual(expected);
      });

      testScheduler.run(({ hot, expectObservable }) => {
        const values = {
          a: validateEntriesByIdAction({ ids: [entry1.id], replace: true }),
          b: setValidationErrorsAction({ [entry1.id]: {} }),
        };
        const action$ = ActionsObservable.from(hot('--a', values));

        const output$ = validationsEpics(action$, state$, {});

        expectObservable(output$).toBe('-- 200ms b', values);
      });
    });

    it('must call the debounced updateValidationErrorsAction after calling the validateEntriesByIdAction', () => {
      const { state$, entry1 } = setupSimpleState();
      const testScheduler = new TestScheduler((actual, expected) => {
        expect(actual).toEqual(expected);
      });

      testScheduler.run(({ hot, expectObservable }) => {
        const values = {
          a: validateEntriesByIdAction({ ids: [entry1.id] }),
          b: updateValidationErrorsAction({ [entry1.id]: {} }),
        };
        const action$ = ActionsObservable.from(hot('--a', values));

        const output$ = validationsEpics(action$, state$, {});

        expectObservable(output$).toBe('-- 200ms b', values);
      });
    });
  });
});

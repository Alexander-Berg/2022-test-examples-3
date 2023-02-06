import { combine, createStore, getTree } from '@reatom/core';
import * as R from 'ramda';

import { DeviceType } from '../common';
import { contentTypesAtom, setContentTypesAction } from '../content-types';
import { entriesAtom, entriesInfoAtom, hiddenByParentExportsAtom, rootEntryIdAtom } from './atoms';
import { createContentType, createEntryType } from './utils';
import {
  setEntriesAction,
  setHiddenExportDevicesAction,
  setRootEntryIdAction,
  unsetEntriesAction,
  updateEntriesAction,
} from './actions';

describe('models/entries/atoms', () => {
  describe('EntriesAtom', () => {
    it('the initial state must be an empty object', () => {
      const store = createStore(entriesAtom);

      const initialState = store.getState(entriesAtom);
      expect(initialState).toEqual({});
    });

    it('should be full updated after the setEntriesAction is called', () => {
      const store = createStore(entriesAtom);
      const entry = createEntryType('Foo');
      const entries = R.indexBy(R.prop('id'), [entry]);
      store.dispatch(setEntriesAction(entries));

      expect(store.getState(entriesAtom)).toBe(entries);
    });

    it('should be partial updated after the updateEntriesAction is called', () => {
      const entry1 = createEntryType('Foo');
      const entry2 = createEntryType('Foo');
      const entry3 = createEntryType('Foo');
      const store = createStore(entriesAtom, {
        [getTree(entriesAtom).id]: R.indexBy(R.prop('id'), [entry1, entry2]),
      });

      const newEntry2 = { ...entry2 };
      store.dispatch(updateEntriesAction(R.indexBy(R.prop('id'), [newEntry2, entry3])));
      const state = store.getState(entriesAtom);

      expect(state).toHaveProperty(entry1.id, entry1);
      expect(state).toHaveProperty(entry2.id, newEntry2);
      expect(state).toHaveProperty(entry3.id, entry3);
    });

    it('should be remove entries after the unsetEntriesAction is called', () => {
      const entry1 = createEntryType('Foo');
      const entry2 = createEntryType('Foo');
      const entry3 = createEntryType('Foo');
      const store = createStore(entriesAtom, {
        [getTree(entriesAtom).id]: R.indexBy(R.prop('id'), [entry1, entry2, entry3]),
      });

      store.dispatch(unsetEntriesAction([entry1.id, entry2.id]));
      const state = store.getState(entriesAtom);

      expect(state).not.toHaveProperty(entry1.id);
      expect(state).not.toHaveProperty(entry2.id);
      expect(state).toHaveProperty(entry3.id, entry3);
    });

    it('should be updated hiddenExports after the setHiddenExportDevicesAction is called', () => {
      const entry1 = createEntryType('Foo');
      const store = createStore(entriesAtom, {
        [getTree(entriesAtom).id]: R.indexBy(R.prop('id'), [entry1]),
      });

      const devices = [DeviceType.DESKTOP];
      store.dispatch(setHiddenExportDevicesAction({ entryId: entry1.id, devices }));
      const state = store.getState(entriesAtom);

      expect(state).toHaveProperty([entry1.id, 'sys', 'hiddenExports'], devices);
    });
  });

  describe('RootEntryIdAtom', () => {
    it('the initial state must be a null', () => {
      const store = createStore(rootEntryIdAtom);

      const initialState = store.getState(rootEntryIdAtom);
      expect(initialState).toBe(null);
    });

    it('should be set entry id after the setRootEntryIdAction is called', () => {
      const store = createStore(rootEntryIdAtom);
      store.dispatch(setRootEntryIdAction('1'));

      expect(store.getState(rootEntryIdAtom)).toBe('1');
    });
  });

  describe('EntriesInfoAtom', () => {
    it('the initial state must be an empty object', () => {
      const store = createStore(entriesInfoAtom);

      const initialState = store.getState(entriesInfoAtom);
      expect(initialState).toEqual({});
    });

    it('should be a throw error if contentType does not exist after setEntriesAction is called', () => {
      const store = createStore(entriesInfoAtom);
      const entries = R.indexBy(R.prop('id'), [createEntryType('Foo')]);

      expect(() => {
        store.dispatch(setEntriesAction(entries));
      }).toThrow();
    });

    it('should be updated if contentType exist after setEntriesAction is called', () => {
      const store = createStore(entriesInfoAtom, {
        [getTree(contentTypesAtom).id]: R.indexBy(R.prop('id'), [createContentType('Foo')]),
      });
      const entry = createEntryType('Foo');
      const entries = R.indexBy(R.prop('id'), [entry]);

      expect(() => {
        store.dispatch(setEntriesAction(entries));
      }).not.toThrow();
      expect(store.getState(entriesInfoAtom)).toHaveProperty([entry.id, 'entry'], entry);
    });

    it('should be updated after setContentTypesAction is called', () => {
      const store = createStore(entriesInfoAtom);

      const initialState = store.getState(entriesInfoAtom);
      store.dispatch(setContentTypesAction(R.indexBy(R.prop('id'), [createContentType('Foo')])));

      expect(store.getState(entriesInfoAtom)).not.toBe(initialState);
    });
  });

  describe('hiddenByParentExportsAtom', () => {
    it('fill child hidings based on parent', () => {
      const store = createStore(combine([entriesAtom, hiddenByParentExportsAtom]));
      const entry = createEntryType('Root');
      entry.sys.hiddenExports = [DeviceType.DESKTOP, DeviceType.PHONE];
      const childEntry = createEntryType('Child');
      childEntry.parentId = entry.id;

      const entries = R.indexBy(R.prop('id'), [entry, childEntry]);
      store.dispatch(setEntriesAction(entries));

      expect(store.getState(hiddenByParentExportsAtom)).toEqual({
        [entry.id]: [],
        [childEntry.id]: [DeviceType.DESKTOP, DeviceType.PHONE],
      });
    });

    it('process 4-levels tree', () => {
      const store = createStore(combine([entriesAtom, hiddenByParentExportsAtom]));
      const rootEntries = [1, 2].map(i => createEntryType(`Root${i}`));
      const firstLevelEntries = [1, 2, 3].map(i => createEntryType(`FirstLevelEntry${i}`));
      const secondLevelEntries = [1, 2].map(i => createEntryType(`SecondLevelEntry${i}`));

      rootEntries[1].sys.hiddenExports = [DeviceType.DESKTOP, DeviceType.PHONE];

      firstLevelEntries[0].parentId = rootEntries[0].id;
      firstLevelEntries[0].sys.hiddenExports = [DeviceType.DESKTOP];

      firstLevelEntries[2].parentId = rootEntries[1].id;
      firstLevelEntries[2].sys.hiddenExports = [DeviceType.DESKTOP];

      secondLevelEntries[1].parentId = firstLevelEntries[0].id;
      secondLevelEntries[1].sys.hiddenExports = [DeviceType.PHONE];

      secondLevelEntries[0].parentId = secondLevelEntries[1].id;

      const entries = R.indexBy(R.prop('id'), [...rootEntries, ...firstLevelEntries, ...secondLevelEntries]);
      store.dispatch(setEntriesAction(entries));

      expect(store.getState(hiddenByParentExportsAtom)).toEqual({
        [rootEntries[0].id]: [],
        [rootEntries[1].id]: [],
        [firstLevelEntries[0].id]: [],
        [firstLevelEntries[1].id]: [],
        [firstLevelEntries[2].id]: [DeviceType.DESKTOP, DeviceType.PHONE],
        [secondLevelEntries[0].id]: [DeviceType.PHONE, DeviceType.DESKTOP],
        [secondLevelEntries[1].id]: [DeviceType.DESKTOP],
      });
    });
  });
});

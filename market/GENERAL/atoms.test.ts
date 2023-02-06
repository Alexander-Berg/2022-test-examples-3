import { createStore } from '@reatom/core';
import * as R from 'ramda';

import { createContentType } from '../entries';
import { contentTypesAtom } from './atoms';
import { setContentTypesAction } from './actions';

describe('models/content-types/atoms', () => {
  describe('ContentTypesAtom', () => {
    it('the initial state must be an empty object', () => {
      const store = createStore(contentTypesAtom);

      const initialState = store.getState(contentTypesAtom);
      expect(initialState).toEqual({});
    });

    it('should be updated after the setContentTypesAction is called', () => {
      const store = createStore(contentTypesAtom);

      const initialState = store.getState(contentTypesAtom);
      expect(initialState).toEqual({});

      const contentType = createContentType('Foo');
      const contentTypes = R.indexBy(R.prop('id'), [contentType]);
      store.dispatch(setContentTypesAction(contentTypes));

      expect(store.getState(contentTypesAtom)).toBe(contentTypes);
    });
  });
});

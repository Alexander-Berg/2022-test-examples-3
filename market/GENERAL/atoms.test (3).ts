import { createStore, getTree } from '@reatom/core';

import { validationErrorsAtom } from './atoms';
import { setValidationErrorsAction, updateValidationErrorsAction } from './actions';
import { ValidationError } from './types';
import { ValidationRuleType } from './utils';

describe('models/validations/atoms', () => {
  describe('ValidationErrorsAtom', () => {
    it('the initial state must be an empty object', () => {
      const store = createStore(validationErrorsAtom);

      const initialState = store.getState(validationErrorsAtom);
      expect(initialState).toEqual({});
    });

    it('should be full updated after the setValidationErrorsAction is called', () => {
      const store = createStore(validationErrorsAtom);
      const validationErrors = {};
      store.dispatch(setValidationErrorsAction(validationErrors));

      expect(store.getState(validationErrorsAtom)).toBe(validationErrors);
    });

    it('should be partial updated after the updateValidationErrorsAction is called', () => {
      const entry1Errors: Record<string, ValidationError[]> = {
        field1: [{ type: ValidationRuleType.Required, message: 'required', details: {} }],
      };
      const entry2Errors: Record<string, ValidationError[]> = {
        field2: [{ type: ValidationRuleType.Required, message: 'required', details: {} }],
      };
      const entry3Errors: Record<string, ValidationError[]> = {
        field3: [{ type: ValidationRuleType.Required, message: 'required', details: {} }],
      };
      const store = createStore(validationErrorsAtom, {
        [getTree(validationErrorsAtom).id]: {
          '1': entry1Errors,
          '2': entry2Errors,
        },
      });

      const newEntry2Errors = { ...entry2Errors };
      store.dispatch(
        updateValidationErrorsAction({
          '2': newEntry2Errors,
          '3': entry3Errors,
        })
      );

      const state = store.getState(validationErrorsAtom);

      expect(state).toHaveProperty('1', entry1Errors);
      expect(state).toHaveProperty('2', newEntry2Errors);
      expect(state).toHaveProperty('3', entry3Errors);
    });
  });
});

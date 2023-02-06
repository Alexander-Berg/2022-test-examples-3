import {ActionTypes} from '../eventDraftConstants';
import {createDraft, updateDraft, resetDraft} from '../eventDraftActions';

describe('eventDraftActions', () => {
  describe('createDraft', () => {
    test('должен вернуть экшен CREATE_DRAFT', () => {
      const data = {
        name: 'draft'
      };
      expect(createDraft(data)).toEqual({
        type: ActionTypes.CREATE_DRAFT,
        data
      });
    });
  });

  describe('updateDraft', () => {
    test('должен вернуть экшен UPDATE_DRAFT', () => {
      const field = 'name';
      const value = 'new_name';
      expect(updateDraft(field, value)).toEqual({
        type: ActionTypes.UPDATE_DRAFT,
        field,
        value
      });
    });
  });

  describe('resetDraft', () => {
    test('должен вернуть экшен RESET_DRAFT', () => {
      expect(resetDraft()).toEqual({
        type: ActionTypes.RESET_DRAFT
      });
    });
  });
});

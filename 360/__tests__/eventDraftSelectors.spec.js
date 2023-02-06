import {isDraftWithZoomConference, getDraftCanEdit} from '../eventDraftSelectors';

describe('eventDraftSelectors', () => {
  describe('isDraftWithZoomConference', () => {
    test('должен возвращать false, если нет conferenceUrl', () => {
      const state = {
        eventDraft: {}
      };

      expect(isDraftWithZoomConference(state)).toBeFalsy();
    });

    test('должен возвращать true, если  есть conferenceUrl', () => {
      const state = {
        eventDraft: {conferenceUrl: 'zoom'}
      };

      expect(isDraftWithZoomConference(state)).toBeTruthy();
    });
  });

  describe('getDraftCanEdit', () => {
    test('должен возвращать actions.edit', () => {
      const state = {
        eventDraft: {actions: {edit: true}}
      };

      expect(getDraftCanEdit(state)).toBeTruthy();
    });
  });
});

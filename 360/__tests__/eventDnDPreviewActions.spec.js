import {ActionTypes} from '../eventDnDPreviewConstants';
import {updatePreview, resetPreview} from '../eventDnDPreviewActions';

describe('eventDnDPreviewActions', () => {
  describe('updatePreview', () => {
    test('должен вернуть экшен UPDATE_PREVIEW', () => {
      const data = {name: 'preview'};
      expect(updatePreview(data)).toEqual({
        type: ActionTypes.UPDATE_PREVIEW,
        data
      });
    });
  });

  describe('resetPreview', () => {
    test('должен вернуть экшен RESET_PREVIEW', () => {
      expect(resetPreview()).toEqual({
        type: ActionTypes.RESET_PREVIEW
      });
    });
  });
});

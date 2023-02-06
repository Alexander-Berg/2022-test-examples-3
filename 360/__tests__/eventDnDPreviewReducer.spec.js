import EventRecord from 'features/events/EventRecord';

import eventDnDPreviewReducer from '../eventDnDPreviewReducer';
import {ActionTypes} from '../eventDnDPreviewConstants';

describe('eventDnDPreviewReducer', () => {
  test('должен обрабатывать начальное состояние', () => {
    expect(eventDnDPreviewReducer(undefined, {})).toBe(null);
  });

  describe('UPDATE_PREVIEW', () => {
    test('должен обновить предпросмотр', () => {
      const data = {
        id: 1,
        start: 1508432437180,
        end: 1508432437180,
        instanceStartTs: 1508432564543
      };
      const action = {
        type: ActionTypes.UPDATE_PREVIEW,
        data
      };
      const state = null;
      const expectedState = new EventRecord({
        ...data,
        id: 'preview',
        isPreview: true
      });

      expect(eventDnDPreviewReducer(state, action)).toEqual(expectedState);
    });

    test('должен обновить предпросмотр, когда передано исходное событие', () => {
      const data = {
        originalEvent: new EventRecord({id: 1}),
        start: 1508432437180,
        end: 1508432437180,
        instanceStartTs: 1508432564543
      };
      const action = {
        type: ActionTypes.UPDATE_PREVIEW,
        data
      };
      const state = null;

      const {originalEvent, ...eventData} = data;
      const expectedState = new EventRecord({
        ...originalEvent.toJS(),
        ...eventData,
        id: 'preview',
        isPreview: true,
        previewOriginalUuid: originalEvent.uuid,
        previewOriginalType: originalEvent.type
      });

      expect(eventDnDPreviewReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('RESET_PREVIEW', () => {
    test('должен удалить превью', () => {
      const action = {
        type: ActionTypes.RESET_PREVIEW
      };
      const state = new EventRecord({
        id: 'preview',
        isPreview: true
      });
      const expectedState = null;

      expect(eventDnDPreviewReducer(state, action)).toEqual(expectedState);
    });
  });
});

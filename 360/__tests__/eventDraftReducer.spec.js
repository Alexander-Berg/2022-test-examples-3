import Decision from 'constants/Decision';
import EventRecord from 'features/events/EventRecord';
import {ActionTypes as EventsActionTypes} from 'features/events/eventsConstants';

import eventDraftReducer from '../eventDraftReducer';
import {ActionTypes} from '../eventDraftConstants';

describe('eventDraftReducer', () => {
  test('должен обрабатывать начальное состояние', () => {
    expect(eventDraftReducer(undefined, {})).toBe(null);
  });

  describe('CREATE_DRAFT', () => {
    test('должен создать черновик', () => {
      const data = {
        start: 1508432437180,
        end: 1508432437180,
        instanceStartTs: 1508432564543
      };
      const action = {
        type: ActionTypes.CREATE_DRAFT,
        data
      };
      const state = null;
      const expectedState = new EventRecord({
        id: 'draft',
        ...data,
        isDraft: true
      });

      expect(eventDraftReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('UPDATE_DRAFT', () => {
    test('должен обновить черновик', () => {
      const data = {
        start: 1508432437180,
        end: 1508432437180,
        instanceStartTs: 1508432564543
      };
      const action = {
        type: ActionTypes.UPDATE_DRAFT,
        field: 'name',
        value: 'new event'
      };
      const state = new EventRecord({
        name: 'some name',
        id: 'draft',
        ...data,
        isDraft: true
      });
      const expectedState = new EventRecord({
        name: 'new event',
        id: 'draft',
        ...data,
        isDraft: true
      });

      expect(eventDraftReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('RESET_DRAFT', () => {
    test('должен удалить черновик', () => {
      const action = {
        type: ActionTypes.RESET_DRAFT
      };
      const state = new EventRecord({
        id: 'draft',
        isDraft: true
      });
      const expectedState = null;

      expect(eventDraftReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('UPDATE_DECISION_SUCCESS', () => {
    test('должен вернуть текущее состояние, если черновика нет', () => {
      const action = {
        type: EventsActionTypes.UPDATE_DECISION_SUCCESS,
        decision: Decision.YES
      };
      const state = null;
      const expectedState = null;

      expect(eventDraftReducer(state, action)).toEqual(expectedState);
    });

    test('должен вернуть текущее состояние, если черновик - новое событие', () => {
      const data = {
        start: 1508432437180,
        end: 1508432437180,
        instanceStartTs: 1508432564543
      };
      const action = {
        type: EventsActionTypes.UPDATE_DECISION_SUCCESS,
        decision: Decision.YES
      };
      const state = new EventRecord({
        id: 'draft',
        ...data,
        isDraft: true
      });
      const expectedState = new EventRecord({
        id: 'draft',
        ...data,
        isDraft: true
      });

      expect(eventDraftReducer(state, action)).toEqual(expectedState);
    });

    test('должен обновить решение у черновика', () => {
      const data = {
        id: 100500,
        start: 1508432437180,
        end: 1508432437180,
        instanceStartTs: 1508432564543
      };
      const action = {
        type: EventsActionTypes.UPDATE_DECISION_SUCCESS,
        decision: Decision.YES,
        myEmail: 'test@ya.ru'
      };
      const state = new EventRecord({
        ...data,
        decision: Decision.MAYBE,
        attendees: {
          'test@ya.ru': {
            email: 'test@ya.ru',
            decision: Decision.MAYBE
          }
        },
        isDraft: true
      });
      const expectedState = new EventRecord({
        ...data,
        decision: Decision.YES,
        attendees: {
          'test@ya.ru': {
            email: 'test@ya.ru',
            decision: Decision.YES
          }
        },
        isDraft: true
      });

      expect(eventDraftReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('CONFIRM_REPETITION_SUCCESS', () => {
    test('должен вернуть текущее состояние, если черновика нет', () => {
      const action = {
        type: EventsActionTypes.CONFIRM_REPETITION_SUCCESS
      };
      const state = null;
      const expectedState = null;

      expect(eventDraftReducer(state, action)).toEqual(expectedState);
    });

    test('должен вернуть текущее состояние, если черновик - новое событие', () => {
      const date = Date.now();
      const data = {
        id: 'draft',
        start: date,
        end: date,
        instanceStartTs: date
      };
      const action = {
        type: EventsActionTypes.CONFIRM_REPETITION_SUCCESS
      };
      const state = new EventRecord(data);
      const expectedState = new EventRecord(data);

      expect(eventDraftReducer(state, action)).toEqual(expectedState);
    });

    test('должен подтверждать актуальность события', () => {
      const date = Date.now();
      const data = {
        repetitionNeedsConfirmation: true,
        start: date,
        end: date,
        instanceStartTs: date
      };
      const action = {
        type: EventsActionTypes.CONFIRM_REPETITION_SUCCESS
      };
      const state = new EventRecord(data);
      const expectedState = new EventRecord({
        ...data,
        repetitionNeedsConfirmation: false
      });

      expect(eventDraftReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('GET_EVENT_SUCCESS', () => {
    test('должен создать черновик на основе полученных данных', () => {
      const event = {
        id: 100500,
        name: 'event name',
        description: 'event description',
        start: 1508432437180,
        end: 1508432437180,
        instanceStartTs: 1508432564543
      };
      const action = {
        type: EventsActionTypes.GET_EVENT_SUCCESS,
        event
      };
      const state = null;
      const expectedState = new EventRecord({
        ...event,
        isDraft: true
      });

      expect(eventDraftReducer(state, action)).toEqual(expectedState);
    });
  });
});

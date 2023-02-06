import cloneDeep from 'lodash/cloneDeep';
import moment from 'moment';

import {login} from 'configs/session';
import Decision from 'constants/Decision';
import {ActionTypes as LayersActionTypes} from 'features/layers/layersConstants';
import {ActionTypes as SettingsActionsTypes} from 'features/settings/settingsConstants';

import EventRecord from '../EventRecord';
import eventsReducer, {initialState, refillList} from '../eventsReducer';
import {ActionTypes} from '../eventsConstants';

function createEventWithOwnerId(data) {
  return new EventRecord({...cloneDeep(data), ownerId: login});
}

const fixtures = {
  common: {
    id: 'common',
    externalId: 'ex_common',
    layerId: 'layer1',
    startTs: '2016-10-11T10:00:00',
    endTs: '2016-10-11T10:30:00',
    instanceStartTs: '2016-10-11T10:00:00',
    attendees: {
      'myEmail@ya.ru': {
        email: 'myEmail@ya.ru',
        decision: Decision.UNDECIDED
      },
      'otherEmail@ya.ru': {
        email: 'otherEmail@ya.ru',
        decision: Decision.UNDECIDED
      }
    },
    ownerId: login
  },
  allday: {
    id: 'allday',
    externalId: 'ex_allday',
    layerId: 'layer1',
    isAllDay: true,
    startTs: '2016-10-11T00:00:00',
    endTs: '2016-10-12T00:00:00',
    instanceStartTs: '2016-10-11T00:00:00'
  },
  common_series: {
    first: {
      id: 'common_series_first',
      externalId: 'ex_common_series',
      layerId: 'layer1',
      startTs: '2016-10-11T10:00:00',
      endTs: '2016-10-11T10:30:00',
      instanceStartTs: '2016-10-11T10:00:00',
      attendees: {
        'myEmail@ya.ru': {
          email: 'myEmail@ya.ru',
          decision: Decision.UNDECIDED
        },
        'otherEmail@ya.ru': {
          email: 'otherEmail@ya.ru',
          decision: Decision.UNDECIDED
        }
      }
    },
    second: {
      id: 'common_series_second',
      externalId: 'ex_common_series',
      layerId: 'layer1',
      startTs: '2016-10-12T10:00:00',
      endTs: '2016-10-12T10:30:00',
      instanceStartTs: '2016-10-12T10:00:00',
      attendees: {
        'myEmail@ya.ru': {
          email: 'myEmail@ya.ru',
          decision: Decision.UNDECIDED
        },
        'otherEmail@ya.ru': {
          email: 'otherEmail@ya.ru',
          decision: Decision.UNDECIDED
        }
      }
    }
  },
  allday_series: {
    first: {
      id: 'allday_series_first',
      externalId: 'ex_allday_series',
      layerId: 'layer1',
      isAllDay: true,
      startTs: '2016-10-11T00:00:00',
      endTs: '2016-10-12T00:00:00',
      instanceStartTs: '2016-10-11T00:00:00'
    },
    second: {
      id: 'allday_series_second',
      externalId: 'ex_allday_series',
      layerId: 'layer1',
      isAllDay: true,
      startTs: '2016-10-12T00:00:00',
      endTs: '2016-10-13T00:00:00',
      instanceStartTs: '2016-10-12T00:00:00'
    }
  },
  repetitionNeedsConfirmation_series: {
    first: {
      id: 'repetitionNeedsConfirmation_series_first',
      externalId: 'ex_repetitionNeedsConfirmation_series',
      layerId: 'layer1',
      startTs: '2016-10-11T10:00:00',
      endTs: '2016-10-11T10:30:00',
      instanceStartTs: '2016-10-11T10:00:00',
      repetitionNeedsConfirmation: true
    },
    second: {
      id: 'repetitionNeedsConfirmation_series_second',
      externalId: 'ex_repetitionNeedsConfirmation_series',
      layerId: 'layer1',
      startTs: '2016-10-12T10:00:00',
      endTs: '2016-10-12T10:30:00',
      instanceStartTs: '2016-10-12T10:00:00',
      repetitionNeedsConfirmation: true
    }
  },
  common1fromList1: {
    id: 'common1fromList1',
    externalId: 'ex_common1fromList1',
    layerId: 'layer1',
    startTs: '2016-10-11T10:00:00',
    endTs: '2016-10-11T10:30:00',
    instanceStartTs: '2016-10-11T10:00:00'
  },
  common2fromList1: {
    id: 'common2fromList1',
    layerId: 'layer1',
    externalId: 'ex_common2fromList1',
    startTs: '2016-10-11T10:00:00',
    endTs: '2016-10-11T10:30:00',
    instanceStartTs: '2016-10-11T10:00:00'
  },
  common1fromList2: {
    id: 'common1fromList2',
    layerId: 'layer2',
    externalId: 'ex_common1fromList2',
    startTs: '2016-10-11T10:00:00',
    endTs: '2016-10-11T10:30:00',
    instanceStartTs: '2016-10-11T10:00:00'
  },
  sameIdDifferentLayer: {
    own: {
      id: 'sameIdDifferentLayer',
      layerId: 'layer_own',
      externalId: 'ex_sameIdDifferentLayer',
      startTs: '2016-10-11T10:00:00',
      endTs: '2016-10-11T10:30:00',
      instanceStartTs: '2016-10-11T10:00:00',
      attendees: {
        'myEmail@ya.ru': {
          email: 'myEmail@ya.ru',
          decision: Decision.UNDECIDED
        },
        'otherEmail@ya.ru': {
          email: 'otherEmail@ya.ru',
          decision: Decision.UNDECIDED
        }
      }
    },
    external: {
      id: 'sameIdDifferentLayer',
      layerId: 'layer_external',
      externalId: 'ex_sameIdDifferentLayer',
      startTs: '2016-10-11T10:00:00',
      endTs: '2016-10-11T10:30:00',
      instanceStartTs: '2016-10-11T10:00:00',
      attendees: {
        'myEmail@ya.ru': {
          email: 'myEmail@ya.ru',
          decision: Decision.UNDECIDED
        },
        'otherEmail@ya.ru': {
          email: 'otherEmail@ya.ru',
          decision: Decision.UNDECIDED
        }
      }
    }
  }
};

// TODO(fresk): Перейти на функцию event вместо fixtures

const event = (id, start, end, props = {}) => ({
  start,
  end,
  layerId: 1,
  externalId: id,
  instanceStartTs: moment.utc(moment(start).valueOf()).format('YYYY-MM-DDTHH:mm'),
  ...props
});

describe('eventsReducer', () => {
  test('должен обрабатывать начальное состояние', () => {
    expect(eventsReducer(undefined, {})).toEqual(initialState);
  });

  describe('GET_EVENTS_SUCCESS', () => {
    test('должен добавить события в пустой стейт', () => {
      const expectedState = refillList(initialState, null, [
        event(1, '2018-09-10T10:00', '2018-09-10T11:00'),
        event(2, '2018-09-10T00:00', '2018-09-11T00:00', {isAllDay: true})
      ]).set('lastUpdateTs', Number(moment('2018-01-01')));
      const action = {
        type: ActionTypes.GET_EVENTS_SUCCESS,
        events: [
          event(1, '2018-09-10T10:00', '2018-09-10T11:00'),
          event(2, '2018-09-10T00:00', '2018-09-11T00:00', {isAllDay: true})
        ],
        lastUpdateTs: Number(moment('2018-01-01'))
      };

      expect(eventsReducer(initialState, action)).toEqual(expectedState);
    });

    test('должен добавить события в непустой стейт, удалив неактуальные события для запрошенного периода', () => {
      const state = refillList(
        initialState,
        null,
        [
          event(1, '2018-09-09T10:00', '2018-09-09T11:00'), // A. вне периода (раньше)
          event(2, '2018-09-10T10:00', '2018-09-10T11:00'), // B. внутри периода (старое)
          event(3, '2018-09-10T00:00', '2018-09-11T00:00', {isAllDay: true}), // C. внутри периода (старое)
          event(4, '2018-09-17T01:00', '2018-09-17T02:00') // D. вне периода (позже)
        ],
        'rideorgtfo'
      );
      const expectedState = refillList(
        initialState,
        null,
        [
          event(1, '2018-09-09T10:00', '2018-09-09T11:00'), // A
          event(5, '2018-09-11T10:00', '2018-09-11T11:00'), // E. внутри периода (новое)
          event(4, '2018-09-17T01:00', '2018-09-17T02:00') // D
        ],
        'rideorgtfo'
      ).set('lastUpdateTs', Number(moment('2018-01-01')));
      const action = {
        type: ActionTypes.GET_EVENTS_SUCCESS,
        events: [event(5, '2018-09-11T10:00', '2018-09-11T11:00')], // E
        lastUpdateTs: Number(moment('2018-01-01')),
        from: '2018-09-10',
        to: '2018-09-16',
        ownerId: 'rideorgtfo'
      };

      expect(eventsReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('GET_EVENTS_FOR_LAYER_SUCCESS', () => {
    test('должен добавить события', () => {
      const state = refillList(initialState, null, cloneDeep(fixtures.common));
      const expectedState = refillList(initialState, null, [
        cloneDeep(fixtures.common),
        cloneDeep(fixtures.allday)
      ]);
      const action = {
        type: ActionTypes.GET_EVENTS_FOR_LAYER_SUCCESS,
        events: [fixtures.allday]
      };

      expect(eventsReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('CREATE_EVENT_SUCCESS', () => {
    test('должен добавить обычное событие', () => {
      const state = refillList(initialState, null, fixtures.common);
      const expectedState = refillList(initialState, null, [fixtures.common, fixtures.allday]);
      const action = {
        type: ActionTypes.CREATE_EVENT_SUCCESS,
        events: [fixtures.allday]
      };

      expect(eventsReducer(state, action)).toEqual(expectedState);
    });

    test('должен добавить серию событий', () => {
      const state = refillList(initialState, null, fixtures.common);
      const expectedState = refillList(initialState, null, [
        fixtures.common,
        fixtures.common_series.first,
        fixtures.common_series.second
      ]);
      const action = {
        type: ActionTypes.CREATE_EVENT_SUCCESS,
        events: [fixtures.common_series.first, fixtures.common_series.second]
      };

      expect(eventsReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('DELETE_EVENT_SUCCESS', () => {
    describe('единичное событие ->', () => {
      test('должен удалить короткое событие', () => {
        const state = refillList(initialState, null, [fixtures.common, fixtures.allday]);
        const expectedState = refillList(initialState, null, fixtures.allday);
        const action = {
          type: ActionTypes.DELETE_EVENT_SUCCESS,
          event: new EventRecord(fixtures.common)
        };

        expect(eventsReducer(state, action)).toEqual(expectedState);
      });

      test('должен удалить событие на весь день', () => {
        const state = refillList(initialState, null, [fixtures.common, fixtures.allday]);
        const expectedState = refillList(initialState, null, fixtures.common);
        const action = {
          type: ActionTypes.DELETE_EVENT_SUCCESS,
          event: new EventRecord(fixtures.allday)
        };

        expect(eventsReducer(state, action)).toEqual(expectedState);
      });
    });

    describe('серия событий ->', () => {
      test('должен удалить одно короткое событие из серии', () => {
        const state = refillList(initialState, null, [
          fixtures.common_series.first,
          fixtures.common_series.second,
          fixtures.allday_series.first,
          fixtures.allday_series.second
        ]);
        const expectedState = refillList(initialState, null, [
          fixtures.common_series.second,
          fixtures.allday_series.first,
          fixtures.allday_series.second
        ]);
        const action = {
          type: ActionTypes.DELETE_EVENT_SUCCESS,
          event: new EventRecord(fixtures.common_series.first),
          newEvents: [fixtures.common_series.second]
        };

        expect(eventsReducer(state, action)).toEqual(expectedState);
      });

      test('должен удалить всю серию событий на весь день', () => {
        const state = refillList(initialState, null, [
          fixtures.common_series.first,
          fixtures.common_series.second,
          fixtures.allday_series.first,
          fixtures.allday_series.second
        ]);
        const expectedState = refillList(initialState, null, [
          fixtures.common_series.first,
          fixtures.common_series.second
        ]);
        const action = {
          type: ActionTypes.DELETE_EVENT_SUCCESS,
          event: new EventRecord(fixtures.allday_series.first)
        };
        expect(eventsReducer(state, action)).toEqual(expectedState);
      });
    });
  });

  describe('UPDATE_EVENT_SUCCESS', () => {
    test('должен обновить одно событие', () => {
      const newEvent = Object.assign({}, fixtures.common, {name: 'test'});

      const state = refillList(initialState, null, fixtures.common);
      const expectedState = refillList(initialState, null, newEvent);
      const action = {
        type: ActionTypes.UPDATE_EVENT_SUCCESS,
        oldEvent: new EventRecord(fixtures.common),
        newEvents: [newEvent]
      };

      expect(eventsReducer(state, action)).toEqual(expectedState);
    });

    test('должен обновить одно событие из серии', () => {
      const newEvent = Object.assign({}, fixtures.common_series.first, {name: 'test'});

      const state = refillList(initialState, null, [
        fixtures.common_series.first,
        fixtures.common_series.second
      ]);
      const expectedState = refillList(initialState, null, [
        newEvent,
        fixtures.common_series.second
      ]);
      const action = {
        type: ActionTypes.UPDATE_EVENT_SUCCESS,
        oldEvent: new EventRecord(fixtures.common_series.first),
        newEvents: [newEvent, fixtures.common_series.second]
      };

      expect(eventsReducer(state, action)).toEqual(expectedState);
    });

    test('должен обновить всю серию', () => {
      const newEvents = [fixtures.common_series.first, fixtures.common_series.second].map(event => {
        return Object.assign({}, event, {name: 'test'});
      });

      const state = refillList(initialState, null, [
        fixtures.common_series.first,
        fixtures.common_series.second
      ]);
      const expectedState = refillList(initialState, null, newEvents);
      const action = {
        type: ActionTypes.UPDATE_EVENT_SUCCESS,
        oldEvent: new EventRecord(fixtures.common_series.first),
        newEvents
      };

      expect(eventsReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('REPLACE_EVENT', () => {
    test('должен заменить одно событие на другое', () => {
      const newEvent = Object.assign({}, fixtures.common, {isInProgress: true});

      const state = refillList(initialState, null, fixtures.common);
      const expectedState = refillList(initialState, null, newEvent);
      const action = {
        type: ActionTypes.REPLACE_EVENT,
        oldEvent: fixtures.common,
        newEvent
      };

      expect(eventsReducer(state, action)).toEqual(expectedState);
    });

    test('должен заменить одно событие на другое, не трогая остальную серию событий', () => {
      const newEvent = Object.assign({}, fixtures.common_series.first, {isInProgress: true});

      const state = refillList(initialState, null, [
        fixtures.common_series.first,
        fixtures.common_series.second
      ]);
      const expectedState = refillList(initialState, null, [
        newEvent,
        fixtures.common_series.second
      ]);
      const action = {
        type: ActionTypes.REPLACE_EVENT,
        oldEvent: fixtures.common_series.first,
        newEvent
      };

      expect(eventsReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('UPDATE_EVENT_HOVERED', () => {
    test('должен включать флаг hovered', () => {
      const state = refillList(initialState, null, cloneDeep(fixtures.common));
      const expectedState = refillList(initialState, null, {
        ...cloneDeep(fixtures.common),
        hovered: true
      });
      const action = {
        type: ActionTypes.UPDATE_EVENT_HOVERED,
        hovered: true,
        event: createEventWithOwnerId(fixtures.common)
      };

      expect(eventsReducer(state, action)).toEqual(expectedState);
    });

    test('должен выключать флаг hovered', () => {
      const state = refillList(initialState, null, {
        ...fixtures.common,
        hovered: true
      });
      const expectedState = refillList(initialState, null, fixtures.common);
      const action = {
        type: ActionTypes.UPDATE_EVENT_HOVERED,
        hovered: false,
        event: createEventWithOwnerId(fixtures.common)
      };

      expect(eventsReducer(state, action)).toEqual(expectedState);
    });

    test('не должен ничего делать, если события нет в сторе', () => {
      const action = {
        type: ActionTypes.UPDATE_EVENT_HOVERED,
        hovered: false,
        event: createEventWithOwnerId(fixtures.common)
      };

      expect(eventsReducer(initialState, action)).toEqual(initialState);
    });
  });

  describe('UPDATE_EVENT_ACTIVATED', () => {
    test('должен включать флаг activated', () => {
      const state = refillList(initialState, null, {...fixtures.common});
      const expectedState = refillList(initialState, null, {
        ...fixtures.common,
        activated: true
      });
      const action = {
        type: ActionTypes.UPDATE_EVENT_ACTIVATED,
        activated: true,
        event: createEventWithOwnerId(fixtures.common)
      };

      expect(eventsReducer(state, action)).toEqual(expectedState);
    });

    test('должен выключать флаг activated', () => {
      const state = refillList(initialState, null, {
        ...fixtures.common,
        activated: true
      });
      const expectedState = refillList(initialState, null, {...fixtures.common});
      const action = {
        type: ActionTypes.UPDATE_EVENT_ACTIVATED,
        activated: false,
        event: createEventWithOwnerId(fixtures.common)
      };

      expect(eventsReducer(state, action)).toEqual(expectedState);
    });

    test('не должен ничего делать, если события нет в сторе', () => {
      const action = {
        type: ActionTypes.UPDATE_EVENT_ACTIVATED,
        activated: false,
        event: createEventWithOwnerId(fixtures.common)
      };

      expect(eventsReducer(initialState, action)).toEqual(initialState);
    });
  });

  describe('UPDATE_DECISION_SUCCESS', () => {
    test('должен принять решение для единичного события', () => {
      const newEvent = cloneDeep(fixtures.common);
      newEvent.decision = Decision.YES;
      newEvent.attendees['myEmail@ya.ru'].decision = Decision.YES;

      const state = refillList(initialState, null, fixtures.common);
      const expectedState = refillList(initialState, null, newEvent);
      const action = {
        type: ActionTypes.UPDATE_DECISION_SUCCESS,
        event: new EventRecord(fixtures.common),
        decision: Decision.YES,
        myEmail: fixtures.common.attendees['myEmail@ya.ru'].email,
        applyToAll: true
      };

      expect(eventsReducer(state, action)).toEqual(expectedState);
    });

    test('должен принять решение для всей серии событий', () => {
      const newEvents = [fixtures.common_series.first, fixtures.common_series.second].map(event => {
        const newEvent = cloneDeep(event);
        newEvent.decision = Decision.MAYBE;
        newEvent.attendees['myEmail@ya.ru'].decision = Decision.MAYBE;

        return newEvent;
      });
      const state = refillList(initialState, null, [
        fixtures.common_series.first,
        fixtures.common_series.second
      ]);
      const expectedState = refillList(initialState, null, newEvents);
      const action = {
        type: ActionTypes.UPDATE_DECISION_SUCCESS,
        event: new EventRecord(fixtures.common_series.first),
        decision: Decision.MAYBE,
        myEmail: fixtures.common_series.first.attendees['myEmail@ya.ru'].email,
        applyToAll: true
      };

      expect(eventsReducer(state, action)).toEqual(expectedState);
    });

    test('должен удалять только одну свою встречу из серии, если отказались только от нее', () => {
      const state = refillList(initialState, null, [
        fixtures.common_series.first,
        fixtures.common_series.second
      ]);
      const expectedState = refillList(initialState, null, fixtures.common_series.second);
      const action = {
        type: ActionTypes.UPDATE_DECISION_SUCCESS,
        event: new EventRecord(fixtures.common_series.first),
        decision: Decision.NO,
        myEmail: fixtures.common_series.first.attendees['myEmail@ya.ru'].email,
        applyToAll: false
      };

      expect(eventsReducer(state, action)).toEqual(expectedState);
    });

    test('должен удалять только свою встречу, если от нее отказались', () => {
      const newEvent = cloneDeep(fixtures.sameIdDifferentLayer.external);
      newEvent.decision = Decision.NO;
      newEvent.attendees['myEmail@ya.ru'].decision = Decision.NO;

      const state = refillList(initialState, null, [
        fixtures.sameIdDifferentLayer.own,
        fixtures.sameIdDifferentLayer.external
      ]);
      const expectedState = refillList(initialState, null, newEvent);
      const action = {
        type: ActionTypes.UPDATE_DECISION_SUCCESS,
        event: new EventRecord(fixtures.sameIdDifferentLayer.own),
        decision: Decision.NO,
        myEmail: fixtures.sameIdDifferentLayer.own.attendees['myEmail@ya.ru'].email,
        applyToAll: true
      };

      expect(eventsReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('CONFIRM_REPETITION_SUCCESS', () => {
    test('должен сбрасывать флаг repetitionNeedsConfirmation у всей серии', () => {
      const state = refillList(initialState, null, [
        fixtures.repetitionNeedsConfirmation_series.first,
        fixtures.repetitionNeedsConfirmation_series.second
      ]);
      const expectedState = refillList(initialState, null, [
        {
          ...fixtures.repetitionNeedsConfirmation_series.first,
          repetitionNeedsConfirmation: false
        },
        {
          ...fixtures.repetitionNeedsConfirmation_series.second,
          repetitionNeedsConfirmation: false
        }
      ]);
      const action = {
        type: ActionTypes.CONFIRM_REPETITION_SUCCESS,
        event: new EventRecord(fixtures.repetitionNeedsConfirmation_series.first)
      };

      expect(eventsReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('GET_MODIFIED_EVENTS_SUCCESS', () => {
    describe('eventsByExternalId ->', () => {
      test('должен добавлять события', () => {
        const state = initialState;
        const expectedState = refillList(initialState, null, [
          fixtures.common_series.first,
          fixtures.common_series.second
        ]).set('lastUpdateTs', Number(moment('2018-01-20')));

        const action = {
          type: ActionTypes.GET_MODIFIED_EVENTS_SUCCESS,
          eventsByExternalId: {
            [fixtures.common_series.first.externalId]: [
              fixtures.common_series.first,
              fixtures.common_series.second
            ]
          },
          eventsByLayerId: {},
          lastUpdateTs: Number(moment('2018-01-20'))
        };

        expect(eventsReducer(state, action)).toEqual(expectedState);
      });

      test('должен обновлять события', () => {
        const state = refillList(initialState, null, [
          fixtures.common_series.first,
          fixtures.common_series.second
        ]);
        const expectedState = refillList(initialState, null, [
          {...fixtures.common_series.first, name: 'new name'},
          fixtures.common_series.second
        ]).set('lastUpdateTs', Number(moment('2018-01-20')));

        const action = {
          type: ActionTypes.GET_MODIFIED_EVENTS_SUCCESS,
          eventsByExternalId: {
            [fixtures.common_series.first.externalId]: [
              {...fixtures.common_series.first, name: 'new name'},
              fixtures.common_series.second
            ]
          },
          eventsByLayerId: {},
          lastUpdateTs: Number(moment('2018-01-20'))
        };

        expect(eventsReducer(state, action)).toEqual(expectedState);
      });

      test('должен обновлять серии событий', () => {
        const state = refillList(initialState, null, [
          fixtures.common_series.first,
          fixtures.common_series.second
        ]);
        const expectedState = refillList(initialState, null, [
          {...fixtures.common_series.first, name: 'new name'},
          {...fixtures.common_series.second, name: 'new name'}
        ]).set('lastUpdateTs', Number(moment('2018-01-20')));

        const action = {
          type: ActionTypes.GET_MODIFIED_EVENTS_SUCCESS,
          eventsByExternalId: {
            [fixtures.common_series.first.externalId]: [
              {...fixtures.common_series.first, name: 'new name'},
              {...fixtures.common_series.second, name: 'new name'}
            ]
          },
          eventsByLayerId: {},
          lastUpdateTs: Number(moment('2018-01-20'))
        };

        expect(eventsReducer(state, action)).toEqual(expectedState);
      });

      test('должен удалять события', () => {
        const state = refillList(initialState, null, [
          fixtures.common_series.first,
          fixtures.common_series.second
        ]);
        const expectedState = refillList(initialState, null, [fixtures.common_series.first]).set(
          'lastUpdateTs',
          Number(moment('2018-01-20'))
        );

        const action = {
          type: ActionTypes.GET_MODIFIED_EVENTS_SUCCESS,
          eventsByExternalId: {
            [fixtures.common_series.first.externalId]: [fixtures.common_series.first]
          },
          eventsByLayerId: {},
          lastUpdateTs: Number(moment('2018-01-20'))
        };

        expect(eventsReducer(state, action)).toEqual(expectedState);
      });

      test('должен удалять серии событий', () => {
        const state = refillList(initialState, null, [
          fixtures.common_series.first,
          fixtures.common_series.second
        ]);
        const expectedState = refillList(initialState, null, []).set(
          'lastUpdateTs',
          Number(moment('2018-01-20'))
        );

        const action = {
          type: ActionTypes.GET_MODIFIED_EVENTS_SUCCESS,
          eventsByExternalId: {
            [fixtures.common_series.first.externalId]: []
          },
          eventsByLayerId: {},
          lastUpdateTs: Number(moment('2018-01-20'))
        };

        expect(eventsReducer(state, action)).toEqual(expectedState);
      });
    });

    describe('eventsByLayerId ->', () => {
      test('должен полностью обновлять события слоя', () => {
        const state = refillList(initialState, null, [
          fixtures.common,
          fixtures.common_series.first,
          fixtures.common_series.second
        ]);
        const expectedState = refillList(initialState, null, [
          {...fixtures.common_series.second, endTs: '2016-10-12T20:30:00'}
        ]).set('lastUpdateTs', Number(moment('2018-01-20')));
        const action = {
          type: ActionTypes.GET_MODIFIED_EVENTS_SUCCESS,
          eventsByExternalId: {},
          eventsByLayerId: {
            [fixtures.common.layerId]: [
              {...fixtures.common_series.second, endTs: '2016-10-12T20:30:00'}
            ]
          },
          lastUpdateTs: Number(moment('2018-01-20'))
        };

        expect(eventsReducer(state, action)).toEqual(expectedState);
      });
    });

    test('должен сохранять флаги activated и hovered при обновлении', () => {
      const state = refillList(initialState, null, [
        fixtures.common_series.first,
        {...fixtures.common_series.second, activated: true, hovered: true},
        {...fixtures.common1fromList2, activated: true, hovered: true}
      ]);
      const expectedState = refillList(initialState, null, [
        {...fixtures.common_series.first, name: 'new name'},
        {...fixtures.common_series.second, name: 'new name', activated: true, hovered: true},
        {
          ...fixtures.common1fromList2,
          description: 'new description',
          activated: true,
          hovered: true
        }
      ]).set('lastUpdateTs', Number(moment('2018-01-20')));

      const action = {
        type: ActionTypes.GET_MODIFIED_EVENTS_SUCCESS,
        eventsByExternalId: {
          [fixtures.common_series.first.externalId]: [
            {...fixtures.common_series.first, name: 'new name'},
            {...fixtures.common_series.second, name: 'new name'}
          ]
        },
        eventsByLayerId: {
          [fixtures.common1fromList2.layerId]: [
            {...fixtures.common1fromList2, description: 'new description'}
          ]
        },
        lastUpdateTs: Number(moment('2018-01-20'))
      };

      expect(eventsReducer(state, action)).toEqual(expectedState);
    });
  });

  describe('Settings/UPDATE_SETTINGS_SUCCESS', () => {
    test('не должен реагировать, если не меняли начало недели', () => {
      const state = refillList(initialState, null, fixtures.common);
      const expectedState = refillList(initialState, null, fixtures.common);
      const action = {
        type: SettingsActionsTypes.UPDATE_SETTINGS_SUCCESS,
        oldSettings: {
          defaultView: 'week'
        },
        newSettings: {
          defaultView: 'month'
        }
      };

      expect(eventsReducer(state, action)).toEqual(expectedState);
    });

    test('не должен реагировать, если новое начало недели равняется предыдущему', () => {
      const state = refillList(initialState, null, fixtures.common);
      const expectedState = refillList(initialState, null, fixtures.common);
      const action = {
        type: SettingsActionsTypes.UPDATE_SETTINGS_SUCCESS,
        oldSettings: {
          weekStartDay: 1
        },
        newSettings: {
          weekStartDay: 1
        }
      };

      expect(eventsReducer(state, action)).toEqual(expectedState);
    });

    test('должен обновить weekTimestamp у всех событий', () => {
      const oldWeekStartDay = moment.localeData().firstDayOfWeek();
      const newWeekStartDay = (oldWeekStartDay + 1) % 6;

      const state = refillList(initialState, null, fixtures.common);

      moment.updateLocale(moment.locale(), {
        week: {
          dow: newWeekStartDay
        }
      });

      const expectedState = refillList(initialState, null, fixtures.common);

      const action = {
        type: SettingsActionsTypes.UPDATE_SETTINGS_SUCCESS,
        oldSettings: {
          weekStartDay: oldWeekStartDay
        },
        newSettings: {
          weekStartDay: newWeekStartDay
        }
      };

      expect(eventsReducer(state, action)).toEqual(expectedState);

      moment.updateLocale(moment.locale(), {
        week: {
          dow: oldWeekStartDay
        }
      });
    });
  });

  describe('Layers/DELETE_LAYER_SUCCESS', () => {
    test('должен переместить события из удаленного слоя в другой слой, если указали слой для перемещения', () => {
      const newEvents = [fixtures.common1fromList1, fixtures.common2fromList1].map(event => {
        return Object.assign({}, event, {
          layerId: fixtures.common1fromList2.layerId
        });
      });

      const state = refillList(initialState, null, [
        fixtures.common1fromList1,
        fixtures.common2fromList1,
        fixtures.common1fromList2
      ]);
      const expectedState = refillList(initialState, null, [
        newEvents[0],
        newEvents[1],
        fixtures.common1fromList2
      ]);
      const action = {
        type: LayersActionTypes.DELETE_LAYER_SUCCESS,
        id: fixtures.common1fromList1.layerId,
        recipientLayerId: fixtures.common1fromList2.layerId
      };

      expect(eventsReducer(state, action)).toEqual(expectedState);
    });

    test('должен удалить события из удаленного слоя, если не указали слой для перемещения', () => {
      const state = refillList(initialState, null, [
        fixtures.common1fromList1,
        fixtures.common2fromList1,
        fixtures.common1fromList2
      ]);
      const expectedState = refillList(initialState, null, fixtures.common1fromList2);
      const action = {
        type: LayersActionTypes.DELETE_LAYER_SUCCESS,
        id: fixtures.common1fromList1.layerId
      };

      expect(eventsReducer(state, action)).toEqual(expectedState);
    });
  });
});

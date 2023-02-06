import moment from 'moment';

import processRestrictions from 'features/timeline/utils/processRestrictions';
import EventFormId from 'features/eventForm/EventFormId';

import reducer, {DEFAULT_STATE} from '../yupiReducer';
import * as actions from '../yupiActions';
import processResourcesSchedule from '../utils/processResourcesSchedule';
import concatOfficesResources from '../utils/concatOfficesResources';
import getOfficesOrder from '../utils/getOfficesOrder';

jest.mock('features/timeline/utils/processRestrictions');
jest.mock('../utils/processResourcesSchedule');
jest.mock('../utils/concatOfficesResources');
jest.mock('../utils/getOfficesOrder');

describe('yupiReducer', () => {
  describe('setShowDate', () => {
    test('должен выставлять showDate', () => {
      const oldDate = new Date(2020, 0, 1).getTime();
      const newDate = new Date(2020, 0, 2).getTime();

      const state = {
        showDate: oldDate
      };
      const expectedState = {
        showDate: newDate
      };
      const action = {
        type: actions.makeSetShowDate.type,
        payload: {
          showDate: newDate
        },
        meta: {
          form: EventFormId.fromParams(EventFormId.VIEWS.POPUP, EventFormId.MODES.EDIT).toString()
        }
      };

      expect(reducer(state, action)).toEqual(expectedState);
    });
  });

  describe('switchShowAllResources', () => {
    test('должен выставлять showAllResources', () => {
      const state = {
        showAllResources: true
      };
      const expectedState = {
        showAllResources: false
      };
      const action = {
        type: actions.switchShowAllResources.type,
        payload: {
          showAllResources: false
        }
      };

      expect(reducer(state, action)).toEqual(expectedState);
    });
  });

  describe('getResourcesStart', () => {
    test('должен выставлять isLoading', () => {
      const state = {
        isLoading: false
      };
      const expectedState = {
        isLoading: true
      };
      const action = {
        type: actions.getResourcesStart.type
      };

      expect(reducer(state, action)).toEqual(expectedState);
    });
  });

  describe('getResourcesDone', () => {
    test('должен выставлять isLoading', () => {
      const state = {
        isLoading: true
      };
      const expectedState = {
        isLoading: false
      };
      const action = {
        type: actions.getResourcesDone.type
      };

      expect(reducer(state, action)).toEqual(expectedState);
    });
  });

  describe('getAllResourcesSuccess', () => {
    test('должен записывать в allResources результат processResourcesSchedule', () => {
      const state = {};
      const allResources = Symbol();
      const intervals = Symbol();
      const event = Symbol();
      const restrictions = Symbol();
      const expectedState = {allResources, restrictions};
      const action = {
        type: actions.getAllResourcesSuccess.type,
        payload: {
          intervals,
          event
        }
      };
      processResourcesSchedule.mockReturnValue(allResources);
      concatOfficesResources.mockReturnValue();
      processRestrictions.mockReturnValue(restrictions);

      expect(reducer(state, action)).toEqual(expectedState);
    });

    test('должен вызывать processResourcesSchedule с нужными параметрами', () => {
      const showDate = Symbol();
      const allResources = Symbol();
      const intervals = Symbol();
      const event = Symbol();
      const restrictions = Symbol();
      const state = {showDate};
      const action = {
        type: actions.getAllResourcesSuccess.type,
        payload: {
          intervals,
          event
        }
      };

      processResourcesSchedule.mockReset();
      processResourcesSchedule.mockReturnValue(allResources);
      concatOfficesResources.mockReturnValue();
      processRestrictions.mockReturnValue(restrictions);
      reducer(state, action);

      expect(processResourcesSchedule).toHaveBeenCalledTimes(1);
      expect(processResourcesSchedule).toHaveBeenCalledWith(intervals, event, showDate);
    });

    test('должен вызывать хелперы обработки ограничений с нужными параметрами', () => {
      const showDate = Symbol();
      const allResources = Symbol();
      const offices = Symbol();
      const intervals = {offices};
      const event = Symbol();
      const concatenatedResources = Symbol();
      const restrictions = Symbol();
      const state = {showDate, restrictions};
      const action = {
        type: actions.getAllResourcesSuccess.type,
        payload: {
          intervals,
          event
        }
      };

      processResourcesSchedule.mockReturnValue(allResources);
      processRestrictions.mockReset();
      concatOfficesResources.mockReset();
      concatOfficesResources.mockReturnValue(concatenatedResources);
      processRestrictions.mockReturnValue(restrictions);
      reducer(state, action);

      expect(concatOfficesResources).toHaveBeenCalledTimes(1);
      expect(concatOfficesResources).toHaveBeenCalledWith(intervals.offices);
      expect(processRestrictions).toHaveBeenCalledTimes(1);
      expect(processRestrictions).toHaveBeenCalledWith(concatenatedResources, showDate);
    });
  });

  describe('getRecommendedResourcesSuccess', () => {
    test('должен записывать recommendedResources', () => {
      const state = {};
      const expectedState = {
        recommendedResources: [],
        restrictions: {}
      };
      const action = {
        type: actions.getRecommendedResourcesSuccess.type,
        payload: {
          event: {},
          offices: []
        }
      };

      getOfficesOrder.mockReturnValue();

      expect(reducer(state, action)).toEqual(expectedState);
    });
  });

  describe('getOnlySelectedResourcesSuccess', () => {
    test('должен записывать в onlySelectedResources результат processResourcesSchedule', () => {
      const state = {};
      const onlySelectedResources = Symbol();
      const intervals = Symbol();
      const event = Symbol();
      const restrictions = Symbol();
      const expectedState = {onlySelectedResources, restrictions};
      const action = {
        type: actions.getOnlySelectedResourcesSuccess.type,
        payload: {
          intervals,
          event
        }
      };
      processResourcesSchedule.mockReturnValue(onlySelectedResources);
      concatOfficesResources.mockReturnValue();
      processRestrictions.mockReturnValue(restrictions);

      expect(reducer(state, action)).toEqual(expectedState);
    });

    test('должен вызывать processResourcesSchedule с нужными параметрами', () => {
      const showDate = Symbol();
      const onlySelectedResources = Symbol();
      const intervals = Symbol();
      const event = Symbol();
      const restrictions = Symbol();
      const state = {showDate};
      const action = {
        type: actions.getOnlySelectedResourcesSuccess.type,
        payload: {
          intervals,
          event
        }
      };

      processResourcesSchedule.mockReset();
      processResourcesSchedule.mockReturnValue(onlySelectedResources);
      concatOfficesResources.mockReturnValue();
      processRestrictions.mockReturnValue(restrictions);
      reducer(state, action);

      expect(processResourcesSchedule).toHaveBeenCalledTimes(1);
      expect(processResourcesSchedule).toHaveBeenCalledWith(intervals, event, showDate, {
        shallFilterNotBookable: false
      });
    });

    test('должен вызывать хелперы обработки ограничений с нужными параметрами', () => {
      const showDate = Symbol();
      const onlySelectedResources = Symbol();
      const offices = Symbol();
      const intervals = {offices};
      const event = Symbol();
      const concatenatedResources = Symbol();
      const restrictions = Symbol();
      const state = {showDate, restrictions};
      const action = {
        type: actions.getOnlySelectedResourcesSuccess.type,
        payload: {
          intervals,
          event
        }
      };

      processResourcesSchedule.mockReturnValue(onlySelectedResources);
      processRestrictions.mockReset();
      concatOfficesResources.mockReset();
      concatOfficesResources.mockReturnValue(concatenatedResources);
      processRestrictions.mockReturnValue(restrictions);
      reducer(state, action);

      expect(concatOfficesResources).toHaveBeenCalledTimes(1);
      expect(concatOfficesResources).toHaveBeenCalledWith(intervals.offices);
      expect(processRestrictions).toHaveBeenCalledTimes(1);
      expect(processRestrictions).toHaveBeenCalledWith(concatenatedResources, showDate);
    });
  });

  describe('getMembersResourcesSuccess', () => {
    test('должен записывать membersResources', () => {
      const intervals1 = 'intervals1';
      const intervals2 = 'intervals2';
      const state = {};
      const expectedState = {
        membersResources: [
          {
            canBook: true,
            email: '1@ya.ru',
            intervals: intervals1
          },
          {
            canBook: true,
            email: '2@ya.ru',
            intervals: intervals2
          }
        ]
      };
      const action = {
        type: actions.getMembersResourcesSuccess.type,
        payload: {
          members: [{email: '1@ya.ru'}, {email: '2@ya.ru'}],
          intervals: {
            '1@ya.ru': intervals1,
            '2@ya.ru': intervals2
          }
        }
      };

      expect(reducer(state, action)).toEqual(expectedState);
    });

    test('должен затирать прошлое состояние membersResources', () => {
      const intervals1 = 'intervals1';
      const intervals2 = 'intervals2';
      const state = {
        membersResources: [
          {
            email: '1@ya.ru',
            intervals: intervals1,
            canBook: true
          },
          {
            email: '2@ya.ru',
            intervals: intervals2,
            canBook: true
          }
        ]
      };
      const expectedState = {
        membersResources: [
          {
            email: '1@ya.ru',
            intervals: intervals1,
            canBook: true
          }
        ]
      };
      const action = {
        type: actions.getMembersResourcesSuccess.type,
        payload: {
          members: [{email: '1@ya.ru'}],
          intervals: {
            '1@ya.ru': intervals1
          }
        }
      };

      expect(reducer(state, action)).toEqual(expectedState);
    });
  });

  describe('dropMembersResourcesIntervals', () => {
    test('должен сбрасывать интервалы участников', () => {
      const intervals1 = 'intervals1';
      const intervals2 = 'intervals2';
      const state = {
        membersResources: [
          {
            email: '1@ya.ru',
            intervals: intervals1
          },
          {
            email: '2@ya.ru',
            intervals: intervals2
          }
        ]
      };
      const expectedState = {
        membersResources: [
          {
            email: '1@ya.ru',
            intervals: []
          },
          {
            email: '2@ya.ru',
            intervals: []
          }
        ]
      };
      const action = {
        type: actions.dropMembersResourcesIntervals.type
      };

      expect(reducer(state, action)).toEqual(expectedState);
    });
  });

  describe('dropResourcesIntervals', () => {
    test('должен сбрасывать интервалы участников', () => {
      const intervals1 = 'intervals1';
      const intervals2 = 'intervals2';
      const stateItem = [
        {
          resources: [
            {
              email: '1@ya.ru',
              intervals: intervals1
            },
            {
              email: '2@ya.ru',
              intervals: intervals2
            }
          ]
        }
      ];
      const expectedStateItem = [
        {
          resources: [
            {
              email: '1@ya.ru',
              intervals: []
            },
            {
              email: '2@ya.ru',
              intervals: []
            }
          ]
        }
      ];

      const state = {
        allResources: stateItem,
        recommendedResources: stateItem,
        onlySelectedResources: stateItem
      };
      const expectedState = {
        allResources: expectedStateItem,
        recommendedResources: expectedStateItem,
        onlySelectedResources: expectedStateItem
      };
      const action = {
        type: actions.dropResourcesIntervals.type
      };

      expect(reducer(state, action)).toEqual(expectedState);
    });
  });

  describe('resetYupiState', () => {
    test('должен возвращать всё к исходному состоянию', () => {
      const state = {};
      const expectedState = Object.assign({}, DEFAULT_STATE);
      const action = {type: actions.resetYupiState.type};

      delete expectedState.showDate;

      const result = reducer(state, action);

      delete result.showDate;

      expect(result).toEqual(expectedState);
    });
  });

  describe('calculateFreeIntervals', () => {
    test(`не должен находить свободные интервалы, если хотя бы один из участников занят весь день`, () => {
      const state = {
        freeIntervals: [],
        recommendedResources: [
          {
            title: 'Рекомендуемые в Симферополь',
            resources: []
          },
          {
            title: 'Рекомендуемые в БЦ Морозов',
            resources: []
          }
        ],
        membersResources: [
          {
            login: 'tet4enko',
            intervals: [
              {
                start: moment('2017-09-21T00:00:00').valueOf(),
                end: moment('2017-10-21T00:00:00').valueOf()
              }
            ]
          },
          {
            login: 'tavria',
            intervals: [
              {
                start: moment('2017-09-21T12:00:00').valueOf(),
                end: moment('2017-09-21T13:00:00').valueOf()
              }
            ]
          }
        ]
      };
      const action = {type: actions.calculateFreeIntervals.type};
      expect(reducer(state, action)).toEqual(state);
    });
    test(`не должен находить свободные интервалы, если хотя у участников нет ни одного общего свободного интервала`, () => {
      const state = {
        freeIntervals: [],
        recommendedResources: [
          {
            title: 'Рекомендуемые в Симферополь',
            resources: []
          },
          {
            title: 'Рекомендуемые в БЦ Морозов',
            resources: []
          }
        ],
        membersResources: [
          {
            login: 'tet4enko',
            intervals: [
              {
                start: moment('2017-09-21T00:00:00').valueOf(),
                end: moment('2017-09-21T12:00:00').valueOf()
              }
            ]
          },
          {
            login: 'tavria',
            intervals: [
              {
                start: moment('2017-09-21T12:00:00').valueOf(),
                end: moment('2017-10-21T00:00:00').valueOf()
              }
            ]
          }
        ]
      };
      const action = {type: actions.calculateFreeIntervals.type};
      expect(reducer(state, action)).toEqual(state);
    });
    test(`не должен отображать интервал свободным,
    если хотя бы в одном БЦ нет ни одной свободной переговорки в это время`, () => {
      const state = {
        freeIntervals: [],
        showDate: moment('2017-09-21T00:00:00').valueOf(),
        recommendedResources: [
          {
            title: 'Рекомендуемые в Симферополь',
            resources: [
              {
                id: 123,
                intervals: [
                  {
                    start: moment('2017-09-21T04:00:00').valueOf(),
                    end: moment('2017-09-21T11:00:00').valueOf()
                  }
                ]
              }
            ]
          },
          {
            title: 'Рекомендуемые в БЦ Морозов',
            resources: [
              {
                id: 1234,
                intervals: [
                  {
                    start: moment('2017-09-21T11:00:00').valueOf(),
                    end: moment('2017-09-21T14:00:00').valueOf()
                  }
                ]
              }
            ]
          }
        ],
        membersResources: [
          {
            login: 'tet4enko',
            intervals: [
              {
                start: moment('2017-09-21T00:00:00').valueOf(),
                end: moment('2017-09-21T12:00:00').valueOf()
              }
            ]
          },
          {
            login: 'tavria',
            intervals: [
              {
                start: moment('2017-09-21T13:00:00').valueOf(),
                end: moment('2017-10-21T13:00:00').valueOf()
              }
            ]
          }
        ]
      };
      const action = {type: actions.calculateFreeIntervals.type};
      expect(reducer(state, action)).toEqual(state);
    });
    test(`должен находить свободные интервалы`, () => {
      const state = {
        freeIntervals: [],
        showDate: moment('2017-09-21T00:00:00').valueOf(),
        recommendedResources: [
          {
            title: 'Рекомендуемые в Симферополь',
            resources: [
              {
                id: 123,
                intervals: [
                  {
                    start: moment('2017-09-21T09:00:00').valueOf(),
                    end: moment('2017-09-21T10:00:00').valueOf()
                  }
                ]
              }
            ]
          },
          {
            title: 'Рекомендуемые в БЦ Морозов',
            resources: [
              {
                id: 1234,
                intervals: [
                  {
                    start: moment('2017-09-21T11:00:00').valueOf(),
                    end: moment('2017-09-21T12:00:00').valueOf()
                  }
                ]
              }
            ]
          }
        ],
        membersResources: [
          {
            login: 'tet4enko',
            intervals: [
              {
                start: moment('2017-09-21T13:00:00').valueOf(),
                end: moment('2017-09-21T14:00:00').valueOf()
              }
            ]
          },
          {
            login: 'tavria',
            intervals: [
              {
                start: moment('2017-09-21T16:00:00').valueOf(),
                end: moment('2017-09-21T18:00:00').valueOf()
              }
            ]
          }
        ]
      };
      const action = {type: actions.calculateFreeIntervals.type};
      expect(reducer(state, action)).toEqual({
        ...state,
        freeIntervals: [
          {
            end: moment('2017-09-21T09:00:00').valueOf(),
            start: moment('2017-09-21T00:00:00').valueOf()
          },
          {
            end: moment('2017-09-21T11:00:00').valueOf(),
            start: moment('2017-09-21T10:00:00').valueOf()
          },
          {
            end: moment('2017-09-21T13:00:00').valueOf(),
            start: moment('2017-09-21T12:00:00').valueOf()
          },
          {
            end: moment('2017-09-21T16:00:00').valueOf(),
            start: moment('2017-09-21T14:00:00').valueOf()
          },
          {
            end: moment('2017-09-22T00:00:00').valueOf(),
            start: moment('2017-09-21T18:00:00').valueOf()
          }
        ]
      });
    });
  });
});

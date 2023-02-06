import processIntervals from 'features/timeline/utils/processIntervals';
import processRestrictions from 'features/timeline/utils/processRestrictions';

import getFloors from '../utils/getFloors';
import reducer from '../inviteReducer';
import * as actions from '../inviteActions';

jest.mock('features/timeline/utils/processIntervals');
jest.mock('features/timeline/utils/processRestrictions');
jest.mock('../utils/getFloors');

describe('inviteReducer', () => {
  describe('interface', () => {
    test('setShowDate', () => {
      const state = {
        filter: {},
        interface: {showDate: 567},
        resources: {}
      };
      const expectedState = {
        filter: {},
        interface: {showDate: 123},
        resources: {}
      };
      const resultingState = reducer(state, {
        type: actions.setShowDate.type,
        payload: {showDate: 123}
      });

      expect(resultingState.interface).toEqual(expectedState.interface);
    });
    test('changeOffice', () => {
      const state = {
        filter: {},
        interface: {officeId: 567, isLoading: false},
        resources: {}
      };
      const expectedState = {
        filter: {},
        interface: {officeId: 123, isLoading: true},
        resources: {}
      };
      const resultingState = reducer(state, {
        type: actions.changeOffice.type,
        payload: {officeId: 123}
      });

      expect(resultingState.interface).toEqual(expectedState.interface);
    });
    test('getResourcesSchedule', () => {
      const state = {
        filter: {},
        interface: {isLoading: false},
        resources: {}
      };
      const expectedState = {
        filter: {},
        interface: {isLoading: true},
        resources: {}
      };
      const resultingState = reducer(state, {type: actions.getResourcesSchedule.type});

      expect(resultingState.interface).toEqual(expectedState.interface);
    });
    test('getResourcesScheduleDone', () => {
      const state = {
        filter: {},
        interface: {isLoading: true},
        resources: {}
      };
      const expectedState = {
        filter: {},
        interface: {isLoading: false},
        resources: {}
      };
      const resultingState = reducer(state, {type: actions.getResourcesScheduleDone.type});

      expect(resultingState.interface).toEqual(expectedState.interface);
    });
  });

  describe('filter', () => {
    test('updateFilter', () => {
      const state = {
        filter: {video: false, conference: true, small: false},
        interface: {},
        resources: {}
      };
      const expectedState = {
        filter: {video: true, conference: true, small: false},
        interface: {},
        resources: {}
      };
      const resultingState = reducer(state, {
        type: actions.updateFilter.type,
        payload: {video: true}
      });

      expect(resultingState.filter).toEqual(expectedState.filter);
    });
  });

  describe('onSiteFilter', () => {
    test('updateOnSiteFilter', () => {
      const state = {
        onSiteFilter: {floors: []},
        interface: {},
        resources: {}
      };
      const newFloorsFilterValue = [2, 3, 4];
      const expectedState = {
        onSiteFilter: {floors: newFloorsFilterValue},
        interface: {},
        resources: {}
      };
      const resultingState = reducer(state, {
        type: actions.updateOnSiteFilter.type,
        payload: {floors: newFloorsFilterValue}
      });

      expect(resultingState.onSiteFilter).toEqual(expectedState.onSiteFilter);
    });
    test('changeOffice', () => {
      const state = {
        onSiteFilter: {floors: [2, 3, 4]},
        interface: {},
        resources: {}
      };
      const expectedState = {
        onSiteFilter: {floors: []},
        interface: {},
        resources: {}
      };
      const resultingState = reducer(state, {
        type: actions.changeOffice.type,
        payload: {officeId: 123}
      });

      expect(resultingState.onSiteFilter).toEqual(expectedState.onSiteFilter);
    });
  });

  describe('resources', () => {
    describe('getResourcesScheduleSuccess', () => {
      beforeEach(() => {
        processIntervals.mockReset();
      });

      test('должен складывать в стейт новые данные об офисах', () => {
        const res1 = {info: {email: 'x5@y.r', floor: 5}, events: []};
        const res2 = {info: {email: 'y7@y.r', floor: 7}, events: []};
        getFloors.mockImplementation(resources => resources);

        const state = {
          filter: {},
          interface: {},
          resources: {offices: {}, intervals: {}, restrictions: {}}
        };
        const expectedState = {
          filter: {},
          interface: {},
          resources: {
            offices: {
              777: {
                floors: [res2, res1]
              }
            }
          }
        };
        const resultingState = reducer(state, {
          type: actions.getResourcesScheduleSuccess.type,
          payload: {
            resources: [res2, res1],
            officeId: 777,
            date: 123
          }
        });

        expect(resultingState.resources.offices).toEqual(expectedState.resources.offices);
      });
      test('должен складывать в стейт новые данные об интервалах', () => {
        const intervals = {intervals: 'intervals'};
        const date = 123456;
        const res1 = {info: {email: 'x5@y.r', floor: 5}, events: [{eventId: 567}]};
        const res2 = {info: {email: 'y7@y.r', floor: 7}, events: [{eventId: 456}]};

        processIntervals.mockReturnValue(intervals);

        const state = {
          filter: {},
          interface: {},
          resources: {offices: {}, intervals: {}, restrictions: {}}
        };
        const expectedState = {
          filter: {},
          interface: {},
          resources: {
            offices: {},
            restrictions: {},
            intervals: {
              123456: intervals
            }
          }
        };
        const resultingState = reducer(state, {
          type: actions.getResourcesScheduleSuccess.type,
          payload: {
            date,
            resources: [res2, res1],
            officeId: 777
          }
        });

        expect(processIntervals).toHaveBeenCalledTimes(1);
        expect(processIntervals).toHaveBeenCalledWith(
          [
            {email: 'y7@y.r', intervals: [{eventId: 456, eventIds: [456]}]},
            {email: 'x5@y.r', intervals: [{eventId: 567, eventIds: [567]}]}
          ],
          date
        );

        expect(resultingState.resources.intervals).toEqual(expectedState.resources.intervals);
      });
      test('должен складывать в стейт новые данные об ограничениях бронирования', () => {
        const restrictions = {restrictions: 'restrictions'};
        const date = 123456;
        const res1 = {info: {email: 'x5@y.r', floor: 5}, events: [{eventId: 567}], restrictions};
        const res2 = {info: {email: 'y7@y.r', floor: 7}, events: [{eventId: 456}]};

        processRestrictions.mockReturnValue(restrictions);

        const state = {
          filter: {},
          interface: {},
          resources: {offices: {}, intervals: {}, restrictions: {}}
        };
        const expectedState = {
          filter: {},
          interface: {},
          resources: {
            offices: {},
            intervals: {},
            restrictions: {
              123456: restrictions
            }
          }
        };
        const resultingState = reducer(state, {
          type: actions.getResourcesScheduleSuccess.type,
          payload: {
            date,
            resources: [res2, res1],
            officeId: 777
          }
        });

        expect(resultingState.resources.restrictions).toEqual(expectedState.resources.restrictions);
      });
    });
  });
});

import {takeLatest, takeEvery} from 'redux-saga/effects';
import {expectSaga} from 'redux-saga-test-plan';
import {LOCATION_CHANGE} from 'connected-react-router';
import {call, select, getContext, fork} from 'redux-saga-test-plan/matchers';
import {throwError} from 'redux-saga-test-plan/providers';

import conditionalSaga from 'utils/conditionalSaga';
import SagaErrorReporter from 'utils/SagaErrorReporter';
import {getShowDateFromLocation} from 'features/router/routerSelectors';
import {getCurrentUser} from 'features/settings/settingsSelectors';
import {ActionTypes as EventsActionTypes} from 'features/events/eventsConstants';
import {getEventsSuccess} from 'features/events/eventsActions';

import * as actions from '../inviteActions';
import InviteApi from '../InviteApi';
import {
  getEnabledOffice,
  getInviteShowDate,
  getResourcesFilter,
  getOfficeIdFromQuery
} from '../inviteSelectors';
import rootSaga, {initInterface, getResourcesSchedule, handleLocationChange} from '../inviteSagas';

const errorReporter = new SagaErrorReporter('invite');

jest.mock('utils/conditionalSaga');
conditionalSaga.mockImplementation((_, saga) => saga);

describe('inviteSagas', () => {
  const inviteApi = new InviteApi();

  const providers = {
    apiContext: [getContext('inviteApi'), inviteApi],
    apiCalls: {
      getResourcesSchedule: val => [call.fn(inviteApi.getResourcesSchedule), val || {}]
    },
    selectors: {
      getShowDateFromLocation: val => [select(getShowDateFromLocation), val],
      getCurrentUser: val => [select(getCurrentUser), val],
      getEnabledOffice: val => [select(getEnabledOffice), val],
      getInviteShowDate: val => [select(getInviteShowDate), val],
      getResourcesFilter: val => [select(getResourcesFilter), val],
      getOfficeIdFromQuery: val => [select(getOfficeIdFromQuery), val]
    },
    errorReporter: [call.fn(errorReporter.send)]
  };

  describe('rootSaga', () => {
    test('должен записывать InviteApi в контекст', () => {
      return expectSaga(rootSaga)
        .provide([[getContext('api'), {}], [fork(initInterface)]])
        .setContext({inviteApi: new InviteApi({})})
        .silentRun(0);
    });

    test('должен подписываться на экшены и запускать инициализацию интерфейса', async () => {
      const {effects} = await expectSaga(rootSaga)
        .provide([[getContext('api'), {}], [fork(initInterface)]])
        .silentRun(0);

      expect(effects.fork).toEqual([
        takeLatest(actions.getResourcesSchedule.type, getResourcesSchedule),
        takeLatest(actions.changeOffice.type, getResourcesSchedule),
        takeLatest(actions.updateFilter.type, getResourcesSchedule),
        takeLatest(EventsActionTypes.CREATE_EVENT_SUCCESS, getResourcesSchedule),
        takeEvery(LOCATION_CHANGE, handleLocationChange),
        fork(initInterface)
      ]);
    });
  });

  describe('initInterface', () => {
    test('должен проставлять актуальную дату и офис', () => {
      const show_date = new Date(2020, 1, 2).valueOf();
      const officeId = 777;

      return expectSaga(initInterface)
        .provide([
          providers.selectors.getShowDateFromLocation(show_date),
          providers.selectors.getCurrentUser({officeId}),
          providers.selectors.getOfficeIdFromQuery(null)
        ])
        .put(actions.setShowDate({showDate: show_date}))
        .put(actions.changeOffice({officeId}))
        .run();
    });
    test('должен проставлять офис из url', () => {
      const show_date = new Date(2020, 1, 2).valueOf();
      const userOfficeId = 777;
      const officeIdFromQuery = 123;

      return expectSaga(initInterface)
        .provide([
          providers.selectors.getShowDateFromLocation(show_date),
          providers.selectors.getCurrentUser({officeId: userOfficeId}),
          providers.selectors.getOfficeIdFromQuery(officeIdFromQuery)
        ])
        .put(actions.changeOffice({officeId: officeIdFromQuery}))
        .run();
    });
    test('должен логировать ошибку', () => {
      const error = {
        name: 'error'
      };

      return expectSaga(initInterface)
        .provide([
          providers.selectors.getShowDateFromLocation(throwError(error)),
          providers.errorReporter
        ])
        .call([errorReporter, errorReporter.send], 'initInterface', error)
        .run();
    });
  });

  describe('getResourcesSchedule', () => {
    test('не должен падать при пустом ответе', () => {
      const showDate = new Date(2020, 1, 2).valueOf();
      const officeId = 777;
      const apiResponse = {intervals: {offices: []}};
      const resourcesFilter = 'video,small';

      return expectSaga(getResourcesSchedule)
        .provide([
          providers.apiContext,
          providers.selectors.getEnabledOffice(officeId),
          providers.selectors.getInviteShowDate(showDate),
          providers.selectors.getResourcesFilter(resourcesFilter),
          providers.apiCalls.getResourcesSchedule(apiResponse)
        ])
        .call([inviteApi, inviteApi.getResourcesSchedule], {
          date: showDate,
          officeId,
          filter: [resourcesFilter],
          bookableOnly: false
        })
        .put(actions.getResourcesScheduleSuccess({date: showDate, officeId, resources: []}))
        .put(actions.getResourcesScheduleDone())
        .run();
    });
    test('должен загружать расписание переговорок на заданную дату', () => {
      const showDate = new Date(2020, 1, 2).valueOf();
      const officeId = 777;
      const resources = [{events: [], info: {}}];
      const apiResponse = {intervals: {offices: [{resources}]}};
      const resourcesFilter = 'video,small';

      return expectSaga(getResourcesSchedule)
        .provide([
          providers.apiContext,
          providers.selectors.getEnabledOffice(officeId),
          providers.selectors.getInviteShowDate(showDate),
          providers.selectors.getResourcesFilter(resourcesFilter),
          providers.apiCalls.getResourcesSchedule(apiResponse)
        ])
        .call([inviteApi, inviteApi.getResourcesSchedule], {
          date: showDate,
          officeId,
          filter: [resourcesFilter],
          bookableOnly: false
        })
        .put(actions.getResourcesScheduleSuccess({date: showDate, officeId, resources}))
        .put(actions.getResourcesScheduleDone())
        .run();
    });
    test('должен загружать события на заданную дату, если они были присланы', () => {
      const showDate = new Date(2020, 1, 2).valueOf();
      const officeId = 777;
      const resources = [{events: [], info: {}}];
      const apiResponse = {
        intervals: {offices: [{resources}]},
        events: {events: [], lastUpdateTs: Date.now()}
      };
      const resourcesFilter = 'video,small';

      return expectSaga(getResourcesSchedule)
        .provide([
          providers.apiContext,
          providers.selectors.getEnabledOffice(officeId),
          providers.selectors.getInviteShowDate(showDate),
          providers.selectors.getResourcesFilter(resourcesFilter),
          providers.apiCalls.getResourcesSchedule(apiResponse)
        ])
        .call([inviteApi, inviteApi.getResourcesSchedule], {
          date: showDate,
          officeId,
          filter: [resourcesFilter],
          bookableOnly: false
        })
        .put(
          getEventsSuccess({
            events: apiResponse.events.events,
            lastUpdateTs: apiResponse.events.lastUpdateTs,
            from: showDate,
            to: showDate
          })
        )
        .run();
    });
    test('должен логировать ошибку', () => {
      const error = {
        name: 'error'
      };

      return expectSaga(getResourcesSchedule)
        .provide([providers.selectors.getEnabledOffice(throwError(error)), providers.errorReporter])
        .call([errorReporter, errorReporter.send], 'getResourcesSchedule', error)
        .put(actions.getResourcesScheduleDone())
        .run();
    });
  });

  describe('handleLocationChange', () => {
    test('должен переключать дату для Invite, если она изменилась', () => {
      const showDate = new Date(2020, 1, 2).valueOf();
      const show_date = new Date(2020, 10, 2).valueOf();
      const officeId = 567;
      const officeIdFromQuery = 567;

      return expectSaga(handleLocationChange)
        .provide([
          providers.selectors.getShowDateFromLocation(show_date),
          providers.selectors.getInviteShowDate(showDate),
          providers.selectors.getEnabledOffice(officeId),
          providers.selectors.getOfficeIdFromQuery(officeIdFromQuery)
        ])
        .put(actions.setShowDate({showDate: show_date}))
        .put(actions.getResourcesSchedule())
        .run();
    });
    test('не должен переключать дату для Invite, если она не изменилась', () => {
      const showDate = new Date(2020, 1, 2).valueOf();
      const show_date = new Date(2020, 1, 2).valueOf();
      const officeId = 567;
      const officeIdFromQuery = 567;

      return expectSaga(handleLocationChange)
        .provide([
          providers.selectors.getShowDateFromLocation(show_date),
          providers.selectors.getInviteShowDate(showDate),
          providers.selectors.getOfficeIdFromQuery(null),
          providers.selectors.getEnabledOffice(officeId),
          providers.selectors.getOfficeIdFromQuery(officeIdFromQuery)
        ])
        .not.put(actions.setShowDate({showDate: show_date}))
        .not.put(actions.getResourcesSchedule())
        .run();
    });
    test('должен переключать офис для Invite, если он изменился', () => {
      const showDate = new Date(2020, 1, 2).valueOf();
      const show_date = new Date(2020, 1, 2).valueOf();
      const officeId = 567;
      const officeIdFromQuery = 123;

      return expectSaga(handleLocationChange)
        .provide([
          providers.selectors.getShowDateFromLocation(show_date),
          providers.selectors.getInviteShowDate(showDate),
          providers.selectors.getEnabledOffice(officeId),
          providers.selectors.getOfficeIdFromQuery(officeIdFromQuery)
        ])
        .put(actions.changeOffice({officeId: officeIdFromQuery}))
        .run();
    });
    test('не должен переключать офис для Invite, если он не изменился', () => {
      const showDate = new Date(2020, 1, 2).valueOf();
      const show_date = new Date(2020, 1, 2).valueOf();
      const officeId = 567;
      const officeIdFromQuery = 567;

      return expectSaga(handleLocationChange)
        .provide([
          providers.selectors.getShowDateFromLocation(show_date),
          providers.selectors.getInviteShowDate(showDate),
          providers.selectors.getEnabledOffice(officeId),
          providers.selectors.getOfficeIdFromQuery(officeIdFromQuery)
        ])
        .not.put(actions.changeOffice({officeId}))
        .run();
    });
    test('должен логировать ошибку', () => {
      const error = {
        name: 'error'
      };

      return expectSaga(handleLocationChange)
        .provide([
          providers.selectors.getInviteShowDate(throwError(error)),
          providers.errorReporter
        ])
        .call([errorReporter, errorReporter.send], 'handleLocationChange', error)
        .run();
    });
  });
});

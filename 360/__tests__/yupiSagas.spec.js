import {takeLatest, takeEvery} from 'redux-saga/effects';
import {expectSaga} from 'redux-saga-test-plan';
import {call, select, getContext} from 'redux-saga-test-plan/matchers';
import {throwError} from 'redux-saga-test-plan/providers';
import {actionTypes as ReduxFormActionTypes} from 'redux-form';

import SagaErrorReporter from 'utils/SagaErrorReporter';
import conditionalSaga from 'utils/conditionalSaga';
import {
  makeEventFormValuesSelector,
  makeGetShouldUseRepetition
} from 'features/eventForm/eventFormSelectors';
import processIntervals from 'features/timeline/utils/processIntervals';
import {getDraftCanEdit} from 'features/eventDraft/eventDraftSelectors';
import {getCurrentUser} from 'features/settings/settingsSelectors';
import EventFormId from 'features/eventForm/EventFormId';

import processMembers from '../utils/processMembers';
import rootSaga, {
  getAllResources,
  getOnlySelectedResources,
  getResources,
  getMembersResources,
  getRecommendedResources,
  handleEventTimeChange,
  handleFormFieldChange,
  getStateDependentResources,
  getResourcesInfo,
  calculateFreeIntervals,
  setShowOnlyMembers,
  setShowOnlyResources
} from '../yupiSagas';
import processResourcesSchedule from '../utils/processResourcesSchedule';
import prepareOfficeResources from '../utils/prepareOfficeResources';
import processResources from '../utils/processResources';
import {getYupiShowDate, getShowAllResources, getIsSpaceshipActive} from '../yupiSelectors';
import * as actions from '../yupiActions';
import YupiApi from '../YupiApi';

jest.mock('utils/i18n');
jest.mock('utils/conditionalSaga');
jest.mock('features/timeline/utils/processIntervals');
jest.mock('features/yupi/utils/processMembers');
jest.mock('features/eventForm/eventFormSelectors');
jest.mock('../utils/processResourcesSchedule');
jest.mock('../utils/prepareOfficeResources');
jest.mock('../utils/processResources');
jest.mock('../utils/getOfficesOrder');
processResourcesSchedule.mockReturnValue([]);
processIntervals.mockReturnValue([]);
processMembers.mockReturnValue([]);
processResources.mockReturnValue([]);
prepareOfficeResources.mockReturnValue([]);
conditionalSaga.mockImplementation((selector, saga) => saga);

const getShouldUseRepetition = state => state;
const getEventFormValues = state => state;
const getReservableTable = state => state;

makeGetShouldUseRepetition.mockReturnValue(getShouldUseRepetition);
makeEventFormValuesSelector.mockReturnValue({getEventFormValues, getReservableTable});

const errorReporter = new SagaErrorReporter('yupi');

const form = EventFormId.fromParams(EventFormId.VIEWS.POPUP, EventFormId.MODES.CREATE).toString();

describe('yupiSagas', () => {
  const yupiApi = new YupiApi();

  const providers = {
    apiContext: [getContext('yupiApi'), yupiApi],
    apiCalls: {
      getAvailabilityIntervals: val => [call.fn(yupiApi.getAvailabilityIntervals), val || {}],
      getResourcesSchedule: val => [call.fn(yupiApi.getResourcesSchedule), val || {}],
      suggestMeetingResources: val => [call.fn(yupiApi.suggestMeetingResources), val || {}]
    },
    selectors: {
      getShouldUseRepetition: value => [select(getShouldUseRepetition), value],
      getEventFormValues: val => [select(getEventFormValues), val || {}],
      getReservableTable: val => [select(getReservableTable), Boolean(val)],
      getCurrentUser: val => [select(getCurrentUser), val || {}],
      getYupiShowDate: val => [select(getYupiShowDate), val || Date.now()],
      getShowAllResources: val => [select(getShowAllResources), val],
      getDraftCanEdit: val => [select(getDraftCanEdit), val],
      getIsSpaceshipActive: val => [select(getIsSpaceshipActive), val]
    },
    errorReporter: [call.fn(errorReporter.send)]
  };

  describe('rootSaga', () => {
    test('должен записывать YupiApi в контекст', () => {
      return expectSaga(rootSaga)
        .provide([[getContext('api'), {}]])
        .setContext({yupiApi: new YupiApi({})})
        .silentRun(0);
    });

    test('должен подписываться на экшены', async () => {
      const {effects} = await expectSaga(rootSaga)
        .provide([[getContext('api'), {}]])
        .silentRun(0);

      expect(effects.fork).toEqual([
        takeLatest(actions.makeGetResources.type, getResources),
        takeLatest(actions.makeGetAllResources.type, getAllResources),
        takeLatest(actions.makeGetRecommendedResources.type, getRecommendedResources),
        takeLatest(actions.makeGetOnlySelectedResources.type, getOnlySelectedResources),
        takeLatest(actions.makeGetMembersResources.type, getMembersResources),
        takeLatest(actions.makeSetShowDate.type, getResources),
        takeLatest(actions.switchShowAllResources.type, calculateFreeIntervals),
        takeLatest(actions.getAllResourcesSuccess.type, calculateFreeIntervals),
        takeLatest(actions.getRecommendedResourcesSuccess.type, calculateFreeIntervals),
        takeLatest(actions.makeSetShowOnlyMembers.type, setShowOnlyMembers),
        takeLatest(actions.makeSetShowOnlyResources.type, setShowOnlyResources),
        takeLatest(actions.getMembersResourcesSuccess.type, calculateFreeIntervals),
        takeEvery(ReduxFormActionTypes.CHANGE, handleFormFieldChange),
        takeEvery(ReduxFormActionTypes.ARRAY_PUSH, handleFormFieldChange),
        takeEvery(ReduxFormActionTypes.ARRAY_SPLICE, handleFormFieldChange)
      ]);
    });
  });

  describe('getResources', () => {
    test('должен вызывать саги загрузки ресурсов и участников', () => {
      return expectSaga(getResources, {meta: {form}})
        .provide([
          [call(getMembersResources, {meta: {form}})],
          [call(getStateDependentResources, {meta: {form}})]
        ])
        .call(getMembersResources, {meta: {form}})
        .call(getStateDependentResources, {meta: {form}})
        .run();
    });
    test('должен управлять флагом загрузки', () => {
      return expectSaga(getResources, {meta: {form}})
        .provide([
          [call(getMembersResources, {meta: {form}})],
          [call(getStateDependentResources, {meta: {form}})]
        ])
        .put(actions.getResourcesStart())
        .put(actions.getResourcesDone())
        .run();
    });
    test('должен сбрасывать значения интервалов', () => {
      expectSaga(getResources, {meta: {form}})
        .provide([
          [call(getMembersResources, {meta: {form}})],
          [call(getStateDependentResources, {meta: {form}})]
        ])
        .put(actions.dropResourcesIntervals())
        .put(actions.dropMembersResourcesIntervals())
        .run();
    });
    test('должен логировать ошибку', () => {
      const error = {
        name: 'error'
      };

      return expectSaga(getResources, {meta: {form}})
        .provide([
          [call(getMembersResources, {meta: {form}}), throwError(error)],
          [call(getStateDependentResources, {meta: {form}}), throwError(error)],
          providers.errorReporter
        ])
        .call([errorReporter, errorReporter.send], 'getResources', error)
        .run();
    });
  });

  describe('getRecommendedResources', () => {
    test('должен загружать рекомендации', () => {
      const event = {
        resources: [{officeId: 1}, {officeId: 2}]
      };
      const showDate = Date.now();
      const shouldUseRepetition = false;
      const apiResponse = {offices: [{id: 1}, {id: 2}]};

      return expectSaga(getRecommendedResources, {meta: {form}})
        .provide([
          providers.apiContext,
          providers.apiCalls.suggestMeetingResources(apiResponse),
          [call.fn(getResourcesInfo), apiResponse.offices],
          providers.selectors.getEventFormValues(event),
          providers.selectors.getYupiShowDate(showDate),
          providers.selectors.getShouldUseRepetition(shouldUseRepetition)
        ])
        .call([yupiApi, yupiApi.suggestMeetingResources], event, showDate, shouldUseRepetition)
        .run();
    });
    test('должен бросать getRecommendedResourcesSuccess с пустым массивом, если переговорок нет в событии', () => {
      const event = {
        resources: []
      };

      return expectSaga(getRecommendedResources, {meta: {form}})
        .provide([providers.selectors.getEventFormValues(event)])
        .put(actions.getRecommendedResourcesSuccess({offices: [], event}))
        .run();
    });
    test('не должен ходить за рекомендациями, если переговорок нет в событии', () => {
      const event = {
        resources: []
      };

      return expectSaga(getRecommendedResources, {meta: {form}})
        .provide([providers.selectors.getEventFormValues(event)])
        .not.call.fn(yupiApi.suggestMeetingResources)
        .run();
    });
    test('должен логировать ошибку', () => {
      const error = {
        name: 'error'
      };

      return expectSaga(getRecommendedResources, {payload: {}, meta: {form}})
        .provide([
          providers.selectors.getEventFormValues(throwError(error)),
          providers.errorReporter
        ])
        .call([errorReporter, errorReporter.send], 'getRecommendedResources', error)
        .run();
    });
  });

  describe('getStateDependentResources', () => {
    const {selectors} = providers;
    test('должен загружать только выбранные переговорки, если нет прав редактировать встречу', () => {
      return expectSaga(getStateDependentResources, {meta: {form}})
        .provide([
          selectors.getDraftCanEdit(false),
          selectors.getEventFormValues({id: 100500}),
          selectors.getReservableTable(false),
          selectors.getShowAllResources(false),
          [call(getOnlySelectedResources, {meta: {form}})],
          [call(getAllResources, {meta: {form}})],
          [call(getRecommendedResources, {meta: {form}})]
        ])
        .call(getOnlySelectedResources, {meta: {form}})
        .not.call(getAllResources, {meta: {form}})
        .not.call(getRecommendedResources, {meta: {form}})
        .run();
    });
    test('должен загружать только выбранные переговорки, если isReservableTable', () => {
      return expectSaga(getStateDependentResources, {meta: {form}})
        .provide([
          selectors.getDraftCanEdit(true),
          selectors.getEventFormValues({}),
          selectors.getReservableTable(true),
          selectors.getShowAllResources(false),
          [call(getOnlySelectedResources, {meta: {form}})],
          [call(getAllResources, {meta: {form}})],
          [call(getRecommendedResources, {meta: {form}})]
        ])
        .call(getOnlySelectedResources, {meta: {form}})
        .not.call(getAllResources, {meta: {form}})
        .not.call(getRecommendedResources, {meta: {form}})
        .run();
    });
    test('должен загружать все переговорки, если есть права редактировать встречу и showAllResources === true', () => {
      return expectSaga(getStateDependentResources, {meta: {form}})
        .provide([
          selectors.getDraftCanEdit(true),
          selectors.getEventFormValues({id: 100500}),
          selectors.getShowAllResources(true),
          selectors.getReservableTable(false),
          [call(getOnlySelectedResources, {meta: {form}})],
          [call(getAllResources, {meta: {form}})],
          [call(getRecommendedResources, {meta: {form}})]
        ])
        .not.call(getOnlySelectedResources, {meta: {form}})
        .call(getAllResources, {meta: {form}})
        .not.call(getRecommendedResources, {meta: {form}})
        .run();
    });
    test('должен загружать только рекомендации, если есть права редактировать встречу и showAllResources === false', () => {
      return expectSaga(getStateDependentResources, {meta: {form}})
        .provide([
          selectors.getDraftCanEdit(true),
          selectors.getEventFormValues({id: 100500}),
          selectors.getShowAllResources(false),
          selectors.getReservableTable(false),
          [call(getOnlySelectedResources, {meta: {form}})],
          [call(getAllResources, {meta: {form}})],
          [call(getRecommendedResources, {meta: {form}})]
        ])
        .not.call(getOnlySelectedResources, {meta: {form}})
        .not.call(getAllResources, {meta: {form}})
        .call(getRecommendedResources, {meta: {form}})
        .run();
    });
    test('должен загружать только рекомендации, при создании встречи', () => {
      return expectSaga(getStateDependentResources, {meta: {form}})
        .provide([
          selectors.getDraftCanEdit(false),
          selectors.getEventFormValues({}),
          selectors.getShowAllResources(false),
          selectors.getReservableTable(false),
          [call(getOnlySelectedResources, {meta: {form}})],
          [call(getAllResources, {meta: {form}})],
          [call(getRecommendedResources, {meta: {form}})]
        ])
        .not.call(getOnlySelectedResources, {meta: {form}})
        .not.call(getAllResources, {meta: {form}})
        .call(getRecommendedResources, {meta: {form}})
        .run();
    });
  });

  describe('getAllResources', () => {
    const {selectors, apiContext} = providers;

    test('должен загружать все переговорки', () => {
      const eventId = 777;
      const event = {
        id: eventId,
        resources: [{officeId: 1}, {officeId: 2}],
        resourcesFilter: {1: '1', 2: '2'}
      };
      const date = Date.now();
      const intervals = {intervals: Symbol()};

      const expectedApiCallParams = {
        officeId: [1, 2],
        filter: ['1', '2'],
        exceptEventId: eventId,
        date
      };

      return expectSaga(getAllResources, {meta: {form}})
        .provide([
          apiContext,
          providers.apiCalls.getResourcesSchedule({intervals}),
          [call.fn(getResourcesInfo), [{id: 1}, {id: 2}]],
          selectors.getEventFormValues(event),
          selectors.getYupiShowDate(date)
        ])
        .call([yupiApi, yupiApi.getResourcesSchedule], expectedApiCallParams)
        .put(actions.getAllResourcesSuccess({intervals, event}))
        .run();
    });

    test('должен логировать ошибку', () => {
      const error = {
        name: 'error'
      };

      return expectSaga(getAllResources, {meta: {form}})
        .provide([
          providers.selectors.getEventFormValues(throwError(error)),
          providers.errorReporter
        ])
        .call([errorReporter, errorReporter.send], 'getAllResources', error)
        .run();
    });
  });
  describe('getOnlySelectedResources', () => {
    const {selectors, apiContext} = providers;
    test('должен загружать выбранные переговорки', () => {
      const eventId = 777;
      const event = {
        id: eventId,
        resources: [
          {officeId: 1, resource: {email: 'x@y.r'}},
          {officeId: 2, resource: {email: 'y@y.r'}}
        ],
        resourcesFilter: {1: '1', 2: '2'}
      };
      const date = Date.now();
      const intervals = {intervals: Symbol()};

      const expectedApiCallParams = {
        officeId: [1, 2],
        email: ['x@y.r', 'y@y.r'],
        exceptEventId: eventId,
        date
      };

      return expectSaga(getOnlySelectedResources, {meta: {form}})
        .provide([
          apiContext,
          providers.apiCalls.getResourcesSchedule({intervals}),
          [call.fn(getResourcesInfo), [{id: 1}, {id: 2}]],
          selectors.getEventFormValues(event),
          selectors.getYupiShowDate(date)
        ])
        .call([yupiApi, yupiApi.getResourcesSchedule], expectedApiCallParams)
        .put(actions.getOnlySelectedResourcesSuccess({intervals, event}))
        .run();
    });

    test('должен логировать ошибку', () => {
      const error = {
        name: 'error'
      };

      return expectSaga(getOnlySelectedResources, {meta: {form}})
        .provide([
          providers.selectors.getEventFormValues(throwError(error)),
          providers.errorReporter
        ])
        .call([errorReporter, errorReporter.send], 'getOnlySelectedResources', error)
        .run();
    });
  });
  describe('getMembersResources', () => {
    const {selectors, apiContext} = providers;
    test('должен загружать занятость участников', () => {
      const eventId = 777;
      const event = {
        id: eventId,
        organizer: {email: 'o@y.r'},
        attendees: [{email: 'x@y.r'}, {email: 'y@y.r'}],
        start: 123,
        end: 456
      };
      const date = Date.now();

      const expectedApiCallParams = {
        emails: ['o@y.r', 'x@y.r', 'y@y.r'],
        exceptEventId: eventId,
        date
      };

      const processedIntevals = [{start: event.start, end: event.end}];
      processMembers.mockReturnValue(processedIntevals);

      const intervals = {intervals: processedIntevals};
      processIntervals.mockReturnValue(intervals);

      return expectSaga(getMembersResources, {meta: {form}})
        .provide([
          apiContext,
          providers.apiCalls.getAvailabilityIntervals({}),
          selectors.getEventFormValues(event),
          selectors.getCurrentUser(),
          selectors.getYupiShowDate(date)
        ])
        .call([yupiApi, yupiApi.getAvailabilityIntervals], expectedApiCallParams)
        .put(
          actions.getMembersResourcesSuccess({
            members: [{email: 'o@y.r'}, {email: 'x@y.r'}, {email: 'y@y.r'}],
            intervals
          })
        )
        .run();
    });
    test('должен загружать занятость текущего пользователя, если нет организатора', () => {
      const eventId = 777;
      const event = {
        id: eventId,
        attendees: [{email: 'x@y.r'}, {email: 'y@y.r'}]
      };
      const date = Date.now();

      const expectedApiCallParams = {
        emails: ['o@y.r', 'x@y.r', 'y@y.r'],
        exceptEventId: eventId,
        date
      };
      const intervals = {intervals: 'intervals'};
      processIntervals.mockReturnValue(intervals);

      return expectSaga(getMembersResources, {meta: {form}})
        .provide([
          apiContext,
          providers.apiCalls.getAvailabilityIntervals({}),
          selectors.getEventFormValues(event),
          selectors.getCurrentUser({email: 'o@y.r'}),
          selectors.getYupiShowDate(date)
        ])
        .call([yupiApi, yupiApi.getAvailabilityIntervals], expectedApiCallParams)
        .put(
          actions.getMembersResourcesSuccess({
            members: [{email: 'o@y.r'}, {email: 'x@y.r'}, {email: 'y@y.r'}],
            intervals
          })
        )
        .run();
    });

    test('должен логировать ошибку', () => {
      const error = {
        name: 'error'
      };

      return expectSaga(getMembersResources, {meta: {form}})
        .provide([providers.selectors.getEventFormValues(throwError(error))])
        .call([errorReporter, errorReporter.send], 'getMembersResources', error)
        .run();
    });
  });
  describe('handleEventTimeChange', () => {
    test('должен выставлять showDate', () => {
      const newDate = Date.now();

      expectSaga(handleEventTimeChange, {meta: {form}})
        .provide([providers.selectors.getEventFormValues({start: newDate})])
        .put(actions.makeSetShowDate({form})({showDate: newDate}))
        .run();
    });
  });
  describe('handleFormFieldChange', () => {
    test('не должен отрабатывать, если не включен Космолёт', () => {
      const action = {
        meta: {
          form
        }
      };

      expectSaga(handleFormFieldChange, action)
        .provide([providers.selectors.getIsSpaceshipActive(false)])
        .not.put(actions.makeGetMembersResources({form})())
        .not.call.fn(handleEventTimeChange)
        .not.fork(getStateDependentResources, {form});
    });
    test('не должен отрабатывать, если форма не "event"', () => {
      const action = {
        meta: {
          form: 'not-event-form'
        }
      };

      expectSaga(handleFormFieldChange, action)
        .provide([providers.selectors.getIsSpaceshipActive(true)])
        .not.put(actions.makeGetMembersResources({form})())
        .not.call.fn(handleEventTimeChange)
        .not.fork(getStateDependentResources);
    });
    test('должен менять дату интерфейса при изменении end', () => {
      const eventStart = Date.now();
      const action = {
        meta: {form, field: 'end'},
        payload: eventStart
      };

      expectSaga(handleFormFieldChange, action)
        .provide([
          [call(handleEventTimeChange, {meta: {form}})],
          providers.selectors.getYupiShowDate(12345),
          providers.selectors.getIsSpaceshipActive(true)
        ])
        .call(handleEventTimeChange, {meta: {form}})
        .not.put(actions.makeGetMembersResources({form})())
        .not.fork(getStateDependentResources);
    });
    test('должен кидать getMembersResources изменении участников', () => {
      const action = {
        meta: {form: 'event', field: 'attendees'}
      };

      expectSaga(handleFormFieldChange, action)
        .provide([
          [call(handleEventTimeChange, {meta: {form}})],
          providers.selectors.getIsSpaceshipActive(true)
        ])
        .not.call.fn(handleEventTimeChange)
        .put(actions.makeGetMembersResources({form})())
        .not.fork(getStateDependentResources);
    });
    test('должен кидать getMembersResources при изменении организатора', () => {
      const action = {
        meta: {form, field: 'organizer'}
      };

      expectSaga(handleFormFieldChange, action)
        .provide([
          [call(handleEventTimeChange, {meta: {form}})],
          providers.selectors.getIsSpaceshipActive(true)
        ])
        .not.call.fn(handleEventTimeChange)
        .put(actions.makeGetMembersResources({form})())
        .not.fork(getStateDependentResources);
    });
    test('должен кидать getMembersResources при изменении флага ignoreUsersEvents', () => {
      const action = {
        meta: {form, field: 'ignoreUsersEvents'}
      };

      expectSaga(handleFormFieldChange, action)
        .provide([
          [call(handleEventTimeChange, {meta: {form}})],
          providers.selectors.getIsSpaceshipActive(true)
        ])
        .not.call.fn(handleEventTimeChange)
        .put(actions.makeGetMembersResources({form})())
        .not.fork(getStateDependentResources);
    });
    test('должен вызывать getStateDependentResources при изменении переговорок', () => {
      const action = {
        meta: {form, field: 'resources[0]'}
      };

      expectSaga(handleFormFieldChange, action)
        .provide([
          [call(handleEventTimeChange, {meta: {form}})],
          providers.selectors.getIsSpaceshipActive(true)
        ])
        .not.call.fn(handleEventTimeChange)
        .not.put(actions.makeGetMembersResources({form})())
        .fork(getStateDependentResources);
    });
    test('должен вызывать getStateDependentResources при изменении фильтра переговорок', () => {
      const action = {
        meta: {form, field: 'resourcesFilter'}
      };

      expectSaga(handleFormFieldChange, action)
        .provide([
          [call(handleEventTimeChange, {meta: {form}})],
          providers.selectors.getIsSpaceshipActive(true)
        ])
        .not.call.fn(handleEventTimeChange)
        .not.put(actions.makeGetMembersResources({form})())
        .fork(getStateDependentResources);
    });
  });
});

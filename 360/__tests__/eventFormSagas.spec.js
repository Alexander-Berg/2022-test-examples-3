import {delay} from 'redux-saga';
import {takeLatest, takeEvery} from 'redux-saga/effects';
import {testSaga, expectSaga} from 'redux-saga-test-plan';
import {call, select, getContext} from 'redux-saga-test-plan/matchers';
import {throwError} from 'redux-saga-test-plan/providers';
import {actionTypes as ReduxFormActionTypes, change, arrayPush, initialize} from 'redux-form';
import {List, Map} from 'immutable';

import config from 'configs/config';
import * as environment from 'configs/environment';
import Availability from 'constants/Availability';
import i18n from 'utils/i18n';
import SagaErrorReporter from 'utils/SagaErrorReporter';
import eventDateFormat from 'utils/dates/eventDateFormat';
import makeActionPatternChecker from 'utils/makeActionPatternChecker';
import conditionalSaga from 'utils/conditionalSaga';
import LayerRecord from 'features/layers/LayerRecord';
import {getLayerById} from 'features/layers/layersSelectors';
import {ActionTypes as LayersActionTypes} from 'features/layers/layersConstants';
import {
  notifyFailure,
  notifySuccess,
  notificationHelpers
} from 'features/notifications/notificationsActions';
import {getOfficesTzOffsets} from 'features/offices/officesActions';
import {getFavoriteContacts} from 'features/abookSuggest/abookSuggestSelectors';
import {getCurrentUser, getIsSpaceshipActivated} from 'features/settings/settingsSelectors';
import {getOffices} from 'features/offices/officesSelectors';
import SuggestMeetingTimesApi from 'features/suggestMeetingTimes/SuggestMeetingTimesApi';
import EventsApi from 'features/events/EventsApi';

import EventFormApi from '../EventFormApi';
import rootSaga, {
  bulkAvailabilityCheck,
  checkMembersAvailability,
  checkSomeMembersAvailability,
  checkOrganizerAvailability,
  checkResourcesAvailability,
  checkFavoriteContactsAvailability,
  getRepetitionDescription,
  findUsers,
  addResource,
  findResources,
  reserveResources,
  cancelResourcesReservation,
  createConferenceCall,
  handleFormFieldChange,
  handleLayerUpdate,
  handleFormLayerUpdate,
  handleOrganizerUpdate,
  changeAttendeesListWithOrganizerChanged,
  handleResourcesUpdate,
  getDataForAvailabilityRequest,
  checkAvailabilities,
  getAttendees,
  getOptionalAttendees,
  tryChangeNotifications,
  tryChangeOthersCanView,
  handleResourceUpdate,
  telemostLinkInjector,
  injectTelemostConferenceLink,
  handleFormInit,
  updateGaps,
  updateSuggestedIntervals,
  addOfficesForNewMembers,
  tryAddOffices,
  getMembersRoomsNecessity
} from '../eventFormSagas';
import {
  makeGetEventFormValues,
  makeGetEventFormInitialValues,
  getIsLoadingAttendees,
  makeGetShouldUseRepetition,
  getAllForms
} from '../eventFormSelectors';
import getNextOfficeId from '../utils/getNextOfficeId';
import processOutputDates from '../utils/processOutputDates';
import processOutputRepetition from '../utils/processOutputRepetition';
import * as actions from '../eventFormActions';
import EventFormId from '../EventFormId';
import {featuresSelector} from '../../appStatus/appStatusSelectors';

jest.mock('utils/i18n');
jest.mock('utils/makeActionPatternChecker');
jest.mock('utils/conditionalSaga');

conditionalSaga.mockImplementation((_, saga) => saga);

const errorReporter = new SagaErrorReporter('eventForm');

const form = EventFormId.fromParams(EventFormId.VIEWS.POPUP, EventFormId.MODES.CREATE).toString();
const getEventFormValues = makeGetEventFormValues(form);
const getEventFormInitialValues = makeGetEventFormInitialValues(form);
const getShouldUseRepetition = makeGetShouldUseRepetition(form);

describe('eventFormSagas', () => {
  beforeEach(() => {
    jest.spyOn(notificationHelpers, 'generateId').mockReturnValue('id');
  });

  describe('rootSaga', () => {
    test('должен работать', () => {
      testSaga(rootSaga)
        .next()
        .getContext('api')
        .next()
        .setContext({
          eventFormApi: new EventFormApi(),
          eventsApi: new EventsApi(),
          suggestMeetingTimesApi: new SuggestMeetingTimesApi()
        })
        .next()
        .all([
          takeLatest(actions.makeCheckMembersAvailability.type, checkMembersAvailability),
          takeLatest(actions.makeCheckSomeMembersAvailability.type, checkSomeMembersAvailability),
          takeLatest(actions.makeCheckOrganizerAvailability.type, checkOrganizerAvailability),
          takeLatest(actions.makeCheckResourcesAvailability.type, checkResourcesAvailability),
          takeLatest(
            actions.makeCheckFavoriteContactsAvailability.type,
            checkFavoriteContactsAvailability
          ),
          takeLatest(actions.getRepetitionDescription.type, getRepetitionDescription),
          takeEvery(actions.makeFindUsers.type, findUsers),
          takeEvery(actions.findResources.type, findResources),
          takeLatest(actions.reserveResources.type, reserveResources),
          takeLatest(actions.cancelResourcesReservation.type, cancelResourcesReservation),
          takeLatest(actions.makeChangeOrganizer.type, handleOrganizerUpdate),
          takeLatest(actions.makeAddResource.type, addResource),
          takeLatest(actions.makeGetAttendees.type, getAttendees),
          takeLatest(actions.makeGetOptionalAttendees.type, getOptionalAttendees),
          takeEvery(actions.makeAddOfficesForNewMembers.type, addOfficesForNewMembers),
          takeEvery(actions.createConferenceCall.type, createConferenceCall),
          takeEvery(ReduxFormActionTypes.INITIALIZE, handleFormInit),
          takeEvery(ReduxFormActionTypes.CHANGE, handleFormFieldChange),
          takeEvery(ReduxFormActionTypes.CHANGE, handleResourceUpdate),
          takeEvery(ReduxFormActionTypes.ARRAY_PUSH, handleResourceUpdate),
          takeEvery(ReduxFormActionTypes.ARRAY_SPLICE, handleResourceUpdate),
          takeLatest(actions.makeCheckAvailabilities.type, checkAvailabilities),
          takeEvery(LayersActionTypes.UPDATE_LAYER_SUCCESS, handleLayerUpdate)
        ])
        .next()
        .isDone();
    });
  });

  describe('getDataForAvailabilityRequest', () => {
    test('должен возвращать объект параметров для проверки занятости', () => {
      const now = Date.now();
      const aMinuteLater = now + 60000;
      const emails = ['pewpew@ya.ru'];
      const timeParams = {
        start: now,
        end: aMinuteLater,
        isAllDay: false
      };
      const {start, end} = processOutputDates(timeParams);

      return expectSaga(getDataForAvailabilityRequest, emails, form)
        .provide([
          [
            select(getEventFormValues),
            {
              ...timeParams,
              instanceStartTs: now,
              repeat: false
            }
          ],
          [select(getShouldUseRepetition), false]
        ])
        .returns({
          start,
          end,
          instanceStartTs: now,
          isAllDay: false,
          repetition: undefined,
          exceptEventId: undefined,
          emails: emails
        })
        .run();
    });

    test('должен возвращать repetition при getShouldUseRepetition === true', async () => {
      const now = Date.now();
      const aMinuteLater = now + 60000;
      const emails = ['pewpew@ya.ru'];
      const timeParams = {
        start: now,
        end: aMinuteLater,
        isAllDay: false
      };

      const {returnValue} = await expectSaga(getDataForAvailabilityRequest, emails, form)
        .provide([
          [
            select(getEventFormValues),
            {
              ...timeParams,
              instanceStartTs: now,
              repeat: true,
              repetition: {},
              id: 100
            }
          ],
          [call(processOutputRepetition), {}],
          [select(getShouldUseRepetition), true]
        ])
        .run();

      expect(returnValue.repetition).toEqual({});
    });

    test('не должен возвращать repetition при getShouldUseRepetition === false', async () => {
      const now = Date.now();
      const aMinuteLater = now + 60000;
      const emails = ['pewpew@ya.ru'];
      const timeParams = {
        start: now,
        end: aMinuteLater,
        isAllDay: false
      };

      const {returnValue} = await expectSaga(getDataForAvailabilityRequest, emails, form)
        .provide([
          [
            select(getEventFormValues),
            {
              ...timeParams,
              instanceStartTs: now,
              repeat: false,
              repetition: {},
              id: 100
            }
          ],
          [select(getShouldUseRepetition), false]
        ])
        .run();

      expect(returnValue.repetition).toEqual(undefined);
    });

    test('должен возвращать пустой объект, если нет формы', async () => {
      const emails = ['pewpew@ya.ru'];

      const {returnValue} = await expectSaga(getDataForAvailabilityRequest, emails, form)
        .provide([[select(getEventFormValues), null]])
        .run();

      expect(returnValue).toEqual({});
    });

    test('должен логировать ошибку', () => {
      return expectSaga(getDataForAvailabilityRequest, [], form)
        .provide([
          [select(getEventFormValues), throwError({name: 'error'})],
          [call.fn(errorReporter.send)]
        ])
        .call([errorReporter, errorReporter.send], 'getDataForAvailabilityRequest', {name: 'error'})
        .run();
    });
  });

  describe('checkMembersAvailability', () => {
    test('должен проверять занятость участников встречи, если они есть', () => {
      const eventFormApi = new EventFormApi();
      const response = [];

      return expectSaga(checkMembersAvailability, {meta: {form}})
        .provide([
          [getContext('eventFormApi'), eventFormApi],
          [
            select(getEventFormValues),
            {attendees: [{email: 'omg@omg.omg'}], optionalAttendees: []}
          ],
          [call.fn(eventFormApi.getAvailabilities), response],
          [call.fn(getDataForAvailabilityRequest), {}, form]
        ])
        .call.fn(eventFormApi.getAvailabilities)
        .put(actions.makeCheckMembersAvailabilitySuccess({form})({membersAvailability: response}))
        .run();
    });

    test('не должен проверять занятость участников встречи, если их нет', () => {
      const eventFormApi = new EventFormApi();

      return expectSaga(checkMembersAvailability, {meta: {form}})
        .provide([
          [getContext('eventFormApi'), eventFormApi],
          [select(getEventFormValues), {attendees: [], optionalAttendees: []}]
        ])
        .not.call.fn(eventFormApi.getAvailabilities)
        .run();
    });

    test('не должен ломаться, если нет формы', async () => {
      const eventFormApi = new EventFormApi();

      return expectSaga(checkMembersAvailability, {meta: {form}})
        .provide([[select(getEventFormValues), null], [getContext('eventFormApi'), eventFormApi]])
        .run();
    });

    test('должен логировать ошибку', () => {
      const error = {
        name: 'error'
      };

      return expectSaga(checkMembersAvailability, {meta: {form}})
        .provide([[getContext('eventFormApi'), throwError(error)], [call.fn(errorReporter.send)]])
        .call([errorReporter, errorReporter.send], 'checkMembersAvailability', error)
        .run();
    });
  });

  describe('checkSomeMembersAvailability', () => {
    test('должен получать занятость по массиву email-адресов', () => {
      const eventFormApi = new EventFormApi();
      const emails = ['pew@pew.ab'];
      const response = [{email: 'pew@pew.ab', availability: 'available'}];

      return expectSaga(checkSomeMembersAvailability, {payload: {emails}, meta: {form}})
        .provide([
          [getContext('eventFormApi'), eventFormApi],
          [call.fn(eventFormApi.getAvailabilities), response],
          [call.fn(getDataForAvailabilityRequest), {}, form]
        ])
        .call.fn(eventFormApi.getAvailabilities)
        .put(actions.makeCheckAvailabilitiesSuccess({form})({availabilities: response}))
        .run();
    });

    test('должен логировать ошибку', () => {
      const error = {
        name: 'error'
      };

      return expectSaga(checkSomeMembersAvailability, {payload: {}, meta: {form}})
        .provide([[getContext('eventFormApi'), throwError(error)], [call.fn(errorReporter.send)]])
        .call([errorReporter, errorReporter.send], 'checkSomeMembersAvailability', error)
        .run();
    });
  });

  describe('checkOrganizerAvailability', () => {
    test('должен проверять занятость организатора', () => {
      const eventFormApi = new EventFormApi();
      const orgAvailability = {
        email: 'f@g.h',
        availability: Availability.AVAILABLE
      };

      return expectSaga(checkOrganizerAvailability, {meta: {form}})
        .provide([
          [select(getEventFormValues), {organizer: {email: 'pewpew@ya.ru'}}],
          [getContext('eventFormApi'), eventFormApi],
          [call.fn(eventFormApi.getAvailabilities), [orgAvailability]],
          [call.fn(getDataForAvailabilityRequest), {}, form]
        ])
        .call.fn(eventFormApi.getAvailabilities)
        .put(actions.makeCheckOrganizerAvailabilitySuccess({form})(orgAvailability))
        .run();
    });

    test('не должен пытаться проверять занятость организатора, если он не указан во встрече', () => {
      const eventFormApi = new EventFormApi();

      return expectSaga(checkOrganizerAvailability, {meta: {form}})
        .provide([
          [
            select(getEventFormValues),
            {
              start: Date.now(),
              end: Date.now() + 1000,
              instanceStartTs: Date.now(),
              id: 0
            }
          ]
        ])
        .not.call.fn(eventFormApi.getAvailabilities)
        .not.put(
          actions.makeCheckOrganizerAvailabilitySuccess({form})({
            email: 'f',
            availability: Availability.BUSY
          })
        )
        .run();
    });

    test('должен логировать ошибку', () => {
      return expectSaga(checkOrganizerAvailability, {meta: {form}})
        .provide([
          [select(getEventFormValues), throwError({name: 'error'})],
          [call.fn(errorReporter.send)]
        ])
        .call([errorReporter, errorReporter.send], 'checkOrganizerAvailability', {name: 'error'})
        .run();
    });
  });

  describe('checkResourcesAvailability', () => {
    test('должен проверять доступность переговорок', () => {
      const eventFormApi = new EventFormApi();
      const formValues = {
        resources: [
          {
            email: 'confroom@ya.ru'
          }
        ]
      };
      const response = [];

      return expectSaga(checkResourcesAvailability, {payload: {}, meta: {form}})
        .provide([
          [getContext('eventFormApi'), eventFormApi],
          [select(getEventFormValues), formValues],
          [call.fn(getDataForAvailabilityRequest), {}, form],
          [call.fn(eventFormApi.getAvailabilities), response]
        ])
        .call.fn(eventFormApi.getAvailabilities, {})
        .put(
          actions.makeCheckResourcesAvailabilitySuccess({form})({resourcesAvailability: response})
        )
        .run();
    });

    test('должен проверять доступность переговорок по переданным данным', () => {
      const eventFormApi = new EventFormApi();
      const formValues = {
        resources: [
          {
            email: 'confroom@ya.ru'
          }
        ]
      };
      const response = [];

      return expectSaga(checkResourcesAvailability, {payload: {emails: []}, meta: {form}})
        .provide([
          [getContext('eventFormApi'), eventFormApi],
          [select(getEventFormValues), formValues],
          [call.fn(eventFormApi.getAvailabilities), response]
        ])
        .call.fn(eventFormApi.getAvailabilities, {})
        .put(
          actions.makeCheckResourcesAvailabilitySuccess({form})({resourcesAvailability: response})
        )
        .run();
    });

    test('должен логировать ошибку', () => {
      return expectSaga(checkResourcesAvailability, {meta: {form}})
        .provide([
          [select(getEventFormValues), throwError({name: 'error'})],
          [call.fn(errorReporter.send)]
        ])
        .call([errorReporter, errorReporter.send], 'checkResourcesAvailability', {name: 'error'})
        .run();
    });
  });

  describe('checkFavoriteContactsAvailability', () => {
    test('должен проверять занятость избранных контактов, если они есть', () => {
      const eventFormApi = new EventFormApi();
      const response = [];

      return expectSaga(checkFavoriteContactsAvailability, {meta: {form}})
        .provide([
          [getContext('eventFormApi'), eventFormApi],
          [select(getFavoriteContacts), ['test@ya.ru']],
          [call.fn(getDataForAvailabilityRequest), {}, form],
          [call.fn(eventFormApi.getAvailabilities), response]
        ])
        .call.fn(eventFormApi.getAvailabilities)
        .put(actions.checkFavoriteContactsAvailabilitySuccess({availabilities: response}))
        .run();
    });

    test('не должен проверять занятость избранных контактов, если их нет', () => {
      const eventFormApi = new EventFormApi();

      return expectSaga(checkFavoriteContactsAvailability, {meta: {form}})
        .provide([[getContext('eventFormApi'), eventFormApi], [select(getFavoriteContacts), []]])
        .not.call.fn(eventFormApi.getAvailabilities)
        .run();
    });

    test('должен логировать ошибку', () => {
      const error = {name: 'error'};

      return expectSaga(checkFavoriteContactsAvailability, {meta: {form}})
        .provide([[getContext('eventFormApi'), throwError(error)], [call.fn(errorReporter.send)]])
        .call([errorReporter, errorReporter.send], 'checkFavoriteContactsAvailability', error)
        .run();
    });
  });

  describe('getRepetitionDescription', () => {
    test('должен получать словесное описание повторений', () => {
      const eventFormApi = new EventFormApi();
      const action = {
        payload: {
          resolve() {}
        }
      };
      const response = {
        description: ''
      };
      const error = {
        name: 'error'
      };

      testSaga(getRepetitionDescription, action)
        .next()
        .getContext('eventFormApi')
        .next(eventFormApi)
        .call(
          [eventFormApi, eventFormApi.getRepetitionDescription],
          action.payload.repetitionParams
        )
        .next(response)
        .call(action.payload.resolve, response.description)
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call([errorReporter, errorReporter.send], 'getRepetitionDescription', error)
        .next()
        .isDone();
    });
  });

  describe('tryAddOffices', () => {
    test('ничего не делать, если не корп', () => {
      sinon.stub(environment, 'isCorp').value(false);
      const eventFormApi = new EventFormApi();

      expectSaga(tryAddOffices, form)
        .not.call.fn(eventFormApi.tryAddOffices)
        .run();
    });

    test('ничего не делать, если встреча повторяющаяся', () => {
      sinon.stub(environment, 'isCorp').value(true);
      const eventFormApi = new EventFormApi();

      expectSaga(tryAddOffices, form)
        .provide([
          [getContext('eventFormApi'), eventFormApi],
          [select(getShouldUseRepetition), true]
        ])
        .not.call.fn(eventFormApi.tryAddOffices)
        .run();
    });

    test('должен делать запрос с началом дня для начала/конца интервала', () => {
      sinon.stub(environment, 'isCorp').value(true);
      const eventFormApi = new EventFormApi();

      expectSaga(tryAddOffices, form)
        .provide([
          [getContext('eventFormApi'), eventFormApi],
          [select(getShouldUseRepetition), false],
          [call.fn(getMembersRoomsNecessity), {tet4enko: {isRoomNeeded: true}}],
          [
            select(getEventFormValues),
            {
              attendees: [{login: 'tet4enko', officeId: 1}],
              start: 123,
              end: 456,
              resources: []
            }
          ]
        ])
        .call(getMembersRoomsNecessity, ['tet4enko'], 123, 456)
        .put(change(form, 'resources', [{officeId: 1, resource: null}]))
        .run();
    });
  });

  describe('updateGaps', () => {
    test('ничего не делать, если не корп', () => {
      sinon.stub(environment, 'isCorp').value(false);
      const eventFormApi = new EventFormApi();

      expectSaga(updateGaps, form)
        .not.call.fn(eventFormApi.getUsersGaps)
        .run();
    });

    test('ничего не делать, если логины невалидны', () => {
      sinon.stub(environment, 'isCorp').value(true);
      const eventFormApi = new EventFormApi();

      expectSaga(updateGaps, form)
        .provide([
          [getContext('eventFormApi'), eventFormApi],
          [
            select(getEventFormValues),
            {
              attendees: [{login: null}],
              organizer: {login: false},
              start: 123,
              end: 456
            }
          ],
          [
            select(featuresSelector),
            {
              isMixedAsRemote: true
            }
          ]
        ])
        .not.call.fn(eventFormApi.getUsersGaps)
        .run();
    });

    test('должен делать запрос с началом дня для начала/конца интервала', () => {
      sinon.stub(environment, 'isCorp').value(true);
      const eventFormApi = new EventFormApi();
      const response = {
        userGaps: 'gaps'
      };

      expectSaga(updateGaps, form)
        .provide([
          [getContext('eventFormApi'), eventFormApi],
          [call.fn(eventFormApi.getUsersGaps), response],
          [
            select(getEventFormValues),
            {
              attendees: [{login: 'tet4enko'}],
              organizer: {login: 'tavria'},
              start: 123,
              end: 456
            }
          ],
          [
            select(featuresSelector),
            {
              isMixedAsRemote: true
            }
          ]
        ])
        .call(
          [eventFormApi, eventFormApi.getUsersGaps],
          ['tet4enko', 'tavria'],
          '1970-01-01T03:00:00',
          '1970-01-01T03:00:00'
        )
        .put(
          actions.makeSetGaps({form})({gaps: response.userGaps, isMixedAsRemote: true, start: 123})
        )
        .run();
    });
  });

  describe('getMembersRoomsNecessity', () => {
    test('должен делать запрос с началом дня для начала/конца интервала', () => {
      const eventFormApi = new EventFormApi();
      const logins = ['tet4enko'];
      const start = 123;
      const end = 456;
      const response = {
        description: ''
      };

      testSaga(getMembersRoomsNecessity, logins, start, end)
        .next()
        .getContext('eventFormApi')
        .next(eventFormApi)
        .call(
          [eventFormApi, eventFormApi.getUsersRoomsNecessity],
          logins,
          '1970-01-01T03:00:00',
          '1970-01-01T03:00:00'
        )
        .next(response)
        .isDone();
    });
  });

  describe('findUsers', () => {
    test('должен получать сведения о пользователе по email', () => {
      const eventFormApi = new EventFormApi();
      const action = {
        payload: {
          loginOrEmails: 'test@ya.ru',
          resolve() {}
        },
        meta: {form}
      };
      const response = {
        users: []
      };

      return expectSaga(findUsers, action)
        .provide([
          [getContext('eventFormApi'), eventFormApi],
          [call.fn(eventFormApi.findUsersAndResources), response]
        ])
        .call.fn(eventFormApi.findUsersAndResources)
        .call(action.payload.resolve, response.users)
        .run();
    });

    test('должен запрашивать занятость полученных пользователей при shouldCheckAvailability', () => {
      const eventFormApi = new EventFormApi();
      const action = {
        payload: {
          loginOrEmails: 'test@ya.ru',
          shouldCheckAvailability: true,
          resolve() {}
        },
        meta: {form}
      };
      const response = {
        users: []
      };

      return expectSaga(findUsers, action)
        .provide([
          [getContext('eventFormApi'), eventFormApi],
          [call.fn(eventFormApi.findUsersAndResources), response]
        ])
        .put(actions.makeCheckSomeMembersAvailability({form})({emails: response.users}))
        .run();
    });

    test('должен логировать ошибку', () => {
      const error = {name: 'error'};

      return expectSaga(findUsers, {payload: {}, meta: {form}})
        .provide([[getContext('eventFormApi'), throwError(error)], [call.fn(errorReporter.send)]])
        .call([errorReporter, errorReporter.send], 'findUsers', error)
        .run();
    });
  });

  describe('findResources', () => {
    test('должен находить переговорку по идентификатору', () => {
      const eventFormApi = new EventFormApi();
      const action = {
        payload: {
          loginOrEmails: 'test@ya.ru',
          resolve() {}
        }
      };
      const response = {
        resources: []
      };
      const error = {
        name: 'error'
      };

      testSaga(findResources, action)
        .next()
        .getContext('eventFormApi')
        .next(eventFormApi)
        .call([eventFormApi, eventFormApi.findUsersAndResources], action.payload.loginOrEmails)
        .next(response)
        .call(action.payload.resolve, response.resources)
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call([errorReporter, errorReporter.send], 'findResources', error)
        .next()
        .isDone();
    });
  });

  describe('reserveResources', () => {
    test('должен бронировать переговорки для встречи', () => {
      const eventFormApi = new EventFormApi();
      const action = {
        payload: {
          reservationId: 100
        }
      };
      const error = {
        name: 'error'
      };

      testSaga(reserveResources, action)
        .next()
        .getContext('eventFormApi')
        .next(eventFormApi)
        .call([eventFormApi, eventFormApi.reserveResources], action.payload)
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call([errorReporter, errorReporter.send], 'reserveResources', error)
        .next()
        .isDone();
    });
  });

  describe('cancelResourcesReservation', () => {
    test('должен отменять бронирование переговорок', () => {
      const eventFormApi = new EventFormApi();
      const action = {
        payload: {
          reservationId: 100
        }
      };
      const error = {
        name: 'error'
      };

      testSaga(cancelResourcesReservation, action)
        .next()
        .getContext('eventFormApi')
        .next(eventFormApi)
        .call([eventFormApi, eventFormApi.cancelResourcesReservation], action.payload.reservationId)
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call([errorReporter, errorReporter.send], 'cancelResourcesReservation', error)
        .next()
        .isDone();
    });
  });

  describe('createConferenceCall', () => {
    test('должен отправлять запрос на создание телефонной или видеоконференции', () => {
      const eventFormApi = new EventFormApi();
      const confCallData = {
        phones: '1030,2040',
        duration: 15
      };
      const action = {
        payload: {
          ...confCallData,
          resolve() {},
          reject() {}
        }
      };
      const error = {
        name: 'error'
      };

      testSaga(createConferenceCall, action)
        .next()
        .getContext('eventFormApi')
        .next(eventFormApi)
        .call([eventFormApi, eventFormApi.createConferenceCall], confCallData)
        .next()
        .put(notifySuccess({message: 'notifications.conferenceCallCreated'}))
        .next()
        .call(action.payload.resolve)
        .next()
        .isDone()

        .restart()
        .next()
        .throw(error)
        .call([errorReporter, errorReporter.send], 'createConferenceCall', error)
        .next()
        .put(notifyFailure({error}))
        .next()
        .call(action.payload.reject)
        .next()
        .isDone();
    });
  });

  describe('handleFormFieldChange', () => {
    test('должен вызывать обработку изменения слоя, если поменялся id слоя на форме события', () => {
      const action = {
        meta: {
          form,
          field: 'layerId'
        },
        payload: 100
      };

      return expectSaga(handleFormFieldChange, action)
        .provide([[call.fn(handleFormLayerUpdate)]])
        .call(
          handleFormLayerUpdate,
          {
            id: action.payload
          },
          form
        )
        .run();
    });

    test('должен вызывать обработчик смены переговорок, если поменялись переговоки', () => {
      const action = {
        meta: {
          form,
          field: 'resources'
        }
      };

      return expectSaga(handleFormFieldChange, action)
        .provide([[call(handleResourcesUpdate, form)]])
        .call(handleResourcesUpdate, form)
        .run();
    });

    test('должен вызывать обработчик изменения времени события, если поменялось время начала', () => {
      const action = {
        meta: {
          form,
          field: 'start'
        }
      };

      return expectSaga(handleFormFieldChange, action)
        .put(actions.makeCheckAvailabilities({form})())
        .run();
    });

    test('должен кидать экшн getOfficesTzOffsets, если поменялось время начала', () => {
      sinon.stub(environment, 'isCorp').value(true);
      const now = Date.now();
      const action = {
        meta: {
          form,
          field: 'start'
        },
        payload: now
      };

      return expectSaga(handleFormFieldChange, action)
        .put(getOfficesTzOffsets({ts: eventDateFormat(now)}))
        .provide([
          [select(getIsSpaceshipActivated), false],
          [call.fn(updateGaps)],
          [call.fn(tryAddOffices)],
          [call.fn(updateSuggestedIntervals)]
        ])
        .call(updateGaps, form)
        .call(tryAddOffices, form)
        .call(updateSuggestedIntervals, form)
        .run();
    });

    test('не должен кидать экшн getOfficesTzOffsets, если не на корпе', () => {
      sinon.stub(environment, 'isCorp').value(false);
      const now = Date.now();
      const action = {
        meta: {
          form,
          field: 'start'
        },
        payload: now
      };

      return expectSaga(handleFormFieldChange, action)
        .not.put(getOfficesTzOffsets({ts: eventDateFormat(now)}))
        .run();
    });

    test('должен вызывать обработчик изменения времени события, если поменялось время конца', () => {
      const action = {
        meta: {
          form,
          field: 'end'
        }
      };

      return expectSaga(handleFormFieldChange, action)
        .put(actions.makeCheckAvailabilities({form})())
        .run();
    });

    test('должен вызывать обработчик изменения времени события, если поменялось значение isAllDay', () => {
      const action = {
        meta: {
          form,
          field: 'isAllDay'
        }
      };

      return expectSaga(handleFormFieldChange, action)
        .put(actions.makeCheckAvailabilities({form})())
        .run();
    });

    test('должен вызывать обработчик изменения времени события, если поменялось повторение', () => {
      const action = {
        meta: {
          form,
          field: 'repetition'
        }
      };

      return expectSaga(handleFormFieldChange, action)
        .put(actions.makeCheckAvailabilities({form})())
        .run();
    });
  });

  describe('bulkAvailabilityCheck', () => {
    test('должен вызывать проверку занятости организаторов, участников, переговорок и избранных', () => {
      const eventFormApi = new EventFormApi();
      const favoriteContacts = [{email: 'pew@ya.ru'}];
      const attendees = [{email: 'pew2@ya.ru', login: 'x'}];
      const resources = [{email: 'confroom@ya.ru'}];
      const organizer = {email: 'pew3@ya.ru'};

      const result = [
        {email: 'pew@ya.ru', availability: 'available'},
        {email: 'pew2@ya.ru', availability: 'available'},
        {email: 'pew3@ya.ru', availability: 'available'},
        {email: 'confroom@ya.ru', availability: 'available'}
      ];

      return expectSaga(bulkAvailabilityCheck, form)
        .provide([
          [getContext('eventFormApi'), eventFormApi],
          [call.fn(eventFormApi.getAvailabilities), result],
          [call.fn(getDataForAvailabilityRequest), {}, form],
          [select(getFavoriteContacts), favoriteContacts],
          [
            select(getEventFormValues),
            {
              attendees,
              organizer,
              resources
            }
          ]
        ])
        .call.fn(eventFormApi.getAvailabilities)
        .put(actions.makeCheckAvailabilitiesSuccess({form})({availabilities: result}))
        .run();
    });

    test('не должен вызывать проверку занятости организаторов, если его нет', () => {
      const eventFormApi = new EventFormApi();
      const favoriteContacts = [{email: 'pew@ya.ru'}];
      const attendees = [{email: 'pew2@ya.ru', login: 'x'}];

      const result = [
        {email: 'pew@ya.ru', availability: 'available'},
        {email: 'pew2@ya.ru', availability: 'available'},
        {email: 'pew3@ya.ru', availability: 'available'}
      ];

      return expectSaga(bulkAvailabilityCheck, form)
        .provide([
          [getContext('eventFormApi'), eventFormApi],
          [call.fn(eventFormApi.getAvailabilities), result],
          [call.fn(getDataForAvailabilityRequest), {}, form],
          [select(getFavoriteContacts), favoriteContacts],
          [select(getEventFormValues), {attendees}]
        ])
        .call.fn(eventFormApi.getAvailabilities)
        .put(actions.makeCheckAvailabilitiesSuccess({form})({availabilities: result}))
        .run();
    });

    test('должен логировать ошибку', () => {
      return expectSaga(bulkAvailabilityCheck, form)
        .provide([
          [getContext('eventFormApi'), throwError({name: 'error'})],
          [call.fn(errorReporter.send)]
        ])
        .call([errorReporter, errorReporter.send], 'bulkAvailabilityCheck', {name: 'error'})
        .run();
    });
  });

  describe('checkAvailabilities', () => {
    test('должен вызывать проверку занятости при валидных входных данных', () => {
      const action = {
        meta: {
          form,
          field: 'end'
        }
      };

      return expectSaga(checkAvailabilities, action)
        .provide([
          [
            select(getEventFormValues),
            {
              start: Date.now(),
              end: Date.now() + 1000,
              shouldCheckAvailability: true
            }
          ],
          [call(bulkAvailabilityCheck, form)]
        ])
        .call(bulkAvailabilityCheck, form)
        .run();
    });

    test('не должен вызывать проверку занятости, если конец события раньше начала', () => {
      const action = {
        meta: {
          form,
          field: 'end'
        }
      };

      return expectSaga(checkAvailabilities, action)
        .provide([
          [
            select(getEventFormValues),
            {
              start: Date.now() + 1000,
              end: Date.now(),
              shouldCheckAvailability: true
            }
          ],
          [call(bulkAvailabilityCheck, form)]
        ])
        .not.call(bulkAvailabilityCheck, form)
        .run();
    });

    test('не должен вызывать проверку занятости, если нет формы', () => {
      const action = {
        meta: {
          form,
          field: 'end'
        }
      };

      return expectSaga(checkAvailabilities, action)
        .provide([[select(getEventFormValues), null]])
        .not.call(bulkAvailabilityCheck, form)
        .run();
    });

    test('не должен вызывать проверку занятости, если нет флага "проверять занятость"', () => {
      const action = {
        meta: {
          form,
          field: 'end'
        }
      };

      return expectSaga(checkAvailabilities, action)
        .provide([
          [
            select(getEventFormValues),
            {
              start: Date.now(),
              end: Date.now() + 1000,
              shouldCheckAvailability: false
            }
          ],
          [call(bulkAvailabilityCheck, form)]
        ])
        .not.call(bulkAvailabilityCheck, form)
        .run();
    });
  });

  describe('handleLayerUpdate', () => {
    describe('успешное выполнение', () => {
      test('не должен менять данные формы, если формы нет', () => {
        return expectSaga(handleLayerUpdate, {id: 100})
          .provide([[select(getAllForms), [form]], [select(getEventFormValues)]])
          .not.put.actionType(ReduxFormActionTypes.CHANGE)
          .not.put.actionType(ReduxFormActionTypes.INITIALIZE)
          .run();
      });

      test('не должен менять данные формы, если изменился не текущий слой', () => {
        return expectSaga(handleLayerUpdate, {id: 100})
          .provide([[select(getAllForms), [form]], [select(getEventFormValues), {layerId: 1}]])
          .not.put.actionType(ReduxFormActionTypes.CHANGE)
          .not.put.actionType(ReduxFormActionTypes.INITIALIZE)
          .run();
      });

      describe('изменение поля notifications в соответствии с настройками слоя', () => {
        test('должен изменять поле, если это создание события и поле ещё не менялось', () => {
          const action = {
            id: 100
          };
          const formValues = {
            layerId: 100,
            notifications: []
          };
          const formInitialValues = {
            name: form,
            notifications: []
          };
          const layer = new LayerRecord({
            id: 100,
            notifications: new List([{offset: '-30m', channel: 'email'}])
          });

          return expectSaga(handleLayerUpdate, action)
            .provide([
              [select(getAllForms), [form]],
              [select(getEventFormValues), formValues],
              [select(getEventFormInitialValues), formInitialValues],
              [select(getLayerById, action.id), layer],
              [call.fn(tryChangeOthersCanView)]
            ])
            .put(change(form, 'notifications', [{offset: '-30m', channel: 'email'}]))
            .put(
              initialize(
                form,
                {name: form, notifications: [{offset: '-30m', channel: 'email'}]},
                {keepDirty: true}
              )
            )
            .run();
        });

        test('не должен изменять поле, если это редактирование события', () => {
          const action = {
            id: 100
          };
          const formValues = {
            layerId: 100,
            id: 1,
            notifications: []
          };
          const formInitialValues = {
            name: form,
            notifications: []
          };
          const layer = new LayerRecord({
            id: 100,
            notifications: new List([{offset: '-30m', channel: 'email'}])
          });

          return expectSaga(handleLayerUpdate, action)
            .provide([
              [select(getAllForms), [form]],
              [select(getEventFormValues), formValues],
              [select(getEventFormInitialValues), formInitialValues],
              [select(getLayerById, action.id), layer],
              [call.fn(tryChangeOthersCanView)]
            ])
            .not.put(change(form, 'notifications', [{offset: '-30m', channel: 'email'}]))
            .not.put(
              initialize(
                form,
                {name: form, notifications: [{offset: '-30m', channel: 'email'}]},
                {keepDirty: true}
              )
            )
            .run();
        });

        test('не должен изменять поле, если поле уже менялось', () => {
          const action = {
            id: 100
          };
          const formValues = {
            layerId: 100,
            notifications: []
          };
          const formInitialValues = {
            name: form,
            notifications: [{offset: '-60m', channel: 'email'}]
          };
          const layer = new LayerRecord({
            id: 100,
            notifications: new List([{offset: '-30m', channel: 'email'}])
          });

          return expectSaga(handleLayerUpdate, action)
            .provide([
              [select(getAllForms), [form]],
              [select(getEventFormValues), formValues],
              [select(getEventFormInitialValues), formInitialValues],
              [select(getLayerById, action.id), layer],
              [call.fn(tryChangeOthersCanView)]
            ])
            .not.put(change(form, 'notifications', [{offset: '-30m', channel: 'email'}]))
            .not.put(
              initialize(
                form,
                {name: form, notifications: [{offset: '-30m', channel: 'email'}]},
                {keepDirty: true}
              )
            )
            .run();
        });
      });

      describe('изменение поля othersCanView в соответствии с настройками слоя', () => {
        test('должен изменять поле, если это создание события и поле ещё не менялось', () => {
          const action = {
            id: 100
          };
          const formValues = {
            layerId: 100,
            othersCanView: true
          };
          const formInitialValues = {
            name: form,
            othersCanView: true
          };
          const layer = new LayerRecord({
            id: 100,
            isEventsClosedByDefault: true
          });

          return expectSaga(handleLayerUpdate, action)
            .provide([
              [select(getAllForms), [form]],
              [select(getEventFormValues), formValues],
              [select(getEventFormInitialValues), formInitialValues],
              [select(getLayerById, action.id), layer],
              [call.fn(tryChangeNotifications)]
            ])
            .put(change(form, 'othersCanView', false))
            .put(initialize(form, {name: form, othersCanView: false}, {keepDirty: true}))
            .run();
        });

        test('не должен изменять поле, если это редактирование события', () => {
          const action = {
            id: 100
          };
          const formValues = {
            layerId: 100,
            id: 1,
            othersCanView: true
          };
          const formInitialValues = {
            name: form,
            othersCanView: true
          };
          const layer = new LayerRecord({
            id: 100,
            isEventsClosedByDefault: true
          });

          return expectSaga(handleLayerUpdate, action)
            .provide([
              [select(getAllForms), [form]],
              [select(getEventFormValues), formValues],
              [select(getEventFormInitialValues), formInitialValues],
              [select(getLayerById, action.id), layer],
              [call.fn(tryChangeNotifications)]
            ])
            .not.put(change(form, 'othersCanView', false))
            .not.put(initialize(form, {name: form, othersCanView: false}, {keepDirty: true}))
            .run();
        });

        test('не должен изменять поле, если поле уже менялось', () => {
          const action = {
            id: 100
          };
          const formValues = {
            layerId: 100,
            othersCanView: true
          };
          const formInitialValues = {
            name: form,
            othersCanView: false
          };
          const layer = new LayerRecord({
            id: 100,
            isEventsClosedByDefault: true
          });

          return expectSaga(handleLayerUpdate, action)
            .provide([
              [select(getAllForms), [form]],
              [select(getEventFormValues), formValues],
              [select(getEventFormInitialValues), formInitialValues],
              [select(getLayerById, action.id), layer],
              [call.fn(tryChangeNotifications)]
            ])
            .not.put(change(form, 'othersCanView', false))
            .not.put(initialize(form, {name: form, othersCanView: false}, {keepDirty: true}))
            .run();
        });
      });
    });

    describe('неуспешное выполнение', () => {
      test('должен логировать ошибку', () => {
        return expectSaga(handleLayerUpdate, {id: 100})
          .provide([
            [select(getAllForms), [form]],
            [select(getEventFormValues), throwError({name: 'error'})],
            [call.fn(errorReporter.send)]
          ])
          .call([errorReporter, errorReporter.send], 'handleLayerUpdate', {name: 'error'})
          .run();
      });
    });
  });

  describe('handleOrganizerUpdate', () => {
    test('должен менять организатора на форме события', () => {
      const organizer = {
        email: 'pepew@ya.ru'
      };
      const initialOrganizer = {
        email: 'pewpew@ya.ru'
      };
      const newOrganizer = {
        email: '123@345.tt'
      };
      const action = {
        payload: {
          organizer: newOrganizer
        },
        meta: {form}
      };

      return expectSaga(handleOrganizerUpdate, action)
        .provide([
          [
            select(getEventFormValues),
            {
              organizer,
              attendees: [],
              optionalAttendees: []
            }
          ],
          [
            select(getEventFormInitialValues),
            {
              organizer: initialOrganizer,
              attendees: [],
              optionalAttendees: []
            }
          ]
        ])
        .put(change(form, 'organizer', newOrganizer))
        .run();
    });

    test('не должен удалять организатора', () => {
      const newOrganizer = null;
      const action = {
        payload: {
          organizer: newOrganizer
        },
        meta: {form}
      };

      return expectSaga(handleOrganizerUpdate, action)
        .not.put(change(form, 'organizer', newOrganizer))
        .run();
    });

    test('должен менять организатора на начального в случае ограничения по переговоркам', () => {
      const organizer = {
        email: 'pepew@ya.ru'
      };

      const initialOrganizer = {
        email: 'pewpew@ya.ru'
      };

      const newOrganizer = {
        email: '123@345.tt'
      };

      const action = {
        payload: {
          organizer: newOrganizer
        },
        meta: {form}
      };

      return expectSaga(handleOrganizerUpdate, action)
        .provide([
          [
            select(getEventFormValues),
            {
              organizer,
              resources: [
                {
                  resource: {
                    type: 'restricted-area'
                  }
                }
              ],
              attendees: [],
              optionalAttendees: []
            }
          ],
          [
            select(getEventFormInitialValues),
            {
              organizer: initialOrganizer,
              resources: [
                {
                  resource: {
                    type: 'room'
                  }
                }
              ],
              attendees: [],
              optionalAttendees: []
            }
          ]
        ])
        .put(change(form, 'organizer', initialOrganizer))
        .run();
    });

    test('не должен менять орга на начальное значение при ограничении по переговоркам и обновлении информации об орге', () => {
      const organizer = {
        email: 'pepew@ya.ru'
      };

      const initialOrganizer = {
        email: 'pewpew@ya.ru'
      };

      const newOrganizer = {
        email: 'pewpew@ya.ru',
        availability: 'ololo'
      };

      const action = {
        payload: {
          organizer: newOrganizer
        },
        meta: {form}
      };

      return expectSaga(handleOrganizerUpdate, action)
        .provide([
          [
            select(getEventFormValues),
            {
              organizer,
              resources: [
                {
                  resource: {
                    type: 'restricted-area'
                  }
                }
              ],
              attendees: [],
              optionalAttendees: []
            }
          ],
          [
            select(getEventFormInitialValues),
            {
              organizer: initialOrganizer,
              resources: [
                {
                  resource: {
                    type: 'room'
                  }
                }
              ],
              attendees: [],
              optionalAttendees: []
            }
          ]
        ])
        .put(change(form, 'organizer', newOrganizer))
        .run();
    });

    test('не должен менять организатора, если организатора не изменяли и нет ограничений по переговоркам', () => {
      const organizer = {
        email: 'pepew@ya.ru'
      };

      const initialOrganizer = {
        email: 'pewpew@ya.ru'
      };

      const action = {
        payload: {organizer},
        meta: {form}
      };

      return expectSaga(handleOrganizerUpdate, action)
        .provide([
          [
            select(getEventFormValues),
            {
              organizer,
              resources: [
                {
                  resource: {
                    type: 'room'
                  }
                }
              ],
              attendees: []
            }
          ],
          [
            select(getEventFormInitialValues),
            {
              organizer: initialOrganizer,
              resources: [
                {
                  resource: {
                    type: 'room'
                  }
                }
              ],
              attendees: []
            }
          ]
        ])
        .not.put(change(form, 'organizer', initialOrganizer))
        .run();
    });

    test('должен удалять организатора из участников, если он был участником', () => {
      const organizer = {
        email: 'pepew@ya.ru'
      };

      const initialOrganizer = {
        email: 'pewpew@ya.ru'
      };

      const newOrganizer = {
        email: '123@345.tt'
      };

      const action = {
        payload: {
          organizer: newOrganizer
        },
        meta: {form}
      };

      return expectSaga(handleOrganizerUpdate, action)
        .provide([
          [
            select(getEventFormValues),
            {
              organizer,
              resources: [
                {
                  resource: {
                    type: 'room'
                  }
                }
              ],
              attendees: [newOrganizer],
              optionalAttendees: []
            }
          ],
          [
            select(getEventFormInitialValues),
            {
              organizer: initialOrganizer,
              resources: [
                {
                  resource: {
                    type: 'room'
                  }
                }
              ],
              attendees: [],
              optionalAttendees: []
            }
          ]
        ])
        .put(change(form, 'attendees', []))
        .run();
    });

    test('должен удалять организатора из  опциональных участников, если он был опциональным участником', () => {
      const organizer = {
        email: 'pepew@ya.ru'
      };

      const initialOrganizer = {
        email: 'pewpew@ya.ru'
      };

      const newOrganizer = {
        email: '123@345.tt'
      };

      const action = {
        payload: {
          organizer: newOrganizer
        },
        meta: {form}
      };

      return expectSaga(handleOrganizerUpdate, action)
        .provide([
          [
            select(getEventFormValues),
            {
              organizer,
              resources: [
                {
                  resource: {
                    type: 'room'
                  }
                }
              ],
              attendees: [],
              optionalAttendees: [newOrganizer]
            }
          ],
          [
            select(getEventFormInitialValues),
            {
              organizer: initialOrganizer,
              resources: [
                {
                  resource: {
                    type: 'room'
                  }
                }
              ],
              attendees: [],
              optionalAttendees: []
            }
          ]
        ])
        .put(change(form, 'optionalAttendees', []))
        .run();
    });

    test('должен добавлять в участники предыдущего организатора при редактировании события', () => {
      const organizer = {
        email: 'pepew@ya.ru'
      };

      const initialOrganizer = {
        email: 'pewpew@ya.ru'
      };

      const newOrganizer = {
        email: '123@345.tt'
      };

      const action = {
        payload: {
          organizer: newOrganizer
        },
        meta: {form}
      };

      return expectSaga(handleOrganizerUpdate, action)
        .provide([
          [
            select(getEventFormValues),
            {
              organizer,
              id: 100,
              resources: [
                {
                  resource: {
                    type: 'room'
                  }
                }
              ],
              attendees: [newOrganizer],
              optionalAttendees: []
            }
          ],
          [
            select(getEventFormInitialValues),
            {
              organizer: initialOrganizer,
              id: 100,
              resources: [
                {
                  resource: {
                    type: 'room'
                  }
                }
              ],
              attendees: [],
              optionalAttendees: []
            }
          ]
        ])
        .put(change(form, 'attendees', [organizer]))
        .run();
    });

    test('не должен добавлять в участники предыдущего организатора при создании события', () => {
      const organizer = {
        email: 'pepew@ya.ru'
      };

      const initialOrganizer = {
        email: 'pewpew@ya.ru'
      };

      const newOrganizer = {
        email: '123@345.tt'
      };

      const action = {
        payload: {
          organizer: newOrganizer
        },
        meta: {form}
      };

      return expectSaga(handleOrganizerUpdate, action)
        .provide([
          [
            select(getEventFormValues),
            {
              organizer,
              resources: [
                {
                  resource: {
                    type: 'room'
                  }
                }
              ],
              attendees: [],
              optionalAttendees: []
            }
          ],
          [
            select(getEventFormInitialValues),
            {
              organizer: initialOrganizer,
              resources: [
                {
                  resource: {
                    type: 'room'
                  }
                }
              ],
              attendees: [],
              optionalAttendees: []
            }
          ]
        ])
        .not.put(change(form, 'attendees', [organizer]))
        .run();
    });

    test('не должен менять состав участников при обновлении информации об орге', () => {
      const organizer = {
        email: 'pepew@ya.ru'
      };

      const initialOrganizer = {
        email: 'pewpew@ya.ru'
      };

      const newOrganizer = {
        email: 'pewpew@ya.ru',
        availability: 'ololo'
      };

      const action = {
        payload: {
          organizer: newOrganizer
        },
        meta: {form}
      };

      return expectSaga(handleOrganizerUpdate, action)
        .provide([
          [
            select(getEventFormValues),
            {
              organizer,
              resources: [
                {
                  resource: {
                    type: 'restricted-area'
                  }
                }
              ],
              attendees: [initialOrganizer],
              optionalAttendees: []
            }
          ],
          [
            select(getEventFormInitialValues),
            {
              organizer: initialOrganizer,
              resources: [
                {
                  resource: {
                    type: 'room'
                  }
                }
              ],
              attendees: [],
              optionalAttendees: []
            }
          ]
        ])
        .not.put(change(form, 'attendees', [initialOrganizer]))
        .run();
    });

    test('должен проверять занятость организатора после его смены', () => {
      const organizer = {
        email: 'pepew@ya.ru'
      };

      const initialOrganizer = {
        email: 'pewpew@ya.ru'
      };

      const newOrganizer = {
        email: '123@345.tt'
      };

      const action = {
        payload: {
          organizer: newOrganizer
        },
        meta: {form}
      };

      return expectSaga(handleOrganizerUpdate, action)
        .provide([
          [
            select(getEventFormValues),
            {
              organizer,
              id: 100,
              resources: [
                {
                  resource: {
                    type: 'room'
                  }
                }
              ],
              attendees: [newOrganizer],
              optionalAttendees: []
            }
          ],
          [
            select(getEventFormInitialValues),
            {
              organizer: initialOrganizer,
              id: 100,
              resources: [
                {
                  resource: {
                    type: 'room'
                  }
                }
              ],
              attendees: [],
              optionalAttendees: []
            }
          ]
        ])
        .put(actions.makeCheckOrganizerAvailability({form})())
        .run();
    });

    test('должен логировать ошибку', () => {
      return expectSaga(handleOrganizerUpdate, {
        payload: {
          organizer: {
            email: '23'
          }
        },
        meta: {form}
      })
        .provide([
          [select(getEventFormValues), throwError({name: 'error'})],
          [call.fn(errorReporter.send)]
        ])
        .call([errorReporter, errorReporter.send], 'handleOrganizerUpdate', {name: 'error'})
        .run();
    });
  });

  describe('changeAttendeesListWithOrganizerChanged', () => {
    test('Должен добавлять орга в участники при редактировании встречи', () => {
      const newOrganizer = {
        email: 'new-org@ya.ru'
      };

      const params = {
        organizer: {
          email: 'old-org@ya.ru'
        },
        attendees: [
          {
            email: 'p1@ya.ru'
          },
          {
            email: 'p2@ya.ru'
          }
        ],
        optionalAttendees: [],
        id: 1
      };

      return expectSaga(changeAttendeesListWithOrganizerChanged, form, params, newOrganizer)
        .put(change(form, 'attendees', params.attendees.concat(params.organizer)))
        .run();
    });

    test('Не должен добавлять орга в участники при создании встречи', () => {
      const newOrganizer = {
        email: 'new-org@ya.ru'
      };

      const params = {
        organizer: {
          email: 'old-org@ya.ru'
        },
        attendees: [
          {
            email: 'p1@ya.ru'
          },
          {
            email: 'p2@ya.ru'
          }
        ],
        optionalAttendees: []
      };

      return expectSaga(changeAttendeesListWithOrganizerChanged, form, params, newOrganizer)
        .not.put(change(form, 'attendees', params.attendees.concat(params.organizer)))
        .run();
    });

    test('Должен удалять орга из участников при создании встречи', () => {
      const newOrganizer = {
        email: 'p1@ya.ru'
      };

      const params = {
        organizer: {
          email: 'old-org@ya.ru'
        },
        attendees: [
          {
            email: 'p1@ya.ru'
          },
          {
            email: 'p2@ya.ru'
          }
        ],
        optionalAttendees: []
      };

      return expectSaga(changeAttendeesListWithOrganizerChanged, form, params, newOrganizer)
        .put(change(form, 'attendees', [{email: 'p2@ya.ru'}]))
        .run();
    });

    test('Должен удалять орга из участников при редактировании встречи, если он был участником', () => {
      const newOrganizer = {
        email: 'p1@ya.ru'
      };

      const params = {
        organizer: {
          email: 'old-org@ya.ru'
        },
        attendees: [
          {
            email: 'p1@ya.ru'
          },
          {
            email: 'p2@ya.ru'
          }
        ],
        optionalAttendees: [],
        id: 1
      };

      return expectSaga(changeAttendeesListWithOrganizerChanged, form, params, newOrganizer)
        .put(change(form, 'attendees', [{email: 'p2@ya.ru'}, {email: 'old-org@ya.ru'}]))
        .run();
    });

    test('Должен удалять орга из опциональных участников при редактировании встречи, если он был опциональным участником', () => {
      const newOrganizer = {
        email: 'p1@ya.ru'
      };

      const params = {
        organizer: {
          email: 'old-org@ya.ru'
        },
        attendees: [],
        optionalAttendees: [
          {
            email: 'p1@ya.ru'
          },
          {
            email: 'p2@ya.ru'
          }
        ],
        id: 1
      };

      return expectSaga(changeAttendeesListWithOrganizerChanged, form, params, newOrganizer)
        .put(change(form, 'optionalAttendees', [{email: 'p2@ya.ru'}, {email: 'old-org@ya.ru'}]))
        .run();
    });
  });

  describe('handleResourcesUpdate', () => {
    test('не должен падать с ошибкой при отсутствии данных формы', () => {
      const initialOrganizer = {
        email: 'pewpew@ya.ru'
      };

      return expectSaga(handleResourcesUpdate, form)
        .provide([[select(getEventFormValues), null], [select(getEventFormInitialValues), null]])
        .not.put(actions.makeChangeOrganizer({form})({organizer: initialOrganizer}))
        .run();
    });

    test('должен менять организатора на начального в случае ограничения по переговоркам', () => {
      const organizer = {
        email: 'pepew@ya.ru'
      };

      const initialOrganizer = {
        email: 'pewpew@ya.ru'
      };

      return expectSaga(handleResourcesUpdate, form)
        .provide([
          [
            select(getEventFormValues),
            {
              organizer,
              resources: [
                {
                  resource: {
                    type: 'prohibited-room'
                  }
                }
              ]
            }
          ],
          [
            select(getEventFormInitialValues),
            {
              organizer: initialOrganizer,
              resources: [
                {
                  resource: {
                    type: 'prohibited-room'
                  }
                },
                {
                  resource: {
                    type: 'prohibited-room'
                  }
                }
              ]
            }
          ]
        ])
        .put(actions.makeChangeOrganizer({form})({organizer: initialOrganizer}))
        .run();
    });

    test('не должен падать с ошибкой при отсутствии организатора', () => {
      const initialOrganizer = {
        email: 'pewpew@ya.ru'
      };

      return expectSaga(handleResourcesUpdate, form)
        .provide([
          [
            select(getEventFormValues),
            {
              resources: [
                {
                  resource: {
                    type: 'prohibited-room'
                  }
                }
              ]
            }
          ],
          [
            select(getEventFormInitialValues),
            {
              resources: [
                {
                  resource: {
                    type: 'prohibited-room'
                  }
                },
                {
                  resource: {
                    type: 'prohibited-room'
                  }
                }
              ]
            }
          ]
        ])
        .not.put(actions.makeChangeOrganizer({form})({organizer: initialOrganizer}))
        .run();
    });

    test('не должен пытаться менять организатора, если его не меняли', () => {
      const organizer = {
        email: 'pewpew@ya.ru'
      };

      const initialOrganizer = {
        email: 'pewpew@ya.ru'
      };

      return expectSaga(handleResourcesUpdate, form)
        .provide([
          [
            select(getEventFormValues),
            {
              organizer,
              resources: [
                {
                  resource: {
                    type: 'prohibited-room'
                  }
                }
              ]
            }
          ],
          [
            select(getEventFormInitialValues),
            {
              organizer: initialOrganizer,
              resources: [
                {
                  resource: {
                    type: 'prohibited-room'
                  }
                },
                {
                  resource: {
                    type: 'prohibited-room'
                  }
                }
              ]
            }
          ]
        ])
        .not.put(actions.makeChangeOrganizer({form})({organizer: initialOrganizer}))
        .run();
    });

    test('не должен пытаться менять организатора, если нет ограничения по переговокам', () => {
      const organizer = {
        email: 'pewpe@ya.ru'
      };

      const initialOrganizer = {
        email: 'pewpew@ya.ru'
      };

      return expectSaga(handleResourcesUpdate, form)
        .provide([
          [
            select(getEventFormValues),
            {
              organizer,
              resources: [
                {
                  resource: {
                    type: 'room'
                  }
                }
              ]
            }
          ],
          [
            select(getEventFormInitialValues),
            {
              organizer: initialOrganizer,
              resources: [
                {
                  resource: {
                    type: 'prohibited-room'
                  }
                },
                {
                  resource: {
                    type: 'prohibited-room'
                  }
                }
              ]
            }
          ]
        ])
        .not.put(actions.makeChangeOrganizer({form})({organizer: initialOrganizer}))
        .run();
    });
  });

  describe('updateSuggestedIntervals', () => {
    test('не должен падать с ошибкой при отсутствии данных формы', () => {
      const suggestMeetingTimesApi = new SuggestMeetingTimesApi();
      sinon.stub(environment, 'isCorp').value(true);

      return expectSaga(updateSuggestedIntervals, form)
        .provide([
          [select(getIsSpaceshipActivated), true],
          [select(getEventFormInitialValues), null],
          [select(getEventFormValues), null],
          [getContext('suggestMeetingTimesApi'), suggestMeetingTimesApi],
          [call.fn(suggestMeetingTimesApi.suggestMeetingTimes), {intervals: []}]
        ])
        .put(actions.makeSetSuggestedIntervals({form})({intervals: []}))
        .not.call([suggestMeetingTimesApi, suggestMeetingTimesApi.suggestMeetingTimes])
        .run();
    });

    test('не должен делать ничего, если не активирован космолет', () => {
      const suggestMeetingTimesApi = new SuggestMeetingTimesApi();
      sinon.stub(environment, 'isCorp').value(true);

      return expectSaga(updateSuggestedIntervals, form)
        .provide([
          [select(getIsSpaceshipActivated), false],
          [getContext('suggestMeetingTimesApi'), suggestMeetingTimesApi]
        ])
        .not.call([suggestMeetingTimesApi, suggestMeetingTimesApi.suggestMeetingTimes], {})
        .run();
    });

    test('успешное выполнение', () => {
      const suggestMeetingTimesApi = new SuggestMeetingTimesApi();
      sinon.stub(environment, 'isCorp').value(true);

      return expectSaga(updateSuggestedIntervals, form)
        .provide([
          [select(getIsSpaceshipActivated), true],
          [select(getEventFormInitialValues), {attendees: [], id: 1, totalAttendees: 10}],
          [
            select(getEventFormValues),
            {
              organizer: {email: 'tavria@yandex-team.ru'},
              attendees: [{email: 'tet4enko@yandex-team.ru'}, {email: 'birhoff@yandex-team.ru'}],
              isAllDay: false,
              start: 1638378000000,
              end: 1638381600000,
              resources: []
            }
          ],
          [getContext('suggestMeetingTimesApi'), suggestMeetingTimesApi],
          [call.fn(suggestMeetingTimesApi.suggestMeetingTimes), {intervals: []}]
        ])
        .put(actions.makeSetSuggestedIntervals({form})({intervals: []}))
        .call([suggestMeetingTimesApi, suggestMeetingTimesApi.suggestMeetingTimes], {
          mode: 'any-room',
          eventStart: '2021-12-01T20:00:00',
          eventEnd: '2021-12-01T21:00:00',
          ignoreUsersEvents: false,
          numberOfOptions: 3,
          users: ['tet4enko@yandex-team.ru', 'birhoff@yandex-team.ru', 'tavria@yandex-team.ru'],
          offices: []
        })
        .run();
    });
  });

  describe('getAttendees', () => {
    describe('успешное выполнение', () => {
      const eventFormApi = new EventFormApi();
      const action = {
        payload: {
          eventId: 1
        },
        meta: {form}
      };
      const response = {
        attendees: []
      };
      const activatedEvent = new Map({id: 1});
      activatedEvent.uuid = '123';

      const defaultProviders = [
        [select(getIsLoadingAttendees), false],
        [getContext('eventFormApi'), eventFormApi],
        [select(getEventFormInitialValues), {attendees: [], id: 1, totalAttendees: 10}],
        [call.fn(eventFormApi.getAttendees), response],
        [select(getEventFormValues), {id: 1}]
      ];

      test('должен делать запрос за участниками', () => {
        return expectSaga(getAttendees, action)
          .provide(defaultProviders)
          .call([eventFormApi, eventFormApi.getAttendees], action.payload.eventId)
          .run();
      });

      test('не должен делать запрос за участниками, если они уже загружены', () => {
        return expectSaga(getAttendees, action)
          .provide([
            defaultProviders[0],
            [select(getEventFormInitialValues), {attendees: [], id: 1, totalAttendees: 0}],
            defaultProviders[2],
            defaultProviders[3]
          ])
          .not.call([eventFormApi, eventFormApi.getAttendees], action.payload.eventId)
          .run();
      });

      test('не должен делать запрос за участниками, если идёт их загрузка', () => {
        return expectSaga(getAttendees, action)
          .provide([...defaultProviders.slice(1), [select(getIsLoadingAttendees), true]])
          .not.call([eventFormApi, eventFormApi.getAttendees], action.payload.eventId)
          .run();
      });

      test('должен записывать полученных участников в стейт', () => {
        return expectSaga(getAttendees, action)
          .provide(defaultProviders)
          .put(change(form, 'attendees', response.attendees))
          .run();
      });

      test('не должен записывать полученных участников в стейт, если изменилась форма', () => {
        return expectSaga(getAttendees, action)
          .provide([...defaultProviders.slice(0, 4), [select(getEventFormValues), {id: 2}]])
          .not.put(change(form, 'attendees', response.attendees))
          .run();
      });

      test('не должен записывать полученных участников в стейт, если нет формы', () => {
        return expectSaga(getAttendees, action)
          .provide([...defaultProviders.slice(0, 4), [select(getEventFormValues), null]])
          .not.put(change(form, 'attendees', response.attendees))
          .run();
      });

      test('должен добавлять новых участников в конец списка', () => {
        const attendees = [{email: 'qqq'}, {email: 'www'}];
        const receivedAttendees = [{email: 'eee'}, {email: 'rrr'}];

        return expectSaga(getAttendees, action)
          .provide([
            defaultProviders[0],
            defaultProviders[1],
            [select(getEventFormInitialValues), {attendees, id: 1}],
            [call.fn(eventFormApi.getAttendees), {attendees: receivedAttendees}],
            defaultProviders[4]
          ])
          .put(change(form, 'attendees', [...attendees, ...receivedAttendees]))
          .run();
      });

      test('должен сохранять порядок среди уже имеющихся участников', () => {
        const attendees = [{email: 'qqq'}, {email: 'www'}];
        const receivedAttendees = [{email: 'www'}, {email: 'eee'}, {email: 'qqq'}, {email: 'rrr'}];

        return expectSaga(getAttendees, action)
          .provide([
            defaultProviders[0],
            defaultProviders[1],
            [select(getEventFormInitialValues), {attendees, id: 1}],
            [call.fn(eventFormApi.getAttendees), {attendees: receivedAttendees}],
            defaultProviders[4]
          ])
          .put(
            change(form, 'attendees', [
              {email: 'qqq'},
              {email: 'www'},
              {email: 'eee'},
              {email: 'rrr'}
            ])
          )
          .run();
      });

      test('должен перезаписывать начальных участников', () => {
        return expectSaga(getAttendees, action)
          .provide(defaultProviders)
          .put(
            initialize(
              form,
              {
                id: 1,
                attendees: response.attendees,
                totalAttendees: 10
              },
              {keepDirty: true}
            )
          )
          .run();
      });

      test('не должен перезаписывать начальных участников, если изменилась форма', () => {
        return expectSaga(getAttendees, action)
          .provide([...defaultProviders.slice(0, 4), [select(getEventFormValues), {id: 2}]])
          .not.put(initialize(form, {attendees: response.attendees}, {keepDirty: true}))
          .run();
      });

      test('не должен перезаписывать начальных участников, если нет формы', () => {
        return expectSaga(getAttendees, action)
          .provide([...defaultProviders.slice(0, 4), [select(getEventFormValues), null]])
          .not.put(initialize(form, {attendees: response.attendees}, {keepDirty: true}))
          .run();
      });
    });

    describe('неуспешное выполнение', () => {
      const action = {
        payload: {
          eventId: 1
        },
        meta: {form}
      };
      const defaultProviders = [
        [getContext('eventFormApi'), throwError({name: 'error'})],
        [call.fn(errorReporter.send)]
      ];

      test('должен логировать ошибку', () => {
        return expectSaga(getAttendees, action)
          .provide(defaultProviders)
          .call([errorReporter, errorReporter.send], 'getAttendees', {name: 'error'})
          .run();
      });

      test('должен показывать нотификацию', () => {
        return expectSaga(getAttendees, action)
          .provide(defaultProviders)
          .put(notifyFailure({error: {name: 'error'}}))
          .run();
      });
    });
  });

  describe('addResource', () => {
    test('должен добавлять переговорку в событие', () => {
      return expectSaga(addResource, {meta: {form}})
        .provide([
          [select(getEventFormValues), {}],
          [select(getCurrentUser), {officeId: 777}],
          [select(getOffices), []],
          [call.fn(getNextOfficeId), 10]
        ])
        .put(
          arrayPush(form, 'resources', {
            resource: null,
            officeId: 10
          })
        )
        .run();
    });
    test('не должен ломаться без формы', () => {
      return expectSaga(addResource, {meta: {form}})
        .provide([
          [select(getEventFormValues), null],
          [select(getCurrentUser), {officeId: 777}],
          [select(getOffices), []],
          [call.fn(getNextOfficeId), 10]
        ])
        .run();
    });
    test('должен логировать ошибку', () => {
      const error = {name: 'error'};

      return expectSaga(addResource, {meta: {form}})
        .provide([[select(getEventFormValues), throwError(error)]])
        .call([errorReporter, errorReporter.send], 'addResource', error)
        .run();
    });
  });

  describe('injectTelemostConferenceLink', () => {
    test('должен вставлять ссылку в нужную форму', () => {
      const uri = Symbol();
      const additionalDescriptionText = 'Some text';

      return expectSaga(injectTelemostConferenceLink, {uri, form})
        .provide([
          [select(getEventFormValues), {description: ''}],
          [call.fn(i18n.get), additionalDescriptionText]
        ])
        .call([i18n, i18n.get], 'event', 'telemostLinkAdditionalDescription', {link: uri})
        .put(change(form, 'description', additionalDescriptionText))
        .run();
    });

    test('должен репортить ошибку', () => {
      const error = {name: 'error'};

      return expectSaga(injectTelemostConferenceLink, {form})
        .provide([[select(getEventFormValues), throwError(error)]])
        .call([errorReporter, errorReporter.send], 'injectTelemostConferenceLink', error)
        .run();
    });
  });

  describe('telemostLinkInjector', () => {
    beforeEach(() => {
      sinon.stub(config.domainConfig, 'enableTelemost').value(true);
    });

    test('не должен ничего делать, если не включен в конфиге', () => {
      sinon.stub(config.domainConfig, 'enableTelemost').value(false);

      testSaga(telemostLinkInjector)
        .next()
        .isDone();
    });

    test('должен получать ссылку на конференцию, дожидаться появления формы и вызывать вставку ссылки', () => {
      const eventFormApi = new EventFormApi();
      const uri = Symbol();
      const initFormAction = {meta: {form}};

      makeActionPatternChecker.mockReturnValue('some string');

      testSaga(telemostLinkInjector)
        .next()
        .getContext('eventFormApi')
        .next(eventFormApi)
        .call([eventFormApi, eventFormApi.getTelemostConferenceLink])
        .next({uri})
        .take(makeActionPatternChecker(ReduxFormActionTypes.INITIALIZE, null, {keepDirty: false}))
        .next(initFormAction)
        .fork(injectTelemostConferenceLink, {form, uri})
        .next()
        .call([eventFormApi, eventFormApi.getTelemostConferenceLink])
        .next({uri})
        .take(makeActionPatternChecker(ReduxFormActionTypes.INITIALIZE, null, {keepDirty: false}))
        .next(initFormAction)
        .fork(injectTelemostConferenceLink, {form, uri})
        .next()
        .finish();
    });

    test('должен репортить ошибку и ретраить получение ссылки на конференцию раз в минуту', () => {
      const eventFormApi = new EventFormApi();
      const error = {};

      makeActionPatternChecker.mockReturnValue('some string');

      testSaga(telemostLinkInjector)
        .next()
        .getContext('eventFormApi')
        .next(eventFormApi)
        .throw(error)
        .call([errorReporter, errorReporter.send], 'telemostLinkInjector', error)
        .next()
        .call(delay, 1000 * 60)
        .next()
        .finish();
    });
  });
});

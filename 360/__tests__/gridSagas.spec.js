import {expectSaga} from 'redux-saga-test-plan';
import {call, select} from 'redux-saga-test-plan/matchers';
import {throwError} from 'redux-saga-test-plan/providers';
import moment from 'moment';

import i18n from 'utils/i18n';
import SagaErrorReporter from 'utils/SagaErrorReporter';
import * as eventsActions from 'features/events/eventsActions';
import * as todoActions from 'features/todo/todoActions';
import {getUserOrResourceInfo} from 'features/roomCard/roomCardActions';
import {notificationHelpers, notifyFailure} from 'features/notifications/notificationsActions';

import {getGridPeriod, getUserFromUrl} from '../gridSelectors';
import * as gridSagas from '../gridSagas';
import * as gridActions from '../gridActions';

const errorReporter = new SagaErrorReporter('grid');

describe('gridSagas', () => {
  beforeEach(() => {
    jest.spyOn(notificationHelpers, 'generateId').mockReturnValue('id');
  });

  describe('getUserInfo', () => {
    test('должен запрашивать информацию о юзере, если он есть в урле', () => {
      const user = {login: 'tavria'};
      return expectSaga(gridSagas.getUserInfo)
        .provide([[select(getUserFromUrl), user]])
        .put(getUserOrResourceInfo(user))
        .run();
    });

    test('не должен запрашивать информацию о юзере, если его нет в урле', () => {
      const user = null;
      return expectSaga(gridSagas.getUserInfo)
        .provide([[select(getUserFromUrl), user]])
        .not.put(getUserOrResourceInfo)
        .run();
    });
  });

  describe('loadEvents, для текущего пользователя', () => {
    const providers = [
      [
        select(getGridPeriod, {delta: -1}),
        {start: Number(moment('2018-10-15')), end: Number(moment('2018-10-21'))}
      ],
      [
        select(getGridPeriod, {delta: 1}),
        {start: Number(moment('2018-10-29')), end: Number(moment('2018-11-04'))}
      ],
      [
        select(getGridPeriod),
        {start: Number(moment('2018-10-22')), end: Number(moment('2018-10-28'))}
      ],
      [select(getUserFromUrl), null]
    ];

    describe('успешное выполнение', () => {
      const getEventsDoneAction = eventsActions.getEventsDone({
        from: Number(moment('2018-10-22')),
        to: Number(moment('2018-10-28'))
      });

      test('должен запрашивать события для предыдущего периода', () => {
        return expectSaga(gridSagas.loadEvents)
          .provide(providers)
          .dispatch(getEventsDoneAction)
          .put(
            eventsActions.getEvents({
              from: Number(moment('2018-10-15')),
              to: Number(moment('2018-10-21'))
            })
          )
          .run();
      });

      test('должен запрашивать события для следующего периода', () => {
        return expectSaga(gridSagas.loadEvents)
          .provide(providers)
          .dispatch(getEventsDoneAction)
          .put(
            eventsActions.getEvents({
              from: Number(moment('2018-10-29')),
              to: Number(moment('2018-11-04'))
            })
          )
          .run();
      });

      test('должен запрашивать события для текущего периода', () => {
        return expectSaga(gridSagas.loadEvents)
          .provide(providers)
          .dispatch(getEventsDoneAction)
          .put(
            eventsActions.getEvents({
              from: Number(moment('2018-10-22')),
              to: Number(moment('2018-10-28'))
            })
          )
          .run();
      });

      test('должен устанавливать флаг о загрузке событий, если они грузятся больше 200ms', () => {
        return expectSaga(gridSagas.loadEvents)
          .provide(providers)
          .delay(210)
          .dispatch(getEventsDoneAction)
          .put(gridActions.setEventsLoading(true))
          .run();
      });

      test('не должен устанавливать флаг о загрузке событий, если они грузятся меньше 200ms', () => {
        return expectSaga(gridSagas.loadEvents)
          .provide(providers)
          .dispatch(getEventsDoneAction)
          .not.put(gridActions.setEventsLoading(true))
          .run();
      });

      test('должен сбрасывать флаг о загрузке событий', () => {
        return expectSaga(gridSagas.loadEvents)
          .provide(providers)
          .dispatch(getEventsDoneAction)
          .fork(gridSagas.setLoading, gridActions.setEventsLoading(false))
          .run();
      });
    });

    describe('неуспешное выполнение', () => {
      const getEventsDoneAction = eventsActions.getEventsDone({
        from: Number(moment('2018-10-22')),
        to: Number(moment('2018-10-28')),
        error: {name: 'error'}
      });

      test('должен логировать ошибку', () => {
        return expectSaga(gridSagas.loadEvents)
          .provide([
            [select.selector(getUserFromUrl), throwError({name: 'error'})],
            [call.fn(errorReporter.send)]
          ])
          .call([errorReporter, errorReporter.send], 'loadEvents', {name: 'error'})
          .run();
      });

      test('должен показывать нотификацию об ошибке', () => {
        return expectSaga(gridSagas.loadEvents)
          .provide([[call.fn(errorReporter.send)], ...providers])
          .dispatch(getEventsDoneAction)
          .put(notifyFailure({message: i18n.get('errors', 'loadEventsFailed')}))
          .run();
      });
    });
  });

  describe('loadEvents, для пользователя из урла', () => {
    const user = {login: 'tavria', email: 'tavria@yandex-team.ru'};
    const providers = [
      [
        select(getGridPeriod, {delta: -1}),
        {start: Number(moment('2018-10-15')), end: Number(moment('2018-10-21'))}
      ],
      [
        select(getGridPeriod, {delta: 1}),
        {start: Number(moment('2018-10-29')), end: Number(moment('2018-11-04'))}
      ],
      [
        select(getGridPeriod),
        {start: Number(moment('2018-10-22')), end: Number(moment('2018-10-28'))}
      ],
      [select(getUserFromUrl), user]
    ];

    describe('успешное выполнение', () => {
      const getEventsDoneAction = eventsActions.getEventsDone({
        from: Number(moment('2018-10-22')),
        to: Number(moment('2018-10-28'))
      });

      test('должен запрашивать события для предыдущего периода', () => {
        return expectSaga(gridSagas.loadEvents)
          .provide(providers)
          .dispatch(getEventsDoneAction)
          .put(
            eventsActions.getEventsByLogin({
              from: Number(moment('2018-10-15')),
              to: Number(moment('2018-10-21')),
              login: user.login,
              email: user.email
            })
          )
          .run();
      });

      test('должен запрашивать события для следующего периода', () => {
        return expectSaga(gridSagas.loadEvents)
          .provide(providers)
          .dispatch(getEventsDoneAction)
          .put(
            eventsActions.getEventsByLogin({
              from: Number(moment('2018-10-29')),
              to: Number(moment('2018-11-04')),
              login: user.login,
              email: user.email
            })
          )
          .run();
      });

      test('должен запрашивать события для текущего периода', () => {
        return expectSaga(gridSagas.loadEvents)
          .provide(providers)
          .dispatch(getEventsDoneAction)
          .put(
            eventsActions.getEventsByLogin({
              from: Number(moment('2018-10-22')),
              to: Number(moment('2018-10-28')),
              login: user.login,
              email: user.email
            })
          )
          .run();
      });
    });
  });

  describe('loadTodos', () => {
    const providers = [
      [
        select(getGridPeriod, {delta: -1}),
        {start: Number(moment('2018-10-15')), end: Number(moment('2018-10-21'))}
      ],
      [
        select(getGridPeriod, {delta: 1}),
        {start: Number(moment('2018-10-29')), end: Number(moment('2018-11-04'))}
      ],
      [
        select(getGridPeriod),
        {start: Number(moment('2018-10-22')), end: Number(moment('2018-10-28'))}
      ]
    ];

    describe('успешное выполнение', () => {
      const loadTodosDoneAction = todoActions.loadTodosDone({
        dueFrom: Number(moment('2018-10-22')),
        dueTo: Number(moment('2018-10-28'))
      });

      test('должен запрашивать дела для предыдущего периода', () => {
        return expectSaga(gridSagas.loadTodos)
          .provide(providers)
          .dispatch(loadTodosDoneAction)
          .put(
            todoActions.loadTodos({
              dueFrom: Number(moment('2018-10-15')),
              dueTo: Number(moment('2018-10-21'))
            })
          )
          .run();
      });

      test('должен запрашивать дела для следующего периода', () => {
        return expectSaga(gridSagas.loadTodos)
          .provide(providers)
          .dispatch(loadTodosDoneAction)
          .put(
            todoActions.loadTodos({
              dueFrom: Number(moment('2018-10-29')),
              dueTo: Number(moment('2018-11-04'))
            })
          )
          .run();
      });

      test('должен запрашивать дела для текущего периода', () => {
        return expectSaga(gridSagas.loadTodos)
          .provide(providers)
          .dispatch(loadTodosDoneAction)
          .put(
            todoActions.loadTodos({
              dueFrom: Number(moment('2018-10-22')),
              dueTo: Number(moment('2018-10-28'))
            })
          )
          .run();
      });

      test('должен устанавливать флаг о загрузке дел, если они грузятся больше 200ms', () => {
        return expectSaga(gridSagas.loadTodos)
          .provide(providers)
          .delay(210)
          .dispatch(loadTodosDoneAction)
          .put(gridActions.setTodosLoading(true))
          .run();
      });

      test('не должен устанавливать флаг о загрузке дел, если они грузятся меньше 200ms', () => {
        return expectSaga(gridSagas.loadTodos)
          .provide(providers)
          .dispatch(loadTodosDoneAction)
          .not.put(gridActions.setTodosLoading(true))
          .run();
      });

      test('должен сбрасывать флаг о загрузке дел', () => {
        return expectSaga(gridSagas.loadTodos)
          .provide(providers)
          .dispatch(loadTodosDoneAction)
          .fork(gridSagas.setLoading, gridActions.setTodosLoading(false))
          .run();
      });
    });

    describe('неуспешное выполнение', () => {
      const loadTodosDoneAction = todoActions.loadTodosDone({
        dueFrom: Number(moment('2018-10-22')),
        dueTo: Number(moment('2018-10-28')),
        error: {name: 'error'}
      });

      test('должен логировать ошибку', () => {
        return expectSaga(gridSagas.loadTodos)
          .provide([
            [select.selector(getGridPeriod), throwError({name: 'error'})],
            [call.fn(errorReporter.send)]
          ])
          .call([errorReporter, errorReporter.send], 'loadTodos', {name: 'error'})
          .run();
      });

      test('должен показывать нотификацию об ошибке', () => {
        return expectSaga(gridSagas.loadTodos)
          .provide([[call.fn(errorReporter.send)], ...providers])
          .dispatch(loadTodosDoneAction)
          .put(notifyFailure({message: i18n.get('errors', 'loadTodosFailed')}))
          .run();
      });
    });
  });
});

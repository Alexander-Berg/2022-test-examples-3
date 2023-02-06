const model = require('../do-update-user-settings');

describe('delete-event', () => {
  const tz = 'TIMEZONE';
  const lang = 'LANGUAGE';
  const uid = 1234567890;
  const login = 'tavria';
  const IS_CORP = true;
  let serviceFn;
  let requestFn;
  let coreMock;
  let methodHandler;

  beforeEach(() => {
    methodHandler = jest.fn();
    serviceFn = jest.fn();
    requestFn = jest.fn();
    serviceFn.mockReturnValue(methodHandler);

    coreMock = {
      service: serviceFn,
      request: requestFn,
      params: {
        _timezone: tz,
        _locale: lang
      },
      auth: {
        get: () => ({uid, login})
      },
      config: {IS_CORP, i18n: {locale: 'ru'}},
      hideParamInLog: jest.fn()
    };
  });

  test('должен использовать сервис calendar, если в параметрах есть календарные настройки', () => {
    const params = {
      autoAcceptEventInvitations: true,
      dayStartHour: 5,
      expiredTodoEmailTime: '08:00',
      hasNoNotificationsDateRange: true,
      hasNoNotificationsTimeRange: true,
      letParticipantsEdit: false,
      noNotificationsDuringAbsence: false,
      noNotificationsFromTime: '23:00',
      noNotificationsSinceDate: '2021-09-26',
      noNotificationsToTime: '07:00',
      noNotificationsUntilDate: '2021-10-10',
      notifyAboutExpiredTodo: true,
      notifyAboutPlannedTodo: true,
      plannedTodoEmailTime: '08:00',
      remindUndecided: true,
      showTodosInGrid: false,
      weekStartDay: 6
    };
    model(params, coreMock);

    expect(serviceFn).toHaveBeenCalledTimes(1);
    expect(serviceFn).toHaveBeenCalledWith('calendar');
  });

  test('не должен использовать сервис calendar, если в параметрах нет календарных настроек', () => {
    const params = {
      telemost_addLinkEnabled: false
    };
    model(params, coreMock);

    expect(serviceFn).toHaveBeenCalledTimes(0);
  });

  test('должен использовать datasync, если в параметрах есть datasync-настройки', () => {
    const params = {
      telemost_addLinkEnabled: false
    };
    model(params, coreMock);

    expect(requestFn).toHaveBeenCalledTimes(1);
  });

  test('не должен использовать datasync, если в параметрах нет datasync-настроек', () => {
    const params = {
      dayStartHour: 5
    };
    model(params, coreMock);

    expect(requestFn).toHaveBeenCalledTimes(0);
  });
});

const model = require('../delete-future-events');

describe('create-event', () => {
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

  test('должен сходить в ручки c правильными параметрами', async () => {
    const params = {
      eventId: 1234,
      instanceStartTs: 'Sun Sep 26 2021 00:44:02'
    };
    const event = {
      addTelemostLink: false,
      addZoomLink: false,
      availability: 'busy',
      description: '',
      descriptionHtml: '',
      endTs: '2021-09-25T23:30:00',
      isAllDay: false,
      attendees: [],
      layerId: 25125,
      location: '',
      locationHtml: '',
      name: 'название',
      notifications: [{channel: 'email', offset: '-15m'}],
      organizer: {email: 'tet4enko@yandex-team.ru'},
      othersCanView: true,
      participantsCanEdit: true,
      participantsCanInvite: true,
      startTs: '2021-09-25T23:00:00',
      telemostAllowExternal: false,
      zoomCreatorPlanType: 1
    };
    requestFn.mockReturnValue(event);
    await model(params, coreMock);

    expect(requestFn).toHaveBeenCalledTimes(3);
    expect(requestFn).toHaveBeenCalledWith('get-event', params);
    expect(requestFn).toHaveBeenCalledWith('delete-event', {
      id: params.eventId,
      instanceStartTs: params.instanceStartTs,
      applyToFuture: false
    });
    const newEvent = {
      ...event,
      start: event.startTs,
      end: event.endTs,
      applyToFuture: true,
      organizer: null,
      extraQuery: {layerId: event.layerId},
      repetition: {
        ...event.repetition,
        dueDate: params.instanceStartTs.slice(0, 10)
      }
    };
    delete newEvent.startTs;
    delete newEvent.endTs;
    expect(requestFn).toHaveBeenCalledWith('update-event', newEvent);
  });
});

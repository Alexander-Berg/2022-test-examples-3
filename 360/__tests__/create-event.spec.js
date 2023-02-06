jest.mock('../../../routes/helpers/i18n');

const model = require('../create-event');

const createEventDataWithoutVideo = {
  addTelemostLink: false,
  addZoomLink: false,
  attendees: ['tavria@yandex-team.ru'],
  availability: 'busy',
  description: '',
  descriptionHtml: '',
  end: '2021-09-25T23:30:00',
  isAllDay: false,
  layerId: 25125,
  location: '',
  locationHtml: '',
  name: 'название',
  notifications: [{channel: 'email', offset: '-15m'}],
  organizer: 'tet4enko@yandex-team.ru',
  othersCanView: true,
  participantsCanEdit: true,
  participantsCanInvite: true,
  start: '2021-09-25T23:00:00',
  telemostAllowExternal: false,
  zoomCreatorPlanType: 1
};

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

  test('должен использовать сервис calendar', () => {
    model(createEventDataWithoutVideo, coreMock);

    expect(serviceFn).toHaveBeenCalledTimes(1);
    expect(serviceFn).toHaveBeenCalledWith('calendar');
  });

  test('должен сходить в ручки телемоста с правильными параметрами, если передан параметр addTelemostLink', async () => {
    const telemostLink = 'telemost uri';
    const calendarEventId = 123;

    requestFn.mockReturnValue({uri: telemostLink});
    methodHandler.mockReturnValue({showEventId: calendarEventId});

    await model({...createEventDataWithoutVideo, addTelemostLink: true}, coreMock);

    expect(requestFn).toHaveBeenCalledTimes(2);
    expect(requestFn).toHaveBeenCalledWith('get-conference-link', {
      allowExternal: createEventDataWithoutVideo.telemostAllowExternal
    });
    expect(requestFn).toHaveBeenCalledWith('link-calendar-event', {
      telemostLink,
      uid,
      calendarEventId
    });
  });

  test('должен сходить в ручку зума с правильными параметрами, если передан параметр addZoomLink', async () => {
    const zoomLink = 'zoom uri';
    const calendarEventId = 123;

    requestFn.mockReturnValue(zoomLink);
    methodHandler.mockReturnValue({showEventId: calendarEventId});

    await model({...createEventDataWithoutVideo, addZoomLink: true}, coreMock);

    expect(requestFn).toHaveBeenCalledTimes(1);
    expect(requestFn).toHaveBeenCalledWith('get-zoom-link', {
      login: 'tet4enko'
    });
  });

  test('создавать зум-встречу от имени текущего пользователя в случае отсутствия организатора', async () => {
    const zoomLink = 'zoom link';
    const calendarEventId = 123;

    requestFn.mockReturnValue(zoomLink);
    methodHandler.mockReturnValue({organizer: calendarEventId});

    await model({...createEventDataWithoutVideo, organizer: null, addZoomLink: true}, coreMock);

    expect(requestFn).toHaveBeenCalledTimes(1);
    expect(requestFn).toHaveBeenCalledWith('get-zoom-link', {
      login: 'tavria'
    });
  });

  test('должен сходить в ручку календаря с правильными параметрами', async () => {
    const calendarEventId = 123;

    methodHandler.mockReturnValue({organizer: calendarEventId});

    await model(createEventDataWithoutVideo, coreMock);

    expect(methodHandler).toHaveBeenCalledTimes(1);
    expect(methodHandler).toHaveBeenCalledWith('/create-event', createEventDataWithoutVideo, {
      timeout: 30000
    });
  });
});

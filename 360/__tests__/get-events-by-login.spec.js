jest.mock('moment', () => {
  const moment = {
    add: jest.fn().mockReturnThis(),
    toISOString: jest.fn()
  };
  const constructor = jest.fn(() => moment);
  constructor.utc = constructor;

  return constructor;
});
const moment = require('moment');

const model = require('../get-events-by-login');

const calendarService = jest.fn();

const attendees = [
  {email: 'test1@yandex-team.ru', decision: 'yes'},
  {email: 'test2@', decision: 'no'}
];

const intervalsResponse = {
  subjectAvailabilities: [
    {
      intervals: [
        {
          attendees,
          organizer: {
            email: 'test@'
          },
          eventId: 123,
          eventName: '321',
          resources: [{email: '123'}],
          start: '01-01-2020',
          end: '02-01-2020'
        }
      ]
    }
  ]
};

const core = {
  request: jest.fn(modelName => {
    if (modelName === 'get-timezones') {
      return {
        UTC: {
          offset: 0
        }
      };
    }
  }),
  service: jest.fn(() => calendarService),
  params: {
    _timezone: 'UTC'
  }
};

describe('get-events-by-login', () => {
  const params = {
    login: 'test1',
    email: 'test1@yandex-team.ru',
    from: '12-12-2020',
    to: '12-13-2020'
  };
  test('должен ходить за email, если он не передан', async () => {
    // mock find-users-and-resources
    core.request.mockImplementationOnce(() => ({
      users: [],
      resources: [{email: 'test1@yandex-team.ru'}]
    }));
    await model({login: 'test1'}, core);

    expect(core.request).toHaveBeenCalledWith('find-users-and-resources', {
      loginOrEmails: 'test1@'
    });
  });
  test('должен ходить за событиями в сервис календарь, /get-availability-intervals c нужными параметрами', async () => {
    await model(params, core);
    expect(core.service).toHaveBeenCalledWith('calendar');
    expect(core.service).lastReturnedWith(calendarService);
    expect(calendarService).toHaveBeenCalledWith('/get-availability-intervals', {
      display: 'events',
      shape: 'full_actions',
      emails: ['test1@yandex-team.ru'],
      from: params.from,
      to: params.to,
      tz: core.params._timezone
    });
  });
  test('должен ходить за событиями с email переговорки, если он не передан', async () => {
    core.request.mockImplementationOnce(() => ({
      users: [],
      resources: [{email: 'test1@yandex-team.ru'}]
    }));

    await model({login: 'test1'}, core);
    expect(calendarService.mock.calls[0][1].emails).toEqual(['test1@yandex-team.ru']);
  });
  test('должен ходить за событиями с email пользователя, если он не передан', async () => {
    core.request.mockImplementationOnce(() => ({
      users: [{email: 'test1@yandex-team.ru'}],
      resources: []
    }));

    await model({login: 'test1'}, core);
    expect(calendarService.mock.calls[0][1].emails).toEqual(['test1@yandex-team.ru']);
  });
  test('должен корректно перекладывать джейсоны интервалов в встречи', async () => {
    calendarService.mockImplementationOnce(() => intervalsResponse);
    moment().toISOString.mockImplementationOnce(() => '2019-12-31T21:00:00.000Z');
    const {events} = await model(params, core);

    const interval = intervalsResponse.subjectAvailabilities[0].intervals[0];

    expect(events).toEqual([
      {
        id: interval.eventId,
        name: interval.eventName,
        resources: [{resource: interval.resources[0]}],
        totalAttendees: attendees.length,
        decision: interval.attendees[0].decision,
        attendees: interval.attendees,
        organizer: interval.organizer,
        start: interval.start,
        end: interval.end,
        instanceStartTs: '2019-12-31T21:00:00.000Z',
        othersCanView: true,
        hidden: false,
        isInterval: true
      }
    ]);
  });
  test('должен находить decision у участников', async () => {
    calendarService.mockImplementationOnce(() => intervalsResponse);
    const {events} = await model(params, core);

    const interval = intervalsResponse.subjectAvailabilities[0].intervals[0];
    const member = interval.attendees.find(member => member.email === params.email);

    expect(events[0].decision).toBe(member.decision);
  });
  test('должен находить decision у организатора', async () => {
    calendarService.mockImplementationOnce(() => intervalsResponse);

    const interval = intervalsResponse.subjectAvailabilities[0].intervals[0];
    const {events} = await model({...params, email: interval.organizer.email}, core);
    const member = interval.organizer;

    expect(events[0].decision).toBe(member.decision);
  });
  test('должен возвращать null в качестве decision, если member не найден', async () => {
    calendarService.mockImplementationOnce(() => intervalsResponse);

    const {events} = await model({...params, email: 'lol'}, core);

    expect(events[0].decision).toBe(null);
  });
});

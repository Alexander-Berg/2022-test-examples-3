const linkCalendarEvent = require('../link-calendar-event');

describe('models:telemost -> link-calendar-event', () => {
  let coreMock;
  let serviceFn;
  let telemostFn;

  beforeEach(() => {
    serviceFn = jest.fn();
    telemostFn = jest.fn();
    coreMock = {
      service: serviceFn
    };

    serviceFn.mockReturnValue(telemostFn);
  });

  test('должен вызывать сервис telemost, если передан корректный telemostLink и calendarEventId', () => {
    telemostFn.mockResolvedValue('');

    linkCalendarEvent(
      {
        telemostLink: 'https://telemost.yandex-team.ru/j/96134514899124302313083256463556225579',
        calendarEventId: 123
      },
      coreMock
    );

    expect(serviceFn).toHaveBeenCalledTimes(1);
    expect(serviceFn).toHaveBeenCalledWith('telemost');
  });

  test('не должен вызывать сервис telemost, если передан корректный telemostLink и calendarEventId', () => {
    telemostFn.mockResolvedValue('');

    linkCalendarEvent({}, coreMock);

    expect(serviceFn).toHaveBeenCalledTimes(0);
  });

  test('должен вызывать ручку telemost с корректными параметрами', () => {
    telemostFn.mockResolvedValue('');
    const calendarEventId = 123;
    const telemostLink = 'https://telemost.yandex-team.ru/j/96134514899124302313083256463556225579';
    linkCalendarEvent(
      {
        telemostLink,
        calendarEventId
      },
      coreMock
    );

    expect(telemostFn).toHaveBeenCalledTimes(1);
    expect(telemostFn).toHaveBeenCalledWith(
      `/v2/telemost/conferences/${encodeURIComponent(
        telemostLink
      )}/link-calendar-event?calendar_event_id=${calendarEventId}`,
      {},
      {timeout: 1000}
    );
  });
});

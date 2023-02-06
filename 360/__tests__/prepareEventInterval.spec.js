import prepareEventInterval from '../prepareEventInterval';

describe('prepareEventInterval', () => {
  test('должен обрабатывать резервирование', () => {
    const start = 123;
    const end = 789;
    const authorInfo = 'ololo';

    const event = {
      reservationId: 10,
      authorInfo,
      start,
      end
    };

    const expectedResult = {
      start,
      end,
      events: [{organizer: authorInfo}]
    };

    expect(prepareEventInterval(event)).toEqual(expectedResult);
  });
  test('должен обрабатывать событие', () => {
    const eventId = 777;
    const start = 123;
    const end = 789;
    const authorInfo = 'ololo';

    const event = {
      eventId,
      authorInfo,
      start,
      end
    };

    const expectedResult = {
      eventId,
      start,
      end,
      authorInfo,
      eventIds: [eventId]
    };

    expect(prepareEventInterval(event)).toEqual(expectedResult);
  });
});

import processMembers from '../processMembers';

describe('processMembers', () => {
  test('должен подготавливать массив участников, доклеивая интервал бронирования', () => {
    const eventStart = 123;
    const eventEnd = 789;
    const intervalStart = 12;
    const intervalEnd = 78;
    const email = 'someemail';
    const members = [
      {
        email,
        intervals: [{start: intervalStart, end: intervalEnd}]
      }
    ];
    const event = {
      start: eventStart,
      end: eventEnd
    };
    const expectedResult = [
      {
        email,
        intervals: [
          {
            isReservation: true,
            eventIds: [],
            start: eventStart,
            end: eventEnd,
            isUntouchable: true
          },
          {start: intervalStart, end: intervalEnd}
        ]
      }
    ];

    expect(processMembers(members, event)).toEqual(expectedResult);
  });
});

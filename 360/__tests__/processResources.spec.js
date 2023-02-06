import prepareEventInterval from 'features/spaceship/utils/prepareEventInterval';

import processResources from '../processResources';

jest.mock('features/spaceship/utils/prepareEventInterval');
prepareEventInterval.mockImplementation(() => interval);

const interval = {
  isReservation: true,
  eventIds: [],
  start: 333,
  end: 444
};

describe('processResources', () => {
  test('должен подготавливать массив переговорок', () => {
    const start = 123;
    const end = 789;
    const resources = [
      {
        events: [{}],
        info: {
          email: '2@ya.ru'
        }
      }
    ];
    const event = {
      resources: [
        {
          resource: {
            email: '1@ya.ru'
          }
        },
        {
          resource: {
            email: '2@ya.ru'
          }
        },
        {
          resource: {
            email: '3@ya.ru'
          }
        }
      ],
      start,
      end
    };
    const expectedResult = [
      {
        email: '2@ya.ru',
        intervals: [interval, {start, end, eventIds: [], isReservation: true}]
      }
    ];

    expect(processResources(resources, event)).toEqual(expectedResult);
  });
});

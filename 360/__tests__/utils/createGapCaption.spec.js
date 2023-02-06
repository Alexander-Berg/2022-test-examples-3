import moment from 'moment';

import createGapCaption from '../../utils/createGapCaption';

jest.mock('moment', () => {
  const moment = {
    format: jest.fn(),
    startOf: jest.fn().mockReturnThis(),
    add: jest.fn().mockReturnThis(),
    isSame: jest.fn().mockReturnThis()
  };
  const constructor = jest.fn(() => moment);
  constructor.utc = constructor;

  return constructor;
});

describe('createGapCaption', () => {
  test('должен вернуть Сегодня на больничном', () => {
    const dateTo = 1576098000000;
    const momentInstance = moment();
    momentInstance.isSame.mockReturnValueOnce(true);

    const type = 'illness';

    expect(createGapCaption(dateTo, type)).toBe('сегодня на больничном');
  });

  test('должен вернуть до 14 декабря на больничном', () => {
    const dateTo = 1576098000000;
    const momentInstance = moment();
    momentInstance.isSame.mockReturnValueOnce(false);
    momentInstance.format.mockReturnValue('14 декабря');
    const type = 'illness';

    expect(createGapCaption(dateTo, type)).toBe('до 14 декабря на больничном');
  });
});

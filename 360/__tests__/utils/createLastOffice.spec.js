import moment from 'moment';

import createLastOffice from '../../utils/createLastOffice';

jest.mock('utils/i18n', () => {
  return {
    get: (keyset, keyname) => {
      const keysets = {
        common: {
          justNow: 'Только что'
        }
      };

      return keysets[keyset][keyname];
    }
  };
});

jest.mock('moment', () => {
  const moment = {
    diff: jest.fn(),
    calendar: jest.fn()
  };
  return jest.fn(() => moment);
});

describe('createLastOffice', () => {
  test('должен вернуть Только что, БЦ Бенуа', () => {
    const momentInstance = moment();
    momentInstance.diff.mockReturnValue(12);
    const duration = 15;
    const availability = {
      updated_at: 123,
      office_name: 'БЦ Бенуа'
    };

    expect(createLastOffice(availability, {duration, unit: 'minutes'})).toEqual({
      ago: 'Только что',
      office: 'БЦ Бенуа'
    });
  });

  test('должен вернуть Сегодня 14:56, БЦ Бенуа', () => {
    const momentInstance = moment();
    momentInstance.diff.mockReturnValue(16);
    momentInstance.calendar.mockReturnValue('Сегодня 14:56');

    const duration = 15;
    const availability = {
      updated_at: 123,
      office_name: 'БЦ Бенуа'
    };

    expect(createLastOffice(availability, {duration, unit: 'minutes'})).toEqual({
      ago: 'Сегодня 14:56',
      office: 'БЦ Бенуа'
    });
  });
});

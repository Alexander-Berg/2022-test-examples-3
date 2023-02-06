import HolidayRecord from '../HolidayRecord';

describe('HolidayRecord', () => {
  describe('isHoliday', () => {
    test('должен возвращать true, если это праздник', () => {
      const holiday = new HolidayRecord({type: 'holiday'});

      expect(holiday.isHoliday()).toBe(true);
    });

    test('должен возвращать true, если это выходной', () => {
      const holiday = new HolidayRecord({type: 'weekend'});

      expect(holiday.isHoliday()).toBe(true);
    });

    test('должен возвращать false, в остальных случаях', () => {
      const holiday = new HolidayRecord({type: 'weekday'});

      expect(holiday.isHoliday()).toBe(false);
    });
  });

  describe('isWeekday', () => {
    test('должен возвращать true, если это рабочий день', () => {
      const holiday = new HolidayRecord({type: 'weekday'});

      expect(holiday.isWeekday()).toBe(true);
    });

    test('должен возвращать false, в остальных случаях', () => {
      const holiday = new HolidayRecord({type: 'holiday'});

      expect(holiday.isWeekday()).toBe(false);
    });
  });
});

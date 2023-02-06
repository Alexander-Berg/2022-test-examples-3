import moment from 'moment';

import processInputRepetition, {getDefaultRepetition} from '../../utils/processInputRepetition';

describe('eventForm/utils/processInputRepetition', () => {
  test('должен вернуть дефолтный повтор, если не передали повтор', () => {
    const defaultRepetition = getDefaultRepetition(moment().valueOf());

    expect(processInputRepetition(moment().valueOf())).toEqual(defaultRepetition);
  });

  describe('поле type', () => {
    test('должно равняться полю type из переданного повтора', () => {
      expect(
        processInputRepetition(moment().valueOf(), {
          type: 'daily'
        }).type
      ).toBe('daily');
    });

    test('должно равняться monthly, если у переданного повтора type == monthly-number', () => {
      expect(
        processInputRepetition(moment().valueOf(), {
          type: 'monthly-number'
        }).type
      ).toBe('monthly');
    });

    test('должно равняться monthly, если у переданного повтора type == monthly-day-weekno', () => {
      expect(
        processInputRepetition(moment().valueOf(), {
          type: 'monthly-day-weekno'
        }).type
      ).toBe('monthly');
    });
  });

  describe('поле dueDate', () => {
    test('должно равняться полю dueDate из переданного повтора', () => {
      expect(
        processInputRepetition(moment().valueOf(), {
          type: 'daily',
          dueDate: '2016-10-10'
        }).dueDate
      ).toBe(moment('2016-10-10').valueOf());
    });

    test('должно равняться дефолтному значению, если у переданного повтора нет dueDate', () => {
      const defaultRepetition = getDefaultRepetition(moment().valueOf());

      expect(
        processInputRepetition(moment().valueOf(), {
          type: 'daily'
        }).dueDate
      ).toBe(defaultRepetition.dueDate);
    });
  });

  describe('поле daily ->', () => {
    test('поле each должно равняться дефолтному значению, если type != daily', () => {
      const defaultRepetition = getDefaultRepetition(moment().valueOf());

      expect(
        processInputRepetition(moment().valueOf(), {
          type: 'monthly'
        }).daily.each
      ).toBe(defaultRepetition.daily.each);
    });

    test('поле each должно равняться переданному значению, если type == daily', () => {
      expect(
        processInputRepetition(moment().valueOf(), {
          type: 'daily',
          each: 5
        }).daily.each
      ).toBe(5);
    });

    test('поле every должно равняться дефолтному значению', () => {
      const defaultRepetition = getDefaultRepetition(moment().valueOf());

      expect(
        processInputRepetition(moment().valueOf(), {
          type: 'daily'
        }).daily.every
      ).toBe(defaultRepetition.daily.every);
    });
  });

  describe('поле weekly ->', () => {
    test('поле each должно равняться дефолтному значению, если type != weekly', () => {
      const defaultRepetition = getDefaultRepetition(moment().valueOf());

      expect(
        processInputRepetition(moment().valueOf(), {
          type: 'daily'
        }).weekly.each
      ).toBe(defaultRepetition.weekly.each);
    });

    test('поле each должно равняться переданному значению, если type == weekly', () => {
      expect(
        processInputRepetition(moment().valueOf(), {
          type: 'weekly',
          each: 10
        }).weekly.each
      ).toBe(10);
    });

    test('поле weeklyDays должно равняться дефолтному значению, если type != weekly', () => {
      const defaultRepetition = getDefaultRepetition(moment().valueOf());

      expect(
        processInputRepetition(moment().valueOf(), {
          type: 'daily'
        }).weekly.weeklyDays
      ).toBe(defaultRepetition.weekly.weeklyDays);
    });

    test('поле weeklyDays должно равняться переданному значению, если type == weekly', () => {
      expect(
        processInputRepetition(moment().valueOf(), {
          type: 'weekly',
          weeklyDays: 'mon,fri'
        }).weekly.weeklyDays
      ).toBe('mon,fri');
    });
  });

  describe('поле monthly ->', () => {
    test('поле eachDay должно равняться дефолтному значению, если type != monthly-number', () => {
      const defaultRepetition = getDefaultRepetition(moment().valueOf());

      expect(
        processInputRepetition(moment().valueOf(), {
          type: 'daily'
        }).monthly.eachDay
      ).toBe(defaultRepetition.monthly.eachDay);
    });

    test('поле eachDay должно равняться переданному значению each, если type == monthly-number', () => {
      expect(
        processInputRepetition(moment().valueOf(), {
          type: 'monthly-number',
          each: 3
        }).monthly.eachDay
      ).toBe(3);
    });

    test('поле eachWeekDay должно равняться дефолтному значению, если type != monthly-day-weekno', () => {
      const defaultRepetition = getDefaultRepetition(moment().valueOf());

      expect(
        processInputRepetition(moment().valueOf(), {
          type: 'daily'
        }).monthly.eachWeekDay
      ).toBe(defaultRepetition.monthly.eachWeekDay);
    });

    test('поле eachWeekDay должно равняться переданному значению each, если type == monthly-day-weekno', () => {
      expect(
        processInputRepetition(moment().valueOf(), {
          type: 'monthly-day-weekno',
          each: 3
        }).monthly.eachWeekDay
      ).toBe(3);
    });

    test('поле monthlyLastweek должно равняться дефолтному значению, если type != monthly-day-weekno', () => {
      const defaultRepetition = getDefaultRepetition(moment().valueOf());

      expect(
        processInputRepetition(moment().valueOf(), {
          type: 'monthly-number'
        }).monthly.monthlyLastweek
      ).toBe(defaultRepetition.monthly.monthlyLastweek);
    });

    test('поле monthlyLastweek должно равняться переданному значению, если type == monthly-day-weekno', () => {
      expect(
        processInputRepetition(moment().valueOf(), {
          type: 'monthly-day-weekno',
          monthlyLastweek: true
        }).monthly.monthlyLastweek
      ).toBe(true);
    });

    test('поле every должно равняться everyNthDay, если type == monthly-number', () => {
      expect(
        processInputRepetition(moment().valueOf(), {
          type: 'monthly-number'
        }).monthly.every
      ).toBe('everyNthDay');
    });

    test('поле every должно равняться everyNthWeekDay, если type == monthly-day-weekno', () => {
      expect(
        processInputRepetition(moment().valueOf(), {
          type: 'monthly-day-weekno'
        }).monthly.every
      ).toBe('everyNthWeekDay');
    });

    test('поле every должно равняться дефолтному значению, если type != monthly-*', () => {
      const defaultRepetition = getDefaultRepetition(moment().valueOf());

      expect(
        processInputRepetition(moment().valueOf(), {
          type: 'daily'
        }).monthly.every
      ).toBe(defaultRepetition.monthly.every);
    });
  });

  describe('поле yearly ->', () => {
    test('поле each должно равняться дефолтному значению, если type != yearly', () => {
      const defaultRepetition = getDefaultRepetition(moment().valueOf());

      expect(
        processInputRepetition(moment().valueOf(), {
          type: 'daily'
        }).yearly.each
      ).toBe(defaultRepetition.yearly.each);
    });

    test('поле each должно равняться переданному значению, если type == yearly', () => {
      expect(
        processInputRepetition(moment().valueOf(), {
          type: 'yearly',
          each: 2
        }).yearly.each
      ).toBe(2);
    });
  });
});

import moment from 'moment';

import processOutputRepetition from '../processOutputRepetition';

describe('eventForm/utils/processOutputRepetition', () => {
  describe('type daily ->', () => {
    test('должен вернуть ежедневный повтор', () => {
      const repetition = {
        type: 'daily',
        daily: {
          each: 5,
          every: 'everyNDay'
        }
      };
      expect(processOutputRepetition(Number(moment()), repetition)).toEqual({
        type: 'daily',
        each: 5
      });
    });

    test('должен вернуть еженедельный повтор по будням', () => {
      const repetition = {
        type: 'daily',
        daily: {
          each: 5,
          every: 'everyWorkDay'
        }
      };
      expect(processOutputRepetition(Number(moment()), repetition)).toEqual({
        type: 'weekly',
        each: 1,
        weeklyDays: 'mon,tue,wed,thu,fri'
      });
    });
  });

  describe('type weekly ->', () => {
    test('должен вернуть еженедельный повтор', () => {
      const repetition = {
        type: 'weekly',
        weekly: {
          each: 5,
          weeklyDays: 'wed,thu,fri'
        }
      };
      expect(processOutputRepetition(Number(moment()), repetition)).toEqual({
        type: 'weekly',
        each: 5,
        weeklyDays: 'wed,thu,fri'
      });
    });
  });

  describe('type monthly ->', () => {
    test('должен вернуть ежемесячный повтор в конкретное число месяца', () => {
      // например, 12 числа каждого месяца
      const repetition = {
        type: 'monthly',
        monthly: {
          every: 'everyNthDay',
          eachDay: 1
        }
      };
      expect(processOutputRepetition(Number(moment('2016-10-12')), repetition)).toEqual({
        type: 'monthly-number',
        each: 1
      });
    });

    test('должен вернуть ежемесячный повтор в конкретный день недели', () => {
      // например, во вторую среду каждого месяца
      const repetition = {
        type: 'monthly',
        monthly: {
          every: 'everyNthWeekDay',
          eachWeekDay: 1
        }
      };
      expect(processOutputRepetition(Number(moment('2016-10-12')), repetition)).toEqual({
        type: 'monthly-day-weekno',
        each: 1,
        monthlyLastweek: false
      });
    });

    test('должен вернуть ежемесячный повтор в конкретный день недели, с флагом "последний день недели"', () => {
      const repetition = {
        type: 'monthly',
        monthly: {
          every: 'everyNthWeekDay',
          eachWeekDay: 1
        }
      };
      expect(processOutputRepetition(Number(moment('2016-10-31')), repetition)).toEqual({
        type: 'monthly-day-weekno',
        each: 1,
        monthlyLastweek: true
      });
    });
  });

  describe('type yearly ->', () => {
    test('должен вернуть ежегодный повтор', () => {
      const repetition = {
        type: 'yearly',
        yearly: {
          each: 1
        }
      };
      expect(processOutputRepetition(Number(moment()), repetition)).toEqual({
        type: 'yearly',
        each: 1
      });
    });
  });

  test('должен добавить к повтору dueDate, если она есть', () => {
    const repetition = {
      type: 'yearly',
      yearly: {
        each: 1
      },
      dueDate: moment().format(moment.HTML5_FMT.DATE)
    };
    expect(processOutputRepetition(Number(moment()), repetition)).toEqual({
      type: 'yearly',
      each: 1,
      dueDate: moment().format(moment.HTML5_FMT.DATE)
    });
  });
});

import i18n from 'utils/i18n';

import getTimeString from '../getTimeString';

jest.mock('utils/i18n');

describe('getTimeString', () => {
  test('должен возвращать начало и конец для события, начинающегося и заканчивающегося в заданный день', () => {
    const dayStart = Number(new Date(2020, 1, 3));
    const start = Number(new Date(2020, 1, 3, 5));
    const end = Number(new Date(2020, 1, 3, 6));
    const isAllDay = false;

    expect(getTimeString({start, end, isAllDay}, dayStart)).toEqual(
      i18n.get('schedule', 'timeRange', {
        startTime: start,
        endTime: end
      })
    );
  });
  test('должен возвращать только начало для события, начинающегося, но не заканчивающегося в заданный день', () => {
    const dayStart = Number(new Date(2020, 1, 3));
    const start = Number(new Date(2020, 1, 3, 10));
    const end = Number(new Date(2020, 1, 4, 5));
    const isAllDay = false;

    expect(getTimeString({start, end, isAllDay}, dayStart)).toEqual(
      i18n.get('schedule', 'timeFrom', {
        time: start
      })
    );
  });
  test('должен возвращать только конец для события, заканчивающегося, но не начинающегося в заданный день', () => {
    const dayStart = Number(new Date(2020, 1, 3));
    const start = Number(new Date(2020, 1, 2, 10));
    const end = Number(new Date(2020, 1, 3, 5));
    const isAllDay = false;

    expect(getTimeString({start, end, isAllDay}, dayStart)).toEqual(
      i18n.get('schedule', 'timeTo', {
        time: end
      })
    );
  });
  test('должен возвращать "весь день" для события, начавшегося ранее и окончившенгося позднее заданного дня', () => {
    const dayStart = Number(new Date(2020, 1, 3));
    const start = Number(new Date(2020, 1, 2, 10));
    const end = Number(new Date(2020, 1, 4, 5));
    const isAllDay = false;

    expect(getTimeString({start, end, isAllDay}, dayStart)).toEqual(i18n.get('event', 'allDay'));
  });
  test('должен возвращать "весь день" для события с флагом allDay', () => {
    const dayStart = Number(new Date(2020, 1, 3));
    const start = Number(new Date(2020, 1, 3, 10));
    const end = Number(new Date(2020, 1, 3, 15));
    const isAllDay = true;

    expect(getTimeString({start, end, isAllDay}, dayStart)).toEqual(i18n.get('event', 'allDay'));
  });
});

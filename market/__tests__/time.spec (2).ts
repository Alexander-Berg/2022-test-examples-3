import dayjs from 'dayjs';

import { formatTime, getDifferenceMinutes } from '@/utils/time';

describe('Тестирование утилит форматирования дат и времени', () => {
  it('Корректное форматирование времени', () => {
    expect(formatTime('2020-01-09T14:30:00')).toEqual('14:30');
  });

  it('Если на вход подавать неверные данные - получим 00:00', () => {
    expect(formatTime('545')).toEqual('00:00');
  });

  it('getDifferenceMinutes корректно определяет разницу в минутах', () => {
    expect(
      getDifferenceMinutes(
        dayjs('2020-04-03T20:00:00.000Z'),
        dayjs('2020-04-03T21:00:00.000Z'),
      ),
    ).toEqual(60);
  });
});

import { getDate } from '../getDate';

describe('getDate', () => {
  it('Возвращает текущую дату в часовом поясе пользователя UTC+1', () => {
    const gDate = getDate(1658234369, 60).format();

    expect(gDate).toBe('2022-07-19T13:39:29+01:00');
  });

  it('Возвращает текущую дату в часовом поясе пользователя UTC(0)', () => {
    const gDate = getDate(1658234369, 0).format();

    expect(gDate).toBe('2022-07-19T12:39:29Z');
  });

  it('Возвращает текущую дату в часовом поясе пользователя UTC-7', () => {
    const gDate = getDate(1658234369, -420).format();

    expect(gDate).toBe('2022-07-19T05:39:29-07:00');
  });

  it('Возвращает текущую дату в часовом поясе пользователя UTC+5,5', () => {
    const gDate = getDate(1658234369, 330).format();

    expect(gDate).toBe('2022-07-19T18:09:29+05:30');
  });
});

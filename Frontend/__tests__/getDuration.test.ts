import { getDuration } from '../getDuration';

describe('getDuration', () => {
  it('Должен выводить 0 минут, если секунд меньше, чем на минуту', () => {
    const count = getDuration(22);
    expect(count).toBe('0:22');
  });

  it('Возвращает минуты и секунды при отправке числа больше одной минуты', () => {
    const count = getDuration(240);
    expect(count).toBe('4:00');
  });
  //расскипать при решении бага https://st.yandex-team.ru/NERPADUTY-2771
  it.skip('Возвращает верный временной отрезок(с часами) при отправке числа больше 3599', () => {
    const count = getDuration(4535);
    expect(count).toBe('01:15:35');
  });
});

import { ESport } from 'sport/types/sport';
import { getHeadingTitle } from '../getHeadingTitle';

describe('getHeadingTitle', () => {
  it('Возвращает Новости футбола, если есть поле Футбол', () => {
    const name = getHeadingTitle(
      ESport.FOOTBALL,
    );
    expect(name).toBe('Новости футбола');
  });

  it('Возвращает Новости, если тип спорта не передан', () => {
    const name = getHeadingTitle();
    expect(name).toBe('Новости');
  });

  it('Возвращает Новости, если передан неизвестный вид спорта', () => {
    const name = getHeadingTitle(
      11111111 as ESport,
    );
    expect(name).toBe('Новости');
  });
});

import { getAgencyLogoByUrl } from '../getAgencyLogoByUrl';

describe('getAgencyLogoByUrl', () => {
  it('Возвращает url с правильным hostname', () => {
    const url = 'https://test.ru/test';
    const resultUrl = getAgencyLogoByUrl(url);

    expect(resultUrl).toBe('https://favicon.yandex.net/favicon/test.ru?stub=1&size=32');
  });

  it('Обрабатывает ошибки', () => {
    const url = '__test__';
    const resultUrl = getAgencyLogoByUrl(url);

    expect(resultUrl).toBe('__test__');
  });

  // Для проверки урла вида http://xn--ma5r, был баг https://st.yandex-team.ru/NERPADUTY-1776
  it('Обрабатывает ошибки урлов вида http://xn--ma5r', () => {
    const url = 'http://xn--ma5r.diapazon.kz/kazakhstan/kaz-othernews/105830-aktyubinskie-vrachi-voshli-v-knigu-rekordov-za-chas-izmerili-davlenie-4270-zhitelyam.html';
    const resultUrl = getAgencyLogoByUrl(url);

    expect(resultUrl).toBe('http://xn--ma5r.diapazon.kz/kazakhstan/kaz-othernews/105830-aktyubinskie-vrachi-voshli-v-knigu-rekordov-za-chas-izmerili-davlenie-4270-zhitelyam.html');
  });
});

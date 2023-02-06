import getYMapsLink from '../getYMapsLink';

describe('getYMapsLink', () => {
  test('ссылка местоположение при type === geo', () => {
    const uri =
      'ymapsbm1://geo?ll=37.621037%2C55.753597&spn=0.005901%2C0.003273&text=%D0%A0%D0%BE%D1%81%D1%81%D0%B8%D1%8F%2C%20%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0%2C%20%D0%9A%D1%80%D0%B0%D1%81%D0%BD%D0%B0%D1%8F%20%D0%BF%D0%BB%D0%BE%D1%89%D0%B0%D0%B4%D1%8C%20';

    const expected =
      // eslint-disable-next-line
      '//yandex.ru/maps?ll=37.621037%2C55.753597&spn=0.005901%2C0.003273&text=%D0%A0%D0%BE%D1%81%D1%81%D0%B8%D1%8F%2C%20%D0%9C%D0%BE%D1%81%D0%BA%D0%B2%D0%B0%2C%20%D0%9A%D1%80%D0%B0%D1%81%D0%BD%D0%B0%D1%8F%20%D0%BF%D0%BB%D0%BE%D1%89%D0%B0%D0%B4%D1%8C%20';
    const actual = getYMapsLink(uri);

    expect(actual).toBe(expected);
  });

  test('ссылка на огранизацию при type === org', () => {
    const uri = 'ymapsbm1://org?oid=1072432761';

    const expected = '//yandex.ru/maps/org/1072432761';
    const actual = getYMapsLink(uri);

    expect(actual).toBe(expected);
  });
});

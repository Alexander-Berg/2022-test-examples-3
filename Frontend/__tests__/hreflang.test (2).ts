import { getAlternates } from '../getAlternates';

const url = new URL('https://yandex.ru/sport/path');

test('getHreflang должна возвращать валидный hreflang для com домена', () => {
  expect(getAlternates(url, ['com']))
    .toEqual([
      { href: 'https://yandex.com/sport/path', hreflang: 'ru' },
    ]);
});

test('getHreflang должна возвращать валидный hreflang для ua домена', () => {
  expect(getAlternates(url, ['ua']))
    .toEqual([
      { href: 'https://yandex.ua/sport/path?lang=uk', hreflang: 'uk-UA' },
      { href: 'https://yandex.ua/sport/path?lang=ru', hreflang: 'ru-UA' },
    ]);
});

test('getHreflang должна возвращать валидный hreflang для домена типа com.ge', () => {
  expect(getAlternates(url, ['com.ge']))
    .toEqual([
      { href: 'https://yandex.com.ge/sport/path', hreflang: 'ru-GE' },
    ]);
});

test('getHreflang должна возвращать валидный hreflang для домена в общем случае', () => {
  expect(getAlternates(url, ['kz']))
    .toEqual([
      { href: 'https://yandex.kz/sport/path', hreflang: 'ru-KZ' },
    ]);
});

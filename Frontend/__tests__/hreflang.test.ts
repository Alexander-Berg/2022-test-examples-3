import { getAlternates } from '../getAlternates';

const url = new URL('https://yandex.ru/news/path');

test('getHreflang должна возвращать валидный hreflang для com домена', () => {
  expect(getAlternates(url, ['com']))
    .toEqual([
      { href: 'https://yandex.com/news/path', hreflang: 'ru' },
    ]);
});

test('getHreflang должна возвращать валидный hreflang для ua домена', () => {
  expect(getAlternates(url, ['ua']))
    .toEqual([
      { href: 'https://yandex.ua/news/path?lang=uk', hreflang: 'uk-UA' },
      { href: 'https://yandex.ua/news/path?lang=ru', hreflang: 'ru-UA' },
    ]);
});

test('getHreflang должна возвращать валидный hreflang для домена типа com.ge', () => {
  expect(getAlternates(url, ['com.ge']))
    .toEqual([
      { href: 'https://yandex.com.ge/news/path', hreflang: 'ru-GE' },
    ]);
});

test('getHreflang должна возвращать валидный hreflang для домена в общем случае', () => {
  expect(getAlternates(url, ['kz']))
    .toEqual([
      { href: 'https://yandex.kz/news/path', hreflang: 'ru-KZ' },
    ]);
});

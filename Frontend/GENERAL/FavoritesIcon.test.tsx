import React from 'react';
import { getJson } from '../internal/getJson';
import { FavoritesIcon } from '.';

describe('FavoritesIcon', () => {
  test('Правильно проставляет ссылки на Коллекции', () => {
    expect(getJson(<FavoritesIcon />).props.href).toEqual('//yandex.ru/collections');
    expect(getJson(<FavoritesIcon linkParams="?getparam=value" />).props.href).toEqual('//yandex.ru/collections?getparam=value');
    expect(getJson(<FavoritesIcon tld="com" />).props.href).toEqual('//yandex.com/collections');
  });

  test('Правильно прокидывает свойства в Link', () => {
    expect(getJson(<FavoritesIcon href="//ya.ru" />).props.href).toEqual('//ya.ru');
  });
  test('Проставляет white', () => {
    expect(getJson(<FavoritesIcon white />).props.className).toContain('FavoritesIcon_white');
  });
});

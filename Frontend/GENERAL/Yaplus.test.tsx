import React from 'react';
import { getJson } from '../internal/getJson';
import { Yaplus } from '.';

describe('Yaplus', () => {
  test('Правильно проставляет ссылки на Плюс', () => {
    expect(getJson(<Yaplus />).props.href).toEqual('//plus.yandex.ru');
    expect(getJson(<Yaplus linkParams="?getparam=value" />).props.href).toEqual('//plus.yandex.ru?getparam=value');
    expect(getJson(<Yaplus tld="com" />).props.href).toEqual('//plus.yandex.com');
  });
});

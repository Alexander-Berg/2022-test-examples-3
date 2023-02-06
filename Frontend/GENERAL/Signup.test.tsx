import React from 'react';
import { getJson } from '../internal/getJson';
import { tldsAndLangs } from '../internal/tldsAndLangs';
import { Signup } from '.';

describe('Signup', () => {
  test('Правильно генерирует ссылку', () => {
    expect(getJson(<Signup />).props.href).toEqual(`//passport.yandex.ru/registration?retpath=${encodeURIComponent('https://yandex.ru')}&origin=header`);

    tldsAndLangs.map(([tld, _]) => {
      const retpath = `https://yandex.${tld}`;
      expect(getJson(<Signup tld={tld} origin="header" retpath={retpath} />).props.href).toEqual(`//passport.yandex.${tld}/registration?retpath=${encodeURIComponent(retpath)}&origin=header`);
    });
  });

  test('Правильно прокидывает свойства в Link', () => {
    expect(getJson(<Signup target="_blank" />).props.target).toEqual('_blank');
  });
});

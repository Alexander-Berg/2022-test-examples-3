import React from 'react';
import { getJson } from '../internal/getJson';
import { tldsAndLangs } from '../internal/tldsAndLangs';
import { Login } from './Login@desktop';

describe('Login', () => {
  test('Правильно генерирует ссылку', () => {
    (['action', 'serp', 'link'] as ('action' | 'serp' | 'link')[]).forEach((theme) => {
        expect(getJson(<Login theme={theme} />).props.href).toEqual(`//passport.yandex.ru/auth?retpath=${encodeURIComponent('https://yandex.ru')}&origin=header`);

        tldsAndLangs.map(([tld, _]) => {
        const retpath = `https://yandex.${tld}`;
        expect(getJson(<Login theme={theme} tld={tld} origin="custom_header" retpath={retpath} />).props.href).toEqual(`//passport.yandex.${tld}/auth?retpath=${encodeURIComponent(retpath)}&origin=custom_header`);
        });
    });
  });
});

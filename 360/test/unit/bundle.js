'use strict';

import { describe } from 'ava-spec';
import config from '../../dist/yandex-services.json';
import yaTeam from '../../dist/domains/yandex-team.json';
import module from '../../dist/bundle.js';

describe('module client interface', (it) => {
    it('При обращении к services должен вернуть полный конфиг', (t) => {
        t.deepEqual(module.services, config);
    });

    it('При обращении к yandex-team должен вернуть конфиг по домену yandex-team', (t) => {
        t.deepEqual(module['yandex-team'], yaTeam);
    });

    it('Функция фильтрации должна работать', (t) => {
        const filter = module.filter;
        t.deepEqual(filter({ domains: 'ru', services: 'mail' }), { mail: config.mail.ru });
    });
});



'use strict';

import { describe } from 'ava-spec';
import config from '../../../../dist/yandex-services.json';
import filter from '../../../../src/filter/index.js';

describe('#filter', (it) => {
    it('Фильтрация по сервису', (t) => {
        t.deepEqual(filter(config, { services: 'mail' }), { mail: config.mail });
    });

    it('Фильтрация по сервисам', (t) => {
        const { mail, disk } = config;

        t.deepEqual(filter(config, { services: ['mail', 'disk'] }), { mail, disk });
    });

    it('Фильтрация по домену', (t) => {
        const localConfig = {
            mail: {
                tr: {
                    url: 'trurl'
                },

                am: {
                    url: 'amurl'
                }
            },

            disk: {
                tr: {
                    url: 'trurl'
                },

                az: {
                    url: 'azurl'
                }
            }
        };

        const { mail: { tr: mailtr }, disk: { tr: disktr } } = localConfig;

        t.deepEqual(filter(localConfig, { domains: 'tr' }), { mail: mailtr , disk: disktr });
    });

    it('Фильтрация по доменам', (t) => {
        const localConfig = {
            mail: {
                tr: {
                    url: 'trurl'
                },

                am: {
                    url: 'amurl'
                },

                az: {
                    url: 'azurl'
                }
            },

            disk: {
                tr: {
                    url: 'trurl'
                },

                az: {
                    url: 'azurl'
                },

                am: {
                    url: 'amurl'
                }
            }
        };

        const { mail: { tr: mailtr, az: mailaz }, disk: { tr: disktr, az: diskaz } } = localConfig;

        t.deepEqual(filter(localConfig, { domains: ['tr', 'az'] }), {
            mail: { tr: mailtr, az: mailaz },
            disk: { tr: disktr, az: diskaz }
        });
    });

    it('Фильтрация по сервису и домену', (t) => {
        t.deepEqual(filter(config, { services: 'mail', domains: 'tr' }), { mail: config.mail.tr });
    });

    it('Фильтрация по сервисам и домену', (t) => {
        const { mail: { tr: mailtr }, disk: { tr: disktr } } = config;

        t.deepEqual(filter(config, { services: ['mail', 'disk'], domains: 'tr' }), {
            mail: mailtr,
            disk: disktr
        });
    });

    it('Фильтрация по сервису и доменам', (t) => {
        const { mail: { tr, am } } = config;

        t.deepEqual(filter(config, { services: 'mail', domains: ['tr', 'am'] }), { mail: { tr, am } });
    });

    it('Фильтрация по сервисам и доменам', (t) => {
        const { mail: { tr: mailtr, am: mailam }, disk: { tr: disktr, am: diskam } } = config;

        t.deepEqual(filter(config, { services: ['mail', 'disk'], domains: ['tr', 'am'] }), {
            mail: { tr: mailtr, am: mailam },
            disk: { tr: disktr, am: diskam }
        });
    });

    it('Должен правильно фильтровать по не обязательным доменам', (t) => {
        const localConfig = {
            mail: {
                tr: {
                    url: 'trurl'
                },

                am: {
                    url: 'amurl'
                },

                az: {
                    url: 'azurl'
                },

                'yandex-team': {
                    url: 'yateam'
                }
            },

            disk: {
                tr: {
                    url: 'trurl'
                },

                az: {
                    url: 'azurl'
                },

                am: {
                    url: 'amurl'
                }
            }
        };

        t.deepEqual(filter(localConfig, { domains: 'yandex-team' }), { mail: { url: 'yateam' } } );
    });

    it('Должен вернуть пустой объект, если попытка фильтровать по не существующим сервисам или доменам', (t) => {
        t.plan(6);

        const filterRes1 = filter(config, { domains: 'test' });
        const filterRes2 = filter(config, { services: 'test' });
        const filterRes3 = filter(config, { domains: 'test', services: 'test' });
        const filterRes4 = filter(config, { services: ['test1', 'test2'], domains: ['test1', 'test2'] });
        const filterRes5 = filter(config, { services: ['test1', 'test2'] });
        const filterRes6 = filter(config, { domains: ['test1', 'test2'] });

        t.deepEqual(filterRes1, {});
        t.deepEqual(filterRes2, {});
        t.deepEqual(filterRes3, {});
        t.deepEqual(filterRes4, {});
        t.deepEqual(filterRes5, {});
        t.deepEqual(filterRes6, {});
    });

    it('Отфильтрованные данные не должны быть ссылкой на основные данные', (t) => {
        t.plan(4);
        const filterRes1 = filter(config);
        const filterRes2 = filter(config, { domains: 'ru' });
        const filterRes3 = filter(config, { services: 'mail' });
        const filterRes4 = filter(config, { services: 'mail', domains: 'ru' });

        filterRes1.mail.ru.url = 'res1';
        filterRes2.mail.url = 'res2';
        filterRes3.mail.ru.url = 'res3';
        filterRes4.mail.url = 'res4';

        t.not(filterRes1.mail.ru.url, config.mail.ru.url);
        t.not(filterRes2.mail.url, config.mail.ru.url);
        t.not(filterRes3.mail.ru.url, config.mail.ru.url);
        t.not(filterRes4.mail.url, config.mail.ru.url);
    });
});

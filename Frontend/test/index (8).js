/* global describe, it */
'use strict';

const request = require('supertest');
const getApp = require('./app.js');

describe('language detection', () => {
    describe('with accept-language', () => {
        it('should not set english for ru domain and en-US language', done => {
            const app = getApp({
                'x-forwarded-host': 'kit.f0rmat1k.nodejs.dev.spec.yandex.ru',
                'accept-language': 'en-US',
                'x-forwarded-for': '87.250.248.233',
            }, {
                availableLanguages: { ru: ['ru', 'en'] },
            });
            request(app)
                .get('/')
                .expect({ id: 'ru', name: 'Ru' })
                .end(done);
        });

        it('should set english for ru domain and `ru-RU,en-US` language', done => {
            const app = getApp({
                'x-forwarded-host': 'kit.f0rmat1k.nodejs.dev.spec.yandex.ru',
                'accept-language': 'ru-RU,en-US',
                'x-forwarded-for': '87.250.248.233',
            }, {
                availableLanguages: { ru: ['ru', 'en'] },
            });
            request(app)
                .get('/')
                .expect({ id: 'ru', name: 'Ru' })
                .end(done);
        });

        it('should set german for com domain with "de" lang', done => {
            const app = getApp({
                'x-forwarded-host': 'kit.f0rmat1k.nodejs.dev.spec.yandex.com',
                'x-forwarded-for': '87.250.248.233',
                'accept-language': 'de',
            }, {
                availableLanguages: ['de', 'en'],
            });
            request(app)
                .get('/')
                .expect({ id: 'de', name: 'Ger' })
                .end(done);
        });
    });

    describe('with defaultLanguage', () => {
        it('should set english for ru domain', done => {
            const app = getApp({
                'x-forwarded-host': 'kit.f0rmat1k.nodejs.dev.spec.yandex.ru',
                'x-forwarded-for': '87.250.248.233',
            }, {
                availableLanguages: { ru: ['tr'] },
                defaultLanguage: 'en',
            });
            request(app)
                .get('/')
                .expect({ id: 'en', name: 'En' })
                .end(done);
        });
    });

    it('should set russian for ru domain', done => {
        const app = getApp({
            'x-forwarded-host': 'kit.f0rmat1k.nodejs.dev.spec.yandex.ru',
            'x-forwarded-for': '87.250.248.233',
        });
        request(app)
            .get('/')
            .expect({ id: 'ru', name: 'Ru' })
            .end(done);
    });

    it('should set english for com domain', done => {
        const app = getApp({
            'x-forwarded-host': 'kit.f0rmat1k.nodejs.dev.spec.yandex.com',
            'x-forwarded-for': '87.250.248.233',
        });
        request(app)
            .get('/')
            .expect({ id: 'en', name: 'En' })
            .end(done);
    });

    it('should return list of related langs with `list` option set', done => {
        const app = getApp({
            'x-forwarded-host': 'kit.f0rmat1k.nodejs.dev.spec.yandex.com.tr',
            'x-forwarded-for': '193.0.152.59',
            'accept-language': 'FR-ca, fr;q=0.8, en;q=0.7',
        }, {
            list: true,
        });
        request(app)
            .get('/')
            .expect(resp => {
                const langs = ['tr', 'en', 'fr', 'ru', 'fr-ca'];
                // Negation here because supertest treats falsy values as success
                const val = resp.body.list.every((lang, idx) => {
                    return langs[idx] === lang.id;
                });
                if (!val) {
                    throw new Error('Language list is wrong');
                }
            })
            .end(done);
    });

    it('it should work if mycookie is invalid', done => {
        const app = getApp({
            'x-forwarded-host': 'kit.f0rmat1k.nodejs.dev.spec.yandex.com.tr',
            'x-forwarded-for': '193.0.152.59',
            'accept-language': 'FR-ca, fr;q=0.8, en;q=0.7',
            cookie: 'my=',
        }, {
            list: true,
        });
        request(app)
            .get('/')
            .expect(200)
            .end(done);
    });

    it('it should set error field if my cookie is invalid', done => {
        const app = getApp({
            'x-forwarded-host': 'kit.f0rmat1k.nodejs.dev.spec.yandex.com.tr',
            'x-forwarded-for': '193.0.152.59',
            'accept-language': 'ru, en;q=0.7',
            cookie: 'my=1',
        }, {
            list: true,
        });
        request(app)
            .get('/')
            .expect(200)
            .expect({
                id: 'ru',
                name: 'Ru',
                list: [
                    { id: 'tr', name: 'Tr' },
                    { id: 'en', name: 'En' },
                    { id: 'ru', name: 'Ru' },
                ],
                errors: { mycookie: {} },
                isError: true,
            })
            .end(done);
    });

    it('it should return default fallback language if error and `fallbackLanguage` is not set', done => {
        const app = getApp({
            'x-forwarded-host': 'kit.f0rmat1k.nodejs.dev.spec.yandex.ru',
            'x-forwarded-for': '193.0.152.59',
            'accept-language': 100,
        });
        request(app)
            .get('/')
            .expect(200)
            .expect({
                id: 'ru',
                name: 'Ru',
                fallbackId: true,
            })
            .end(done);
    });

    it('it should return fallback language if error and `fallbackLanguage` is set', done => {
        const app = getApp({
            'x-forwarded-host': 'kit.f0rmat1k.nodejs.dev.spec.yandex.ru',
            'x-forwarded-for': '193.0.152.59',
            'accept-language': 100,
        }, {
            fallbackLanguage: { id: 'en', name: 'En' },
        });
        request(app)
            .get('/')
            .expect(200)
            .expect({
                id: 'en',
                name: 'En',
                fallbackId: true,
            })
            .end(done);
    });

    it('it should return fallback language list if error and `fallbackLanguage` is set', done => {
        const app = getApp({
            'x-forwarded-host': 'kit.f0rmat1k.nodejs.dev.spec.yandex.ru',
            'x-forwarded-for': '193.0.152.59',
            'accept-language': 100,
        }, {
            fallbackLanguage: { id: 'en', name: 'En' },
            list: true,
        });
        request(app)
            .get('/')
            .expect(200)
            .expect(res => {
                if (res.body.errors.list) {
                    res.body.errors.list = true;
                }
            })
            .expect({
                id: 'en',
                name: 'En',
                list: [
                    {
                        id: 'en',
                        name: 'En',
                    },
                ],
                fallbackId: true,
                fallbackList: true,
                isError: true,
                errors: {
                    list: true,
                },
            })
            .end(done);
    });
});

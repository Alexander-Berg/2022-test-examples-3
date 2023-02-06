/* global describe, it */

const request = require('supertest');
const getApp = require('./app.js');

describe('language detection', function() {
    describe('with accept-language', function() {
        it('should set russian for com domain and ru-RU language', function(done) {
            const app = getApp({
                'x-forwarded-host': 'kit.f0rmat1k.nodejs.dev.spec.yandex.com',
                'accept-language': 'ru-RU',
                'x-forwarded-for': '87.250.248.233',
            }, {
                availableLanguages: { com: ['ru', 'en'] },
            });
            request(app)
                .get('/')
                .expect({ id: 'ru', name: 'Ru' })
                .end(done);
        });

        it('should set russian for ru domain and `ru-RU,en-US` language', function(done) {
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
    });

    describe('with defaultLanguage', function() {
        it('should set english for ru domain', function(done) {
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

    it('should set russian for ru domain', function(done) {
        const app = getApp({
            'x-forwarded-host': 'kit.f0rmat1k.nodejs.dev.spec.yandex.ru',
            'x-forwarded-for': '87.250.248.233',
        });
        request(app)
            .get('/')
            .expect({ id: 'ru', name: 'Ru' })
            .end(done);
    });

    it('should set english for com domain', function(done) {
        const app = getApp({
            'x-forwarded-host': 'kit.f0rmat1k.nodejs.dev.spec.yandex.com',
            'x-forwarded-for': '87.250.248.233',
        });
        request(app)
            .get('/')
            .expect({ id: 'en', name: 'En' })
            .end(done);
    });
});

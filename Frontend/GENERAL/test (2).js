import { URLSearchParams } from 'url';
import test from 'ava';
import request from 'supertest';
import express from 'express';

import expressAlternateLangs from './index.js';

function createApp(config) {
    const app = express();

    app.use(expressAlternateLangs(config));
    app.all('/*', (req, res) => res.send());
    app.use((err, req, res) => {
        console.error(err);
        res.sendStatus(500);
    });

    return app;
}

test('headers', async t => {
    const app = createApp({
        domainsByLang: {
            ru: 'ru',
            en: 'com',
        },
    });

    await request(app)
        .post('/')
        .set('Host', 'yandex.ru')
        .then(response => {
            t.is(response.statusCode, 200);

            t.deepEqual(response.headers.link, [
                '<http://yandex.ru/>; rel="alternate"; hreflang="ru"',
                '<http://yandex.com/>; rel="alternate"; hreflang="en"',
            ].join(', '));
        });
});

test('x-default', async t => {
    const app = createApp({
        domainsByLang: {
            'x-default': 'ru',
            ru: 'ru',
            'en-US': 'com',
            'en-UK': 'com',
        },
    });

    await request(app)
        .post('/')
        .set('Host', 'yandex.ru')
        .then(response => {
            t.is(response.statusCode, 200);

            t.deepEqual(response.headers.link, [
                '<http://yandex.ru/>; rel="alternate"; hreflang="x-default"',
                '<http://yandex.ru/>; rel="alternate"; hreflang="ru"',
                '<http://yandex.com/>; rel="alternate"; hreflang="en-US"',
                '<http://yandex.com/>; rel="alternate"; hreflang="en-UK"',
            ].join(', '));
        });
});

test('Check default', async t => {
    const app = createApp();

    await request(app)
        .post('/')
        .set('Host', 'yandex.ru')
        .then(response => {
            t.is(response.statusCode, 200);

            t.deepEqual(response.headers.link, [
                '<http://yandex.ru/>; rel="alternate"; hreflang="ru"',
            ].join(', '));
        });
});

test('Check config', async t => {
    const config = {
        domainsByLang: {
            ru: 'ru',
            en: 'com',
            tr: 'com.tr',
            uk: 'com.ua',
            by: 'by',
            fr: 'fr',
        },
    };

    const app = createApp(config);

    await Promise.all(Object.keys(config.domainsByLang).map(tld => {
        return request(app)
            .post('/')
            .set('Host', `yandex.${tld}`)
            .then(response => {
                t.is(response.statusCode, 200);

                t.deepEqual(response.headers.link, [
                    '<http://yandex.ru/>; rel="alternate"; hreflang="ru"',
                    '<http://yandex.com/>; rel="alternate"; hreflang="en"',
                    '<http://yandex.com.tr/>; rel="alternate"; hreflang="tr"',
                    '<http://yandex.com.ua/>; rel="alternate"; hreflang="uk"',
                    '<http://yandex.by/>; rel="alternate"; hreflang="by"',
                    '<http://yandex.fr/>; rel="alternate"; hreflang="fr"',
                ].join(', '));
            });
    }));
});

test('All in .ru', async t => {
    const app = createApp({
        domainsByLang: {
            ru: 'ru',
            en: 'ru',
            tr: 'ru',
            uk: 'ru',
            by: 'ru',
            fr: 'ru',
        },
    });

    await request(app)
        .post('/')
        .set('Host', 'yandex.ru')
        .then(response => {
            t.is(response.statusCode, 200);

            t.deepEqual(response.headers.link, [
                '<http://yandex.ru/>; rel="alternate"; hreflang="ru"',
                '<http://yandex.ru/>; rel="alternate"; hreflang="en"',
                '<http://yandex.ru/>; rel="alternate"; hreflang="tr"',
                '<http://yandex.ru/>; rel="alternate"; hreflang="uk"',
                '<http://yandex.ru/>; rel="alternate"; hreflang="by"',
                '<http://yandex.ru/>; rel="alternate"; hreflang="fr"',
            ].join(', '));
        });
});

test('All in .ru with custom url creator', async t => {
    const app = createApp({
        domainsByLang: {
            ru: 'ru',
            en: 'ru',
            tr: 'ru',
            uk: 'ru',
            by: 'ru',
            fr: 'ru',
        },
        urlTransformer: (url, tld, lang) => {
            const params = new URLSearchParams(url.query);
            params.set('lang', lang);

            url.search = params;

            return url;
        },
    });

    await request(app)
        .post('/')
        .set('Host', 'yandex.ru')
        .then(response => {
            t.is(response.statusCode, 200);

            t.deepEqual(response.headers.link, [
                '<http://yandex.ru/?lang=ru>; rel="alternate"; hreflang="ru"',
                '<http://yandex.ru/?lang=en>; rel="alternate"; hreflang="en"',
                '<http://yandex.ru/?lang=tr>; rel="alternate"; hreflang="tr"',
                '<http://yandex.ru/?lang=uk>; rel="alternate"; hreflang="uk"',
                '<http://yandex.ru/?lang=by>; rel="alternate"; hreflang="by"',
                '<http://yandex.ru/?lang=fr>; rel="alternate"; hreflang="fr"',
            ].join(', '));
        });
});

test('save query', async t => {
    const app = createApp({
        domainsByLang: {
            ru: 'ru',
            en: 'com',
        },
    });

    await request(app)
        .post('/download?partner_id=portal')
        .set('Host', 'yandex.ru')
        .then(response => {
            t.is(response.statusCode, 200);

            t.deepEqual(response.headers.link, [
                '<http://yandex.ru/download?partner_id=portal>; rel="alternate"; hreflang="ru"',
                '<http://yandex.com/download?partner_id=portal>; rel="alternate"; hreflang="en"',
            ].join(', '));
        });
});

test('x-original-url exist', async t => {
    const app = createApp({
        domainsByLang: {
            ru: 'ru',
            en: 'com',
        },
    });

    await request(app)
        .post('/download?partner_id=portal')
        .set('X-Original-Url', 'https://browser.yandex.ru:80/foo?bar')
        .set('Host', 'yandex.ru')
        .then(response => {
            t.is(response.statusCode, 200);

            t.deepEqual(response.headers.link, [
                '<https://browser.yandex.ru:80/foo?bar>; rel="alternate"; hreflang="ru"',
                '<https://browser.yandex.com:80/foo?bar>; rel="alternate"; hreflang="en"',
            ].join(', '));
        });
});

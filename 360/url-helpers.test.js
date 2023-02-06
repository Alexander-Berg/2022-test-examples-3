'use strict';

const Url = require('url');
const urlHelpers = require('./url-helpers.js');

describe('buildUrl', function() {
    it('заменяет адрес', function() {
        const parsedUrl = Url.parse('http://[::1]:1234');
        expect(urlHelpers.buildUrl(parsedUrl, 'a.ru')).toEqual('http://a.ru:1234/');
    });

    it('работает с опциями из got', function() {
        const parsedUrl = {
            method: 'GET',
            headers: {
                host: 'hound.prestable-new-tabs.hound.mail.stable.qloud-d.yandex.net'
            },
            href: 'http://[::1]:1234/p/a/t/h',
            pathname: '/p/a/t/h',
            search: null,
            hash: null,
            hostname: '::1',
            port: '1234',
            host: '::1',
            path: '/p/a/t/h?a=1&b=2&b=3',
            protocol: 'http:'
        };
        expect(urlHelpers.buildUrl(parsedUrl, 'example.com'))
            .toEqual('http://example.com:1234/p/a/t/h?a=1&b=2&b=3');
    });
});

describe('getHost', function() {
    it('возвращает хост из заголовков, если есть', function() {
        const parsedUrl = Url.parse('http://[::1]:1234');
        const requestInfo = {
            options: parsedUrl,
            headers: {
                'host': 'a.ru'
            }
        };
        expect(urlHelpers.getHost(requestInfo)).toEqual('a.ru');
    });

    it('возвращает оригинальный хост, если в заголовках нет', function() {
        const parsedUrl = Url.parse('http://[::1]:1234');
        const requestInfo = {
            options: parsedUrl
        };
        expect(urlHelpers.getHost(requestInfo)).toEqual('::1');
    });
});

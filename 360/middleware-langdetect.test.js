'use strict';

const httpMock = require('node-mocks-http');
const langdetect = require('./middleware-langdetect.js');

let req;
let res;
let middleware;

beforeEach(() => {
    res = httpMock.createResponse();
});

test('query param', (done) => {
    req = httpMock.createRequest({ query: { lang: 'ru' } });
    middleware = langdetect();

    middleware(req, res, () => {
        expect(req.locale).toEqual('ru');
        done();
    });
});

test('custom query param', (done) => {
    req = httpMock.createRequest({ query: { lng: 'ru' } });
    middleware = langdetect({ targetQuery: 'lng' });

    middleware(req, res, () => {
        expect(req.locale).toEqual('ru');
        done();
    });
});

test('body param', (done) => {
    req = httpMock.createRequest({ body: { _locale: 'en' } });
    middleware = langdetect();

    middleware(req, res, () => {
        expect(req.locale).toEqual('en');
        done();
    });
});

test('custom body param', (done) => {
    req = httpMock.createRequest({ body: { _loc: 'en' } });
    middleware = langdetect({ targetBody: '_loc' });

    middleware(req, res, () => {
        expect(req.locale).toEqual('en');
        done();
    });
});

test('header param', (done) => {
    req = httpMock.createRequest({ headers: { 'x-lang': 'tr' } });
    middleware = langdetect({ targetHeader: 'x-lang' });

    middleware(req, res, () => {
        expect(req.locale).toEqual('tr');
        done();
    });
});

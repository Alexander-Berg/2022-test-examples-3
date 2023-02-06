'use strict';

const httpMock = require('node-mocks-http');

let router;
const mockRoute = jest.fn();

jest.mock('./models/route.js', () => (req, res, next) => {
    mockRoute();
    res.status(200).send('ok');
    next();
});
jest.unmock('@yandex-int/duffman');

describe('call-api-method', () => {
    beforeEach(() => {
        router = require('./index.js');
    });

    it('skip unmatched url', (done) => {
        const req = httpMock.createRequest({ url: '/skip/me' });
        const res = httpMock.createResponse();
        router(req, res, (err) => {
            expect(mockRoute).not.toHaveBeenCalled();
            done(err);
        });
    });

    it('process matched url', (done) => {
        const req = httpMock.createRequest({ url: '/models' });
        const res = httpMock.createResponse();
        router(req, res, (err) => {
            expect(mockRoute).toHaveBeenCalled();
            done(err);
        });
    });

    describe('checks cors', () => {
        it('corp', (done) => {
            process.env.ENVIRONMENT_NAME = 'intranet';
            const req = httpMock.createRequest({
                url: '/models', headers: {
                    Origin: 'https://mail.yandex-team.ru',
                    Host: 'mail.yandex-team.ru'
                }
            });
            const res = httpMock.createResponse();
            router(req, res, (err) => {
                delete process.env.ENVIRONMENT_NAME;
                expect(mockRoute).toHaveBeenCalled();
                expect(res.getHeader('Access-Control-Allow-Origin')).toBe('https://mail.yandex-team.ru');
                done(err);
            });
        });

        it('corp with wrong tld', (done) => {
            process.env.ENVIRONMENT_NAME = 'intranet';
            const req = httpMock.createRequest({
                url: '/models', headers: {
                    Origin: 'https://mail.yandex.ru',
                    Host: 'mail.yandex-team.ru'
                }
            });
            const res = httpMock.createResponse();
            router(req, res, (err) => {
                delete process.env.ENVIRONMENT_NAME;
                expect(mockRoute).toHaveBeenCalled();
                expect(res.getHeader('Access-Control-Allow-Origin')).toBeUndefined();
                done(err);
            });
        });

        it('known tld', (done) => {
            const req = httpMock.createRequest({
                url: '/models', headers: {
                    Origin: 'https://mail360.yandex.ru',
                    Host: 'mail.yandex.ru'
                }
            });
            const res = httpMock.createResponse();
            router(req, res, (err) => {
                expect(mockRoute).toHaveBeenCalled();
                expect(res.getHeader('Access-Control-Allow-Origin')).toBe('https://mail360.yandex.ru');
                done(err);
            });
        });

        it('wrong tld', (done) => {
            const req = httpMock.createRequest({
                url: '/models', headers: {
                    Origin: 'https://mail360.yandex.ru',
                    Host: 'mail.yandex.com.tr'
                }
            });
            const res = httpMock.createResponse();
            router(req, res, (err) => {
                expect(mockRoute).toHaveBeenCalled();
                expect(res.getHeader('Access-Control-Allow-Origin')).toBeUndefined();
                done(err);
            });
        });

        it('unknown origin', (done) => {
            const req = httpMock.createRequest({
                url: '/models', headers: {
                    Origin: 'example.info',
                    Host: 'mail.yandex.ru'
                }
            });
            const res = httpMock.createResponse();
            router(req, res, (err) => {
                expect(mockRoute).toHaveBeenCalled();
                expect(res.getHeader('Access-Control-Allow-Origin')).toBeUndefined();
                done(err);
            });
        });

        it('unknown domain', (done) => {
            const req = httpMock.createRequest({
                url: '/models', headers: {
                    Origin: 'example.info',
                    Host: 'mail.yandex.info'
                }
            });
            const res = httpMock.createResponse();
            router(req, res, (err) => {
                expect(mockRoute).toHaveBeenCalled();
                expect(res.getHeader('Access-Control-Allow-Origin')).toBeUndefined();
                done(err);
            });
        });
    });
});

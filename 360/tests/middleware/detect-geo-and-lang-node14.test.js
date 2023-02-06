jest.mock('../../helpers/bindings-node14');
const mockedBindings = require('../../helpers/bindings-node14');
import { popFnCalls } from '../helpers.js';
const getDetectGeoAndLangMiddleware = require('../../middleware/detect-geo-and-lang-node14');

const DEFAULT_CONFIG = { detectLang: true, tld: 'ru' };
const DEFAULT_REQ = { headers: { host: 'disk.yandex.ru', cookies: {} } };

describe('getDetectGeoAndLangMiddleware', () => {
    it('должна положить в req regionId', (done) => {
        const req = Object.assign({}, DEFAULT_REQ);
        getDetectGeoAndLangMiddleware(DEFAULT_CONFIG)(req, {}, () => {
            expect(req.regionId).toBe(10493);
            expect(popFnCalls(mockedBindings.langDetector.find).length).toEqual(1);
            done();
        });
    });

    it('должна положить в req флаг lang', (done) => {
        const req = Object.assign({}, DEFAULT_REQ);
        getDetectGeoAndLangMiddleware(DEFAULT_CONFIG)(req, {}, () => {
            expect(req.lang).toBe('ru');
            expect(popFnCalls(mockedBindings.langDetector.find).length).toEqual(1);
            done();
        });
    });

    it('должна корректно учитывать язык пользователя', (done) => {
        const req = Object.assign({}, DEFAULT_REQ, { user: { lang: 'en' } });
        getDetectGeoAndLangMiddleware(DEFAULT_CONFIG)(req, {}, () => {
            expect(req.lang).toBe('en');
            expect(popFnCalls(mockedBindings.langDetector.find).length).toEqual(1);
            done();
        });
    });

    it('должна корректно учитывать куку my', (done) => {
        const req = Object.assign({}, DEFAULT_REQ, { cookies: { my: 'YycCAAgA' } });
        getDetectGeoAndLangMiddleware(DEFAULT_CONFIG)(req, {}, () => {
            expect(req.lang).toBe('tr');
            expect(popFnCalls(mockedBindings.langDetector.find).length).toEqual(1);
            done();
        });
    });

    it('не должна определять язык если передан параметр detectLang==false', (done) => {
        const req = Object.assign({}, DEFAULT_REQ);
        const config = Object.assign({}, DEFAULT_CONFIG, { detectLang: false });
        getDetectGeoAndLangMiddleware(config)(req, {}, () => {
            expect(req.lang).toBeUndefined();
            expect(popFnCalls(mockedBindings.langDetector.find).length).toEqual(0);
            done();
        });
    });

    it('yadi.sk - должна определять язык с учётом домена если не передан параметр ignoreDomain', (done) => {
        const req = Object.assign({}, DEFAULT_REQ, {
            headers: Object.assign({}, DEFAULT_REQ.headers, { host: 'yadi.sk' })
        });
        const config = Object.assign({}, DEFAULT_CONFIG, {
            tld: 'sk'
        });
        getDetectGeoAndLangMiddleware(config)(req, {}, () => {
            expect(req.lang).toBe('ru');

            const findCalls = popFnCalls(mockedBindings.langDetector.find);
            expect(findCalls.length).toEqual(1);
            expect(findCalls[0][0].domain).toEqual('yadi.sk');
            expect(popFnCalls(mockedBindings.langDetector.findWithoutDomain).length).toEqual(0);

            done();
        });
    });

    it('yadi.sk - должна определять язык без учёта домена если передан параметр ignoreDomain==true', (done) => {
        const req = Object.assign({}, DEFAULT_REQ, {
            headers: Object.assign({}, DEFAULT_REQ.headers, { host: 'yadi.sk' })
        });
        const config = Object.assign({}, DEFAULT_CONFIG, {
            tld: 'sk',
            ignoreDomain: true
        });
        getDetectGeoAndLangMiddleware(config)(req, {}, () => {
            expect(req.lang).toBe('en');

            expect(popFnCalls(mockedBindings.langDetector.find).length).toEqual(0);
            const findWithoutDomainCalls = popFnCalls(mockedBindings.langDetector.findWithoutDomain);
            expect(findWithoutDomainCalls.length).toEqual(1);
            expect(findWithoutDomainCalls[0][0].findWithoutDomainCalls).toBeUndefined();

            done();
        });
    });

    it('должна определять таймзону если передан параметр shouldGetTimezone == true', (done) => {
        const req = Object.assign({}, DEFAULT_REQ);
        const config = Object.assign({}, DEFAULT_CONFIG, { shouldGetTimezone: true });
        getDetectGeoAndLangMiddleware(config)(req, {}, () => {
            expect(req.geoTimezone).toEqual('Europe/Moscow');
            done();
        });
    });
});

import express from 'express';
import request from 'supertest';

jest.mock('@ps-int/ufo-server-side-commons/helpers/bindings-node14', () => ({}));
import detectGeoAndLang from '../../../app/middleware/detect-geo-and-lang';
import logger from '../../../app/tskv';

import mockedSSCDetectGeoAndLang from '@ps-int/ufo-server-side-commons/middleware/detect-geo-and-lang-node14';

describe('detect-geo-and-lang middleware', function() {
    beforeEach(() => {
        this.app = express();
        this.app.use((req, res, next) => {
            if (req.query.url) {
                req.fileUrl = req.query.url;
                req.parsedUrl = {
                    protocol: req.query.url.split('//')[0]
                };
            }

            next();
        });
        this.agent = request(this.app);
    });

    const check = (app, agent, path, done, expectedParams) => {
        detectGeoAndLang(app);

        agent.get(path)
            .end(() => {
                const geoAndLangDetectCalls = popFnCalls(mockedSSCDetectGeoAndLang);
                expect(geoAndLangDetectCalls.length).toEqual(1);
                expect(geoAndLangDetectCalls[0][0]).toEqual(expectedParams);

                done();
            });
    };

    it('for `ya-mail:` document', (done) => {
        check(this.app, this.agent, `/?url=${encodeURIComponent('ya-mail://mail-attach-path')}`, done, {
            detectLang: true,
            ignoreDomain: false,
            logger
        });
    });

    it('for `ya-disk-public:` document', (done) => {
        check(this.app, this.agent, `/?url=${encodeURIComponent('ya-disk-public://public-hash')}`, done, {
            detectLang: true,
            ignoreDomain: false,
            logger
        });
    });

    it('for `ya-disk:` document', (done) => {
        check(this.app, this.agent, `/?url=${encodeURIComponent('ya-disk:///disk/Загрузки/хз.doc')}`, done, {
            detectLang: true,
            ignoreDomain: false,
            logger
        });
    });

    it('for `ya-browser:` document', (done) => {
        check(this.app, this.agent, `/?url=${encodeURIComponent('ya-browser://mds-key')}`, done, {
            detectLang: true,
            ignoreDomain: true,
            logger
        });
    });

    it('for root page', (done) => {
        check(this.app, this.agent, '/', done, {
            detectLang: true,
            ignoreDomain: undefined,
            logger
        });
    });

    it('should not detect lang if has lang query parameter', (done) => {
        this.app.use((req, res, next) => {
            req.query.lang = 'en';
            next();
        });
        check(this.app, this.agent, '/', done, {
            detectLang: false,
            ignoreDomain: undefined,
            logger
        });
    });

    it('should detect lang if has lang query parameter but lang is not supported', (done) => {
        this.app.use((req, res, next) => {
            req.query.lang = 'by';
            next();
        });
        check(this.app, this.agent, '/', done, {
            detectLang: true,
            ignoreDomain: undefined,
            logger
        });
    });

    it('should detect lang if has lang query parameter but lang is not in available langs list', (done) => {
        this.app.use((req, res, next) => {
            req.query.lang = 'ru';
            req.tld = 'tr';
            next();
        });
        check(this.app, this.agent, '/', done, {
            detectLang: true,
            ignoreDomain: undefined,
            logger
        });
    });
});

'use strict';

const ip = '5.45.241.138';
const request = require('supertest');
const mockery = require('mockery');

const path = require('path');

process.env.NODE_ENV = process.env.NODE_ENV || require('yandex-environment') || 'development';
process.env.CFG_DIR = path.join(__dirname, '..', '..', '..', 'configs');

const config = require('cfg');

const getSessionIdOfUser = require('../helpers/helpers').getSessionIdOfUser;
const nocks = require('../helpers/nocks');

require('chai').should();

describe('Exam integration', () => {
    var app;

    function testRender(req, res, next) {
        res.bundle = {
            render: function (bundleName, data) {
                res.json(data);
            }
        };
        next();
    }

    before(() => {
        // Отключаем рендеринг
        mockery.registerMock('./middleware/main-menu', () => function (req, res, next) {
            req.base = {
                mainMenu: {
                    rootBreadcrumbs: [],
                    additionalBreadcrumbs: []
                },
                breadcrumbsChain: []
            };
            next();
        });
        mockery.registerMock('./middleware/express-bundle-response', () => testRender);
        mockery.registerMock('express-bunker', () => function (req, res, next) {
            req.bunker = {
                sources: {
                    ru: {
                        seo: {
                            exam: {
                                title: '{{content.exam.title}}',
                                description: '{{content.exam.seoDescription}}',
                                keywords: '',
                                ogTitle: '{{content.exam.title}}',
                                ogDescription: '{{content.exam.ogDescription}}',
                                ogImage: ''
                            }
                        },
                        settings: {
                            maintenance: { exams: [] }
                        },
                        exams: {
                            direct: { terms: {}}
                        }
                    }
                }
            };
            next();
        });
        mockery.enable({
            warnOnReplace: false,
            warnOnUnregistered: false,
            useCleanCache: true
        });
        app = require('../../app');
    });

    it('should redirect to /auth when user is unauthorized', done => {
        nocks.nockMyCertificates();

        request(app)
            .get(`${config.router.namespace}/certificates/my`)
            .set('x-forwarded-for', ip)
            .set('Host', 'yandex.ru')
            .expect(302, (err, data) => {
                var headers = data.headers;
                headers.location.should.to.equal('/adv/expert/auth?retpath=');
                done();
            });
    });

    it('should return exam when user is authorized', done => {
        getSessionIdOfUser('bloguser-05', 'bloguser')
            .then(sessionId => {
                nocks.nockExamDirect();
                nocks.nockAttemptDirectCheck();

                request(app)
                    .get(`${config.router.namespace}/exam/direct`)
                    .set('x-forwarded-for', ip)
                    .set('Cookie', sessionId)
                    .set('Host', 'yandex.ru')
                    .expect(200, (err, data) => {
                        var body = data.res.body;
                        body.should.to.have.property('content')
                            .that.is.an('object');

                        done();
                    });
            });
    });

    it('should contain correct `seo` field', done => {
        getSessionIdOfUser('bloguser-05', 'bloguser')
            .then(sessionId => {
                nocks.nockExamDirect();
                nocks.nockAttemptDirectCheck();

                request(app)
                    .get(`${config.router.namespace}/exam/direct`)
                    .set('x-forwarded-for', ip)
                    .set('Cookie', sessionId)
                    .set('Host', 'yandex.ru')
                    .expect(200, (err, data) => {
                        var body = data.res.body;

                        body.should.to.have.property('seo').that.is.an('object');
                        body.seo.should.to.have.all.keys([
                            'title', 'description', 'keywords', 'ogTitle', 'ogDescription', 'ogImage'
                        ]);

                        done();
                    });
            });
    });

    it('should return 404 if `id` is not specified', done => {
        nocks.nockExamCluster('exam');

        request(app)
            .get(`${config.router.namespace}/exam/`)
            .set('x-forwarded-for', ip)
            .set('Host', 'yandex.ru')
            .expect(404, done);
    });

    afterEach(() => {
        nocks.cleanAll();
    });

    after(() => {
        mockery.disable();
    });
});

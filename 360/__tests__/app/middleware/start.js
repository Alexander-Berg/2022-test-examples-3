import express from 'express';
import request from 'supertest';

let mockIsBrowserSupported = true;

jest.mock('asker-as-promised', () => ({ Error: Object }));
jest.mock('../../../app/render');
jest.mock('@ps-int/ufo-server-side-commons/helpers/geo-and-lang-detect-node14', () => ({ getAvailableLangs: () => ['fr'] }));
jest.mock('../../../app/check-browser-support', () => () => mockIsBrowserSupported);
import render from '../../../app/render';
import tokenHelper from '../../../app/token';
const popRenderCalls = render.popCalls;

import start from '../../../app/middleware/start';

jest.mock('../../../config', () => require('../../../configs/index.testing'));
jest.mock('terror');

jest.mock('../../../app/secrets');

tokenHelper.parse = () => ({});
tokenHelper.format = (data) => `token for ${JSON.stringify(Object.assign({}, data, { ts: 0 }))} (ts replaced with 0)`;

describe('middleware start', function() {
    describe('url `/`', () => {
        beforeEach(() => {
            this.app = express();

            this.mockStart = jest.fn().mockResolvedValue({});

            this.app.use((req, res, next) => {
                req.user = {
                    id: '0',
                    login: ''
                };
                req.cookies = {
                    yandexuid: 1234
                };
                req.backend = {
                    start: this.mockStart
                };
                req.ua = {};
                req.token = {};

                next();
            });

            this.agent = request(this.app);
        });

        it('should render NO-URL for request without url', (done) => {
            start(this.app);

            this.agent.get('/')
                .end((error) => {
                    expect(error).toBeNull();
                    expect(popRenderCalls()).toMatchSnapshot();
                    done();
                });
        });

        it('should render 400 for unknown protocol', (done) => {
            this.app.use((req, res, next) => {
                req.parsedUrl = {
                    protocol: 'foo:'
                };
                next();
            });
            start(this.app);

            this.agent.get('/?url=foo://bar')
                .end((error) => {
                    expect(error).toBeNull();
                    expect(popRenderCalls()).toMatchSnapshot();
                    done();
                });
        });

        it('should render passport-redirect if auth is needed but not present', (done) => {
            this.app.use((req, res, next) => {
                req.parsedUrl = {
                    protocol: 'ya-mail:'
                };
                next();
            });
            start(this.app);

            this.agent.get('/?url=ya-mail://bar')
                .end((error) => {
                    expect(error).toBeNull();
                    expect(popRenderCalls()).toMatchSnapshot();
                    done();
                });
        });

        it('should render passport-redirect if uid in query is not equal to current uid', (done) => {
            this.app.use((req, res, next) => {
                req.user = {
                    id: 1234,
                    auth: true
                };
                req.parsedUrl = {
                    protocol: 'ya-mail:'
                };

                next();
            });
            start(this.app);

            this.agent.get('/?url=ya-mail://bar&uid=4321')
                .end((error) => {
                    expect(error).toBeNull();
                    expect(popRenderCalls()).toMatchSnapshot();
                    done();
                });
        });

        it('should render 403 error page for second time if auth is needed but not present', (done) => {
            this.app.use((req, res, next) => {
                req.fileUrl = 'ya-mail://some-file-attachment';
                req.parsedUrl = {
                    protocol: 'ya-mail:'
                };
                req.cookies.needAuth = req.fileUrl;
                next();
            });
            start(this.app);

            this.agent.get('/?url=ya-mail://some-file-attachment')
                .end((error) => {
                    expect(error).toBeNull();
                    expect(popRenderCalls()).toMatchSnapshot();
                    done();
                });
        });

        it('should render old browser stub if ua is not supported', (done) => {
            mockIsBrowserSupported = false;

            this.app.use((req, res, next) => {
                req.user = {
                    id: 1234,
                    auth: true
                };
                req.parsedUrl = {
                    protocol: 'ya-mail:'
                };

                next();
            });

            start(this.app);

            this.agent.get('/').end((error) => {
                expect(error).toBeNull();
                expect(popRenderCalls()).toMatchSnapshot();

                mockIsBrowserSupported = true;
                done();
            });
        });

        it('should render error if start answered with error', (done) => {
            this.mockStart.mockResolvedValueOnce({
                error: true,
                statusCode: 520
            });

            this.app.use((req, res, next) => {
                req.user = {
                    id: 1234,
                    auth: true
                };
                req.parsedUrl = {
                    protocol: 'ya-mail:'
                };

                next();
            });

            start(this.app);
            this.agent.get('/?url=ya-mail://bar&name=bar')
                .end((error) => {
                    expect(error).toBeNull();
                    expect(popRenderCalls()).toMatchSnapshot();
                    done();
                });
        });

        it('should render error if start rejected', (done) => {
            this.app.use((req, res, next) => {
                req.user = {
                    id: 1234,
                    auth: true
                };
                req.parsedUrl = {
                    protocol: 'ya-mail:'
                };
                req.backend = {
                    start: jest.fn(() => Promise.reject(new Error('Test error')))
                };

                next();
            });

            start(this.app);
            this.agent.get('/?url=ya-mail://bar&name=bar')
                .end((error) => {
                    expect(error).toBeNull();
                    expect(popRenderCalls()).toMatchSnapshot();
                    done();
                });
        });

        it('should render start if everything is fine', (done) => {
            this.app.use((req, res, next) => {
                req.user = {
                    id: 1234,
                    auth: true
                };
                req.parsedUrl = {
                    protocol: 'ya-mail:'
                };

                next();
            });

            start(this.app);
            this.agent.get('/?url=ya-mail://bar&name=bar')
                .end((error) => {
                    expect(error).toBeNull();
                    expect(popRenderCalls()).toMatchSnapshot();
                    done();
                });
        });

        it('should redirect to URL without action if action is in query', (done) => {
            this.app.use((req, res, next) => {
                req.user = {
                    id: 1234,
                    auth: true
                };
                req.parsedUrl = {
                    protocol: 'ya-disk-public:'
                };

                next();
            });

            start(this.app);
            this.agent.get('/?url=ya-disk-public://some-hash&name=some-name&action=save-to-disk')
                .end((error) => {
                    expect(error).toBeNull();
                    const renderCalls = popRenderCalls();
                    expect(renderCalls).toMatchSnapshot();

                    // на всякий случай явно проверяем наличие redirect и отсутствие action в query в нём
                    expect(renderCalls.length).toEqual(1);
                    expect(renderCalls[0][1].redirect).toBeDefined();
                    expect(renderCalls[0][1].redirect.query.action).toBeUndefined();

                    done();
                });
        });
    });

    describe('url `/view/`', () => {
        beforeEach(() => {
            this.app = express();

            this.app.use((req, res, next) => {
                req.token = {};
                req.cookies = {};

                next();
            });

            this.agent = request(this.app);
        });

        it('should render 400 if token is missing', (done) => {
            this.app.use((req, res, next) => {
                delete req.token;
                next();
            });
            start(this.app);

            this.agent.get('/view/0/').end((error) => {
                expect(error).toBeNull();
                expect(popRenderCalls()).toMatchSnapshot();

                done();
            });
        });

        it('should render error if previous middleware errored', (done) => {
            this.app.use((req, res, next) => {
                req.error = true;
                req.errorCode = 403;
                next();
            });
            start(this.app);

            this.agent.get('/view/0/').end((error) => {
                expect(error).toBeNull();
                expect(popRenderCalls()).toMatchSnapshot();

                done();
            });
        });

        it('should render old browser stub if ua is not supported', (done) => {
            mockIsBrowserSupported = false;

            start(this.app);

            this.agent.get('/view/0/').end((error) => {
                expect(error).toBeNull();
                expect(popRenderCalls()).toMatchSnapshot();

                mockIsBrowserSupported = true;
                done();
            });
        });

        it('should redirect to passport if password is not set yet', (done) => {
            this.app.use((req, res, next) => {
                req.cookies = {
                    'save-to-disk': 'needAuth:/disk/some-file'
                };
                req.user = {
                    auth: true,
                    hasPassword: false,
                    needsUpgrade: true
                };
                req.fileUrl = '/disk/some-file';

                next();
            });

            start(this.app);

            this.agent.get('/view/0/')
                .expect('set-cookie', /save-to-disk=noPassword/)
                .end((error) => {
                    expect(error).toBeNull();
                    expect(popRenderCalls()).toMatchSnapshot();

                    done();
                });
        });

        it('should call `next` if current user matches user from token', (done) => {
            const viewMiddlewareMock = jest.fn((_, res) => res.end());

            this.app.use((req, res, next) => {
                req.token.uid = '1';
                req.user = { id: '1' };

                next();
            });

            start(this.app);

            this.app.use(viewMiddlewareMock);

            this.agent.get('/view/0/').end((error) => {
                expect(error).toBeNull();

                expect(viewMiddlewareMock.mock.calls.length).toBe(1);
                done();
            });
        });

        it('should redirect to passport for private protocols if token user mismatches authorized one', (done) => {
            this.app.use((req, res, next) => {
                req.token.uid = '1';
                req.user = { id: '2' };
                req.parsedUrl = { protocol: 'ya-disk:' };

                next();
            });

            start(this.app);

            this.agent.get('/view/0/').end((error) => {
                expect(error).toBeNull();
                expect(popRenderCalls()).toMatchSnapshot();

                done();
            });
        });

        it('should generate new token and successfully render', (done) => {
            this.app.use((req, res, next) => {
                req.token = {
                    uid: '2'
                };
                req.user = {
                    id: '1'
                };
                req.parsedUrl = {
                    protocol: 'ya-disk-public:'
                };
                req.query = {
                    lang: 'fr'
                };
                req.cookies = {
                    yandexuid: '222'
                };
                req.backend = {
                    start: jest.fn().mockResolvedValueOnce({})
                };

                next();
            });

            start(this.app);

            this.agent.get('/view/0/').end((error) => {
                expect(error).toBeNull();
                expect(popRenderCalls()).toMatchSnapshot();

                done();
            });
        });
    });
});

import express from 'express';
import request from 'supertest';

jest.mock('asker-as-promised', () => ({ Error: Object }));
jest.mock('../../../app/render');
jest.mock('../../../app/clck', () => (url, yandexuid) => `clck?url=${url}&yandexuid=${yandexuid}`);
import render from '../../../app/render';
const popRenderCalls = render.popCalls;

jest.mock('../../../app/secrets');
jest.mock('@ps-int/ufo-server-side-commons/helpers/bindings-node14');

jest.mock('../../../app/render/docviewer.ru', () => require('../../../app/render/__mocks__/docviewer'), { virtual: true });
import dvMock from '../../../app/render/docviewer.ru';
const matchActionSnapshots = () => {
    for (const prop in dvMock.actions) {
        const calls = popFnCalls(dvMock.actions[prop]);
        calls.unshift(`calls of action ${prop}`);
        expect(calls).toMatchSnapshot();
    }
};

jest.mock('../../../config', () => require('../../../configs/index.testing'));

import view from '../../../app/middleware/view';

const originalDateNow = Date.now;

describe('middleware view', function() {
    beforeEach(() => {
        Date.now = () => 1523274528093;
        this.app = express();

        this.mockBackend = jest.fn();
        this.mockStart = jest.fn(() => Promise.resolve({}));
        this.addMDSKeyMock = jest.fn();
        this.addDsidMock = jest.fn();
        this.app.use((req, res, next) => {
            req.user = {
                id: 0
            };
            req.token = {
                url: 'some-url',
                uid: '0'
            };

            req.yandexServices = {};
            req.ua = {};
            req.cookies = {};
            req.lang = 'ru';

            req.backend = {
                start: this.mockStart,
                getAPI: jest.fn(() => this.mockBackend),
                addMDSKey: this.addMDSKeyMock,
                addDsid: this.addDsidMock
            };
            req.tvmTickets = {
                backend: 'ticket'
            };
            req.fileUrl = 'some-url';

            next();
        });

        this.agent = request(this.app);
    });

    afterEach(() => {
        Date.now = originalDateNow;
    });

    it('should sync uids if uid in params and token do not match', (done) => {
        this.app.use((req, res, next) => {
            req.token = {
                url: 'yet-another-url',
                uid: '1',
                val: 'some-token-for-1'
            };

            next();
        });
        view(this.app);
        this.agent.get('/view/0/?*=some-token-for-1')
            .expect(() => {
                matchActionSnapshots();
                expect(popRenderCalls()).toMatchSnapshot();
                expect(popFnCalls(this.mockStart)).toMatchSnapshot();
            })
            .end(done);
    });

    it('should return 500 if convert saga is rejected', (done) => {
        const convert = dvMock.sagas.convert;
        dvMock.sagas.convert = () => Promise.reject();

        view(this.app);
        this.agent.get('/view/0')
            .expect(() => {
                matchActionSnapshots();
                expect(popRenderCalls()).toMatchSnapshot();
                expect(popFnCalls(this.mockStart)).toMatchSnapshot();
                dvMock.sagas.convert = convert;
            })
            .end(done);
    });

    it('proxy should allow uid 0', (done) => {
        view(this.app);
        this.agent.get('/view/0/source')
            .end((error, res) => {
                expect(res.status).toBe(204);
                expect(popRenderCalls()).toMatchSnapshot();
                done();
            });
    });

    it('proxy should allow if user uid corresponds to the one in url', (done) => {
        this.app.use((req, res, next) => {
            req.user = {
                id: '1234'
            };

            next();
        });
        view(this.app);
        this.agent.get('/view/1234/source')
            .end((error, res) => {
                expect(res.status).toBe(204);
                expect(popRenderCalls()).toMatchSnapshot();
                done();
            });
    });

    it('proxy should not allow if user uid does not correspond to the on in url', (done) => {
        this.app.use((req, res, next) => {
            req.user = {
                id: '1235'
            };

            next();
        });
        view(this.app);
        this.agent.get('/view/1234/source')
            .end((error, res) => {
                expect(res.status).toBe(403);
                expect(popRenderCalls().length).toEqual(0);
                done();
            });
    });

    it('proxy should allow if user uid is present in one of the accounts', (done) => {
        this.app.use((req, res, next) => {
            req.user = {
                id: '12345',
                accounts: [{ id: '1234' }]
            };

            next();
        });
        view(this.app);
        this.agent.get('/view/1234/source')
            .end((error, res) => {
                expect(res.status).toBe(204);
                expect(popRenderCalls().length).toEqual(0);
                done();
            });
    });

    it('proxy should not allow if user uid is not present in accounts', (done) => {
        this.app.use((req, res, next) => {
            req.user = {
                id: '12345',
                accounts: [{ id: '123456' }]
            };

            next();
        });
        view(this.app);
        this.agent.get('/view/1234/source')
            .end((error, res) => {
                expect(res.status).toBe(403);
                expect(popRenderCalls().length).toEqual(0);
                done();
            });
    });

    it('proxy should call addMDSKey and addDsid', (done) => {
        view(this.app);
        this.agent.get('/view/0/source')
            .end(() => {
                expect(popFnCalls(this.addMDSKeyMock)).toMatchSnapshot();
                expect(popFnCalls(this.addDsidMock)).toMatchSnapshot();
                done();
            });
    });

    it('should set doc.serpUrl & doc.serpHost for document from SERP', (done) => {
        this.app.use((req, res, next) => {
            req.token.url = 'ya-serps://host/path?search';
            req.token.val = 'some-token';
            req.parsedUrl = {
                protocol: 'ya-serps:'
            };
            req.cookies = {
                yandexuid: 987654321
            };
            next();
        });

        view(this.app);
        this.agent.get('/view/0/?*=some-token')
            .end(() => {
                expect(popRenderCalls()).toMatchSnapshot();

                // явно проверяем что вызвался updateDoc с serpUrl и serpHost
                const updateDocCalls = dvMock.actions.updateDoc.mock.calls;
                expect(updateDocCalls.length).toEqual(1);
                expect(updateDocCalls[0][0].serpUrl).toBeDefined();
                expect(updateDocCalls[0][0].serpHost).toEqual('host');

                matchActionSnapshots();
                done();
            });
    });

    it('should dispatch save:forceRun if has save-to-disk cookie', (done) => {
        this.app.use((req, res, next) => {
            req.user = {
                id: 345,
                auth: true,
                hasPassword: true
            };
            req.fileUrl = 'ya-disk://some-file';
            req.cookies = {
                'save-to-disk': 'needAuth:ya-disk://some-file'
            };
            req.token = {
                uid: '345'
            };
            next();
        });

        view(this.app);
        this.agent.get('/view/345/?*=some-token')
            .end(() => {
                const renderCalls = popRenderCalls();
                expect(renderCalls.length).toEqual(1);
                expect(renderCalls[0]).toMatchSnapshot();

                // а явно проверяем что вызвался updateAction с save: { forceRun: true }
                const updateActionCalls = dvMock.actions.updateAction.mock.calls;
                expect(updateActionCalls.length).toEqual(1);
                expect(updateActionCalls[0][0]).toEqual('save');
                expect(updateActionCalls[0][1]).toEqual({ forceRun: true });

                matchActionSnapshots();

                done();
            });
    });
});

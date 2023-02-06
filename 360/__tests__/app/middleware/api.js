import express from 'express';
import request from 'supertest';

jest.mock('asker-as-promised', () => ({ Error: Object }));
const mockLogger = require('@ps-int/ufo-server-side-commons/tskv/default-logger')();

import api from '../../../app/middleware/api';

const popLoggerCalls = () => popFnCalls(mockLogger.log);

describe('middleware api', function() {
    beforeEach(() => {
        this.app = express();

        this.mockBackend = jest.fn();
        this.app.use((req, res, next) => {
            req.user = {
                id: 0,
                login: ''
            };

            req.backend = {
                getAPI: jest.fn().mockReturnValue(this.mockBackend)
            };

            next();
        });

        this.agent = request(this.app);
    });

    it('shouldn\'t allow non-POST requests', (done) => {
        api(this.app);
        this.agent.get('/api/yo')
            .expect((res) => {
                expect(res.text).toMatchSnapshot();
                expect(popLoggerCalls()).toMatchSnapshot();
            })
            .end(done);
    });

    it('shouldn\'t allow requests without token', (done) => {
        api(this.app);
        this.agent.post('/api/yoyo')
            .expect((res) => {
                expect(res.text).toMatchSnapshot();
                expect(popLoggerCalls()).toMatchSnapshot();
            })
            .end(done);
    });

    it('shouldn\'t allow requests when user uid doesn\'t correspond to the one in token', (done) => {
        this.app.use((req, res, next) => {
            req.token = {};
            next();
        });
        api(this.app);

        this.agent.post('/api/yoyo')
            .expect((res) => {
                expect(res.text).toMatchSnapshot();
                expect(popLoggerCalls()).toEqual([]);
            })
            .end(done);
    });

    it('should proxy method to backend', (done) => {
        this.app.use((req, res, next) => {
            req.token = {
                uid: 0
            };
            req.backend.getAPI().mockReturnValue(new Promise(
                (resolve) => resolve({})
            ));

            next();
        });
        api(this.app);

        this.agent.post('/api/yoyo')
            .send({ some: 'params' })
            .expect((res) => {
                expect(res.text).toMatchSnapshot();
                expect(popLoggerCalls()).toEqual([]);
            })
            .end(done);
    });

    it('should proxy error', (done) => {
        this.app.use((req, res, next) => {
            req.token = {
                uid: 0
            };
            req.backend.getAPI().mockReturnValue(new Promise(
                (resolve, reject) => setTimeout(
                    () => reject({
                        statusCode: 500,
                        message: 'yoyo'
                    })
                )
            ));

            next();
        });
        api(this.app);

        this.agent.post('/api/yoyo')
            .send({ some: 'params' })
            .expect((res) => {
                expect(res.text).toMatchSnapshot();
                expect(popLoggerCalls()).toEqual([]);
            })
            .end(done);
    });
});

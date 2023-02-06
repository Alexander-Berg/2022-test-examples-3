import express from 'express';
import request from 'supertest';
import mdsKey from '../../../app/middleware/mds-key';

jest.mock('../../../config', () => ({
    cloudAPI: {
        hostname: ''
    }
}));

jest.mock('asker-as-promised', () => ({ Error: Object }));
jest.mock('../../../app/helpers');
import helpersMock from '../../../app/helpers';
const popDelayCalls = () => popFnCalls(helpersMock.delay);

let retriesLeft;
const requestMock = (fileUrl) => {
    const testType = fileUrl.replace(/^ya-browser:\/\//, '');
    switch (testType) {
        case 'encrypted-mds-key':
        case 'yet-another-encrypted-mds-key':
            return Promise.resolve({
                statusCode: 200,
                meta: { time: {} },
                data: { mds_key: 'mds-key-from-cloud-api' }
            });
        case '404':
            return Promise.resolve({
                statusCode: 404,
                meta: { time: {} },
                data: {}
            });
        case '429':
        case '500':
        case '502':
        case '503':
            if (--retriesLeft) {
                return Promise.resolve({
                    statusCode: Number(testType),
                    meta: { time: {} },
                    data: {}
                });
            } else {
                return Promise.resolve({
                    statusCode: 200,
                    meta: { time: {} },
                    data: { mds_key: 'some-mds-key' }
                });
            }
        case '400':
        case '403':
        case '410':
        case '405':
        case '567':
            return Promise.resolve({
                meta: { time: {} },
                statusCode: Number(testType)
            });
        case 'error':
            return Promise.reject(new Error('test mds-key error'));
    }
};

describe('middleware mds-key', function() {
    beforeEach(() => {
        this.app = express();
        this.app.use((req, res, next) => {
            req.user = {
                id: 0,
                login: ''
            };
            req.cookies = {
                yandexuid: 1234
            };
            req.parsedUrl = {
                protocol: 'ya-browser:'
            };
            req.tvmTickets = {};
            req.intapi = { getFileData: requestMock };

            next();
        });
        this.agent = request(this.app);
    });

    const check = (app, agent, done, reqParamsShouldBe, retries) => {
        mdsKey(app);

        let reqAfterMdsKey;
        app.use((req, res, next) => {
            reqAfterMdsKey = req;

            next();
        });

        agent.get('/')
            .end((error) => {
                const maxCalls = Math.min(retries || 0, 3);

                expect(error).toBeNull();
                const delayCalls = popDelayCalls();
                expect(delayCalls.length).toEqual(Math.max(maxCalls - 1, 0));
                for (let i = 0; i < maxCalls - 1; i++) {
                    expect(delayCalls[i][0]).toEqual(1000);
                }
                expect(reqAfterMdsKey.mdsKey).toEqual(reqParamsShouldBe.mdsKey);
                expect(reqAfterMdsKey.error).toEqual(reqParamsShouldBe.error);
                expect(reqAfterMdsKey.errorCode).toEqual(reqParamsShouldBe.errorCode);
                expect(reqAfterMdsKey.errorType).toEqual(reqParamsShouldBe.errorType);

                done();
            });
    };

    describe('should not do anything', () => {
        it('for `ya-mail:` in token', (done) => {
            this.app.use((req, res, next) => {
                req.token = {
                    url: 'ya-mail://12345678/1.1'
                };
                req.parsedUrl = {
                    protocol: 'ya-mail:'
                };

                next();
            });
            check(this.app, this.agent, done, {});
        });

        it('for `ya-serp:` in query', (done) => {
            this.app.use((req, res, next) => {
                req.query = {
                    url: 'ya-serp://www.ya.ru/some_doc.docx'
                };
                req.parsedUrl = {
                    protocol: 'ya-serp:'
                };

                next();
            });
            check(this.app, this.agent, done, {});
        });
    });

    describe('should set mdsKey for 200 statusCode from cloud-API', () => {
        it('for `ya-browser:` in token', (done) => {
            this.app.use((req, res, next) => {
                req.token = {
                    url: 'ya-browser://encrypted-mds-key'
                };

                next();
            });
            check(this.app, this.agent, done, {
                mdsKey: 'mds-key-from-cloud-api'
            }, 1);
        });

        it('for `ya-browser:` in query', (done) => {
            this.app.use((req, res, next) => {
                req.query = {
                    url: 'ya-browser://yet-another-encrypted-mds-key'
                };

                next();
            });
            check(this.app, this.agent, done, {
                mdsKey: 'mds-key-from-cloud-api'
            }, 1);
        });
    });

    describe('should set error=true, errorCode=404, errorType=FILE_NOT_FOUND for 404 from cloud-API', () => {
        it('for `ya-browser:` in token', (done) => {
            this.app.use((req, res, next) => {
                req.token = {
                    url: 'ya-browser://404'
                };

                next();
            });
            check(this.app, this.agent, done, {
                error: true,
                errorCode: 404,
                errorType: 'FILE_NOT_FOUND'
            }, 1);
        });

        it('for `ya-browser:` in query', (done) => {
            this.app.use((req, res, next) => {
                req.query = {
                    url: 'ya-browser://404'
                };

                next();
            });
            check(this.app, this.agent, done, {
                error: true,
                errorCode: 404,
                errorType: 'FILE_NOT_FOUND'
            }, 1);
        });
    });

    describe('should set error=true, errorType=BROWSER_FILE_EXPIRED for 410 from cloud-API', () => {
        it('for `ya-browser:` in token', (done) => {
            this.app.use((req, res, next) => {
                req.token = {
                    url: 'ya-browser://410'
                };

                next();
            });
            check(this.app, this.agent, done, {
                error: true,
                errorCode: 410,
                errorType: 'BROWSER_FILE_EXPIRED'
            }, 1);
        });

        it('for `ya-browser:` in query', (done) => {
            this.app.use((req, res, next) => {
                req.query = {
                    url: 'ya-browser://410'
                };

                next();
            });
            check(this.app, this.agent, done, {
                error: true,
                errorCode: 410,
                errorType: 'BROWSER_FILE_EXPIRED'
            }, 1);
        });
    });

    describe('should set mdsKey after few retries', () => {
        it('for `ya-browser:` in token, 429 for 1st, 200 for 2nd request to cloud-API', (done) => {
            this.app.use((req, res, next) => {
                req.token = {
                    url: 'ya-browser://429'
                };

                next();
            });
            retriesLeft = 2;
            check(this.app, this.agent, done, {
                mdsKey: 'some-mds-key'
            }, retriesLeft);
        });

        it('for `ya-browser:` in query, 500 for 1st & 2nd, 200 for 3rd request to cloud-API', (done) => {
            this.app.use((req, res, next) => {
                req.query = {
                    url: 'ya-browser://500'
                };

                next();
            });
            retriesLeft = 3;
            check(this.app, this.agent, done, {
                mdsKey: 'some-mds-key'
            }, retriesLeft);
        });

        it('for `ya-browser:` in token, 502 for 1st & 2nd, 200 for 3rd request to cloud-API', (done) => {
            this.app.use((req, res, next) => {
                req.token = {
                    url: 'ya-browser://502'
                };

                next();
            });
            retriesLeft = 3;
            check(this.app, this.agent, done, {
                mdsKey: 'some-mds-key'
            }, retriesLeft);
        });

        it('for `ya-browser:` in query, 503 for 1st, 200 for 2nd request to cloud-API', (done) => {
            this.app.use((req, res, next) => {
                req.query = {
                    url: 'ya-browser://503'
                };

                next();
            });
            retriesLeft = 2;
            check(this.app, this.agent, done, {
                mdsKey: 'some-mds-key'
            }, retriesLeft);
        });
    });

    describe('should set error=true, errorCode after 3 retries', () => {
        it('for `ya-browser:` in query, 429 for all 3 requests to cloud-API', (done) => {
            this.app.use((req, res, next) => {
                req.query = {
                    url: 'ya-browser://429'
                };

                next();
            });
            retriesLeft = 4;
            check(this.app, this.agent, done, {
                error: true,
                errorCode: 500
            }, 3);
        });

        it('for `ya-browser:` in token, 500 for all 3 requests to cloud-API', (done) => {
            this.app.use((req, res, next) => {
                req.token = {
                    url: 'ya-browser://500'
                };

                next();
            });
            retriesLeft = 4;
            check(this.app, this.agent, done, {
                error: true,
                errorCode: 500
            }, 3);
        });

        it('for `ya-browser:` in query, 502 for all 3 requests to cloud-API', (done) => {
            this.app.use((req, res, next) => {
                req.query = {
                    url: 'ya-browser://502'
                };

                next();
            });
            retriesLeft = 4;
            check(this.app, this.agent, done, {
                error: true,
                errorCode: 500
            }, 3);
        });

        it('for `ya-browser:` in token, 503 for all 3 requests to cloud-API', (done) => {
            this.app.use((req, res, next) => {
                req.token = {
                    url: 'ya-browser://503'
                };

                next();
            });
            retriesLeft = 4;
            check(this.app, this.agent, done, {
                error: true,
                errorCode: 500
            }, 3);
        });
    });

    describe('should set error=true, errorCode from 1st retry', () => {
        it('for `ya-browser:` in query, 400 from cloud-API', (done) => {
            this.app.use((req, res, next) => {
                req.query = {
                    url: 'ya-browser://400'
                };

                next();
            });
            check(this.app, this.agent, done, {
                error: true,
                errorCode: 400
            }, 1);
        });

        it('for `ya-browser:` in token, 403 from cloud-API', (done) => {
            this.app.use((req, res, next) => {
                req.token = {
                    url: 'ya-browser://403'
                };

                next();
            });
            check(this.app, this.agent, done, {
                error: true,
                errorCode: 403
            }, 1);
        });

        it('for `ya-browser:` in query, 405 from cloud-API', (done) => {
            this.app.use((req, res, next) => {
                req.query = {
                    url: 'ya-browser://405'
                };

                next();
            });
            check(this.app, this.agent, done, {
                error: true,
                errorCode: 500
            }, 1);
        });

        it('for `ya-browser:` in token, 567 from cloud-API', (done) => {
            this.app.use((req, res, next) => {
                req.token = {
                    url: 'ya-browser://567'
                };

                next();
            });
            check(this.app, this.agent, done, {
                error: true,
                errorCode: 500
            }, 1);
        });
    });

    describe('should set error=true, errorCode=500 for error from cloud-API', () => {
        it('for `ya-browser:` in query, error from cloud-API', (done) => {
            this.app.use((req, res, next) => {
                req.query = {
                    url: 'ya-browser://error'
                };

                next();
            });
            check(this.app, this.agent, done, {
                error: true,
                errorCode: 500
            }, 1);
        });

        it('for `ya-browser:` in query, error from cloud-API', (done) => {
            this.app.use((req, res, next) => {
                req.query = {
                    url: 'ya-browser://error'
                };

                next();
            });
            check(this.app, this.agent, done, {
                error: true,
                errorCode: 500
            }, 1);
        });
    });
});

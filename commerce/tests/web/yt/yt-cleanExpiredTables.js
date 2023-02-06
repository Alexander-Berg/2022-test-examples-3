require('co-mocha');

const api = require('api');
const { yt } = require('yandex-config');
const request = require('co-supertest').agent(api.callback());
const { expect } = require('chai');
const moment = require('moment');
const nock = require('nock');
const sinon = require('sinon');

const log = require('logger');
const nockYT = require('tests/helpers/yt');

describe('`cleanExpiredTables`', () => {
    beforeEach(() => {
        sinon.spy(log, 'warn');
        sinon.spy(log, 'error');
    });

    afterEach(() => {
        log.warn.restore();
        log.error.restore();

        nock.cleanAll();
    });

    it('should success choose and remove old tables', function *() {
        nockYT({
            list: {
                response: [
                    {
                        $value: 'one',
                        $attributes: {
                            'creation_time': '2018-07-31T10:00:00.000Z'
                        }
                    },
                    {
                        $value: 'two',
                        $attributes: {
                            'creation_time': '2018-11-31T10:00:00.000Z'
                        }
                    }
                ]
            },
            query: {
                path: `${yt.path}/archive/input`,
                attributes: ['creation_time']
            }
        });

        nockYT({
            list: {
                response: [
                    {
                        $value: 'three',
                        $attributes: {
                            'creation_time': moment().subtract(3, 'month').toISOString()
                        }
                    }
                ]
            },
            query: {
                path: `${yt.path}/archive/output`,
                attributes: ['creation_time']
            }
        });

        nockYT({
            list: {
                response: [
                    {
                        $value: 'four',
                        $attributes: {
                            'creation_time': '2019-07-31T10:00:00.000Z'
                        }
                    },
                    {
                        $value: 'five',
                        $attributes: {
                            'creation_time': '2019-01-01T10:00:00.000Z'
                        }
                    }
                ]
            },
            query: {
                path: `${yt.path}/input`,
                attributes: ['creation_time']
            }
        });

        nockYT({
            list: {
                response: [
                    {
                        $value: 'six',
                        $attributes: {
                            'creation_time': '2018-07-31T10:00:00.000Z'
                        }
                    }
                ]
            },
            query: {
                path: `${yt.path}/output`,
                attributes: ['creation_time']
            }
        });

        nockYT({
            list: { response: [] },
            query: {
                path: `${yt.path}/logs`,
                attributes: ['creation_time']
            }
        });

        nockYT({
            remove: { response: { statusCode: 200 }, times: 5 }
        });

        yield request
            .get('/v1/yt/cleanExpiredTables')
            .expect(204);

        expect(log.warn.notCalled).to.be.true;
        expect(log.error.notCalled).to.be.true;
    });

    it('should throw 500 when can not list YT tables', function *() {
        nockYT({
            list: { code: 500 }
        });

        yield request
            .get('/v1/yt/cleanExpiredTables')
            .expect(500)
            .expect({
                message: 'Internal Server Error',
                internalCode: '500_CLT'
            })
            .end();

        expect(log.warn.calledOnce).to.be.true;
        expect(log.error.calledOnce).to.be.true;
    });
});

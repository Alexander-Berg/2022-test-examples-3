require('co-mocha');

const { expect } = require('chai');
const ip = require('ip');
let log = require('logger');
const mockery = require('mockery');
const nock = require('nock');
const sinon = require('sinon');

const dbHelper = require('tests/helpers/clear');
const mockMailer = require('tests/helpers/mailer');
const mockCache = require('tests/helpers/cache');
const nockYT = require('tests/helpers/yt');

const BBHelper = require('tests/helpers/blackbox');
const nockBlackbox = BBHelper.nockExtSeveralUids;

const proctoringResponsesFactory = require('tests/factory/proctoringResponsesFactory');

let api;
let request;

describe('`uploadComdepReport`', () => {
    before(() => {
        mockMailer();
        mockCache();

        api = require('api');
        request = require('co-supertest').agent(api.callback());
    });

    after(() => {
        mockery.disable();
        mockery.deregisterAll();
    });

    beforeEach(function *() {
        yield dbHelper.clear();

        log = require('logger');
        sinon.spy(log, 'warn');
        sinon.spy(log, 'error');

        nockBlackbox({
            uid: '123',
            userip: ip.address(),
            response: {
                users: [
                    {
                        uid: { value: 123 },
                        'address-list': [
                            { address: 'email@yandex.ru' }
                        ]
                    }
                ]
            }
        });
    });

    afterEach(() => {
        log.warn.restore();
        log.error.restore();

        nock.cleanAll();
    });

    const trialTemplate = { id: 1, slug: 'direct-pro' };
    const user = {
        id: 1,
        uid: 123,
        login: 'test',
        firstname: 'A',
        lastname: 'B'
    };

    it('should successfully upload comdep report', function *() {
        yield proctoringResponsesFactory.createWithRelations(
            {
                verdict: 'pending',
                isLast: true,
                isSentToToloka: false,
                isRevisionRequested: false
            },
            {
                trial: {
                    id: 3,
                    passed: 1,
                    nullified: 0,
                    started: new Date(2020, 2, 3),
                    expired: 1
                },
                trialTemplate,
                user
            }
        );

        yield proctoringResponsesFactory.createWithRelations(
            {
                verdict: 'failed',
                isLast: true,
                isSentToToloka: false,
                isRevisionRequested: true
            },
            {
                trial: {
                    id: 4,
                    passed: 1,
                    nullified: 0,
                    started: new Date(2020, 3, 4),
                    expired: 1
                },
                trialTemplate,
                user
            }
        );

        yield proctoringResponsesFactory.createWithRelations(
            {
                verdict: 'failed',
                isLast: true,
                isSentToToloka: false,
                isRevisionRequested: false
            },
            {
                trial: {
                    id: 5,
                    passed: 0,
                    nullified: 0,
                    started: new Date(2020, 4, 5),
                    expired: 1
                },
                trialTemplate,
                user
            }
        );

        const response = { statusCode: 200 };
        const { lightNock, heavyNock } = nockYT({
            create: { response },
            proxy: { response: ['heavy-proxy'], times: 2 },
            write: { response, times: 2 }
        });

        yield request
            .get('/v1/yt/uploadComdepReport')
            .expect(204);

        expect(log.warn.notCalled).to.be.true;
        expect(log.error.notCalled).to.be.true;
        expect(lightNock.isDone()).to.be.true;
        expect(heavyNock.isDone()).to.be.true;
    });

    it('should throw 500 when can not create table', function *() {
        const { lightNock } = nockYT({
            create: { code: 500 }
        });

        yield request
            .get('/v1/yt/uploadComdepReport')
            .expect(500)
            .expect({
                message: 'Internal Server Error',
                internalCode: '500_URF',
                details: 'Table not created'
            })
            .end();

        expect(log.warn.calledOnce).to.be.true;
        expect(log.error.calledOnce).to.be.true;
        expect(lightNock.isDone()).to.be.true;
    });
});

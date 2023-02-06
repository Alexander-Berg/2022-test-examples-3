require('co-mocha');

let api = require('api');
let request = require('co-supertest').agent(api.callback());
const { expect } = require('chai');
const mockery = require('mockery');
const nock = require('nock');
const sinon = require('sinon');

let log = require('logger');

const mockCache = require('tests/helpers/cache');
const nockYT = require('tests/helpers/yt');

describe('`uploadToStat`', () => {
    const writeBody = 'write-body';
    const heavyProxy = 'heavy-proxy';

    before(() => {
        mockCache();

        api = require('api');
        request = require('co-supertest').agent(api.callback());
        log = require('logger');
    });

    after(() => {
        mockery.disable();
        mockery.deregisterAll();
    });

    beforeEach(() => {
        sinon.spy(log, 'warn');
    });

    afterEach(() => {
        log.warn.restore();

        nock.cleanAll();
    });

    it('should get report data and upload to YT', function *() {
        nockYT({
            proxy: { response: [heavyProxy], times: 3 },
            write: { response: writeBody, times: 3 }
        });

        yield request
            .get('/v1/yt/uploadStatReports')
            .expect(204);

        expect(log.warn.notCalled).to.be.true;
    });

    it('should throw error when it was not possible to write data', function *() {
        nockYT({
            proxy: { response: [heavyProxy] },
            write: { code: 500 }
        });

        yield request
            .get('/v1/yt/uploadStatReports')
            .expect(500)
            .expect({
                message: 'Internal Server Error',
                internalCode: '500_CWD'
            })
            .end();

        expect(log.warn.calledOnce).to.be.true;
    });
});

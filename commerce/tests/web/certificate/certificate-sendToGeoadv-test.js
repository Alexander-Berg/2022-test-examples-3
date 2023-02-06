require('co-mocha');

const { expect } = require('chai');
const log = require('logger');
const nock = require('nock');
const sinon = require('sinon');

const { geoadv } = require('yandex-config');

const tvm = require('helpers/tvm');

const dbHelper = require('tests/helpers/clear');
const nockTvm = require('tests/helpers/nockTvm');

const certificatesFactory = require('tests/factory/certificatesFactory');

const { Certificate } = require('db/postgres');

const api = require('api');
const request = require('co-supertest').agent(api.callback());

describe('Certificate sendToGeoadv controller', () => {
    const ticket = 'someTicket';

    beforeEach(function *() {
        yield dbHelper.clear();

        sinon.spy(log, 'error');
    });

    afterEach(() => {
        log.error.restore();
        tvm.cache.reset();
        nock.cleanAll();
    });

    it('should successfully send certificates to geoadv', function *() {
        yield certificatesFactory.createWithRelations(
            { id: 222, active: 1, isSentToGeoadv: false },
            {
                trialTemplate: { slug: 'msp' },
                user: { id: 1, uid: 123456 },
                trial: { id: 123, nullified: 0 }
            }
        );
        yield certificatesFactory.createWithRelations(
            { id: 333, active: 1, isSentToGeoadv: false },
            {
                trialTemplate: { slug: 'msp' },
                user: { id: 2, uid: 756789 },
                trial: { id: 456, nullified: 0 }
            }
        );

        const firstCertData = {
            certId: 222,
            uid: 123456,
            examSlug: 'msp'
        };
        const secondCertData = {
            certId: 333,
            uid: 756789,
            examSlug: 'msp'
        };
        const tvmNock = nockTvm.getTicket({ 'geoadv-testing': { ticket } }, 1);
        const geoadvNockFirst = nock(geoadv.host)
            .put(geoadv.path, firstCertData)
            .reply(200);
        const geoadvNockSecond = nock(geoadv.host)
            .put(geoadv.path, secondCertData)
            .reply(200);

        yield request
            .get('/v1/certificates/sendToGeoadv')
            .expect(204)
            .end();

        expect(log.error.notCalled).to.be.true;
        expect(tvmNock.isDone()).to.be.true;
        expect(geoadvNockFirst.isDone()).to.be.true;
        expect(geoadvNockSecond.isDone()).to.be.true;

        const actualDbData = yield Certificate.findAll({
            attributes: ['id', 'isSentToGeoadv'],
            raw: true
        });

        expect(actualDbData).to.deep.equal([
            { id: 222, isSentToGeoadv: true },
            { id: 333, isSentToGeoadv: true }
        ]);
    });

    it('should send 204 when there are no suitable certificates', function *() {
        yield request
            .get('/v1/certificates/sendToGeoadv')
            .expect(204)
            .end();

        expect(log.error.notCalled).to.be.true;
    });
});

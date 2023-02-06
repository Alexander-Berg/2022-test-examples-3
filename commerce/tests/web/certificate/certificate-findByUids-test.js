require('co-mocha');

const api = require('api');
const request = require('co-supertest').agent(api.callback());
const { expect } = require('chai');
const moment = require('moment');
const nock = require('nock');

const MdsModel = require('models/mds');

const dbHelper = require('tests/helpers/clear');
const { convert } = require('tests/helpers/dateHelper');
const nockTvm = require('tests/helpers/nockTvm');
const tvmClientFactory = require('tests/factory/tvmClientsFactory');
const certificateFactory = require('tests/factory/certificatesFactory');

describe('Certificate findByUids controller', () => {
    const now = new Date();
    const user = { uid: 1234567890 };
    const authType = { code: 'web' };
    const trialTemplate = { id: 3, slug: 'direct' };
    const finished = moment(now).subtract(1, 'd').startOf('day');
    const dueDate = moment(now).add(1, 'y').startOf('day');
    const service = { id: 12, code: 'direct', title: 'Yandex.Direct' };
    const type = { code: 'cert' };
    const certificate = {
        id: 13,
        dueDate,
        firstname: 'Vasya',
        lastname: 'Pupkin',
        active: 1,
        confirmedDate: finished,
        imagePath: '255/38472434872_13'
    };

    beforeEach(dbHelper.clear);
    afterEach(nock.cleanAll);

    it('should return certificates for users', function *() {
        const tvmRequest = nockTvm.checkTicket({ src: 1234 });

        yield tvmClientFactory.create({ clientId: 1234 });

        const trial = {
            id: 2,
            finished,
            passed: 1,
            nullified: 0
        };

        yield certificateFactory.createWithRelations(
            certificate,
            { trial, user, trialTemplate, service, type, authType }
        );

        const res = yield request
            .post('/v1/certificates/findByUids')
            .set('force-tvm-check', '1')
            .set('x-ya-service-ticket', 'ticket')
            .send({ uids: [1234567890] })
            .expect(200)
            .end();

        const actual = res.body;

        tvmRequest.done();

        const expectedUserCertificates = [
            {
                certId: 13,
                certType: 'cert',
                dueDate: convert(dueDate.toDate()),
                firstname: 'Vasya',
                lastname: 'Pupkin',
                active: 1,
                confirmedDate: convert(finished.toDate()),
                imagePath: MdsModel.getAvatarsPath('255/38472434872_13'),
                service,
                previewImagePath: '',
                exam: { id: 3, slug: 'direct' }
            }
        ];

        expect(actual).to.deep.equal({ [user.uid]: expectedUserCertificates });
    });

    it('should throw 400 when `x-ya-service-ticket` header is absent', function *() {
        yield request
            .post('/v1/certificates/findByUids')
            .set('force-tvm-check', '1')
            .expect(400)
            .expect({
                message: 'Ticket is not defined',
                internalCode: '400_TIN'
            })
            .end();
    });

    it('should throw 403 when tvm cannot parse ticket', function *() {
        const tvmRequest = nockTvm.checkTicket({}, 400);

        const res = yield request
            .post('/v1/certificates/findByUids')
            .set('force-tvm-check', '1')
            .set('x-ya-service-ticket', 'ticket')
            .expect(403)
            .end();

        tvmRequest.done();
        expect(res.body.internalCode).to.equal('403_CPT');
        expect(res.body.message).to.equal('Cannot parse ticket');
    });

    it('should throw 403 when tvm client has no access', function *() {
        const tvmRequest = nockTvm.checkTicket({ src: 1234 });

        const res = yield request
            .post('/v1/certificates/findByUids')
            .set('force-tvm-check', '1')
            .set('x-ya-service-ticket', 'ticket')
            .expect(403)
            .end();

        tvmRequest.done();
        expect(res.body.internalCode).to.equal('403_CNA');
        expect(res.body.message).to.equal('Client has no access');
    });

    it('should throw 400 when `body.uids` is not array', function *() {
        const tvmRequest = nockTvm.checkTicket({ src: 1234 });

        yield tvmClientFactory.create({ clientId: 1234 });

        yield request
            .post('/v1/certificates/findByUids')
            .set('force-tvm-check', '1')
            .set('x-ya-service-ticket', 'ticket')
            .send({ uids: 1234 })
            .expect(400)
            .expect({
                message: 'Uids is required and should be array',
                internalCode: '400_USA'
            })
            .end();

        tvmRequest.done();
    });

    it('should throw 400 when `body.uids` contains not number', function *() {
        const tvmRequest = nockTvm.checkTicket({ src: 1234 });

        yield tvmClientFactory.create({ clientId: 1234 });

        yield request
            .post('/v1/certificates/findByUids')
            .set('force-tvm-check', '1')
            .set('x-ya-service-ticket', 'ticket')
            .send({ uids: [1234, 'not_uid'] })
            .expect(400)
            .expect({
                message: 'Uid is invalid',
                internalCode: '400_UII',
                uid: 'not_uid'
            })
            .end();

        tvmRequest.done();
    });
});

require('co-mocha');

let api = require('api');
let request = require('co-supertest').agent(api.callback());
const { expect } = require('chai');
const moment = require('moment');
const mockery = require('mockery');

const nockBlackbox = require('tests/helpers/blackbox').nockIntBlackbox;
const dbHelper = require('tests/helpers/clear');
const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');
const trialsFactory = require('tests/factory/trialsFactory');
const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');

const { Freezing } = require('db/postgres');

function *createAdmin() {
    const admin = { uid: 1234567890 };
    const role = { code: 'developer' };

    yield adminsToRolesFactory.createWithRelations({}, { admin, role });
}

describe('Admin freeze controller', () => {
    before(() => {
        nockBlackbox({
            response: {
                uid: { value: '1234567890' }
            }
        });

        const mailerMock = function () {
            return new Promise(resolve => {
                resolve();
            });
        };

        mockery.registerMock('helpers/mailer', mailerMock);
        mockery.enable({
            useCleanCache: true,
            warnOnReplace: false,
            warnOnUnregistered: false
        });
        api = require('api');
        request = require('co-supertest').agent(api.callback());
    });

    after(() => {
        require('nock').cleanAll();
        mockery.disable();
    });

    beforeEach(function *() {
        yield dbHelper.clear();
    });

    it('should success freeze', function *() {
        const now = new Date();

        yield trialTemplatesFactory.createWithRelations({ id: 1 });
        yield trialTemplatesFactory.createWithRelations({ id: 2 });
        yield createAdmin();
        yield trialsFactory.createWithRelations(
            { id: 4, timeLimit: 1900000, started: now, trialTemplateId: 1 },
            {}
        );

        const lastAttemptFinish = moment(now).add(1900000, 'ms').toISOString();

        yield request
            .post('/v1/admin/freeze')
            .send({ examIds: [1, 2] })
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect('Content-Type', /json/)
            .expect({ lastAttemptFinish })
            .end();

        const freezingData = yield Freezing.findAll();

        expect(freezingData.length).to.equal(2);
    });

    it('should throw 400 if examIds is not array', function *() {
        yield createAdmin();

        yield request
            .post('/v1/admin/freeze')
            .send({ examIds: true })
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(400)
            .expect('Content-Type', /json/)
            .expect({
                message: 'examIds should be array',
                internalCode: '400_ESA',
                examIds: true
            })
            .end();
    });

    it('should throw 400 if examId is not a number', function *() {
        yield createAdmin();

        yield request
            .post('/v1/admin/freeze')
            .send({ examIds: ['direct'] })
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(400)
            .expect('Content-Type', /json/)
            .expect({
                message: 'Exam identity is invalid',
                internalCode: '400_EII',
                examId: 'direct'
            })
            .end();
    });

    it('should throw 401 when user not authorized', function *() {
        yield request
            .post('/v1/admin/freeze')
            .expect(401)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 403 when user not developer', function *() {
        yield request
            .post('/v1/admin/freeze')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect({
                message: 'User is not developer',
                internalCode: '403_UND',
                uid: 1234567890
            })
            .end();
    });
});

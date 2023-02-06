require('co-mocha');

let api = require('api');
let request = require('co-supertest').agent(api.callback());
const { expect } = require('chai');
const moment = require('moment');
const mockery = require('mockery');
const syncDelay = require('yandex-config').direct.delay;

const nockDirect = require('tests/helpers/directHelper');
const BBHelper = require('tests/helpers/blackbox');
const nockBlackbox = BBHelper.nockIntBlackbox;
const nockTvmTicket = require('tests/helpers/nockTvm').getTicket;
const dbHelper = require('tests/helpers/clear');
const certificatesFactory = require('tests/factory/certificatesFactory');
const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');
const agenciesFactory = require('tests/factory/agenciesFactory');
const directSyncFactory = require('tests/factory/directSyncFactory');

const { User, DirectSync } = require('db/postgres');

describe('Admin sync agencies info controller', () => {
    before(() => {
        nockBlackbox({ response: { uid: { value: '1234567890' } } });
        nockDirect();
        nockTvmTicket({ 'direct-api-testing': { ticket: 'some_ticket' } }, Infinity);

        const sleepMock = function () {
            return new Promise(resolve => setTimeout(resolve, 0));
        };
        const mailerMock = function () {
            return new Promise(resolve => {
                resolve();
            });
        };

        mockery.registerMock('helpers/sleep', sleepMock);
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
        BBHelper.cleanAll();
        mockery.disable();
    });

    beforeEach(function *() {
        yield dbHelper.clear();
    });

    const now = new Date();
    const authType = { id: 1, code: 'web' };

    function *createAnalyst() {
        const admin = { uid: 1234567890 };
        const role = { code: 'analyst' };

        yield adminsToRolesFactory.createWithRelations({}, { admin, role });
    }

    it('should success sync agencies info', function *() {
        yield createAnalyst();

        const user = { id: 3, uid: 173462178932 };
        const trial = { id: 12, nullified: 0 };
        const agency = { id: 23, login: 'admin-agency' };

        yield certificatesFactory.createWithRelations(
            { id: 234, dueDate: moment(now).add(5, 'month') },
            { trial, agency, authType, user }
        );

        const otherUser = { id: 34, uid: 1324354637 };
        const otherUserTrial = { id: 5, nullified: 0 };
        const otherUserAgency = { id: 24, login: 'other-pupkin' };

        yield certificatesFactory.createWithRelations(
            { id: 345, dueDate: moment(now).add(3, 'month') },
            { trial: otherUserTrial, user: otherUser, agency: otherUserAgency, authType }
        );

        const otherAgency = { id: 123, login: 'i-pupkin' };

        yield agenciesFactory.create(otherAgency);

        yield request
            .post('/v1/admin/syncAgenciesInfo')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(204)
            .end();

        const users = yield User.findAll({
            attributes: ['id', 'agencyId']
        });

        expect(users[0].agencyId).to.equal(123);
        expect(users[1].agencyId).to.equal(123);
    });

    it('should create new record in `direct_sync` and fill `finishTime` when sync complete', function *() {
        yield createAnalyst();

        const recordsBefore = yield DirectSync.findAll();

        expect(recordsBefore.length).to.equal(0);

        yield request
            .post('/v1/admin/syncAgenciesInfo')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(204)
            .end();

        const recordsAfter = yield DirectSync.findAll();

        expect(recordsAfter.length).to.equal(1);
        expect(recordsAfter[0].get('startTime')).to.not.be.null;
        expect(recordsAfter[0].get('finishTime')).to.not.be.null;
    });

    it('should throw 401 when user not authorized', function *() {
        yield request
            .post('/v1/admin/syncAgenciesInfo')
            .expect(401)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 403 when user not analyst', function *() {
        yield request
            .post('/v1/admin/syncAgenciesInfo')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect({
                message: 'User is not analyst',
                internalCode: '403_NAN'
            })
            .end();
    });

    it('should throw 403 when sync has already begun', function *() {
        const startTime = new Date();
        const availabilityTime = moment(startTime)
            .add(syncDelay)
            .startOf('minute')
            .toISOString();

        yield directSyncFactory.create({ id: 3, startTime });
        yield createAnalyst();

        yield request
            .post('/v1/admin/syncAgenciesInfo')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect({
                message: 'Sync has already begun',
                internalCode: '403_SAB',
                availabilityTime
            })
            .end();
    });
});

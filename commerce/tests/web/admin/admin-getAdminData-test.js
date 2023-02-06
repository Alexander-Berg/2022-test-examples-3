require('co-mocha');

const api = require('api');
const request = require('co-supertest').agent(api.callback());
const moment = require('moment');

const nockBlackbox = require('tests/helpers/blackbox').nockIntBlackbox;
const dbHelper = require('tests/helpers/clear');
const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');
const directSyncFactory = require('tests/factory/directSyncFactory');
const freezingFactory = require('tests/factory/freezingFactory');
const locksFactory = require('tests/factory/locksFactory');

describe('Admin data controller', () => {
    before(() => {
        nockBlackbox({ response: { uid: { value: '1234567890' } } });
    });

    after(() => {
        require('nock').cleanAll();
    });

    beforeEach(function *() {
        yield dbHelper.clear();
    });

    function *createAdmin() {
        const role = { code: 'admin' };
        const admin = { uid: 1234567890 };

        yield adminsToRolesFactory.createWithRelations({}, { admin, role });
    }

    it('should return correct object with options', function *() {
        yield createAdmin();

        const startTime = moment.utc();
        const finishTime = startTime.add(10, 'minutes');

        const startTimeString = startTime.toDate().toISOString();

        const firstLockDate = new Date(2019, 7, 7);
        const secondLockDate = new Date(2019, 10, 5);

        yield directSyncFactory.create({ startTime, finishTime });
        yield freezingFactory.createWithRelations({
            frozenBy: 12345,
            startTime,
            finishTime
        }, { trialTemplate: { id: 1 } });

        yield locksFactory.createWithRelations(
            { id: 4, lockDate: firstLockDate },
            { trialTemplate: { id: 2, slug: 'winter' }, admin: { login: 'rinka' } }
        );
        yield locksFactory.createWithRelations(
            { id: 5, lockDate: secondLockDate },
            { trialTemplate: { id: 3, slug: 'summer' }, admin: { login: 'simani' } }
        );

        yield request
            .get('/v1/admin/')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(200)
            .expect({
                lastSyncAgenciesDate: startTimeString,
                currentFrozenExams: [{ trialTemplateId: 1, startTime: startTimeString }],
                access: ['admin'],
                lockedExams: {
                    winter: {
                        login: 'rinka',
                        lockDate: firstLockDate.toISOString()
                    },
                    summer: {
                        login: 'simani',
                        lockDate: secondLockDate.toISOString()
                    }
                }
            })
            .end();
    });

    it('should throw 401 when user not authorized', function *() {
        yield request
            .get('/v1/admin/')
            .expect(401)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 403 when user have not any roles', function *() {
        yield request
            .get('/v1/admin/')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect({
                message: 'User has no any access',
                internalCode: '403_NAA'
            })
            .end();
    });
});

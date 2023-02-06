require('co-mocha');

const api = require('api');
const { expect } = require('chai');
const nock = require('nock');
const request = require('co-supertest').agent(api.callback());

const dbHelper = require('tests/helpers/clear');
const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');
const trialsFactory = require('tests/factory/trialsFactory');
const usersFactory = require('tests/factory/usersFactory');

const nockBlackbox = require('tests/helpers/blackbox').nockIntBlackbox;

describe('Attempt attemptsInfo controller', () => {
    beforeEach(function *() {
        nockBlackbox({ response: { uid: { value: '1234567890' } } });

        yield dbHelper.clear();
    });

    afterEach(nock.cleanAll);

    function *createAdmin() {
        const admin = { uid: 1234567890 };
        const role = { code: 'admin' };

        yield adminsToRolesFactory.createWithRelations({}, { admin, role });
    }

    it('should return trials info', function *() {
        yield createAdmin();

        const authType = { id: 2, code: 'web' };

        const trialTemplate = {
            id: 10,
            slug: 'some-exam'
        };
        const user = {
            id: 2020,
            uid: 1234567890,
            login: 'first-user'
        };
        const firstStarted = new Date(2019, 7, 7);
        const secondStarted = new Date(2019, 10, 5);

        yield usersFactory.createWithRelations(user, { authType });

        yield trialsFactory.createWithRelations({
            id: 1,
            nullified: 0,
            started: firstStarted
        }, { trialTemplate, user });
        yield trialsFactory.createWithRelations({
            id: 2,
            nullified: 1,
            started: secondStarted
        }, { trialTemplate, user });

        yield createAdmin();
        const res = yield request
            .post('/v1/attempt/info')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ attemptIds: [1, 2, 3] })
            .expect(200);
        const actual = res.body;

        expect(actual.attemptsInfo).to.deep.equal([
            {
                trialId: 1,
                nullified: 0,
                started: firstStarted.toISOString(),
                login: 'first-user',
                trialTemplateSlug: 'some-exam'
            },
            {
                trialId: 2,
                nullified: 1,
                started: secondStarted.toISOString(),
                login: 'first-user',
                trialTemplateSlug: 'some-exam'
            }
        ]);
    });

    it('should return 400 when attempt ids is not array', function *() {
        yield createAdmin();

        yield request
            .post('/v1/attempt/info')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ attemptIds: 1 })
            .expect(400, {
                internalCode: '400_ASA',
                message: 'attemptIds should be array',
                attemptIds: 1
            });
    });

    it('should return 400 when attempt id is invalid', function *() {
        yield createAdmin();

        yield request
            .post('/v1/attempt/info')
            .set('Cookie', ['Session_id=user_session_id'])
            .send({ attemptIds: ['not-a-number'] })
            .expect(400, {
                internalCode: '400_AII',
                message: 'Attempt id is invalid',
                attemptId: 'not-a-number'
            });
    });

    it('should throw 401 when user not authorized', function *() {
        yield request
            .post('/v1/attempt/info')
            .expect(401)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 403 when user has no support access', function *() {
        yield request
            .post('/v1/attempt/info')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect({
                message: 'User has no support access',
                internalCode: '403_UNS'
            })
            .end();
    });
});

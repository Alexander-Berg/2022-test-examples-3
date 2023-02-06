require('co-mocha');

let api = require('api');
let request = require('co-supertest').agent(api.callback());
const { expect } = require('chai');
const moment = require('moment');
const mockery = require('mockery');
const freezingTime = require('yandex-config').freezing.time;

const nockBlackbox = require('tests/helpers/blackbox').nockIntBlackbox;
const dbHelper = require('tests/helpers/clear');
const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');
const freezingFactory = require('tests/factory/freezingFactory');

const { Freezing } = require('db/postgres');

describe('Admin unfreeze controller', () => {
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

    function *createDeveloper() {
        const admin = { uid: 1234567890 };
        const role = { code: 'developer' };

        yield adminsToRolesFactory.createWithRelations({}, { admin, role });
    }

    it('should success unfreeze', function *() {
        const now = new Date();
        const finishTime = moment(now).add(freezingTime).toDate();

        yield createDeveloper();
        yield freezingFactory.createWithRelations({
            id: 3,
            frozenBy: '1234567890',
            startTime: now,
            finishTime
        }, { trialTemplate: { id: 1 } });

        yield request
            .post('/v1/admin/unfreeze')
            .send({ examIds: [1] })
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(204)
            .end();

        const freezingData = yield Freezing.findOne({});

        expect(freezingData.get('finishTime')).to.be.below(finishTime);
    });

    it('should throw 401 when user not authorized', function *() {
        yield request
            .post('/v1/admin/unfreeze')
            .expect(401)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 403 when user not developer', function *() {
        yield request
            .post('/v1/admin/unfreeze')
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

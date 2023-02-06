require('co-mocha');

const api = require('api');
const request = require('co-supertest').agent(api.callback());
const { expect } = require('chai');
const moment = require('moment');

const Excel = require('models/excel');

const nockBlackbox = require('tests/helpers/blackbox').nockIntBlackbox;
const dbHelper = require('tests/helpers/clear');
const adminsToRolesFactory = require('tests/factory/adminsToRolesFactory');
const certificatesFactory = require('tests/factory/certificatesFactory');

describe('Admin getAgenciesInfo controller', () => {
    before(() => {
        nockBlackbox({ response: { uid: { value: '1234567890' } } });
    });

    after(() => {
        require('nock').cleanAll();
    });

    beforeEach(function *() {
        yield dbHelper.clear();
    });

    const simpleUser = {
        id: 45,
        uid: 123425523242,
        login: 'simple-user'
    };
    const authType = { id: 1, code: 'web' };

    function *createAnalyst() {
        const role = { code: 'analyst' };
        const admin = { uid: 1234567890 };

        yield adminsToRolesFactory.createWithRelations({}, { admin, role });
    }

    it('should success download agenciesInfo', function *() {
        const trialTemplate = { id: 3, title: 'Yandex.Direct', isProctoring: false };
        const otherTrialTemplate = { id: 4, title: 'Yandex.Metrika' };
        const simpleUserFirstTrial = { id: 12 };
        const simpleUserSecondTrial = { id: 13 };
        const agency = { id: 4, login: 'admin-agency', manager: 'admin-pupkin' };
        const otherAgency = { id: 5, login: 'other-agency', manager: 'first-pupkin' };
        const firstUser = {
            id: 1,
            uid: 9876543210,
            login: 'first-user'
        };
        const firstUserTrial = { id: 14, nullified: 0 };
        const secondUser = {
            id: 2,
            uid: 1928374650,
            login: 'second-user'
        };
        const secondUserTrial = { id: 15, nullified: 0 };
        const otherRole = { id: 3, code: 'user', title: 'User' };
        const now = Date.now();

        yield createAnalyst();

        yield certificatesFactory.createWithRelations(
            {
                id: 23,
                active: 1,
                firstname: 'Ivan',
                lastname: 'Ivanov',
                dueDate: moment(now).add(2, 'month'),
                confirmedDate: moment(now).subtract(2, 'month')
            },
            { trialTemplate, user: simpleUser, trial: simpleUserFirstTrial, agency, authType }
        );

        yield certificatesFactory.createWithRelations(
            {
                id: 24,
                active: 1,
                firstname: 'Ivan',
                lastname: 'Ivanov',
                dueDate: moment(now).add(5, 'month'),
                confirmedDate: moment(now)
            },
            { trialTemplate: otherTrialTemplate, user: simpleUser, trial: simpleUserSecondTrial, agency, authType }
        );

        yield certificatesFactory.createWithRelations(
            {
                id: 25,
                active: 1,
                firstname: 'Petya',
                lastname: 'Petrov',
                dueDate: moment(now).add(6, 'month'),
                confirmedDate: moment(now)
            },
            { trialTemplate, user: firstUser, trial: firstUserTrial, role: otherRole, agency: otherAgency, authType }
        );

        yield certificatesFactory.createWithRelations(
            {
                id: 26,
                active: 1,
                firstname: 'Vasya',
                lastname: 'Pupkin',
                dueDate: moment(now).add(9, 'month'),
                confirmedDate: moment(now)
            },
            { trialTemplate, user: secondUser, trial: secondUserTrial, role: otherRole, agency: otherAgency, authType }
        );

        const res = yield request
            .get('/v1/admin/agenciesInfo')
            .set('Cookie', ['Session_id=user_session_id'])
            .parse(require('tests/helpers/binaryParser'))
            .expect(200)
            .end();
        const excel = Excel.tryLoad(res.body);

        expect(res.type).to.equal('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet');
        expect(excel.worksheet['!ref']).to.equal('A1:K6');
    });

    it('should throw 401 when user not authorized', function *() {
        yield request
            .get('/v1/admin/agenciesInfo')
            .expect(401)
            .expect({
                message: 'User not authorized',
                internalCode: '401_UNA'
            })
            .end();
    });

    it('should throw 403 when user not analyst', function *() {
        yield request
            .get('/v1/admin/agenciesInfo')
            .set('Cookie', ['Session_id=user_session_id'])
            .expect(403)
            .expect({
                message: 'User is not analyst',
                internalCode: '403_NAN'
            })
            .end();
    });
});

require('co-mocha');

const { expect } = require('chai');
const moment = require('moment');
const _ = require('lodash');
const mockery = require('mockery');
const syncDelay = require('yandex-config').direct.delay;

const dbHelper = require('tests/helpers/clear');
const agenciesFactory = require('tests/factory/agenciesFactory');
const rolesFactory = require('tests/factory/rolesFactory');
const certificatesFactory = require('tests/factory/certificatesFactory');
const usersFactory = require('tests/factory/usersFactory');
const directSyncFactory = require('tests/factory/directSyncFactory');
const nockDirect = require('tests/helpers/directHelper');
const nockTvmTicket = require('tests/helpers/nockTvm').getTicket;
const catchError = require('tests/helpers/catchError').generator;

let Direct = require('models/direct');

const {
    User,
    Agency,
    Role,
    DirectSync
} = require('db/postgres');

describe('Direct model', () => {
    before(() => {
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
        Direct = require('models/direct');
    });

    after(() => {
        mockery.disable();
    });

    describe('`selectUIDs`', () => {
        beforeEach(function *() {
            yield dbHelper.clear();
        });

        const now = new Date();
        const user = { id: 23, uid: 1234567890 };
        const authType = { id: 2, code: 'web' };

        beforeEach(function *() {
            const trial = { id: 3, nullified: 0 };
            const certificate = {
                id: 24,
                dueDate: moment(now).add(5, 'month')
            };

            yield certificatesFactory.createWithRelations(
                certificate,
                { trial, user, authType }
            );
        });

        it('should return correct uids who have actual certificates', function *() {
            const otherUser = { id: 9876543210 };
            const otherTrial = { id: 4, nullified: 0 };
            const otherCertificate = {
                id: 25,
                dueDate: moment(now).subtract(3, 'month')
            };

            yield certificatesFactory.createWithRelations(
                otherCertificate,
                { trial: otherTrial, user: otherUser }
            );

            const actual = yield Direct.selectUIDs();

            expect(actual).to.deep.equal([user.uid]);
        });

        it('should return correct uid who have certificates belong to not nullified attempts', function *() {
            const otherUser = { id: 45, uid: 9876543210 };
            const otherTrial = { id: 4, nullified: 1 };
            const otherCertificate = {
                id: 25,
                dueDate: moment(now).add(6, 'month')
            };

            yield certificatesFactory.createWithRelations(
                otherCertificate,
                { trial: otherTrial, user: otherUser, authType }
            );

            const actual = yield Direct.selectUIDs();

            expect(actual).to.deep.equal([user.uid]);
        });

        it('should return uids only for `web` users', function *() {
            const otherUser = { id: 67, uid: 9876543210 };
            const otherTrial = { id: 4, nullified: 0 };
            const otherAuthType = { id: 3, code: 'telegram' };
            const otherCertificate = {
                id: 25,
                dueDate: moment(now).add(6, 'month')
            };

            yield certificatesFactory.createWithRelations(
                otherCertificate,
                { trial: otherTrial, user: otherUser, authType: otherAuthType }
            );

            const actual = yield Direct.selectUIDs();

            expect(actual).to.deep.equal([user.uid]);
        });
    });

    describe('`getAgencyInfo`', () => {
        before(() => {
            nockDirect();
            nockTvmTicket({ 'direct-api-testing': { ticket: 'some_ticket' } }, Infinity);
        });

        after(() => {
            require('nock').cleanAll();
        });

        it('should make one request and parse data', function *() {
            const uid = '123456789';
            const actual = yield Direct.getAgencyInfo(uid);

            expect(actual).to.deep.equal({
                agency: {
                    login: 'i-pupkin',
                    title: 'i-pupkin',
                    manager: 'yndx-pupkin',
                    directId: 65432
                },
                uid,
                role: 'agency'
            });
        });
    });

    describe('`makeRequestsToDirect`', () => {
        before(() => {
            nockDirect();
            nockTvmTicket({ 'direct-api-testing': { ticket: 'some_ticket' } }, Infinity);
        });

        after(() => {
            require('nock').cleanAll();
        });

        it('should make all requests', function *() {
            const uids = [1123, 1456, 1789];
            const actual = yield Direct.makeRequestsToDirect(uids);

            const baseResponseData = {
                agency: {
                    login: 'i-pupkin',
                    title: 'i-pupkin',
                    manager: 'yndx-pupkin',
                    directId: 65432
                },
                role: 'agency'
            };

            expect(actual.length).to.equal(3);

            const firstResponseData = _.assign(baseResponseData, { uid: 1123 });

            expect(actual[0]).to.deep.equal(firstResponseData);

            const secondResponseData = _.assign(baseResponseData, { uid: 1456 });

            expect(actual[1]).to.deep.equal(secondResponseData);

            const thirdResponseData = _.assign(baseResponseData, { uid: 1789 });

            expect(actual[2]).to.deep.equal(thirdResponseData);
        });
    });

    describe('`updateUserAndAgency`', () => {
        const now = new Date();
        const user = { id: 2345, uid: 1234567890 };
        const authType = { id: 1, code: 'web' };
        const oldAgency = { id: 2 };
        const oldRole = { id: 3, code: 'old-role' };
        const agency = {
            id: 123,
            login: 'i-pupkin',
            title: 'new-agency',
            manager: 'p.vasya',
            directId: 65432
        };

        beforeEach(function *() {
            yield dbHelper.clear();

            const trial = { id: 3, nullified: 0 };
            const certificate = {
                id: 24,
                dueDate: moment(now).add(5, 'month')
            };

            yield certificatesFactory.createWithRelations(
                certificate,
                { trial, user, agency: oldAgency, role: oldRole, authType }
            );
        });

        it('should update `agencyId` field when agency exists in db', function *() {
            yield agenciesFactory.create(agency);

            const dataForUpdate = {
                agency,
                uid: user.uid,
                role: 'user'
            };

            yield Direct.updateUserAndAgency(dataForUpdate);

            const userData = yield User.findById(user.id);

            expect(userData.agencyId).to.equal(agency.id);
        });

        it('should update `agencyId` field when agency does not exist in db', function *() {
            const newAgency = {
                login: 'new-agency',
                title: 'new-agency',
                manager: 'p.vasya',
                directId: 4321
            };

            const dataForUpdate = {
                agency: newAgency,
                uid: user.uid,
                role: 'user'
            };

            yield Direct.updateUserAndAgency(dataForUpdate);

            const agencyData = yield Agency.findOne({
                where: { login: 'new-agency' }
            });

            const userData = yield User.findById(user.id);

            expect(userData.agencyId).to.equal(agencyData.get('id'));
        });

        it('should set `agencyId` field to null when user does not belong to any agency', function *() {
            const userWithoutAgency = { id: 12, uid: 9876543210 };
            const trial = { id: 9, nullified: 0 };
            const certificate = {
                id: 11,
                dueDate: moment(now).add(6, 'month')
            };

            yield certificatesFactory.createWithRelations(
                certificate,
                { user: userWithoutAgency, trial, agency: oldAgency, authType }
            );

            const dataForUpdate = {
                agency: {},
                uid: userWithoutAgency.uid,
                role: 'user'
            };

            yield Direct.updateUserAndAgency(dataForUpdate);

            const userData = yield User.findById(userWithoutAgency.id);

            expect(userData.agencyId).to.be.null;
        });

        it('should update agency data in db without updating `title` field', function *() {
            const agencyWithOldData = {
                id: 7,
                login: 'agency',
                title: 'Agency',
                manager: 'pupkin',
                directId: 54321
            };

            yield agenciesFactory.create(agencyWithOldData);

            const dataForUpdate = {
                agency: {
                    login: 'agency',
                    title: 'agency',
                    manager: 'ivanov',
                    directId: 54321
                },
                uid: user.uid,
                role: 'user'
            };

            yield Direct.updateUserAndAgency(dataForUpdate);

            const agencyData = yield Agency.findById(agencyWithOldData.id);
            const expectedAgencyData = _.assign(
                dataForUpdate.agency,
                {
                    id: agencyWithOldData.id,
                    title: 'Agency'
                }
            );

            expect(agencyData.toJSON()).to.deep.equal(expectedAgencyData);
        });

        it('should update `roleId` field when role exists in db', function *() {
            const role = { id: 5, code: 'agency' };

            yield rolesFactory.create(role);
            yield agenciesFactory.create(agency);

            const dataForUpdate = {
                agency,
                uid: user.uid,
                role: role.code
            };

            yield Direct.updateUserAndAgency(dataForUpdate);

            const userData = yield User.findById(user.id);

            expect(userData.roleId).to.equal(role.id);
        });

        it('should update `roleId` field when role does not exist in db', function *() {
            yield agenciesFactory.create(agency);

            const dataForUpdate = {
                agency,
                uid: user.uid,
                role: 'user-agent'
            };

            yield Direct.updateUserAndAgency(dataForUpdate);

            const roleData = yield Role.findOne({
                where: { code: 'user-agent' }
            });

            const userData = yield User.findById(user.id);

            expect(userData.roleId).to.equal(roleData.get('id'));
        });

        it('should set user role to default value when `role` field from Direct is undefined', function *() {
            const defaultRole = { id: 2, code: 'user' };

            yield agenciesFactory.create(agency);
            yield rolesFactory.create(defaultRole);

            const dataForUpdate = {
                agency,
                uid: user.uid,
                role: undefined
            };

            yield Direct.updateUserAndAgency(dataForUpdate);

            const userData = yield User.findById(user.id);

            expect(userData.roleId).to.equal(defaultRole.id);
        });
    });

    describe('`syncAgenciesInfo`', () => {
        before(() => {
            nockDirect();
            nockTvmTicket({ 'direct-api-testing': { ticket: 'some_ticket' } }, Infinity);
        });

        after(() => {
            require('nock').cleanAll();
        });

        beforeEach(function *() {
            yield dbHelper.clear();
        });

        const now = new Date();
        const user = { id: 12, uid: 1234567890 };
        const authType = { id: 1, code: 'web' };
        const oldAgency = { id: 2 };
        const oldRole = { id: 4, code: 'old' };

        beforeEach(function *() {
            const trial = { id: 3, nullified: 0 };
            const certificate = {
                id: 24,
                dueDate: moment(now).add(5, 'month')
            };

            yield certificatesFactory.createWithRelations(
                certificate,
                { trial, user, agency: oldAgency, role: oldRole, authType }
            );
        });

        it('should create new agency when it is missing in db', function *() {
            const agenciesBefore = yield Agency.findAll();

            expect(agenciesBefore.length).to.equal(1);

            yield Direct.syncAgenciesInfo();

            const agenciesAfter = yield Agency.findAll();

            expect(agenciesAfter.length).to.equal(2);
        });

        it('should linked user and agency when agency does not exist in db', function *() {
            yield Direct.syncAgenciesInfo();

            const agencyData = yield Agency.findOne({
                where: { login: 'i-pupkin' }
            });

            const userData = yield User.findById(user.id);

            expect(userData.agencyId).to.equal(agencyData.id);
        });

        it('should linked user and agency when agency exists in db', function *() {
            const agency = {
                id: 25,
                login: 'i-pupkin'
            };

            yield agenciesFactory.create(agency);
            yield Direct.syncAgenciesInfo();

            const userData = yield User.findById(user.id);

            expect(userData.agencyId).to.equal(agency.id);
        });

        it('should update `agencyId` and `roleId` for all users who have actual certificates', function *() {
            const otherUser = { id: 23, uid: 1324354637 };
            const otherUserTrial = { id: 5, nullified: 0 };
            const agency = { id: 90, login: 'i-pupkin' };
            const role = { id: 34, code: 'agency' };

            yield agenciesFactory.create(agency);
            yield rolesFactory.create(role);
            yield certificatesFactory.createWithRelations(
                {
                    id: 345,
                    dueDate: moment(now).add(3, 'month')
                },
                { trial: otherUserTrial, user: otherUser, agency: oldAgency, role: oldRole, authType }
            );

            yield Direct.syncAgenciesInfo();

            const users = yield User.findAll();

            expect(users[0].agencyId).to.equal(90);
            expect(users[1].agencyId).to.equal(90);
            expect(users[0].roleId).to.equal(34);
            expect(users[1].roleId).to.equal(34);
        });

        it('should not update `roleId` and set `agencyId` to null when user does not belong to agency', function *() {
            const userWithoutAgency = { id: 34, uid: 9876543210 };
            const trial = { id: 9, nullified: 0 };
            const certificate = {
                id: 10,
                dueDate: moment(now).add(6, 'month')
            };

            yield certificatesFactory.createWithRelations(
                certificate,
                { user: userWithoutAgency, trial, agency: oldAgency, role: oldRole, authType }
            );

            yield Direct.syncAgenciesInfo();

            const userData = yield User.findById(userWithoutAgency.id);

            expect(userData.agencyId).to.be.null;
            expect(userData.roleId).to.equal(oldRole.id);
        });

        it('should not update `agencyId` and `roleId` when user does not have actual certificate', function *() {
            const otherUser = { id: 45, uid: 123456 };
            const trial = { id: 10, nullified: 0 };
            const certificate = {
                id: 2,
                dueDate: moment(now).subtract(6, 'month')
            };

            yield certificatesFactory.createWithRelations(
                certificate,
                { user: otherUser, trial, agency: oldAgency, role: oldRole, authType }
            );

            yield Direct.syncAgenciesInfo();

            const userData = yield User.findById(otherUser.id);

            expect(userData.agencyId).to.equal(oldAgency.id);
            expect(userData.roleId).to.equal(oldRole.id);
        });

        it('should not update `agencyId` and `roleId` when user does not have certificates', function *() {
            const otherUser = { id: 56, uid: 123456 };

            yield usersFactory.createWithRelations(otherUser, { agency: oldAgency, role: oldRole, authType });
            yield Direct.syncAgenciesInfo();

            const userData = yield User.findById(otherUser.id);

            expect(userData.agencyId).to.equal(oldAgency.id);
            expect(userData.roleId).to.equal(oldRole.id);
        });

        it('should not update `agencyId` and `roleId` when certificate belongs to nullified trial', function *() {
            const otherUser = { id: 67, uid: 123456 };
            const trial = { id: 11, nullified: 1 };
            const certificate = {
                id: 45,
                dueDate: moment(now).add(6, 'month')
            };

            yield certificatesFactory.createWithRelations(
                certificate,
                { user: otherUser, trial, agency: oldAgency, role: oldRole, authType }
            );

            yield Direct.syncAgenciesInfo();

            const userData = yield User.findById(otherUser.id);

            expect(userData.agencyId).to.equal(oldAgency.id);
            expect(userData.roleId).to.equal(oldRole.id);
        });

        it('should update agency data in db', function *() {
            const agencyWithOldData = {
                id: 7,
                login: 'i-pupkin',
                title: 'Agency',
                manager: 'pupkin',
                directId: 65432
            };

            yield agenciesFactory.create(agencyWithOldData);

            yield Direct.syncAgenciesInfo();

            const agencyData = yield Agency.findById(7);
            const expectedAgencyData = {
                id: 7,
                login: 'i-pupkin',
                title: 'Agency',
                manager: 'yndx-pupkin',
                directId: 65432
            };

            expect(agencyData.toJSON()).to.deep.equal(expectedAgencyData);
        });

        it('should update data only for `web` users', function *() {
            const otherUser = { id: 78, uid: 1324354637 };
            const otherAuthType = { id: 2, code: 'telegram' };
            const otherUserTrial = { id: 5, nullified: 0 };
            const agency = { id: 90, login: 'i-pupkin' };
            const role = { id: 34, code: 'agency' };

            yield agenciesFactory.create(agency);
            yield rolesFactory.create(role);
            yield certificatesFactory.createWithRelations(
                {
                    id: 345,
                    dueDate: moment(now).add(3, 'month')
                },
                {
                    trial: otherUserTrial,
                    user: otherUser,
                    agency: oldAgency,
                    role: oldRole,
                    authType: otherAuthType
                }
            );

            yield Direct.syncAgenciesInfo();

            const users = yield User.findAll({
                order: [['id']]
            });

            expect(users[0].agencyId).to.equal(90);
            expect(users[1].agencyId).to.equal(oldAgency.id);
            expect(users[0].roleId).to.equal(34);
            expect(users[1].roleId).to.equal(oldRole.id);
        });

        it('should create new record in `direct_sync` and fill `finishTime` when sync complete', function *() {
            const recordsBefore = yield DirectSync.findAll();

            expect(recordsBefore.length).to.equal(0);

            yield Direct.syncAgenciesInfo();

            const recordsAfter = yield DirectSync.findAll();

            expect(recordsAfter.length).to.equal(1);
            expect(recordsAfter[0].get('startTime')).to.not.be.null;
            expect(recordsAfter[0].get('finishTime')).to.not.be.null;
        });

        it('should throw 403 when sync has already begun', function *() {
            const startTime = new Date();
            const availabilityTime = moment(startTime)
                .add(syncDelay)
                .startOf('minute')
                .toISOString();

            yield DirectSync.create({ id: 4, startTime });
            const error = yield catchError(Direct.syncAgenciesInfo.bind(Direct));

            expect(error.statusCode).to.equal(403);
            expect(error.message).to.equal('Sync has already begun');
            expect(error.options).to.deep.equal({ internalCode: '403_SAB', availabilityTime });
        });
    });

    describe('`getLastSyncDate`', () => {
        after(() => {
            require('nock').cleanAll();
        });

        beforeEach(function *() {
            yield dbHelper.clear();
        });

        it('should return undefined when sync is not begin', function *() {
            const lastSyncAgenciesDate = yield Direct.getLastSyncDate();

            expect(lastSyncAgenciesDate).to.be.undefined;
        });

        it('should return undefined when sync is not finished yet', function *() {
            yield directSyncFactory.create();

            const lastSyncAgenciesDate = yield Direct.getLastSyncDate();

            expect(lastSyncAgenciesDate).to.be.undefined;
        });

        it('should return date of finished sync when one sync finished', function *() {
            const startTime = moment().startOf('second');
            const finishTime = startTime.add(30, 'minutes');

            yield directSyncFactory.create({ startTime, finishTime });

            const lastSyncAgenciesDate = yield Direct.getLastSyncDate();

            expect(lastSyncAgenciesDate).to.deep.equal(startTime.toDate());
        });

        it('should return last date of finished sync when sync of several is not finished yet', function *() {
            const firstStartTime = moment().startOf('second');
            const firstFinishTime = firstStartTime.add(30, 'minutes');
            const secondStartTime = firstStartTime.add(50, 'minutes');

            yield directSyncFactory.create({ startTime: firstStartTime, finishTime: firstFinishTime });
            yield directSyncFactory.create({ startTime: secondStartTime });

            const lastSyncAgenciesDate = yield Direct.getLastSyncDate();

            expect(lastSyncAgenciesDate).to.deep.equal(firstStartTime.toDate());
        });

        it('should return last date of finished sync when all syncs finished', function *() {
            const firstStartTime = moment().startOf('second');
            const firstFinishTime = firstStartTime.add(10, 'minutes');
            const lastStartTime = firstStartTime.add(20, 'minutes');
            const lastFinishTime = firstStartTime.add(30, 'minutes');

            yield directSyncFactory.create({ startTime: firstStartTime, finishTime: firstFinishTime });
            yield directSyncFactory.create({ startTime: lastStartTime, finishTime: lastFinishTime });

            const lastSyncAgenciesDate = yield Direct.getLastSyncDate();

            expect(lastSyncAgenciesDate).to.deep.equal(lastStartTime.toDate());
        });
    });
});

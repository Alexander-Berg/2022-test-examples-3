require('co-mocha');

const { expect } = require('chai');
const moment = require('moment');

const catchError = require('tests/helpers/catchError').generator;
const dbHelper = require('tests/helpers/clear');

const adminFactory = require('tests/factory/adminsFactory');
const banFactory = require('tests/factory/bansFactory');
const trialTemplatesFactory = require('tests/factory/trialTemplatesFactory');
const usersFactory = require('tests/factory/usersFactory');

const BanModel = require('models/ban');
const { Ban } = require('db/postgres');

describe('Ban model', () => {
    beforeEach(dbHelper.clear);

    const authType = { id: 2, code: 'web' };
    const admin = {
        id: 4321,
        uid: 92837465,
        login: 'anyok'
    };

    describe('`banUser`', () => {
        it('should create new ban records', function *() {
            const now = Date.now();
            const expiredDate = moment(now)
                .add(1, 'year')
                .startOf('day')
                .toDate();

            yield adminFactory.create(admin);
            yield usersFactory.createWithRelations(
                { id: 1010, login: 'banned-login' },
                { globalUser: { id: 1 }, authType }
            );
            yield trialTemplatesFactory.createWithRelations({ id: 1 });
            yield trialTemplatesFactory.createWithRelations({ id: 2 });

            yield BanModel.banUser({
                globalUserId: 1,
                adminId: 4321,
                trialTemplateIds: [1, 2],
                reason: 'Забанен потому что забанен',
                userLogin: 'banned-login'
            });

            const actual = yield Ban.findAll({
                attributes: [
                    'globalUserId',
                    'adminId',
                    'reason',
                    'startedDate',
                    'expiredDate',
                    'action',
                    'trialTemplateId',
                    'userLogin',
                    'isLast'
                ],
                order: [['trialTemplateId', 'ASC']],
                raw: true
            });

            expect(actual[0].globalUserId).to.equal(1);
            expect(actual[0].adminId).to.equal(4321);
            expect(actual[0].reason).to.equal('Забанен потому что забанен');
            expect(actual[0].startedDate).to.be.above(now);
            expect(actual[0].expiredDate).to.deep.equal(expiredDate);
            expect(actual[0].action).to.equal('ban');
            expect(actual[0].trialTemplateId).to.equal(1);
            expect(actual[0].userLogin).to.equal('banned-login');
            expect(actual[0].isLast).to.be.true;

            expect(actual[1].globalUserId).to.equal(1);
            expect(actual[1].adminId).to.equal(4321);
            expect(actual[1].reason).to.equal('Забанен потому что забанен');
            expect(actual[1].startedDate).to.be.above(now);
            expect(actual[1].expiredDate).to.deep.equal(expiredDate);
            expect(actual[1].action).to.equal('ban');
            expect(actual[1].trialTemplateId).to.equal(2);
            expect(actual[1].userLogin).to.equal('banned-login');
            expect(actual[1].isLast).to.be.true;
        });

        it('should set `isLast` to false for previous ban record for current user', function *() {
            const startedDate = new Date(2017, 1, 2, 3, 4, 5);
            const firstGlobalUser = { id: 1, actualLogin: 'first' };
            const secondGlobalUser = { id: 2, actualLogin: 'second' };
            const firstUser = { id: 10, login: 'first', uid: 123 };
            const secondUser = { id: 20, login: 'second', uid: 456 };

            yield adminFactory.create(admin);
            yield usersFactory.createWithRelations(firstUser, { globalUser: firstGlobalUser, authType });
            yield usersFactory.createWithRelations(secondUser, { globalUser: secondGlobalUser, authType });

            yield banFactory.createWithRelations({
                id: 1,
                reason: 'reason',
                isLast: true,
                startedDate
            }, {
                globalUser: firstGlobalUser,
                trialTemplate: { id: 1 },
                admin
            });
            yield banFactory.createWithRelations({
                id: 2,
                reason: 'reason',
                isLast: true,
                startedDate
            }, {
                globalUser: firstGlobalUser,
                trialTemplate: { id: 2 },
                admin
            });
            yield banFactory.createWithRelations({
                id: 3,
                reason: 'reason',
                isLast: true,
                startedDate
            }, {
                globalUser: secondGlobalUser,
                trialTemplate: { id: 3 },
                admin
            });

            yield BanModel.banUser({
                globalUserId: 1,
                adminId: admin.id,
                reason: 'Снова забанен',
                trialTemplateIds: [1, 3],
                userLogin: 'some'
            });

            const actual = yield Ban.findAll({
                attributes: ['globalUserId', 'trialTemplateId', 'isLast'],
                order: [['globalUserId'], ['trialTemplateId'], ['startedDate']],
                raw: true
            });

            expect(actual).to.have.length(5);

            expect(actual[0].globalUserId).to.equal(1);
            expect(actual[0].isLast).to.be.false;
            expect(actual[0].trialTemplateId).to.equal(1);

            expect(actual[1].globalUserId).to.equal(1);
            expect(actual[1].isLast).to.be.true;
            expect(actual[1].trialTemplateId).to.equal(1);

            expect(actual[2].globalUserId).to.equal(1);
            expect(actual[2].isLast).to.be.true;
            expect(actual[2].trialTemplateId).to.equal(2);

            expect(actual[3].globalUserId).to.equal(1);
            expect(actual[3].isLast).to.be.true;
            expect(actual[3].trialTemplateId).to.equal(3);

            expect(actual[4].globalUserId).to.equal(2);
            expect(actual[4].isLast).to.be.true;
            expect(actual[4].trialTemplateId).to.equal(3);
        });

        it('should not set expiredDate for superban', function *() {
            const now = Date.now();

            yield adminFactory.create(admin);
            yield usersFactory.createWithRelations({ id: 1010 }, { authType, globalUser: { id: 1 } });
            yield trialTemplatesFactory.createWithRelations({ id: 1 });

            yield BanModel.banUser({
                globalUserId: 1,
                adminId: 4321,
                trialTemplateIds: [1],
                reason: 'Superban',
                isSuperban: true,
                userLogin: 'summer'
            });

            const actual = yield Ban.findAll({
                attributes: [
                    'globalUserId',
                    'adminId',
                    'startedDate',
                    'expiredDate',
                    'action',
                    'trialTemplateId',
                    'userLogin',
                    'isLast'
                ],
                order: [['trialTemplateId', 'ASC']],
                raw: true
            });

            expect(actual[0].globalUserId).to.equal(1);
            expect(actual[0].adminId).to.equal(4321);
            expect(actual[0].startedDate).to.be.above(now);
            expect(actual[0].expiredDate).to.be.null;
            expect(actual[0].action).to.equal('ban');
            expect(actual[0].trialTemplateId).to.equal(1);
            expect(actual[0].userLogin).to.equal('summer');
            expect(actual[0].isLast).to.be.true;
        });

        it('should throw 400 if trial template does not exist', function *() {
            yield adminFactory.create(admin);
            yield usersFactory.createWithRelations({ id: 1010 }, { authType, globalUser: { id: 1 } });

            const banData = {
                globalUserId: 1,
                adminId: 4321,
                trialTemplateIds: [1, 2],
                reason: 'Забанен потому что забанен',
                userLogin: 'winter'
            };

            const error = yield catchError(BanModel.banUser.bind(this, banData));

            expect(error.statusCode).to.equal(400);
            expect(error.message).to.equal('Foreign key constraint error');
            expect(error.options).to.deep.equal({
                internalCode: '400_FCE',
                message: 'insert or update on table "bans" violates foreign key' +
                    ' constraint "bans_trial_template_id_fkey"'
            });
        });
    });

    describe('`findLastBan`', () => {
        it('should return null if bans not found', function *() {
            const res = yield BanModel.findLastBan(123, 'direct');

            expect(res).to.be.null;
        });

        it('should return last ban record', function *() {
            const globalUser = { id: 1 };
            const user = { id: 2 };
            const trialTemplate = { id: 10 };
            const otherAdmin = { id: 1234 };
            const expiredDate = new Date();

            yield usersFactory.createWithRelations(user, { authType, globalUser });

            yield banFactory.createWithRelations({
                id: 1,
                isLast: false,
                action: 'ban',
                expiredDate: moment(expiredDate).subtract(1, 'month').toDate()
            }, { globalUser, trialTemplate, admin: otherAdmin });
            yield banFactory.createWithRelations({
                id: 2,
                action: 'ban',
                isLast: true,
                expiredDate
            }, { globalUser, trialTemplate, admin: otherAdmin });

            const res = yield BanModel.findLastBan(1, 10);

            expect(res.expiredDate).to.deep.equal(expiredDate);
        });

        it('should return null if user unbaned', function *() {
            const globalUser = { id: 1 };
            const user = { id: 2 };
            const trialTemplate = { id: 10 };
            const otherAdmin = { id: 1234 };

            yield usersFactory.createWithRelations(user, { authType, globalUser });

            yield banFactory.createWithRelations({
                id: 2,
                action: 'unban',
                isLast: true
            }, { globalUser, trialTemplate, admin: otherAdmin });

            const res = yield BanModel.findLastBan(1, 10);

            expect(res).to.be.null;
        });

        it('should return ban record for requested global user', function *() {
            const trialTemplate = { id: 10 };
            const otherAdmin = { id: 1234 };
            const expiredDate = new Date();

            yield usersFactory.createWithRelations({ id: 20, login: 'other', uid: 123 }, {
                authType,
                globalUser: { id: 2 }
            });
            yield usersFactory.createWithRelations({ id: 2, uid: 456 }, {
                authType,
                globalUser: { id: 1 }
            });

            yield banFactory.createWithRelations({
                id: 1,
                isLast: true,
                action: 'ban',
                expiredDate: moment(expiredDate).add(1, 'month').toDate()
            }, { globalUser: { id: 2 }, trialTemplate, admin: otherAdmin });
            yield banFactory.createWithRelations({
                id: 2,
                action: 'ban',
                isLast: true,
                expiredDate
            }, { globalUser: { id: 1 }, trialTemplate, admin: otherAdmin });

            const res = yield BanModel.findLastBan(1, 10);

            expect(res.expiredDate).to.deep.equal(expiredDate);
        });

        it('should return ban record for requested exam', function *() {
            const globalUser = { id: 1 };
            const user = { id: 2 };
            const trialTemplate = { id: 10 };
            const otherAdmin = { id: 1234 };
            const expiredDate = new Date();

            yield usersFactory.createWithRelations(user, { authType, globalUser });

            yield banFactory.createWithRelations({
                id: 1,
                isLast: true,
                action: 'ban',
                expiredDate
            }, { globalUser, trialTemplate, admin: otherAdmin });
            yield banFactory.createWithRelations({
                id: 2,
                action: 'ban',
                isLast: true,
                expiredDate: moment(expiredDate).add(1, 'month').toDate()
            }, { globalUser, trialTemplate: { id: 1 }, admin: otherAdmin });

            const res = yield BanModel.findLastBan(1, 10);

            expect(res.expiredDate).to.deep.equal(expiredDate);
        });
    });

    describe('`unbanUser`', () => {
        it('should create new unban records', function *() {
            const user = { id: 1, login: 'some-login' };
            const globalUser = { id: 1 };
            const otherAdmin = { id: 12345 };

            yield usersFactory.createWithRelations(user, { authType, globalUser });

            yield banFactory.createWithRelations({
                id: 1,
                reason: 'Some reason',
                isLast: true,
                startedDate: new Date(2017, 0, 1),
                expiredDate: new Date(2018, 0, 1),
                userLogin: user.login
            }, { admin: otherAdmin, trialTemplate: { id: 10 }, globalUser });
            yield banFactory.createWithRelations({
                id: 2,
                reason: 'Some reason',
                isLast: true,
                startedDate: new Date(2017, 0, 1),
                expiredDate: new Date(2018, 0, 1),
                userLogin: user.login
            }, { admin: otherAdmin, trialTemplate: { id: 11 }, globalUser });

            const dataForUnban = {
                globalUserId: 1,
                adminId: 12345,
                reason: 'User is not cheater',
                trialTemplateIds: [10, 11],
                userLogin: 'some-login'
            };

            yield BanModel.unbanUser(dataForUnban);

            const actual = yield Ban.findAll({
                order: [['id']],
                attributes: ['globalUserId', 'isLast', 'action', 'trialTemplateId', 'reason', 'userLogin'],
                raw: true
            });

            const expected = [
                {
                    globalUserId: 1,
                    isLast: false,
                    action: 'ban',
                    reason: 'Some reason',
                    trialTemplateId: 10,
                    userLogin: 'some-login'
                },
                {
                    globalUserId: 1,
                    isLast: false,
                    action: 'ban',
                    reason: 'Some reason',
                    trialTemplateId: 11,
                    userLogin: 'some-login'
                },
                {
                    globalUserId: 1,
                    isLast: true,
                    action: 'unban',
                    reason: 'User is not cheater',
                    trialTemplateId: 10,
                    userLogin: 'some-login'
                },
                {
                    globalUserId: 1,
                    isLast: true,
                    action: 'unban',
                    reason: 'User is not cheater',
                    trialTemplateId: 11,
                    userLogin: 'some-login'
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should set `isLast` to false for previous ban record for exam', function *() {
            const startedDate = new Date(2019, 1, 2);
            const otherAdmin = { id: 1234 };
            const user = { id: 10, login: 'first' };
            const globalUser = { id: 1 };

            yield usersFactory.createWithRelations(user, { globalUser });

            yield banFactory.createWithRelations({
                id: 1,
                reason: 'first reason',
                isLast: true,
                startedDate
            }, { globalUser, trialTemplate: { id: 1 }, admin: otherAdmin });
            yield banFactory.createWithRelations({
                id: 2,
                reason: 'second reason',
                isLast: true,
                startedDate
            }, { globalUser, trialTemplate: { id: 2 }, admin: otherAdmin });

            yield BanModel.unbanUser({
                globalUserId: 1,
                adminId: 1234,
                trialTemplateIds: [1],
                userLogin: 'first'
            });

            const actual = yield Ban.findAll({
                order: [['id']],
                attributes: ['globalUserId', 'isLast', 'action', 'trialTemplateId'],
                raw: true
            });

            const expected = [
                {
                    globalUserId: 1,
                    isLast: false,
                    action: 'ban',
                    trialTemplateId: 1
                },
                {
                    globalUserId: 1,
                    isLast: true,
                    action: 'ban',
                    trialTemplateId: 2
                },
                {
                    globalUserId: 1,
                    isLast: true,
                    action: 'unban',
                    trialTemplateId: 1
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should set `isLast` to false for correct user', function *() {
            const startedDate = new Date(2019, 1, 2);
            const otherAdmin = { id: 1234 };
            const firstUser = { id: 10, login: 'first' };
            const secondUser = { id: 20, login: 'second' };

            yield usersFactory.createWithRelations(firstUser, { globalUser: { id: 1 } });
            yield usersFactory.createWithRelations(secondUser, { globalUser: { id: 2 } });

            yield banFactory.createWithRelations({
                id: 1,
                reason: 'first reason',
                isLast: true,
                startedDate,
                userLogin: firstUser.login
            }, { globalUser: { id: 1 }, trialTemplate: { id: 1 }, admin: otherAdmin });

            yield banFactory.createWithRelations({
                id: 3,
                reason: 'third reason',
                isLast: true,
                startedDate,
                userLogin: secondUser.login
            }, { globalUser: { id: 2 }, trialTemplate: { id: 1 }, admin: otherAdmin });

            yield BanModel.unbanUser({
                globalUserId: 1,
                adminId: 1234,
                trialTemplateIds: [1],
                userLogin: 'first'
            });

            const actual = yield Ban.findAll({
                order: [['id']],
                attributes: ['globalUserId', 'isLast', 'action', 'trialTemplateId', 'userLogin'],
                raw: true
            });

            const expected = [
                {
                    globalUserId: 1,
                    isLast: false,
                    action: 'ban',
                    trialTemplateId: 1,
                    userLogin: 'first'
                },
                {
                    globalUserId: 2,
                    isLast: true,
                    action: 'ban',
                    trialTemplateId: 1,
                    userLogin: 'second'
                },
                {
                    globalUserId: 1,
                    isLast: true,
                    action: 'unban',
                    trialTemplateId: 1,
                    userLogin: 'first'
                }
            ];

            expect(actual).to.deep.equal(expected);
        });

        it('should throw 400 if trial template does not exist', function *() {
            yield adminFactory.create(admin);
            yield usersFactory.createWithRelations({ id: 1010 }, { authType, globalUser: { id: 1 } });

            const unbanData = {
                globalUserId: 1,
                adminId: 4321,
                trialTemplateIds: [1],
                userLogin: 'some'
            };

            const error = yield catchError(BanModel.unbanUser.bind(this, unbanData));

            expect(error.statusCode).to.equal(400);
            expect(error.message).to.equal('Foreign key constraint error');
            expect(error.options).to.deep.equal({
                internalCode: '400_FCE',
                message: 'insert or update on table "bans" violates foreign key' +
                    ' constraint "bans_trial_template_id_fkey"'
            });
        });
    });
});

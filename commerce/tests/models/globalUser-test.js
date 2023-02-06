require('co-mocha');

const { expect } = require('chai');
const moment = require('moment');

const dbHelper = require('tests/helpers/clear');

const GlobalUserModel = require('models/globalUser');

const bansFactory = require('tests/factory/bansFactory');
const globalUsersFactory = require('tests/factory/globalUsersFactory');
const usersFactory = require('tests/factory/usersFactory');
const trialsFactory = require('tests/factory/trialsFactory');
const certificatesFactory = require('tests/factory/certificatesFactory');

const { Ban, GlobalUser, User } = require('db/postgres');

describe('GlobalUser model', () => {
    beforeEach(dbHelper.clear);

    describe('`findById`', () => {
        it('should return null if global user not found', function *() {
            const actual = yield GlobalUserModel.findById({ id: 1 });

            expect(actual).to.be.null;
        });

        it('should return global user with attributes', function *() {
            yield globalUsersFactory.create({ id: 1, actualLogin: 'some' });

            const actual = yield GlobalUserModel.findById({ id: 1, attributes: ['id', 'actualLogin'] });

            expect(actual.toJSON()).to.deep.equal({
                id: 1,
                actualLogin: 'some'
            });
        });
    });

    describe('`getOrCreate`', () => {
        it('should return globalUserId and not create new, if exist', function *() {
            const user = yield usersFactory.createWithRelations(
                { id: 1, login: 'some-user' },
                { globalUser: { id: 10 } }
            );

            const actual = yield GlobalUserModel.getOrCreate(user);

            expect(actual).to.equal(10);

            const storedCount = yield GlobalUser.count();

            expect(storedCount).to.equal(1);
        });

        it('should create global user and return id', function *() {
            const user = yield usersFactory.createWithRelations({
                login: 'some-user',
                globalUserId: null
            }, {});

            const actual = yield GlobalUserModel.getOrCreate(user);

            expect(actual).to.be.a.number;
        });

        it('should set globalUserId to user', function *() {
            const user = yield usersFactory.createWithRelations({
                login: 'some-user',
                globalUserId: null
            }, { role: { id: 1 } });

            const actual = yield GlobalUserModel.getOrCreate(user);
            const storedUsers = yield User.findAll({
                attributes: ['login', 'globalUserId'],
                raw: true
            });

            expect(storedUsers).to.deep.equal([
                { login: 'some-user', globalUserId: actual }
            ]);
        });
    });

    describe('`chooseActualLogin`', () => {
        it('should return login by first pro trial when trials associated with one pro exam', function *() {
            const proTrialTemplate = { id: 13, isProctoring: true };

            yield trialsFactory.createWithRelations(
                { id: 1, started: new Date(1, 1, 1) },
                { user: { id: 1, login: 'doto' }, trialTemplate: proTrialTemplate }
            );

            yield certificatesFactory.createWithRelations(
                { id: 2, active: 1 },
                {
                    trial: { id: 2, started: new Date(2, 2, 2) },
                    user: { id: 2, login: 'koto' },
                    trialTemplate: proTrialTemplate
                }
            );

            const actual = yield GlobalUserModel.chooseActualLogin(['doto', 'koto']);

            expect(actual).to.equal('doto');
        });

        it('should filter pro trials by logins', function *() {
            const proTrialTemplate = { id: 13, isProctoring: true };

            yield trialsFactory.createWithRelations(
                { id: 1, started: new Date(1, 1, 1) },
                { user: { id: 1, login: 'doto' }, trialTemplate: proTrialTemplate }
            );
            yield trialsFactory.createWithRelations(
                { id: 2, started: new Date(2, 2, 2) },
                { user: { id: 2, login: 'koto' }, trialTemplate: proTrialTemplate }
            );

            const actual = yield GlobalUserModel.chooseActualLogin(['koto', 'midyac']);

            expect(actual).to.equal('koto');
        });

        it('should return login by first pro trial when trials associated with different pro exams', function *() {
            yield trialsFactory.createWithRelations(
                { id: 1, started: new Date(1, 1, 1) },
                { user: { id: 1, login: 'doto' }, trialTemplate: { id: 13, isProctoring: true } }
            );
            yield trialsFactory.createWithRelations(
                { id: 2, started: new Date(2, 2, 2) },
                { user: { id: 2, login: 'koto' }, trialTemplate: { id: 14, isProctoring: true } }
            );

            const actual = yield GlobalUserModel.chooseActualLogin(['doto', 'koto']);

            expect(actual).to.equal('doto');
        });

        it('should choose correct login when there are pro trial and not pro cert', function *() {
            yield trialsFactory.createWithRelations(
                { id: 1, started: new Date(3, 3, 3) },
                { user: { id: 1, login: 'doto' }, trialTemplate: { id: 13, isProctoring: true } }
            );
            yield certificatesFactory.createWithRelations(
                { id: 2, active: 1 },
                {
                    trial: { id: 2, started: new Date(2, 2, 2) },
                    user: { id: 2, login: 'koto' },
                    trialTemplate: { id: 14, isProctoring: false }
                }
            );

            const actual = yield GlobalUserModel.chooseActualLogin(['doto', 'koto']);

            expect(actual).to.equal('doto');
        });

        it('should choose correct login when there are no pro trial and no cert', function *() {
            const trialTemplate = { id: 14, isProctoring: false };

            yield trialsFactory.createWithRelations(
                { id: 1, started: new Date(3, 3, 3) },
                { user: { id: 1, login: 'doto' }, trialTemplate }
            );
            yield trialsFactory.createWithRelations(
                { id: 2, started: new Date(4, 4, 4) },
                { user: { id: 2, login: 'koto' }, trialTemplate }
            );

            const actual = yield GlobalUserModel.chooseActualLogin(['doto', 'koto']);

            expect(actual).to.equal('koto');
        });

        it('should filter not pro trials by logins', function *() {
            const trialTemplate = { id: 14, isProctoring: false };

            yield trialsFactory.createWithRelations(
                { id: 1, started: new Date(1, 1, 1) },
                { user: { id: 1, login: 'doto' }, trialTemplate }
            );
            yield certificatesFactory.createWithRelations(
                { id: 2, active: 1 },
                {
                    trial: { id: 2, started: new Date(2, 2, 2) },
                    user: { id: 2, login: 'koto' },
                    trialTemplate
                }
            );

            const actual = yield GlobalUserModel.chooseActualLogin(['doto', 'midyac']);

            expect(actual).to.equal('doto');
        });

        it('should choose correct login when there are no pro trial and one cert', function *() {
            yield trialsFactory.createWithRelations(
                { id: 1, started: new Date(3, 3, 3) },
                {
                    user: { id: 1, login: 'doto' },
                    trialTemplate: { id: 13, isProctoring: false }
                }
            );
            yield certificatesFactory.createWithRelations(
                { id: 2, active: 1 },
                {
                    trial: { id: 2, started: new Date(2, 2, 2) },
                    user: { id: 2, login: 'koto' },
                    trialTemplate: { id: 14, isProctoring: false }
                }
            );

            const actual = yield GlobalUserModel.chooseActualLogin(['doto', 'koto']);

            expect(actual).to.equal('koto');
        });

        it('should choose correct login when there are several not pro trials', function *() {
            const firstUser = { id: 1, login: 'doto' };
            const secondUser = { id: 2, login: 'koto' };
            const trialTemplate = { id: 13, isProctoring: false };

            yield certificatesFactory.createWithRelations(
                { id: 1, active: 1 },
                {
                    trial: { id: 1, started: new Date(2, 2, 2) },
                    user: firstUser,
                    trialTemplate
                }
            );
            yield certificatesFactory.createWithRelations(
                { id: 2, active: 1 },
                {
                    trial: { id: 2, started: new Date(3, 3, 3) },
                    user: firstUser,
                    trialTemplate
                }
            );
            yield trialsFactory.createWithRelations(
                { id: 3, started: new Date(4, 4, 4) },
                { user: firstUser, trialTemplate }
            );

            yield certificatesFactory.createWithRelations(
                { id: 4, active: 1 },
                {
                    trial: { id: 4, started: new Date(1, 1, 1) },
                    user: secondUser,
                    trialTemplate
                }
            );
            yield trialsFactory.createWithRelations(
                { id: 5, started: new Date(5, 5, 5) },
                { user: secondUser, trialTemplate }
            );

            const actual = yield GlobalUserModel.chooseActualLogin(['doto', 'koto']);

            expect(actual).to.equal('doto');
        });

        it('should return first passed login when users do not have trials', function *() {
            yield usersFactory.createWithRelations({ id: 1, login: 'midyac' });
            yield usersFactory.createWithRelations({ id: 2, login: 'Michael.dyko' });

            const actual = yield GlobalUserModel.chooseActualLogin(['midyac', 'Michael.dyko']);

            expect(actual).to.equal('midyac');
        });
    });

    describe('`associateUsers`', () => {
        const role = { id: 1 };
        const authType = { id: 2, code: 'web' };

        it('should create new global user if users has no global ids', function *() {
            const firstUser = yield usersFactory.createWithRelations({
                id: 1,
                login: 'user-1',
                globalUserId: null
            }, { role });
            const secondUser = yield usersFactory.createWithRelations({
                id: 2,
                login: 'user-2',
                globalUserId: null
            }, { role });

            yield GlobalUserModel.associateUsers([firstUser, secondUser]);

            const actualGlobal = yield GlobalUser.findAll({
                attributes: ['actualLogin', 'isBanned', 'isActive'],
                raw: true
            });

            expect(actualGlobal).to.deep.equal([{
                actualLogin: 'user-1',
                isBanned: false,
                isActive: true
            }]);
        });

        it('should set global user id if users has no global ids', function *() {
            const firstUser = yield usersFactory.createWithRelations({
                id: 1,
                login: 'user-1',
                globalUserId: null
            }, { role });
            const secondUser = yield usersFactory.createWithRelations({
                id: 2,
                login: 'user-2',
                globalUserId: null
            }, { role });

            yield GlobalUserModel.associateUsers([firstUser, secondUser]);

            const actualGlobal = yield GlobalUser.findOne({ attributes: ['id'], raw: true });

            const actual = yield User.findAll({
                attributes: ['globalUserId'],
                raw: true
            });

            expect(actual).to.deep.equal([
                { globalUserId: actualGlobal.id },
                { globalUserId: actualGlobal.id }
            ]);
        });

        it('should not change global user if users has one globalUserId', function *() {
            const globalUser = {
                id: 10,
                actualLogin: 'user-1',
                isBanned: false,
                isActive: true
            };
            const firstUser = yield usersFactory.createWithRelations(
                { id: 1, login: 'user-1' },
                { globalUser, role }
            );
            const secondUser = yield usersFactory.createWithRelations(
                { id: 2, login: 'user-2' },
                { globalUser, role }
            );
            const thirdUser = yield usersFactory.createWithRelations({ id: 3, login: 'user-3' }, { role });

            yield GlobalUserModel.associateUsers([firstUser, secondUser, thirdUser]);

            const actual = yield GlobalUser.findAll({
                attributes: ['actualLogin', 'isBanned', 'isActive'],
                raw: true
            });

            expect(actual).to.deep.equal([{
                actualLogin: 'user-1',
                isBanned: false,
                isActive: true
            }]);
        });

        it('should set global user if users has one globalUserId', function *() {
            const globalUser = {
                id: 10,
                actualLogin: 'user-1',
                isBanned: false,
                isActive: true
            };
            const firstUser = yield usersFactory.createWithRelations(
                { id: 1, login: 'user-1' },
                { globalUser, role }
            );
            const secondUser = yield usersFactory.createWithRelations(
                { id: 2, login: 'user-2' },
                { globalUser, role }
            );
            const thirdUser = yield usersFactory.createWithRelations(
                { id: 3, login: 'user-3', globalUserId: null },
                { role }
            );

            yield GlobalUserModel.associateUsers([firstUser, secondUser, thirdUser]);

            const actual = yield User.findAll({
                attributes: ['globalUserId'],
                raw: true
            });

            expect(actual).to.deep.equal([
                { globalUserId: 10 },
                { globalUserId: 10 },
                { globalUserId: 10 }
            ]);
        });

        it('should merge global users if user has different globalUserId', function *() {
            yield usersFactory.createWithRelations(
                { id: 1, uid: 10, login: 'user-1' },
                {
                    globalUser: {
                        id: 1,
                        actualLogin: 'user-1',
                        isBanned: false,
                        isActive: true
                    },
                    role
                }
            );
            yield usersFactory.createWithRelations(
                { id: 2, uid: 20, login: 'user-2' },
                {
                    globalUser: {
                        id: 2,
                        actualLogin: 'user-2',
                        isBanned: false,
                        isActive: true
                    },
                    role
                }
            );
            const users = yield User.findAll({
                attributes: ['id', 'login', 'globalUserId'],
                raw: true
            });

            yield GlobalUserModel.associateUsers(users);

            const actual = yield GlobalUser.findAll({
                attributes: ['id', 'isActive'],
                raw: true
            });

            expect(actual).to.deep.equal([
                { id: 1, isActive: true },
                { id: 2, isActive: false }
            ]);
        });

        it('should set global user id if users has different globalUserId', function *() {
            yield usersFactory.createWithRelations(
                { id: 1, uid: 10, login: 'user-1' },
                {
                    globalUser: {
                        id: 1,
                        actualLogin: 'user-1',
                        isBanned: false,
                        isActive: true
                    },
                    role
                }
            );
            yield usersFactory.createWithRelations(
                { id: 2, uid: 20, login: 'user-2' },
                {
                    globalUser: {
                        id: 2,
                        actualLogin: 'user-2',
                        isBanned: false,
                        isActive: true
                    },
                    role
                }
            );
            const users = yield User.findAll({
                attributes: ['id', 'login', 'globalUserId'],
                raw: true
            });

            yield GlobalUserModel.associateUsers(users);

            const actual = yield User.findAll({
                attributes: ['globalUserId'],
                raw: true
            });

            expect(actual).to.deep.equal([
                { globalUserId: 1 },
                { globalUserId: 1 }
            ]);
        });

        it('should copy superban', function *() {
            const firstUser = { id: 11, uid: 10, login: 'user-1', globalUserId: 1 };
            const secondUser = { id: 22, uid: 20, login: 'user-2', globalUserId: 2 };

            yield usersFactory.createWithRelations(firstUser, {
                globalUser: { id: 1, isBanned: false, isActive: true },
                role,
                authType
            });
            yield usersFactory.createWithRelations(secondUser, {
                globalUser: { id: 2, isBanned: true, isActive: true },
                role,
                authType
            });

            yield GlobalUserModel.associateUsers([firstUser, secondUser]);

            const actual = yield GlobalUser.findAll({
                attributes: ['id', 'isActive', 'isBanned'],
                order: [['id']],
                raw: true
            });

            expect(actual).to.deep.equal([
                { id: 1, isActive: true, isBanned: true },
                { id: 2, isActive: false, isBanned: true }
            ]);
        });

        it('should correctly set `isLast` for superban when copying', function *() {

            /*
                The order of bans before copying:
                - 2 global user + superban (both exam)
                - 1 global user + 2 exam

                ActualGlobalUser - 1
            */
            const firstUser = { id: 11, uid: 10, login: 'user-1', globalUserId: 1 };
            const secondUser = { id: 22, uid: 20, login: 'user-2', globalUserId: 2 };
            const firstTrialTemplate = { id: 1 };
            const secondTrialTemplate = { id: 2 };

            yield usersFactory.createWithRelations(firstUser, {
                globalUser: { id: 1, isBanned: false, isActive: true },
                role,
                authType
            });
            yield usersFactory.createWithRelations(secondUser, {
                globalUser: { id: 2, isBanned: true, isActive: true },
                role,
                authType
            });

            const startedSuperBan = moment().subtract(1, 'year').startOf('day').toDate();
            const startedSimpleBan = moment().subtract(3, 'month').startOf('day').toDate();
            const expiredSimpleBan = moment().add(1, 'year').startOf('day').toDate();

            yield bansFactory.createWithRelations(
                {
                    id: 1,
                    action: 'ban',
                    isLast: true,
                    startedDate: startedSuperBan,
                    expiredDate: null
                },
                {
                    trialTemplate: firstTrialTemplate,
                    globalUser: { id: 2 },
                    admin: { id: 123 }
                }
            );
            yield bansFactory.createWithRelations(
                {
                    id: 2,
                    action: 'ban',
                    isLast: true,
                    startedDate: startedSuperBan,
                    expiredDate: null
                },
                {
                    trialTemplate: secondTrialTemplate,
                    globalUser: { id: 2 },
                    admin: { id: 123 }
                }
            );
            yield bansFactory.createWithRelations(
                {
                    id: 3,
                    action: 'ban',
                    isLast: true,
                    startedDate: startedSimpleBan,
                    expiredDate: expiredSimpleBan
                },
                {
                    trialTemplate: secondTrialTemplate,
                    globalUser: { id: 1 },
                    admin: { id: 123 }
                }
            );

            yield GlobalUserModel.associateUsers([firstUser, secondUser]);

            const actual = yield Ban.findAll({
                attributes: [
                    'globalUserId',
                    'startedDate',
                    'expiredDate',
                    'trialTemplateId',
                    'isLast'
                ],
                order: [['globalUserId'], ['startedDate'], ['trialTemplateId']],
                raw: true
            });

            expect(actual).to.deep.equal([
                {
                    globalUserId: 1,
                    startedDate: startedSuperBan,
                    expiredDate: null,
                    trialTemplateId: 1,
                    isLast: true
                },
                {
                    globalUserId: 1,
                    startedDate: startedSuperBan,
                    expiredDate: null,
                    trialTemplateId: 2,
                    isLast: true
                },
                {
                    globalUserId: 1,
                    startedDate: startedSimpleBan,
                    expiredDate: expiredSimpleBan,
                    trialTemplateId: 2,
                    isLast: false
                },
                {
                    globalUserId: 2,
                    startedDate: startedSuperBan,
                    expiredDate: null,
                    trialTemplateId: 1,
                    isLast: true
                },
                {
                    globalUserId: 2,
                    startedDate: startedSuperBan,
                    expiredDate: null,
                    trialTemplateId: 2,
                    isLast: true
                }
            ]);
        });

        it('should set `isLast = false` only for active global user', function *() {
            const firstUser = { id: 11, uid: 10, login: 'user-1', globalUserId: 1 };
            const secondUser = { id: 22, uid: 20, login: 'user-2', globalUserId: 2 };
            const trialTemplate = { id: 1 };
            const admin = { id: 123 };

            yield usersFactory.createWithRelations(firstUser, {
                globalUser: { id: 1, isBanned: false, isActive: true },
                role,
                authType
            });
            yield usersFactory.createWithRelations(secondUser, {
                globalUser: { id: 2, isBanned: false, isActive: true },
                role,
                authType
            });

            yield bansFactory.createWithRelations(
                {
                    id: 1,
                    action: 'ban',
                    isLast: true,
                    startedDate: moment().subtract(1, 'year').toDate(),
                    userLogin: firstUser.login
                },
                {
                    trialTemplate,
                    globalUser: { id: 1 },
                    admin
                }
            );
            yield bansFactory.createWithRelations(
                {
                    id: 2,
                    action: 'ban',
                    isLast: true,
                    startedDate: new Date(),
                    userLogin: secondUser.login
                },
                {
                    trialTemplate,
                    globalUser: { id: 2 },
                    admin
                }
            );

            yield GlobalUserModel.associateUsers([firstUser, secondUser]);

            const actual = yield Ban.findAll({
                attributes: [
                    'globalUserId',
                    'userLogin',
                    'isLast'
                ],
                order: [['globalUserId'], ['startedDate']],
                raw: true
            });

            expect(actual).to.deep.equal([
                {
                    globalUserId: 1,
                    userLogin: 'user-1',
                    isLast: false
                },
                {
                    globalUserId: 1,
                    userLogin: 'user-2',
                    isLast: true
                },
                {
                    globalUserId: 2,
                    userLogin: 'user-2',
                    isLast: true
                }
            ]);
        });

        it('should merge superbans correctly', function *() {
            const firstUser = { id: 11, uid: 10, login: 'user-1', globalUserId: 1 };
            const secondUser = { id: 22, uid: 20, login: 'user-2', globalUserId: 2 };
            const trialTemplate = { id: 1 };
            const admin = { id: 123 };

            yield usersFactory.createWithRelations(firstUser, {
                globalUser: { id: 1, isBanned: true, isActive: true },
                role,
                authType
            });
            yield usersFactory.createWithRelations(secondUser, {
                globalUser: { id: 2, isBanned: true, isActive: true },
                role,
                authType
            });

            const firstSuperStarted = moment().subtract(1, 'year').startOf('day').toDate();
            const secondSuperStarted = moment().startOf('day').toDate();

            yield bansFactory.createWithRelations(
                {
                    id: 1,
                    action: 'ban',
                    isLast: true,
                    startedDate: firstSuperStarted,
                    expiredDate: null,
                    userLogin: firstUser.login
                },
                {
                    trialTemplate,
                    globalUser: { id: 1 },
                    admin
                }
            );
            yield bansFactory.createWithRelations(
                {
                    id: 2,
                    action: 'ban',
                    isLast: true,
                    startedDate: secondSuperStarted,
                    expiredDate: null,
                    userLogin: secondUser.login
                },
                {
                    trialTemplate,
                    globalUser: { id: 2 },
                    admin
                }
            );

            yield GlobalUserModel.associateUsers([firstUser, secondUser]);

            const actualBans = yield Ban.findAll({
                attributes: [
                    'globalUserId',
                    'startedDate',
                    'userLogin',
                    'isLast'
                ],
                order: [['globalUserId'], ['startedDate']],
                raw: true
            });

            expect(actualBans).to.deep.equal([
                {
                    globalUserId: 1,
                    startedDate: firstSuperStarted,
                    userLogin: 'user-1',
                    isLast: false
                },
                {
                    globalUserId: 1,
                    startedDate: secondSuperStarted,
                    userLogin: 'user-2',
                    isLast: true
                },
                {
                    globalUserId: 2,
                    startedDate: secondSuperStarted,
                    userLogin: 'user-2',
                    isLast: true
                }
            ]);
        });

        it('should copy all bans to empty global user', function *() {
            const firstUser = { id: 11, uid: 10, login: 'user-1', globalUserId: 1 };
            const secondUser = { id: 22, uid: 20, login: 'user-2', globalUserId: 2 };
            const admin = { id: 123 };

            yield usersFactory.createWithRelations(firstUser, {
                globalUser: { id: 1, isBanned: false, isActive: true },
                role,
                authType
            });
            yield usersFactory.createWithRelations(secondUser, {
                globalUser: { id: 2, isBanned: false, isActive: true },
                role,
                authType
            });

            yield bansFactory.createWithRelations(
                {
                    id: 1,
                    action: 'ban',
                    isLast: true,
                    startedDate: new Date(1, 1, 1),
                    reason: 'I am tired'
                },
                {
                    trialTemplate: { id: 1 },
                    globalUser: { id: 2 },
                    admin
                }
            );
            yield bansFactory.createWithRelations(
                {
                    id: 2,
                    action: 'ban',
                    isLast: true,
                    startedDate: new Date(2, 2, 2),
                    reason: 'it is morning'
                },
                {
                    trialTemplate: { id: 2 },
                    globalUser: { id: 2 },
                    admin
                }
            );

            yield GlobalUserModel.associateUsers([firstUser, secondUser]);

            const actualBans = yield Ban.findAll({
                attributes: [
                    'globalUserId',
                    'trialTemplateId',
                    'reason',
                    'isLast'
                ],
                order: [['globalUserId'], ['startedDate']],
                raw: true
            });

            expect(actualBans).to.deep.equal([
                {
                    globalUserId: 1,
                    trialTemplateId: 1,
                    reason: 'I am tired',
                    isLast: true
                },
                {
                    globalUserId: 1,
                    trialTemplateId: 2,
                    reason: 'it is morning',
                    isLast: true
                },
                {
                    globalUserId: 2,
                    trialTemplateId: 1,
                    reason: 'I am tired',
                    isLast: true
                },
                {
                    globalUserId: 2,
                    trialTemplateId: 2,
                    reason: 'it is morning',
                    isLast: true
                }
            ]);
        });

        it('should set `isLast` correctly when there is unban for superban', function *() {
            const firstUser = { id: 11, uid: 10, login: 'user-1', globalUserId: 1 };
            const secondUser = { id: 22, uid: 20, login: 'user-2', globalUserId: 2 };
            const trialTemplate = { id: 1 };
            const admin = { id: 123 };

            yield usersFactory.createWithRelations(firstUser, {
                globalUser: { id: 1, isBanned: false, isActive: true },
                role,
                authType
            });
            yield usersFactory.createWithRelations(secondUser, {
                globalUser: { id: 2, isBanned: false, isActive: true },
                role,
                authType
            });

            yield bansFactory.createWithRelations(
                {
                    id: 1,
                    action: 'ban',
                    isLast: false,
                    startedDate: new Date(1, 1, 1),
                    expiredDate: null,
                    userLogin: secondUser.login
                },
                {
                    trialTemplate,
                    globalUser: { id: 2 },
                    admin
                }
            );
            yield bansFactory.createWithRelations(
                {
                    id: 2,
                    action: 'unban',
                    isLast: true,
                    startedDate: new Date(2, 2, 2),
                    expiredDate: null,
                    userLogin: secondUser.login
                },
                {
                    trialTemplate,
                    globalUser: { id: 2 },
                    admin
                }
            );
            yield bansFactory.createWithRelations(
                {
                    id: 3,
                    action: 'ban',
                    isLast: true,
                    startedDate: new Date(3, 3, 3),
                    expiredDate: new Date(3, 3, 4),
                    userLogin: firstUser.login
                },
                {
                    trialTemplate,
                    globalUser: { id: 1 },
                    admin
                }
            );

            yield GlobalUserModel.associateUsers([firstUser, secondUser]);

            const actualBans = yield Ban.findAll({
                attributes: [
                    'globalUserId',
                    'action',
                    'startedDate',
                    'isLast'
                ],
                order: [['globalUserId'], ['startedDate']],
                raw: true
            });

            expect(actualBans).to.deep.equal([
                {
                    globalUserId: 1,
                    action: 'ban',
                    startedDate: new Date(1, 1, 1),
                    isLast: false
                },
                {
                    globalUserId: 1,
                    action: 'unban',
                    startedDate: new Date(2, 2, 2),
                    isLast: false
                },
                {
                    globalUserId: 1,
                    action: 'ban',
                    startedDate: new Date(3, 3, 3),
                    isLast: true
                },
                {
                    globalUserId: 2,
                    action: 'ban',
                    startedDate: new Date(1, 1, 1),
                    isLast: false
                },
                {
                    globalUserId: 2,
                    action: 'unban',
                    startedDate: new Date(2, 2, 2),
                    isLast: true
                }
            ]);
        });

        it('should set global user id for all associated users', function *() {
            const firstGlobalUser = {
                id: 1,
                actualLogin: 'user-1',
                isBanned: false,
                isActive: true
            };
            const secondGlobalUser = {
                id: 2,
                actualLogin: 'user-2',
                isBanned: false,
                isActive: true
            };

            yield usersFactory.createWithRelations(
                { id: 1, uid: 10, login: 'user-1' },
                {
                    globalUser: firstGlobalUser,
                    role,
                    authType
                }
            );
            yield usersFactory.createWithRelations(
                { id: 2, uid: 20, login: 'user-2' },
                {
                    globalUser: secondGlobalUser,
                    role,
                    authType
                }
            );
            yield usersFactory.createWithRelations(
                { id: 3, uid: 30, login: 'user-3' },
                {
                    globalUser: secondGlobalUser,
                    role,
                    authType
                }
            );

            yield GlobalUserModel.associateUsers([
                { id: 1, login: 'user-1', globalUserId: 1 },
                { id: 2, login: 'user-2', globalUserId: 2 }
            ]);

            const actual = yield User.findAll({
                attributes: ['globalUserId'],
                raw: true
            });

            expect(actual.map(user => user.globalUserId)).to.deep.equal([1, 1, 1]);
        });
    });

    describe('`getBansInfo`', () => {
        it('should return global user by id', function *() {
            yield globalUsersFactory.create({
                id: 1,
                actualLogin: 'user-1',
                isBanned: false,
                isActive: true
            });
            yield globalUsersFactory.create({
                id: 2,
                actualLogin: 'user-2',
                isBanned: true,
                isActive: true
            });

            const actual = yield GlobalUserModel.getBansInfo(1);

            expect(actual.toJSON()).to.deep.equal({
                actualLogin: 'user-1',
                isBanned: false,
                bans: []
            });
        });

        it('should return user bans', function *() {
            const startedDate = new Date(2017, 3, 1);
            const otherStartedDate = moment(startedDate).add(1, 'hour').toDate();
            const unbanDate = moment(startedDate).add(2, 'hour').toDate();
            const expiredDate = moment(startedDate).add(1, 'month').toDate();
            const firstAdmin = { id: 1, login: 'anyok' };
            const secondAdmin = { id: 2, login: 'dotokoto' };
            const firstGlobalUser = {
                id: 1,
                actualLogin: 'user-1',
                isBanned: false,
                isActive: true
            };

            yield bansFactory.createWithRelations({
                action: 'ban',
                reason: 'какая-то причина',
                startedDate,
                expiredDate,
                userLogin: 'first-exam-ban',
                isLast: true
            }, { globalUser: firstGlobalUser, trialTemplate: { id: 1 }, admin: firstAdmin });
            yield bansFactory.createWithRelations({
                action: 'ban',
                reason: 'другая причина',
                startedDate: otherStartedDate,
                expiredDate,
                userLogin: 'second-exam-ban',
                isLast: false
            }, { globalUser: firstGlobalUser, trialTemplate: { id: 2 }, admin: secondAdmin });
            yield bansFactory.createWithRelations({
                action: 'unban',
                reason: 'причина разбана',
                startedDate: unbanDate,
                userLogin: 'second-exam-unban',
                isLast: true
            }, { globalUser: firstGlobalUser, trialTemplate: { id: 2 }, admin: secondAdmin });
            yield bansFactory.createWithRelations({
                action: 'ban',
                reason: 'совсем другая причина',
                startedDate,
                expiredDate,
                userLogin: 'first-exam-ban',
                isLast: true
            }, { globalUser: { id: 2 }, trialTemplate: { id: 1 }, admin: firstAdmin });

            const actual = yield GlobalUserModel.getBansInfo(1);

            expect(actual.toJSON()).to.deep.equal({
                actualLogin: 'user-1',
                isBanned: false,
                bans: [
                    {
                        reason: 'причина разбана',
                        startedDate: unbanDate,
                        expiredDate: null,
                        admin: { login: 'dotokoto' },
                        trialTemplateId: 2,
                        userLogin: 'second-exam-unban',
                        action: 'unban',
                        isLast: true
                    },
                    {
                        reason: 'другая причина',
                        startedDate: otherStartedDate,
                        expiredDate,
                        admin: { login: 'dotokoto' },
                        trialTemplateId: 2,
                        userLogin: 'second-exam-ban',
                        action: 'ban',
                        isLast: false
                    },
                    {
                        reason: 'какая-то причина',
                        startedDate,
                        expiredDate,
                        admin: { login: 'anyok' },
                        trialTemplateId: 1,
                        userLogin: 'first-exam-ban',
                        action: 'ban',
                        isLast: true
                    }
                ]
            });
        });

        it('should return null if global user not active', function *() {
            yield globalUsersFactory.create({
                id: 1,
                actualLogin: 'user-1',
                isBanned: false,
                isActive: false
            });

            const actual = yield GlobalUserModel.getBansInfo(1);

            expect(actual).to.be.null;
        });

        it('should return null if global user not found', function *() {
            const actual = yield GlobalUserModel.getBansInfo(1);

            expect(actual).to.be.null;
        });
    });

    describe('`updateSuperban`', () => {
        it('should ban user by id', function *() {
            yield globalUsersFactory.create({
                id: 1,
                actualLogin: 'user-1',
                isBanned: false
            });
            yield globalUsersFactory.create({
                id: 2,
                actualLogin: 'user-2',
                isBanned: false
            });

            yield GlobalUserModel.updateSuperban({ globalUserId: 1, isBanned: true });

            const actual = yield GlobalUser.findAll({
                attributes: ['isBanned'],
                raw: true,
                order: [['id']]
            });

            expect(actual).to.deep.equal([
                { isBanned: true },
                { isBanned: false }
            ]);
        });

        it('should do nothing if user not found', function *() {
            yield GlobalUserModel.updateSuperban({ globalUserId: 1, isBanned: true });

            const actual = yield GlobalUser.findAll();

            expect(actual).to.be.empty;
        });
    });
});
